const mongoose = require("mongoose");

const stopSchema = new mongoose.Schema(
  {
    stop_number: { type: Number, required: true },
    name: { type: String, required: true },
    planned_time: { type: String, required: true },
    lat: { type: Number, required: true },
    lng: { type: Number, required: true }
  },
  { _id: false }
);

const routeSchema = new mongoose.Schema(
  {
    route_number: { type: String, required: true },
    route_name: { type: String, required: true },
    vehicle_type: { type: String, default: "bus" },
    stops: { type: [stopSchema], default: [] },
    is_active: { type: Boolean, default: true }
  },
  { timestamps: false }
);

module.exports = mongoose.model("Route", routeSchema);
