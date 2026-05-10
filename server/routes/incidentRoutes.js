const express = require("express");
const { body } = require("express-validator");
const { createIncident, listIncidents } = require("../controllers/incidentController");

const router = express.Router();

router.get("/", listIncidents);
router.post(
  "/",
  [
    body("waybill_id").isString().notEmpty(),
    body("type").isIn(["accident", "breakdown", "traffic_jam", "other"]),
    body("description").optional({ nullable: true }).isString(),
    body("location.lat").isFloat(),
    body("location.lng").isFloat(),
    body("photo_url").optional({ nullable: true }).isString()
  ],
  createIncident
);

module.exports = router;
