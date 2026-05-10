const { validationResult } = require("express-validator");
const Incident = require("../models/Incident");

async function createIncident(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const incident = await Incident.create({
    waybill_id: req.body.waybill_id,
    driver_id: req.user.driver_id,
    type: req.body.type,
    description: req.body.description || "",
    location: req.body.location,
    photo_url: req.body.photo_url || null,
    reported_at: new Date(),
    status: "open"
  });

  return res.status(201).json(incident);
}

module.exports = { createIncident };
