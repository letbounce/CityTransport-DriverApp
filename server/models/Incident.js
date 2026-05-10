const mongoose = require("mongoose");

const incidentSnapshotSchema = new mongoose.Schema(
  {
    type: { type: String },
    description: { type: String },
    location: {
      lat: { type: Number },
      lng: { type: Number }
    },
    reported_at: { type: Date },
    stop_label: { type: String },
    can_move_independently: { type: Boolean },
    photo_url: { type: String, default: null }
  },
  { _id: false }
);

const versionEntrySchema = new mongoose.Schema(
  {
    saved_at: { type: Date, default: Date.now },
    driver_id: { type: String, required: true },
    snapshot: { type: incidentSnapshotSchema, required: true }
  },
  { _id: false }
);

const incidentSchema = new mongoose.Schema(
  {
    waybill_id: { type: mongoose.Schema.Types.ObjectId, ref: "Waybill", required: true },
    driver_id: { type: String, required: true, index: true },
    type: { type: String, enum: ["accident", "breakdown", "traffic_jam", "other"], required: true },
    description: { type: String, required: true },
    location: {
      lat: { type: Number, required: true },
      lng: { type: Number, required: true }
    },
    photo_url: { type: String, default: null },
    reported_at: { type: Date, default: Date.now },
    stop_label: { type: String, default: "" },
    can_move_independently: { type: Boolean, default: false },
    status: {
      type: String,
      enum: ["open", "resolved", "completed"],
      default: "open"
    },
    deleted_at: { type: Date, default: null },
    deletion_reason_code: { type: String, default: null },
    deletion_reason_note: { type: String, default: null },
    /** Позначення «Змінений» у застосунку та БД */
    is_modified: { type: Boolean, default: false },
    last_edited_at: { type: Date, default: null },
    version_history: { type: [versionEntrySchema], default: [] }
  },
  { timestamps: false }
);

module.exports = mongoose.model("Incident", incidentSchema);
