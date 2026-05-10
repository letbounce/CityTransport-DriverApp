const mongoose = require("mongoose");

const driverSchema = new mongoose.Schema(
  {
    driver_id: { type: String, required: true, unique: true, index: true },
    full_name: { type: String, required: true },
    phone: { type: String, default: "" },
    password_hash: { type: String, required: true },
    role: { type: String, default: "driver" },
    is_active: { type: Boolean, default: true }
  },
  { timestamps: { createdAt: "created_at", updatedAt: false } }
);

module.exports = mongoose.model("Driver", driverSchema);
