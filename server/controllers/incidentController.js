const { validationResult } = require("express-validator");
const Incident = require("../models/Incident");
const { saveIncidentPhotoBase64 } = require("../utils/incidentPhoto");
const { isValidArchiveReason } = require("../constants/archiveReasons");

function snapshotFromDoc(doc) {
  return {
    type: doc.type,
    description: doc.description,
    location: { lat: doc.location.lat, lng: doc.location.lng },
    reported_at: doc.reported_at,
    stop_label: doc.stop_label || "",
    can_move_independently: !!doc.can_move_independently,
    photo_url: doc.photo_url || null
  };
}

async function createIncident(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const description = (req.body.description || "").trim();
  if (!description.length) {
    return res.status(400).json({ message: "Опис інциденту не може бути порожнім" });
  }

  let photoUrl = req.body.photo_url || null;
  if (req.body.photo_base64) {
    try {
      photoUrl = saveIncidentPhotoBase64(req.body.photo_base64);
    } catch (e) {
      const code = e.statusCode || 500;
      return res.status(code).json({ message: e.message || "Image save failed" });
    }
  }

  const reportedAt = req.body.reported_at ? new Date(req.body.reported_at) : new Date();
  if (Number.isNaN(reportedAt.getTime())) {
    return res.status(400).json({ message: "Invalid reported_at" });
  }

  const incident = await Incident.create({
    waybill_id: req.body.waybill_id,
    driver_id: req.user.driver_id,
    type: req.body.type,
    description,
    location: req.body.location,
    photo_url: photoUrl,
    reported_at: reportedAt,
    stop_label: req.body.stop_label || "",
    can_move_independently: Boolean(req.body.can_move_independently),
    status: "open",
    version_history: [],
    is_modified: false,
    last_edited_at: null
  });

  return res.status(201).json(incident);
}

async function getIncidentById(req, res) {
  const incident = await Incident.findOne({
    _id: req.params.id,
    driver_id: req.user.driver_id,
    deleted_at: null
  }).lean();

  if (!incident) {
    return res.status(404).json({ message: "Incident not found" });
  }
  return res.json(incident);
}

async function updateIncident(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const incident = await Incident.findOne({
    _id: req.params.id,
    driver_id: req.user.driver_id,
    deleted_at: null
  });

  if (!incident) {
    return res.status(404).json({ message: "Incident not found" });
  }

  const description = (req.body.description !== undefined ? req.body.description : incident.description) || "";
  const trimmed = description.trim();
  if (!trimmed.length) {
    return res.status(400).json({ message: "Опис інциденту не може бути порожнім" });
  }

  incident.version_history.push({
    driver_id: req.user.driver_id,
    snapshot: snapshotFromDoc(incident)
  });

  if (req.body.type !== undefined) incident.type = req.body.type;
  incident.description = trimmed;
  if (req.body.location !== undefined) {
    incident.location = req.body.location;
  }
  if (req.body.reported_at !== undefined) {
    const d = new Date(req.body.reported_at);
    if (Number.isNaN(d.getTime())) {
      return res.status(400).json({ message: "Invalid reported_at" });
    }
    incident.reported_at = d;
  }
  if (req.body.stop_label !== undefined) incident.stop_label = req.body.stop_label || "";
  if (req.body.can_move_independently !== undefined) {
    incident.can_move_independently = Boolean(req.body.can_move_independently);
  }

  if (req.body.photo_base64) {
    try {
      incident.photo_url = saveIncidentPhotoBase64(req.body.photo_base64);
    } catch (e) {
      const code = e.statusCode || 500;
      return res.status(code).json({ message: e.message || "Image save failed" });
    }
  } else if (req.body.clear_photo === true) {
    incident.photo_url = null;
  } else if (req.body.photo_url !== undefined) {
    incident.photo_url = req.body.photo_url;
  }

  incident.is_modified = true;
  incident.last_edited_at = new Date();
  await incident.save();
  return res.json(incident);
}

async function listIncidents(req, res) {
  const limit = Math.min(parseInt(req.query.limit, 10) || 100, 300);
  const incidents = await Incident.find({ driver_id: req.user.driver_id, deleted_at: null })
    .sort({ reported_at: -1 })
    .limit(limit)
    .lean();
  return res.json(incidents);
}

async function listArchivedIncidents(req, res) {
  const limit = Math.min(parseInt(req.query.limit, 10) || 100, 300);
  const incidents = await Incident.find({
    driver_id: req.user.driver_id,
    deleted_at: { $ne: null }
  })
    .sort({ deleted_at: -1 })
    .limit(limit)
    .lean();
  return res.json(incidents);
}

async function archiveIncident(req, res) {
  const code = req.body.reason_code;
  if (!isValidArchiveReason(code)) {
    return res.status(400).json({ message: "Invalid or missing reason_code" });
  }

  const incident = await Incident.findOne({
    _id: req.params.id,
    driver_id: req.user.driver_id,
    deleted_at: null
  });

  if (!incident) {
    return res.status(404).json({ message: "Incident not found" });
  }

  incident.deleted_at = new Date();
  incident.status = "completed";
  incident.deletion_reason_code = code;
  incident.deletion_reason_note = (req.body.reason_note && String(req.body.reason_note).trim()) || "";

  await incident.save();
  return res.json(incident);
}

module.exports = {
  createIncident,
  getIncidentById,
  updateIncident,
  listIncidents,
  listArchivedIncidents,
  archiveIncident
};
