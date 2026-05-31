# Wireframe Navigation Map

## Screen Flow

1. `LoginScreen`
   - Trigger: tap `УВІЙТИ`
   - Success: navigate to `RouteDashboardScreen`
   - Failure: stay on `LoginScreen` and show error message
2. `RouteDashboardScreen`
   - Trigger: tap `ОТРИМАТИ ДОРОЖНІЙ ЛИСТ`
   - Success: create waybill and navigate to `ActiveTripScreen`
   - If active waybill exists on load: navigate directly to `ActiveTripScreen`
3. `ActiveTripScreen`
   - Trigger: tap `ІНЦИДЕНТ` -> navigate to `IncidentReportScreen`
   - Trigger: tap `ЗАВЕРШИТИ РЕЙС` -> complete waybill and navigate to `RouteDashboardScreen`
4. `IncidentReportScreen`
   - Trigger: tap `ВІДПРАВИТИ ЗВІТ`
   - Success: return to `ActiveTripScreen`
   - Failure: stay on `IncidentReportScreen` and show error message

## Route Definitions (planned)

- `login`
- `dashboard`
- `active_trip/{waybillId}`
- `incident/{waybillId}`

## Input/Output Contract by Screen

- `LoginScreen`: outputs `jwtToken`, `driverId`, `driverName`
- `RouteDashboardScreen`: outputs `waybillId`
- `ActiveTripScreen`: outputs `complete` event or `openIncident` event
- `IncidentReportScreen`: outputs `incidentSaved` event
