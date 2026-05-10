require("dotenv").config();

const express = require("express");
const cors = require("cors");
const { connectDatabase } = require("./config/db");
const { requireAuth } = require("./middleware/auth");

const authRoutes = require("./routes/authRoutes");
const routeRoutes = require("./routes/routeRoutes");
const waybillRoutes = require("./routes/waybillRoutes");
const telemetryRoutes = require("./routes/telemetryRoutes");
const incidentRoutes = require("./routes/incidentRoutes");

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => {
  res.json({ status: "ok" });
});

app.use("/api/auth", authRoutes);
app.use("/api/routes", requireAuth, routeRoutes);
app.use("/api/waybills", requireAuth, waybillRoutes);
app.use("/api/telemetry", requireAuth, telemetryRoutes);
app.use("/api/incidents", requireAuth, incidentRoutes);

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(500).json({ message: "Internal server error" });
});

connectDatabase(process.env.MONGO_URI).then(() => {
  app.listen(port, () => {
    console.log(`Server running on http://localhost:${port}`);
  });
});
