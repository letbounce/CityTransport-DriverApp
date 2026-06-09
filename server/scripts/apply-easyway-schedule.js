/**
 * Застосовує розклад EasyWay з app/src/main/assets/map/schedules/route_*.json
 * до stops_*_route.geojson (planned_time + trip_direction).
 *
 * Використання: node scripts/apply-easyway-schedule.js [114]
 */
const fs = require("fs");
const path = require("path");

const assetsRoot = path.join(__dirname, "..", "..", "app", "src", "main", "assets", "map");
const schedulesDir = path.join(assetsRoot, "schedules");

function normalizePlannedTime(raw) {
  const s = String(raw ?? "").trim();
  const m = s.match(/^(\d{1,2}):(\d{2})$/);
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  if (h < 0 || h > 23 || min < 0 || min > 59) return null;
  return `${String(h).padStart(2, "0")}:${String(min).padStart(2, "0")}`;
}

function buildTimes(tripStart, intervals) {
  const [sh, sm] = tripStart.split(":").map(Number);
  let total = sh * 60 + sm;
  const times = [normalizePlannedTime(tripStart)];
  for (const iv of intervals) {
    total += iv;
    const h = Math.floor(total / 60) % 24;
    const m = total % 60;
    times.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
  }
  return times;
}

function applySchedule(routeNumber) {
  const schedulePath = path.join(schedulesDir, `route_${routeNumber}.json`);
  const geoPath = path.join(assetsRoot, `stops_${routeNumber}_route.geojson`);
  if (!fs.existsSync(schedulePath)) {
    throw new Error(`Немає розкладу: ${schedulePath}`);
  }
  if (!fs.existsSync(geoPath)) {
    throw new Error(`Немає GeoJSON: ${geoPath}`);
  }

  const schedule = JSON.parse(fs.readFileSync(schedulePath, "utf8"));
  const fc = JSON.parse(fs.readFileSync(geoPath, "utf8"));
  const features = fc.features || [];
  const pointIdx = [];
  for (let i = 0; i < features.length; i++) {
    if (features[i]?.geometry?.type === "Point") pointIdx.push(i);
  }

  const outboundTimes = buildTimes(
    schedule.outbound.trip_start,
    schedule.outbound.intervals_minutes
  );
  const inboundTimes = buildTimes(
    schedule.inbound.trip_start,
    schedule.inbound.intervals_minutes
  );

  const evenIdx = pointIdx.filter((_, i) => i % 2 === 0);
  const oddIdx = pointIdx.filter((_, i) => i % 2 === 1);
  const oddDisplayOrder = [...oddIdx].reverse();

  if (outboundTimes.length !== evenIdx.length) {
    console.warn(
      `route ${routeNumber}: outbound times ${outboundTimes.length} != even stops ${evenIdx.length}`
    );
  }
  if (inboundTimes.length !== oddDisplayOrder.length) {
    console.warn(
      `route ${routeNumber}: inbound times ${inboundTimes.length} != odd stops ${oddDisplayOrder.length}`
    );
  }

  evenIdx.forEach((featIdx, i) => {
    const p = features[featIdx].properties || (features[featIdx].properties = {});
    p.trip_direction = "outbound";
    if (outboundTimes[i]) p.planned_time = outboundTimes[i];
  });

  oddDisplayOrder.forEach((featIdx, i) => {
    const p = features[featIdx].properties || (features[featIdx].properties = {});
    p.trip_direction = "inbound";
    if (inboundTimes[i]) p.planned_time = inboundTimes[i];
  });

  fs.writeFileSync(geoPath, JSON.stringify(fc, null, 2) + "\n", "utf8");
  console.log(
    `OK route ${routeNumber}: ${evenIdx.length} outbound, ${oddDisplayOrder.length} inbound stops updated in ${geoPath}`
  );
}

function applyAll() {
  const files = fs
    .readdirSync(schedulesDir)
    .filter((f) => /^route_\d+\.json$/.test(f))
    .sort((a, b) => Number(a.match(/\d+/)[0]) - Number(b.match(/\d+/)[0]));
  let ok = 0;
  for (const f of files) {
    const routeNumber = f.match(/^route_(\d+)\.json$/)[1];
    try {
      applySchedule(routeNumber);
      ok += 1;
    } catch (e) {
      console.error(`FAIL route ${routeNumber}:`, e.message);
    }
  }
  console.log(`Done: ${ok}/${files.length} routes`);
}

const arg = process.argv[2];
if (arg === "--all") {
  applyAll();
} else {
  applySchedule(arg || "114");
}
