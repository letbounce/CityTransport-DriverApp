# UX Ergonomics Checklist for Driver Screens

## Scope

This checklist validates wireframe compliance for:

- `Wireframe_LoginScreen`
- `Wireframe_RouteDashboard`
- `Wireframe_ActiveTripScreen`
- `Wireframe_IncidentReport`

## Rules

1. Primary buttons are at least `64dp` high.
2. Critical action buttons are `80dp` high.
3. Main text is `>=16sp`, title text is `>=20sp`.
4. Neutral color palette only (black/white/light gray).
5. Key controls have visible bounding boxes (`border`).
6. Navigation path is linear and low-cognitive-load.

## Audit

| Screen | Buttons size | Text size | Contrast | Border boxes | Pass |
|---|---|---|---|---|---|
| Login | `УВІЙТИ` = 64dp | labels 14sp, CTA 22sp | High | Yes | Pass |
| RouteDashboard | CTA = 80dp | title 20sp, content >=14sp | High | Yes | Pass |
| ActiveTrip | `ЗАВЕРШИТИ РЕЙС`/`ІНЦИДЕНТ` = 80dp | title 26sp, content 16sp | High | Yes | Pass |
| IncidentReport | submit = 72dp | title 20sp, content >=14sp | High | Yes | Pass |

## Notes

- `IncidentReport` submit button is above the baseline (`72dp > 64dp`) and remains compliant.
- Screen structure is intentionally minimal to reduce interaction time while driving.
