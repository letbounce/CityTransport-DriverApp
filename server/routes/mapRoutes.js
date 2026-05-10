const express = require("express");
const { listLiveTripMarkers } = require("../controllers/mapController");

const router = express.Router();

router.get("/live-trips", listLiveTripMarkers);

module.exports = router;
