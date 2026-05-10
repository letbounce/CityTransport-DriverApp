const Waybill = require("../models/Waybill");
const Telemetry = require("../models/Telemetry");

/** GET /api/map/live-trips — остання відома позиція з телеметрії для активних дорожніх листів */
async function listLiveTripMarkers(req, res) {
  const activeWaybills = await Waybill.find({
    deleted_at: null,
    status: { $in: ["assigned", "in_progress"] }
  })
    .select("_id driver_id route_number")
    .lean();

  if (!activeWaybills.length) {
    return res.json({ markers: [] });
  }

  const ids = activeWaybills.map((w) => w._id);
  const telemetryBuckets = await Telemetry.find({ waybill_id: { $in: ids } })
    .sort({ bucket_start: -1 })
    .lean();

  const latestByWaybill = new Map();
  for (const bucket of telemetryBuckets) {
    const wid = String(bucket.waybill_id);
    if (latestByWaybill.has(wid)) continue;
    const locs = bucket.locations || [];
    const last = locs[locs.length - 1];
    if (
      !last ||
      typeof last.lat !== "number" ||
      typeof last.lng !== "number" ||
      Number.isNaN(last.lat) ||
      Number.isNaN(last.lng)
    ) {
      continue;
    }
    latestByWaybill.set(wid, {
      lat: last.lat,
      lng: last.lng,
      updated_at: last.timestamp || bucket.bucket_start,
      driver_id: bucket.driver_id
    });
  }

  const mine = req.user.driver_id;
  const markers = activeWaybills
    .map((w) => {
      const snap = latestByWaybill.get(String(w._id));
      if (!snap) return null;
      const ts = snap.updated_at instanceof Date ? snap.updated_at.toISOString() : snap.updated_at;
      return {
        waybill_id: String(w._id),
        driver_id: w.driver_id,
        route_number: w.route_number,
        lat: snap.lat,
        lng: snap.lng,
        updated_at: ts || null,
        is_self: w.driver_id === mine
      };
    })
    .filter(Boolean);

  return res.json({ markers });
}

module.exports = { listLiveTripMarkers };
