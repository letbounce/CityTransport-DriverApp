#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000/api}"

LOGIN_JSON=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"driver_id":"DRV-1042","password":"password123"}')

TOKEN=$(echo "$LOGIN_JSON" | node -e "const fs=require('fs');const j=JSON.parse(fs.readFileSync(0,'utf8'));console.log(j.token||'')")
if [ -z "$TOKEN" ]; then
  echo "Login failed"
  exit 1
fi

ROUTES_JSON=$(curl -s -X GET "${BASE_URL}/routes" -H "Authorization: Bearer ${TOKEN}")
ROUTE_ID=$(echo "$ROUTES_JSON" | node -e "const fs=require('fs');const j=JSON.parse(fs.readFileSync(0,'utf8'));console.log(j[0]?._id||'')")
if [ -z "$ROUTE_ID" ]; then
  echo "No route found"
  exit 1
fi

WAYBILL_JSON=$(curl -s -X POST "${BASE_URL}/waybills" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"route_id\":\"${ROUTE_ID}\",\"vehicle_id\":\"BUS-007\"}")

WAYBILL_ID=$(echo "$WAYBILL_JSON" | node -e "const fs=require('fs');const j=JSON.parse(fs.readFileSync(0,'utf8'));console.log(j._id||'')")
if [ -z "$WAYBILL_ID" ]; then
  echo "Waybill create failed"
  exit 1
fi

curl -s -X POST "${BASE_URL}/telemetry" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"waybill_id\":\"${WAYBILL_ID}\",\"locations\":[{\"lat\":50.4501,\"lng\":30.5234,\"speed_kmh\":35}]}" > /dev/null

curl -s -X POST "${BASE_URL}/incidents" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"waybill_id\":\"${WAYBILL_ID}\",\"type\":\"breakdown\",\"description\":\"curl smoke incident\",\"location\":{\"lat\":50.4501,\"lng\":30.5234}}" > /dev/null

curl -s -X PATCH "${BASE_URL}/waybills/${WAYBILL_ID}/complete" \
  -H "Authorization: Bearer ${TOKEN}" > /dev/null

echo "Smoke flow completed with waybill ${WAYBILL_ID}"
