const express = require("express");
const { body } = require("express-validator");
const { createTelemetryBatch } = require("../controllers/telemetryController");

const router = express.Router();

router.post(
  "/",
  [
    body("waybill_id").isString().notEmpty(),
    body("locations").isArray({ min: 1 }),
    body("locations.*.lat").isFloat(),
    body("locations.*.lng").isFloat(),
    body("locations.*.speed_kmh").optional().isFloat()
  ],
  createTelemetryBatch
);

module.exports = router;
