const mongoose = require("mongoose");

const locationSchema = new mongoose.Schema(
  {
    lat: { type: Number, required: true },
    lng: { type: Number, required: true },
    speed_kmh: { type: Number, default: 0 },
    timestamp: { type: Date, default: Date.now }
  },
  { _id: false }
);

const telemetrySchema = new mongoose.Schema(
  {
    waybill_id: { type: mongoose.Schema.Types.ObjectId, ref: "Waybill", required: true, index: true },
    driver_id: { type: String, required: true, index: true },
    bucket_start: { type: Date, default: Date.now },
    locations: { type: [locationSchema], default: [] },
    count: { type: Number, default: 0 }
  },
  { timestamps: false }
);

module.exports = mongoose.model("Telemetry", telemetrySchema);
