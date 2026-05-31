const express = require("express");
const { body } = require("express-validator");
const {
  createWaybill,
  completeWaybill,
  getActiveWaybill,
  listWaybills,
  listArchivedWaybills,
  archiveWaybill,
  getWaybillById,
  updateWaybill
} = require("../controllers/waybillController");

const router = express.Router();

router.get("/active", getActiveWaybill);
router.get("/archived", listArchivedWaybills);
router.get("/", listWaybills);
router.patch("/:id/complete", completeWaybill);
router.post(
  "/:id/archive",
  [
    body("reason_code").isString().notEmpty(),
    body("reason_note").optional({ nullable: true }).isString().isLength({ max: 2000 })
  ],
  archiveWaybill
);
router.patch(
  "/:id",
  [
    body("vehicle_id").optional().isString().isLength({ min: 1, max: 64 }),
    body("notes").optional().isString().isLength({ max: 4000 })
  ],
  updateWaybill
);
router.get("/:id", getWaybillById);
router.post(
  "/",
  [
    body("route_id").isString().notEmpty(),
    body("vehicle_id").optional().isString(),
    body("notes").optional().isString().isLength({ max: 4000 })
  ],
  createWaybill
);

module.exports = router;
