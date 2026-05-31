const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const { validationResult } = require("express-validator");
const Driver = require("../models/Driver");

async function loginDriver(req, res) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ message: "Validation failed", errors: errors.array() });
  }

  const { driver_id, password } = req.body;
  const driver = await Driver.findOne({ driver_id, is_active: true });
  if (!driver) {
    return res.status(401).json({ message: "Invalid credentials" });
  }

  const isValid = await bcrypt.compare(password, driver.password_hash);
  if (!isValid) {
    return res.status(401).json({ message: "Invalid credentials" });
  }

  const token = jwt.sign(
    { driver_id: driver.driver_id, full_name: driver.full_name, role: driver.role },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || "8h" }
  );

  return res.json({
    token,
    driver: {
      driver_id: driver.driver_id,
      full_name: driver.full_name
    }
  });
}

module.exports = { loginDriver };
