# RoutePulse

Android-додаток **RoutePulse** (раніше прототип CityTransport-DriverApp). Клієнт-серверний прототип робочих процесів для водіїв:

- Android app (`/app`) on Kotlin + Jetpack Compose
- Local backend (`/server`) on Node.js + MongoDB

## 1) Run backend

```bash
cd server
cp .env.example .env
npm install
npm run seed
npm start
```

Backend default URL: `http://localhost:3000`

## 2) Run Android app

1. Open project in Android Studio.
2. Build `app` module.
3. Start emulator and run app.

For Android emulator networking, API base URL is configured as:

- `http://10.0.2.2:3000/`

## 3) Backend API quick flow

Endpoints:

- `POST /api/auth/login`
- `GET /api/routes`
- `POST /api/waybills`
- `GET /api/waybills/active`
- `POST /api/telemetry`
- `POST /api/incidents`
- `PATCH /api/waybills/:id/complete`

## 4) Smoke test options

- Node script: `npm run smoke`
- Curl script: `bash scripts/smoke-curl.sh`

## 5) Key Android folders

- `app/src/main/java/com/example/cityapp/data`
- `app/src/main/java/com/example/cityapp/domain`
- `app/src/main/java/com/example/cityapp/presentation`
- `app/src/main/java/com/example/cityapp/service`
- `app/src/main/java/com/example/cityapp/work`
