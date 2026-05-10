require("dotenv").config();

const bcrypt = require("bcrypt");
const mongoose = require("mongoose");
const { connectDatabase } = require("../config/db");
const Driver = require("../models/Driver");
const Route = require("../models/Route");
const Waybill = require("../models/Waybill");
const Telemetry = require("../models/Telemetry");
const Incident = require("../models/Incident");

async function seed() {
  await connectDatabase(process.env.MONGO_URI);

  await Promise.all([
    Driver.deleteMany({}),
    Route.deleteMany({}),
    Waybill.deleteMany({}),
    Telemetry.deleteMany({}),
    Incident.deleteMany({})
  ]);

  const passwordHash = await bcrypt.hash("password123", 10);

  await Driver.insertMany([
    {
      driver_id: "DRV-1042",
      full_name: "Коксюк О.В.",
      phone: "+380991234567",
      password_hash: passwordHash,
      role: "driver",
      is_active: true
    },
    {
      driver_id: "DRV-2001",
      full_name: "Іваненко П.С.",
      phone: "+380991234568",
      password_hash: passwordHash,
      role: "driver",
      is_active: true
    }
  ]);

  await Route.insertMany([
    {
      route_number: "12",
      route_name: "Центр - Вокзал",
      vehicle_type: "bus",
      is_active: true,
      stops: [
        { stop_number: 1, name: "Центральна площа", planned_time: "08:10", lat: 50.45, lng: 30.52 },
        { stop_number: 2, name: "Проспект Миру", planned_time: "08:18", lat: 50.46, lng: 30.53 },
        { stop_number: 3, name: "Ринок", planned_time: "08:25", lat: 50.47, lng: 30.54 },
        { stop_number: 4, name: "Вокзал", planned_time: "08:33", lat: 50.48, lng: 30.55 }
      ]
    },
    {
      route_number: "7",
      route_name: "Аеропорт - Університет",
      vehicle_type: "bus",
      is_active: true,
      stops: [
        { stop_number: 1, name: "Аеропорт", planned_time: "09:00", lat: 50.40, lng: 30.40 },
        { stop_number: 2, name: "Термінал Південний", planned_time: "09:08", lat: 50.41, lng: 30.41 },
        { stop_number: 3, name: "Парк", planned_time: "09:16", lat: 50.42, lng: 30.42 },
        { stop_number: 4, name: "Площа Науки", planned_time: "09:24", lat: 50.43, lng: 30.43 },
        { stop_number: 5, name: "Університет", planned_time: "09:33", lat: 50.44, lng: 30.44 }
      ]
    }
  ]);

  console.log("Seed completed");
  await mongoose.disconnect();
}

seed().catch(async (error) => {
  console.error(error);
  await mongoose.disconnect();
  process.exit(1);
});
