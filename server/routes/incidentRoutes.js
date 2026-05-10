const express = require("express");
const { body } = require("express-validator");
const {
  createIncident,
  getIncidentById,
  updateIncident,
  listIncidents,
  listArchivedIncidents,
  archiveIncident
} = require("../controllers/incidentController");

const router = express.Router();

router.get("/archived", listArchivedIncidents);
router.get("/", listIncidents);
router.post(
  "/",
  [
    body("waybill_id").isString().notEmpty(),
    body("type").isIn(["accident", "breakdown", "traffic_jam", "other"]),
    body("description").isString(),
    body("location.lat").isFloat(),
    body("location.lng").isFloat(),
    body("photo_url").optional({ nullable: true }).isString(),
    body("photo_base64").optional({ nullable: true }).isString(),
    body("reported_at").optional({ nullable: true }).isISO8601(),
    body("stop_label").optional({ nullable: true }).isString().isLength({ max: 256 }),
    body("can_move_independently").optional({ nullable: true }).isBoolean()
  ],
  createIncident
);
router.get("/:id", getIncidentById);
router.patch(
  "/:id",
  [
    body("type").optional().isIn(["accident", "breakdown", "traffic_jam", "other"]),
    body("description").optional().isString(),
    body("reported_at").optional({ nullable: true }).isISO8601(),
    body("stop_label").optional({ nullable: true }).isString().isLength({ max: 256 }),
    body("can_move_independently").optional({ nullable: true }).isBoolean(),
    body("photo_base64").optional({ nullable: true }).isString(),
    body("photo_url").optional({ nullable: true }).isString(),
    body("clear_photo").optional({ nullable: true }).isBoolean()
  ],
  updateIncident
);
router.post(
  "/:id/archive",
  [
    body("reason_code").isString().notEmpty(),
    body("reason_note").optional({ nullable: true }).isString().isLength({ max: 2000 })
  ],
  archiveIncident
);

module.exports = router;
