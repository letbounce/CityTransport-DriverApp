const Vehicle = require("../models/Vehicle");

async function listVehicles(_req, res) {
  const vehicles = await Vehicle.find({ is_active: true }).sort({ vehicle_id: 1 }).lean();
  return res.json(vehicles);
}

module.exports = { listVehicles };
