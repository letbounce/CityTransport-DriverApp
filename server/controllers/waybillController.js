const { validationResult } = require("express-validator");
const Route = require("../models/Route");
const Waybill = require("../models/Waybill");

async function createWaybill(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const existing = await Waybill.findOne({
    driver_id: req.user.driver_id,
    status: { $in: ["assigned", "in_progress"] }
  });
  if (existing) {
    return res.status(409).json({ message: "Active waybill already exists", waybill: existing });
  }

  const route = await Route.findById(req.body.route_id);
  if (!route) {
    return res.status(404).json({ message: "Route not found" });
  }

  const waybill = await Waybill.create({
    driver_id: req.user.driver_id,
    route_id: route._id,
    route_number: route.route_number,
    status: "in_progress",
    started_at: new Date(),
    vehicle_id: req.body.vehicle_id || "BUS-007",
    notes: req.body.notes || ""
  });

  return res.status(201).json(waybill);
}

async function completeWaybill(req, res) {
  const waybill = await Waybill.findOne({
    _id: req.params.id,
    driver_id: req.user.driver_id,
    status: { $in: ["assigned", "in_progress"] }
  });

  if (!waybill) {
    return res.status(404).json({ message: "Active waybill not found" });
  }

  waybill.status = "completed";
  waybill.completed_at = new Date();
  await waybill.save();
  return res.json(waybill);
}

async function getActiveWaybill(req, res) {
  const active = await Waybill.findOne({
    driver_id: req.user.driver_id,
    status: { $in: ["assigned", "in_progress"] }
  }).sort({ created_at: -1 });

  if (!active) {
    return res.status(404).json({ message: "No active waybill" });
  }

  return res.json(active);
}

async function listWaybills(req, res) {
  const limit = Math.min(parseInt(req.query.limit, 10) || 50, 200);
  const waybills = await Waybill.find({ driver_id: req.user.driver_id })
    .sort({ created_at: -1 })
    .limit(limit)
    .lean();
  return res.json(waybills);
}

async function getWaybillById(req, res) {
  const waybill = await Waybill.findOne({
    _id: req.params.id,
    driver_id: req.user.driver_id
  }).lean();
  if (!waybill) {
    return res.status(404).json({ message: "Waybill not found" });
  }
  return res.json(waybill);
}

async function updateWaybill(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const waybill = await Waybill.findOne({
    _id: req.params.id,
    driver_id: req.user.driver_id
  });

  if (!waybill) {
    return res.status(404).json({ message: "Waybill not found" });
  }

  if (req.body.vehicle_id !== undefined) {
    waybill.vehicle_id = req.body.vehicle_id;
  }
  if (req.body.notes !== undefined) {
    waybill.notes = req.body.notes;
  }

  await waybill.save();
  return res.json(waybill);
}

module.exports = {
  createWaybill,
  completeWaybill,
  getActiveWaybill,
  listWaybills,
  getWaybillById,
  updateWaybill
};
