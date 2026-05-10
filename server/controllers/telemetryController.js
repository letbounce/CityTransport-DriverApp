const { validationResult } = require("express-validator");
const Telemetry = require("../models/Telemetry");

async function createTelemetryBatch(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const locations = req.body.locations || [];
  const telemetry = await Telemetry.create({
    waybill_id: req.body.waybill_id,
    driver_id: req.user.driver_id,
    bucket_start: new Date(),
    locations,
    count: locations.length
  });

  return res.status(201).json(telemetry);
}

module.exports = { createTelemetryBatch };
