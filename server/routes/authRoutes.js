const express = require("express");
const { body } = require("express-validator");
const { loginDriver } = require("../controllers/authController");

const router = express.Router();

router.post(
  "/login",
  [
    body("driver_id").isString().notEmpty(),
    body("password").isString().isLength({ min: 6 })
  ],
  loginDriver
);

module.exports = router;
