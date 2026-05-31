/**
 * Генерує route_*.json для всіх stops_*_route.geojson.
 * Маршрут 114 залишає існуючий файл (калібрування EasyWay).
 * Інші — інтервали за OSM-парами (1–2 хв) + масштабування до типової тривалості рейсу.
 *
 * node scripts/generate-all-schedules.js
 * node scripts/apply-easyway-schedule.js --all
 */
const fs = require("fs");
const path = require("path");

const assetsRoot = path.join(__dirname, "..", "..", "app", "src", "main", "assets", "map");
const schedulesDir = path.join(assetsRoot, "schedules");

/** Перший рейс (outbound ≈ Троєщина/кінець А → вокзал/кінець Б), inbound ≈ зворотно */
const ROUTE_TRIP_STARTS = {
  "7": { outbound: "06:20", inbound: "06:15" },
  "11": { outbound: "05:45", inbound: "05:40" },
  "18": { outbound: "06:05", inbound: "06:00" },
  "24": { outbound: "06:10", inbound: "06:05" },
  "50": { outbound: "05:55", inbound: "05:50" },
  "55": { outbound: "06:08", inbound: "06:03" },
  "62": { outbound: "06:15", inbound: "06:10" },
  "101": { outbound: "06:00", inbound: "05:55" },
  "114": { outbound: "07:05", inbound: "07:00" },
  "115": { outbound: "06:12", inbound: "06:07" }
};

const EASYWAY_PAIR_PATTERN = [
  1, 2, 1, 2, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 2, 2, 2
];

function getName(props) {
  return String(props["name:uk"] || props.name || "").trim();
}

function parseMinutes(hhmm) {
  const m = String(hhmm || "").trim().match(/^(\d{1,2}):(\d{2})$/);
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  if (h < 0 || h > 23 || min < 0 || min > 59) return null;
  return h * 60 + min;
}

function loadPointFeatures(geoPath) {
  const fc = JSON.parse(fs.readFileSync(geoPath, "utf8"));
  const features = fc.features || [];
  const pointIdx = [];
  for (let i = 0; i < features.length; i++) {
    if (features[i]?.geometry?.type === "Point") pointIdx.push(i);
  }
  return { fc, features, pointIdx };
}

function deriveOutboundIntervals(features, pointIdx) {
  const evenIdx = pointIdx.filter((_, i) => i % 2 === 0);
  const n = evenIdx.length;
  if (n <= 1) return [];

  const raw = [];
  for (let k = 0; k < n - 1; k++) {
    const i = evenIdx[k];
    const j = evenIdx[k + 1];
    const pairedAtK =
      i + 1 < pointIdx.length && getName(features[i].properties) === getName(features[i + 1].properties);

    if (pairedAtK) {
      raw.push(EASYWAY_PAIR_PATTERN[k % EASYWAY_PAIR_PATTERN.length] ?? 2);
    } else {
      const t1 = parseMinutes(features[i].properties?.planned_time);
      const t2 = parseMinutes(features[j].properties?.planned_time);
      let iv = 3;
      if (t1 != null && t2 != null) {
        const diff = t2 - t1;
        if (diff > 0 && diff <= 12) iv = diff;
      }
      raw.push(Math.min(5, Math.max(2, iv)));
    }
  }

  const target = Math.min(75, Math.max(18, Math.round(n * 1.65)));
  const sum = raw.reduce((a, b) => a + b, 0);
  if (sum <= 0) return raw.map(() => 2);

  const scaled = raw.map((v) => Math.max(1, Math.round((v * target) / sum)));
  let fix = target - scaled.reduce((a, b) => a + b, 0);
  let p = 0;
  while (fix !== 0 && scaled.length) {
    const idx = p % scaled.length;
    if (fix > 0) {
      scaled[idx] += 1;
      fix -= 1;
    } else if (scaled[idx] > 1) {
      scaled[idx] -= 1;
      fix += 1;
    }
    p += 1;
    if (p > scaled.length * 20) break;
  }
  return scaled;
}

function inboundFromOutbound(outboundIntervals, oddStopCount) {
  const need = Math.max(0, oddStopCount - 1);
  const rev = [...outboundIntervals].reverse();
  while (rev.length < need) rev.unshift(2);
  return rev.slice(0, need);
}

function generateForRoute(routeNumber) {
  const geoPath = path.join(assetsRoot, `stops_${routeNumber}_route.geojson`);
  if (!fs.existsSync(geoPath)) {
    console.warn(`skip ${routeNumber}: no geojson`);
    return;
  }

  const schedulePath = path.join(schedulesDir, `route_${routeNumber}.json`);
  if (routeNumber === "114" && fs.existsSync(schedulePath)) {
    console.log(`skip ${routeNumber}: keep manual EasyWay schedule`);
    return;
  }

  const { features, pointIdx } = loadPointFeatures(geoPath);
  const evenCount = pointIdx.filter((_, i) => i % 2 === 0).length;
  const oddCount = pointIdx.length - evenCount;
  const outboundIntervals = deriveOutboundIntervals(features, pointIdx);
  const inboundIntervals = inboundFromOutbound(outboundIntervals, oddCount);

  const starts = ROUTE_TRIP_STARTS[routeNumber] || { outbound: "06:00", inbound: "05:55" };

  const schedule = {
    route_number: routeNumber,
    source:
      "Згенеровано з OSM GeoJSON: пари зупинок 1–2 хв (як EasyWay), інші ділянки 2–5 хв, тривалість рейсу ~1.65 хв/зупинку",
    outbound: {
      label_ua: "Прямий (перша зупинка пари → кінець маршруту)",
      trip_start: starts.outbound,
      intervals_minutes: outboundIntervals
    },
    inbound: {
      label_ua: "Зворотний (вокзал/кінець Б → початок А)",
      trip_start: starts.inbound,
      intervals_minutes: inboundIntervals
    }
  };

  fs.mkdirSync(schedulesDir, { recursive: true });
  fs.writeFileSync(schedulePath, JSON.stringify(schedule, null, 2) + "\n", "utf8");
  console.log(
    `generated route_${routeNumber}.json: outbound ${evenCount} stops (${outboundIntervals.length} iv), inbound ${oddCount} stops (${inboundIntervals.length} iv)`
  );
}

const geoFiles = fs
  .readdirSync(assetsRoot)
  .filter((f) => /^stops_\d+_route\.geojson$/.test(f))
  .sort((a, b) => {
    const na = Number(a.match(/\d+/)[0]);
    const nb = Number(b.match(/\d+/)[0]);
    return na - nb;
  });

for (const f of geoFiles) {
  const routeNumber = f.match(/^stops_(\d+)_route\.geojson$/)[1];
  generateForRoute(routeNumber);
}
