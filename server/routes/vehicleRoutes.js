const express = require("express");
const { listVehicles } = require("../controllers/vehicleController");

const router = express.Router();

router.get("/", listVehicles);

module.exports = router;
