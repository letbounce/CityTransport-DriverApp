# Integration Smoke Test Protocol

## Preconditions

- MongoDB is running on `localhost:27017`.
- Backend started from `/server`.
- Seed executed at least once.
- Android emulator is running.

## Backend-only smoke

1. `cd server`
2. `npm run smoke`
3. Expect: `Smoke flow completed`

## End-to-end smoke

1. Launch Android app.
2. Login with:
   - Driver ID: `DRV-1042`
   - Password: `password123`
3. Verify dashboard route is visible.
4. Tap `ОТРИМАТИ ДОРОЖНІЙ ЛИСТ`.
5. On active trip screen:
   - Tap `ІНЦИДЕНТ`, submit report.
   - Tap `ЗАВЕРШИТИ РЕЙС`.
6. Verify app returns to dashboard.

## Expected DB side effects

- New `waybills` document created and completed.
- At least one `incidents` document created.
- `telemetry` batches appear after sender integration.
