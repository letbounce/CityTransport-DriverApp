require("dotenv").config();

const bcrypt = require("bcrypt");
const mongoose = require("mongoose");
const { connectDatabase } = require("../config/db");
const Driver = require("../models/Driver");
const Route = require("../models/Route");
const Vehicle = require("../models/Vehicle");
const Waybill = require("../models/Waybill");
const Telemetry = require("../models/Telemetry");
const Incident = require("../models/Incident");

async function seed() {
  await connectDatabase(process.env.MONGO_URI);

  await Promise.all([
    Driver.deleteMany({}),
    Route.deleteMany({}),
    Vehicle.deleteMany({}),
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

  await Vehicle.insertMany([
    { vehicle_id: "BUS-007", label: "Mercedes Conecto (57 місць)", plate_number: "AA 1234 BC", is_active: true },
    { vehicle_id: "BUS-101", label: "MAN Lion’s City (45 місць)", plate_number: "AA 5678 BC", is_active: true },
    { vehicle_id: "BUS-205", label: "Electron Т191 (електробус)", plate_number: "KA 9012 TT", is_active: true },
    { vehicle_id: "TRAM-03", label: "Трамвай Tatra KT4", plate_number: "ТМ-03", is_active: true },
    { vehicle_id: "MINI-12", label: "Маршрутне таксі Ford Transit", plate_number: "BI 3456 MM", is_active: true }
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
        { stop_number: 1, name: "Аеропорт", planned_time: "09:00", lat: 50.4, lng: 30.4 },
        { stop_number: 2, name: "Термінал Південний", planned_time: "09:08", lat: 50.41, lng: 30.41 },
        { stop_number: 3, name: "Парк", planned_time: "09:16", lat: 50.42, lng: 30.42 },
        { stop_number: 4, name: "Площа Науки", planned_time: "09:24", lat: 50.43, lng: 30.43 },
        { stop_number: 5, name: "Університет", planned_time: "09:33", lat: 50.44, lng: 30.44 }
      ]
    },
    {
      route_number: "3",
      route_name: "Оболонь - Печерськ",
      vehicle_type: "bus",
      is_active: true,
      stops: [
        { stop_number: 1, name: "Оболонь (метро)", planned_time: "07:05", lat: 50.51, lng: 30.5 },
        { stop_number: 2, name: "Набережна", planned_time: "07:18", lat: 50.49, lng: 30.53 },
        { stop_number: 3, name: "Хрещатик", planned_time: "07:35", lat: 50.45, lng: 30.52 },
        { stop_number: 4, name: "Печерськ", planned_time: "07:48", lat: 50.425, lng: 30.535 }
      ]
    },
    {
      route_number: "14",
      route_name: "Троєщина - Теремки",
      vehicle_type: "bus",
      is_active: true,
      stops: [
        { stop_number: 1, name: "Троєщина", planned_time: "06:40", lat: 50.53, lng: 30.59 },
        { stop_number: 2, name: "Дарницький міст", planned_time: "07:05", lat: 50.47, lng: 30.58 },
        { stop_number: 3, name: "Либідська", planned_time: "07:28", lat: 50.407, lng: 30.522 },
        { stop_number: 4, name: "Теремки", planned_time: "07:45", lat: 50.367, lng: 30.467 }
      ]
    },
    {
      route_number: "22",
      route_name: "Сихівка - Шевченківський район",
      vehicle_type: "tram",
      is_active: true,
      stops: [
        { stop_number: 1, name: "Сихівський масив", planned_time: "08:00", lat: 49.83, lng: 24.02 },
        { stop_number: 2, name: "Вокзал Львів", planned_time: "08:22", lat: 49.84, lng: 24.03 },
        { stop_number: 3, name: "Площа Ринок", planned_time: "08:35", lat: 49.841, lng: 24.032 },
        { stop_number: 4, name: "Університетська", planned_time: "08:45", lat: 49.839, lng: 24.025 }
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
