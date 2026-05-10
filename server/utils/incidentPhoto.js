const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const MAX_BYTES = 5 * 1024 * 1024;

/**
 * Зберігає base64 зображення (опційно з префіксом data:image/...;base64,) у uploads/incidents.
 * Повертає відносний шлях для photo_url, наприклад /uploads/incidents/uuid.jpg
 */
function saveIncidentPhotoBase64(base64Input) {
  if (!base64Input || typeof base64Input !== "string") {
    return null;
  }

  let ext = "jpg";
  let rawBase64 = base64Input.trim();

  const dataUrl = /^data:image\/(\w+);base64,(.+)$/i.exec(rawBase64);
  if (dataUrl) {
    ext = dataUrl[1].toLowerCase() === "jpeg" ? "jpg" : dataUrl[1].toLowerCase();
    rawBase64 = dataUrl[2];
  }

  const buf = Buffer.from(rawBase64, "base64");
  if (!buf.length || buf.length > MAX_BYTES) {
    const err = new Error("Invalid or too large image payload");
    err.statusCode = 400;
    throw err;
  }

  const safeExt = ["jpg", "jpeg", "png", "webp"].includes(ext) ? ext : "jpg";
  const fileName = `${crypto.randomUUID()}.${safeExt}`;
  const dir = path.join(__dirname, "..", "uploads", "incidents");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, fileName), buf);
  return `/uploads/incidents/${fileName}`;
}

module.exports = { saveIncidentPhotoBase64 };
