/* eslint-disable no-console */
/**
 * Smoke-тести відповідно до табл. 3.7 диплома (контрольний приклад DRV-1042, маршрут №114).
 * Перед запуском: MongoDB + npm run seed + npm start (або npm run smoke:all).
 */
const {
  DRIVER_ID,
  DRIVER_PASSWORD,
  ROUTE_NUMBER,
  VEHICLE_ID,
  TINY_JPEG_BASE64,
  waitForServer,
  api,
  assert,
  printTable
} = require("./smoke-lib");

const tableRows = [];
let authHeaders = null;
let token = null;
let routeId = null;
let waybillId = null;

function record(id, req, status) {
  tableRows.push({ id, req, status });
}

async function ensureNoActiveWaybill() {
  const { res, data } = await api("/waybills/active", { headers: authHeaders });
  if (res.status === 404) return;
  assert(res.ok, `GET /waybills/active: ${res.status}`);
  const id = data._id;
  if (!id) return;
  const complete = await api(`/waybills/${id}/complete`, {
    method: "PATCH",
    headers: authHeaders
  });
  if (complete.res.ok) return;
  const archive = await api(`/waybills/${id}/archive`, {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({ reason_code: "other", reason_note: "smoke cleanup" })
  });
  assert(archive.res.ok, `Не вдалось завершити попередній рейс: ${archive.res.status}`);
}

async function tc01_login() {
  const { res, data } = await api("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ driver_id: DRIVER_ID, password: DRIVER_PASSWORD })
  });
  assert(res.status === 200, `TC-01: очікував HTTP 200, отримано ${res.status}`);
  assert(data.token, "TC-01: у відповіді немає JWT token");
  assert(data.driver?.driver_id === DRIVER_ID, "TC-01: невірний driver_id у відповіді");
  token = data.token;
  authHeaders = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`
  };
  record("TC-01", "FR-01 / UC-01", "Пройдено");
  console.log("TC-01 Login DRV-1042 — JWT, driver OK");
}

async function tc02_createWaybill() {
  const routes = await api("/routes", { headers: authHeaders });
  assert(routes.res.ok, `TC-02: GET /routes — ${routes.res.status}`);
  const list = Array.isArray(routes.data) ? routes.data : [];
  const route = list.find((r) => String(r.route_number) === ROUTE_NUMBER);
  assert(route?._id, `TC-02: маршрут №${ROUTE_NUMBER} не знайдено (npm run seed)`);
  routeId = route._id;

  const { res, data } = await api("/waybills", {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({
      route_id: routeId,
      vehicle_id: VEHICLE_ID
    })
  });
  assert(res.status === 201, `TC-02: очікував HTTP 201, отримано ${res.status} — ${data.message || ""}`);
  assert(data.status === "in_progress", `TC-02: status=${data.status}, очікував in_progress`);
  assert(String(data.route_number) === ROUTE_NUMBER, "TC-02: невірний route_number у waybill");
  waybillId = data._id;
  record("TC-02", "FR-03 / UC-02", "Пройдено");
  console.log(`TC-02 Створення waybill м.${ROUTE_NUMBER} — HTTP 201, in_progress`);
}

async function tc03_telemetry() {
  const points = Array.from({ length: 3 }, (_, i) => ({
    lat: 50.45 + i * 0.001,
    lng: 30.52 + i * 0.001,
    speed_kmh: 30 + i,
    timestamp: new Date().toISOString()
  }));
  const { res, data } = await api("/telemetry", {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({ waybill_id: waybillId, locations: points })
  });
  assert(res.status === 201, `TC-03: очікував HTTP 201, отримано ${res.status}`);
  assert(data.count >= 1 || (data.locations && data.locations.length >= 1), "TC-03: телеметрія не збережена");
  record("TC-03", "FR-09 / UC-04", "Пройдено");
  console.log("TC-03 GPS batch — HTTP 201 (сервер); Room+Worker — на клієнті Android");
}

async function tc04_incidentPhoto() {
  const { res, data } = await api("/incidents", {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({
      waybill_id: waybillId,
      type: "breakdown",
      description: "Smoke TC-04: технічна несправність на маршруті 114",
      location: { lat: 50.4501, lng: 30.5234 },
      stop_label: "Зупинка 1",
      photo_base64: TINY_JPEG_BASE64
    })
  });
  assert(res.status === 201, `TC-04: очікував HTTP 201, отримано ${res.status} — ${data.message || ""}`);
  assert(data.photo_url && String(data.photo_url).includes("/uploads/incidents/"), "TC-04: немає photo_url");
  record("TC-04", "FR-06 / UC-05", "Пройдено");
  console.log(`TC-04 Інцидент + фото — HTTP 201, ${data.photo_url}`);
}

async function tc05_complete() {
  const { res, data } = await api(`/waybills/${waybillId}/complete`, {
    method: "PATCH",
    headers: authHeaders
  });
  assert(res.ok, `TC-05: PATCH complete — ${res.status}`);
  assert(data.status === "completed", `TC-05: status=${data.status}`);
  record("TC-05", "FR-04 / UC-06", "Пройдено");
  console.log("TC-05 Завершення рейсу — status=completed");
}

async function tc06_routeMap() {
  const { res, data } = await api(`/routes/${routeId}`, { headers: authHeaders });
  assert(res.ok, `TC-06: GET /routes/:id — ${res.status}`);
  const stops = data.stops || [];
  assert(stops.length > 0, "TC-06: маршрут без зупинок (перевірте seed / assets/map)");
  const withCoords = stops.filter((s) => Number.isFinite(s.lat) && Number.isFinite(s.lng));
  assert(withCoords.length > 0, "TC-06: зупинки без координат (GeoJSON у seed)");
  assert(String(data.route_number) === ROUTE_NUMBER, "TC-06: невірний маршрут");
  record("TC-06", "FR-08 / UC-03", "Пройдено");
  console.log(`TC-06 Карта/OSM — маршрут ${ROUTE_NUMBER}, зупинок: ${stops.length}, з GPS: ${withCoords.length}`);
}

async function runSmokeTests() {
  await waitForServer();
  await tc01_login();
  await ensureNoActiveWaybill();
  await tc02_createWaybill();
  await tc03_telemetry();
  await tc04_incidentPhoto();
  await tc05_complete();
  await tc06_routeMap();
  printTable(tableRows);
  console.log("Усі 6 тест-кейсів табл. 3.7 пройдено успішно.");
}

if (require.main === module) {
  runSmokeTests().catch((error) => {
    console.error("\nSmoke test failed:", error.message);
    if (tableRows.length) printTable(tableRows);
    process.exit(1);
  });
}

module.exports = { runSmokeTests };
