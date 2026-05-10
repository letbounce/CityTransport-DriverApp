const mongoose = require("mongoose");

const vehicleSchema = new mongoose.Schema(
  {
    vehicle_id: { type: String, required: true, unique: true, index: true },
    label: { type: String, required: true },
    plate_number: { type: String, default: "" },
    is_active: { type: Boolean, default: true }
  },
  { timestamps: { createdAt: "created_at", updatedAt: false } }
);

module.exports = mongoose.model("Vehicle", vehicleSchema);
