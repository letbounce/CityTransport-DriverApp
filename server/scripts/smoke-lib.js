/* eslint-disable no-console */
require("dotenv").config({ path: require("path").join(__dirname, "..", ".env") });

const HEALTH_URL = process.env.SMOKE_HEALTH_URL || "http://localhost:3000/health";
const API_BASE = process.env.SMOKE_BASE_URL || "http://localhost:3000/api";

const DRIVER_ID = process.env.SMOKE_DRIVER_ID || "DRV-1042";
const DRIVER_PASSWORD = process.env.SMOKE_DRIVER_PASSWORD || "password123";
const ROUTE_NUMBER = process.env.SMOKE_ROUTE_NUMBER || "114";
const VEHICLE_ID = process.env.SMOKE_VEHICLE_ID || "KP-3204";

/** Мінімальний валідний JPEG (~1x1) для TC-04 */
const TINY_JPEG_BASE64 =
  "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAb/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCfAA//2Q==";

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForServer(maxMs = 45000, intervalMs = 500) {
  const deadline = Date.now() + maxMs;
  let lastErr = null;
  while (Date.now() < deadline) {
    try {
      const res = await fetch(HEALTH_URL);
      if (res.ok) {
        const body = await res.json();
        if (body.status === "ok") return true;
      }
    } catch (e) {
      lastErr = e;
    }
    await sleep(intervalMs);
  }
  const hint =
    "Сервер недоступний. Запустіть у іншому терміналі: cd server && npm start\n" +
    "або: npm run smoke:all (автозапуск сервера). Переконайтесь, що MongoDB працює та є .env (cp .env.example .env).";
  throw new Error(lastErr?.message ? `${hint}\n(${lastErr.message})` : hint);
}

async function api(path, options = {}) {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const res = await fetch(url, options);
  let data = null;
  const text = await res.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { raw: text };
    }
  }
  return { res, data };
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function printTable(rows) {
  console.log("\nТаблиця 3.7 — результати smoke-тестування (DRV-1042)\n");
  console.log("ID\tВимога/UC\tСтатус");
  for (const r of rows) {
    console.log(`${r.id}\t${r.req}\t${r.status}`);
  }
  console.log("");
}

module.exports = {
  HEALTH_URL,
  API_BASE,
  DRIVER_ID,
  DRIVER_PASSWORD,
  ROUTE_NUMBER,
  VEHICLE_ID,
  TINY_JPEG_BASE64,
  waitForServer,
  api,
  assert,
  printTable,
  sleep
};
