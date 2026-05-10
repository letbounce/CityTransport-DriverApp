const express = require("express");
const { body } = require("express-validator");
const { createIncident } = require("../controllers/incidentController");

const router = express.Router();

router.post(
  "/",
  [
    body("waybill_id").isString().notEmpty(),
    body("type").isIn(["accident", "breakdown", "traffic_jam", "other"]),
    body("description").optional().isString(),
    body("location.lat").isFloat(),
    body("location.lng").isFloat(),
    body("photo_url").optional({ nullable: true }).isString()
  ],
  createIncident
);

module.exports = router;
