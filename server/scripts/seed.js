require("dotenv").config();

const fs = require("fs");
const path = require("path");
const bcrypt = require("bcrypt");
const mongoose = require("mongoose");
const { connectDatabase } = require("../config/db");
const Driver = require("../models/Driver");
const Route = require("../models/Route");
const Vehicle = require("../models/Vehicle");
const Waybill = require("../models/Waybill");
const Telemetry = require("../models/Telemetry");
const Incident = require("../models/Incident");

/** Узгоджено з TripMapCatalog / assets/map/stops_{ref}_route.geojson */
const KYIV_ROUTE_ORDER = ["7", "11", "18", "24", "50", "55", "62", "101", "114", "115"];

const KYIV_ROUTE_NAMES = {
  "7": "Львівська площа — Залізничний вокзал «Центральний»",
  "11": "Станція метро «Лісова» — Радіоцентр",
  "18": "Станція метро «Харківська» — Харківське шосе",
  "24": "Музей історії України у Другій світовій війні — Залізничний вокзал",
  "50": "Залізничний вокзал «Центральний» — Вулиця Північна",
  "55": "Дарницька площа — Станція метро «Палац спорту»",
  "62": "Контрактова площа — Ботанічний сад",
  "101": "вул. Милославська — ст. м. Почайна",
  "114": "вул. Радунська — Залізничний вокзал «Центральний»",
  "115": "Контрактова площа — Будинок культури"
};

function loadKyivStopsFromAssets(routeNumber) {
  const assetsRoot = path.join(__dirname, "..", "..", "app", "src", "main", "assets", "map");
  const fp = path.join(assetsRoot, `stops_${routeNumber}_route.geojson`);
  if (!fs.existsSync(fp)) {
    console.warn(`seed: немає файлу зупинок ${fp}`);
    return [];
  }
  let fc;
  try {
    fc = JSON.parse(fs.readFileSync(fp, "utf8"));
  } catch (e) {
    console.warn(`seed: не вдалося прочитати ${fp}`, e.message);
    return [];
  }
  const features = fc.features || [];
  const stops = [];
  let n = 1;
  for (const f of features) {
    if (!f || f.geometry?.type !== "Point" || !Array.isArray(f.geometry.coordinates)) continue;
    const [lng, lat] = f.geometry.coordinates;
    const p = f.properties || {};
    const name = String(p["name:uk"] || p.name || "").trim() || `Зупинка ${n}`;
    stops.push({
      stop_number: n,
      name,
      planned_time: "--:--",
      lat,
      lng
    });
    n += 1;
  }
  return stops;
}

function buildKyivRoutesForSeed() {
  return KYIV_ROUTE_ORDER.map((route_number) => ({
    route_number,
    route_name: KYIV_ROUTE_NAMES[route_number] || `Київський автобус ${route_number}`,
    vehicle_type: "bus",
    is_active: true,
    stops: loadKyivStopsFromAssets(route_number)
  }));
}

/**
 * Типові моделі автобусів на київських маршрутах (КП «Київпастранс», закупівлі міста),
 * узагальнено за відкритими джерелами ~2025–2026: масовий парк МАЗ-203 та зчленовані МАЗ-107;
 * імпортні Citaro / MAN Lion's City / Irisbus Citelis (зокрема гуманітарні партії);
 * електробуси «Електрон» Т191; нові низькопідлогові Anadolu Isuzu (Citibus) на окремих лініях
 * (див. напр. lb.ua, Wikipedia «Київський автобус», огляди pas-transport).
 * Поля vehicle_id — умовні «борти» для демо-БД; номери держреєстрації вигадані.
 */
const DEFAULT_KYIV_VEHICLE_ID = "KP-3204";

