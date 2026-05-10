const express = require("express");
const { getRoutes, getRouteById } = require("../controllers/routeController");

const router = express.Router();

router.get("/", getRoutes);
router.get("/:id", getRouteById);

module.exports = router;
