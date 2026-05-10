require("dotenv").config();

const path = require("path");
const express = require("express");
const cors = require("cors");
const { connectDatabase } = require("./config/db");
const { requireAuth } = require("./middleware/auth");

const authRoutes = require("./routes/authRoutes");
const routeRoutes = require("./routes/routeRoutes");
const waybillRoutes = require("./routes/waybillRoutes");
const telemetryRoutes = require("./routes/telemetryRoutes");
const incidentRoutes = require("./routes/incidentRoutes");
const mapRoutes = require("./routes/mapRoutes");
const vehicleRoutes = require("./routes/vehicleRoutes");

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: "12mb" }));
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

app.get("/health", (_req, res) => {
  res.json({ status: "ok" });
});

app.use("/api/auth", authRoutes);
app.use("/api/routes", requireAuth, routeRoutes);
app.use("/api/waybills", requireAuth, waybillRoutes);
app.use("/api/telemetry", requireAuth, telemetryRoutes);
app.use("/api/incidents", requireAuth, incidentRoutes);
app.use("/api/map", requireAuth, mapRoutes);
app.use("/api/vehicles", requireAuth, vehicleRoutes);

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(500).json({ message: "Internal server error" });
});

connectDatabase(process.env.MONGO_URI).then(() => {
  app.listen(port, () => {
    console.log(`Server running on http://localhost:${port}`);
  });
});
