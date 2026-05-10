const Route = require("../models/Route");

async function getRoutes(_req, res) {
  const routes = await Route.find({ is_active: true }).sort({ route_number: 1 });
  return res.json(routes);
}

async function getRouteById(req, res) {
  const route = await Route.findById(req.params.id);
  if (!route) {
    return res.status(404).json({ message: "Route not found" });
  }
  return res.json(route);
}

module.exports = { getRoutes, getRouteById };
