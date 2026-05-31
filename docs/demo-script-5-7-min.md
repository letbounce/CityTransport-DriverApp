# Demo Script (5-7 min)

## 0:00-0:45 Intro

- Explain goal: digital driver terminal for dispatching.
- Show architecture split: Android + Node.js + MongoDB.

## 0:45-1:30 Backend launch

- Show `.env` config and startup command.
- Run seed and show that test data exists.

## 1:30-3:30 Android user flow

1. Login (`DRV-1042` / `password123`)
2. Open dashboard and inspect route stops.
3. Start waybill.
4. Open incident screen and submit incident.
5. Complete trip and return to dashboard.

## 3:30-4:30 Data verification

- Show created waybill in MongoDB.
- Show incident record in MongoDB.

## 4:30-5:30 Technical highlights

- Clean Architecture + MVVM packages.
- Retrofit API layer.
- Room tables for offline-ready flow.
- Foreground service and WorkManager placeholders.

## 5:30-7:00 Q&A reserve

- Explain next increment:
  - telemetry batching finalization
  - full offline sync behavior
  - camera incident attachment
