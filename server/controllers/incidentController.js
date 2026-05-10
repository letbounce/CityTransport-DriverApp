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

async function listIncidents(req, res) {
  const limit = Math.min(parseInt(req.query.limit, 10) || 100, 300);
  const incidents = await Incident.find({ driver_id: req.user.driver_id })
    .sort({ reported_at: -1 })
    .limit(limit)
    .lean();
  return res.json(incidents);
}

module.exports = { createIncident, listIncidents };
