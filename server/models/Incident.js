const mongoose = require("mongoose");

const incidentSchema = new mongoose.Schema(
  {
    waybill_id: { type: mongoose.Schema.Types.ObjectId, ref: "Waybill", required: true },
    driver_id: { type: String, required: true, index: true },
    type: { type: String, enum: ["accident", "breakdown", "traffic_jam", "other"], required: true },
    description: { type: String, default: "" },
    location: {
      lat: { type: Number, required: true },
      lng: { type: Number, required: true }
    },
    photo_url: { type: String, default: null },
    reported_at: { type: Date, default: Date.now },
    status: { type: String, enum: ["open", "resolved"], default: "open" }
  },
  { timestamps: false }
);

module.exports = mongoose.model("Incident", incidentSchema);