function kyivFleetVehiclesForSeed() {
  return [
    {
      vehicle_id: DEFAULT_KYIV_VEHICLE_ID,
      label: "МАЗ-203.069 · основний клас Києва (~110 місць)",
      plate_number: "КА 4821 ВХ",
      is_active: true
    },
    {
      vehicle_id: "KP-3188",
      label: "МАЗ-203.069 · серія 2018–2020 рр.",
      plate_number: "КА 9156 ВІ",
      is_active: true
    },
    {
      vehicle_id: "KP-2901",
      label: "МАЗ-107 · зчленований (~175 місць)",
      plate_number: "КА 3012 АМ",
      is_active: true
    },
    {
      vehicle_id: "KP-2907",
      label: "МАЗ-107 · зчленований",
      plate_number: "КА 7744 ВТ",
      is_active: true
    },
    {
      vehicle_id: "KP-4120",
      label: "Mercedes-Benz Citaro O530",
      plate_number: "КА 2288 СЕ",
      is_active: true
    },
    {
      vehicle_id: "KP-4155",
      label: "Mercedes-Benz Citaro O530 · низькопідлоговий",
      plate_number: "КА 6391 НН",
      is_active: true
    },
    {
      vehicle_id: "KP-4203",
      label: "MAN Lion's City A23",
      plate_number: "КА 5044 ММ",
      is_active: true
    },
    {
      vehicle_id: "KP-4217",
      label: "MAN Lion's City · клас А",
      plate_number: "КА 8177 РР",
      is_active: true
    },
    {
      vehicle_id: "KP-4308",
      label: "Irisbus Citelis 12M",
      plate_number: "КА 9920 ТТ",
      is_active: true
    },
    {
      vehicle_id: "KP-4401",
      label: "Electron Т191 · електробус (ПрАТ «Електрон»)",
      plate_number: "КА 101 ЕЕ",
      is_active: true
    },
    {
      vehicle_id: "KP-4412",
      label: "Electron Т191 · електробус",
      plate_number: "КА 202 ЕЕ",
      is_active: true
    },
    {
      vehicle_id: "KP-5102",
      label: "Anadolu Isuzu Citibus · низькопідлоговий (поставки 2025 р.)",
      plate_number: "КА 7788 ІІ",
      is_active: true
    },
    {
      vehicle_id: "KP-5110",
      label: "Anadolu Isuzu · новий парк на лініях міста",
      plate_number: "КА 8899 ІІ",
      is_active: true
    },
    {
      vehicle_id: "KP-5055",
      label: "Богдан А601 · залишковий парк (рідше)",
      plate_number: "КА 3344 ВВ",
      is_active: true
    },
    {
      vehicle_id: "KP-6001",
      label: "Резерв / технічний огляд (узагальнений клас МАЗ-203)",
      plate_number: "",
      is_active: true
    }
  ];
}

async function seed() {
  await connectDatabase(process.env.MONGO_URI);

  await Promise.all([
    Driver.deleteMany({}),
    Route.deleteMany({}),
    Vehicle.deleteMany({}),
    Waybill.deleteMany({}),
    Telemetry.deleteMany({}),
    Incident.deleteMany({})
  ]);

  const passwordHash = await bcrypt.hash("password123", 10);

  await Driver.insertMany([
    {
      driver_id: "DRV-1042",
      full_name: "Коксюк О.В.",
      phone: "+380991234567",
      password_hash: passwordHash,
      role: "driver",
      is_active: true
    },
    {
      driver_id: "DRV-2001",
      full_name: "Іваненко П.С.",
      phone: "+380991234568",
      password_hash: passwordHash,
      role: "driver",
      is_active: true
    }
  ]);

  await Vehicle.insertMany(kyivFleetVehiclesForSeed());

  const kyivRoutes = buildKyivRoutesForSeed();
  await Route.insertMany(kyivRoutes);

  console.log("Seed completed");
  await mongoose.disconnect();
}

seed().catch(async (error) => {
  console.error(error);
  await mongoose.disconnect();
  process.exit(1);
});
