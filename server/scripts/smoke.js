/* eslint-disable no-console */
const baseUrl = process.env.SMOKE_BASE_URL || "http://localhost:3000/api";

async function run() {
  const loginRes = await fetch(`${baseUrl}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ driver_id: "DRV-1042", password: "password123" })
  });
  const login = await loginRes.json();
  if (!login.token) throw new Error("Login failed");

  const authHeaders = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${login.token}`
  };

  const routesRes = await fetch(`${baseUrl}/routes`, { headers: authHeaders });
  const routes = await routesRes.json();
  const routeId = routes[0]?._id;
  if (!routeId) throw new Error("No routes available");

  const waybillRes = await fetch(`${baseUrl}/waybills`, {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({ route_id: routeId, vehicle_id: "KP-3204" })
  });
  const waybill = await waybillRes.json();
  const waybillId = waybill._id;
  if (!waybillId) throw new Error("Waybill create failed");

  await fetch(`${baseUrl}/telemetry`, {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({
      waybill_id: waybillId,
      locations: [
        { lat: 50.4501, lng: 30.5234, speed_kmh: 35 },
        { lat: 50.4512, lng: 30.5245, speed_kmh: 40 }
      ]
    })
  });

  await fetch(`${baseUrl}/incidents`, {
    method: "POST",
    headers: authHeaders,
    body: JSON.stringify({
      waybill_id: waybillId,
      type: "breakdown",
      description: "Smoke test incident",
      location: { lat: 50.4501, lng: 30.5234 }
    })
  });

  await fetch(`${baseUrl}/waybills/${waybillId}/complete`, {
    method: "PATCH",
    headers: authHeaders
  });

  console.log("Smoke flow completed");
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
