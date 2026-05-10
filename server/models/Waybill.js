const mongoose = require("mongoose");

const waybillSchema = new mongoose.Schema(
  {
    driver_id: { type: String, required: true, index: true },
    route_id: { type: mongoose.Schema.Types.ObjectId, ref: "Route", required: true },
    route_number: { type: String, required: true },
    status: {
      type: String,
      enum: ["assigned", "in_progress", "completed", "cancelled"],
      default: "in_progress"
    },
    started_at: { type: Date, default: Date.now },
    completed_at: { type: Date, default: null },
    vehicle_id: { type: String, default: "BUS-007" }
  },
  { timestamps: { createdAt: "created_at", updatedAt: false } }
);

module.exports = mongoose.model("Waybill", waybillSchema);
