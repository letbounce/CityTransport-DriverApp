const express = require("express");
const { body } = require("express-validator");
const {
  createWaybill,
  completeWaybill,
  getActiveWaybill
} = require("../controllers/waybillController");

const router = express.Router();

router.post(
  "/",
  [body("route_id").isString().notEmpty(), body("vehicle_id").optional().isString()],
  createWaybill
);
router.patch("/:id/complete", completeWaybill);
router.get("/active", getActiveWaybill);

module.exports = router;
