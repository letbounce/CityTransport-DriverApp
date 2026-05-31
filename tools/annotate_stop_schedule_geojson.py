#!/usr/bin/env python3
"""
Додає в assets/map/stops_*_route.geojson поле planned_time (HH:MM) для кожної зупинки.

Час орієнтовний: накопичувально від часу відправлення з першої зупинки лінії
за відстанями між сусідніми точками та середньою швидкістю руху в місті.
Це не офіційний розклад «Київпастранс», а узгоджений ілюстративний графік для UI у дорожньому листі.

Запуск з кореня репозиторію: python tools/annotate_stop_schedule_geojson.py
"""
from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any, Dict, List, Tuple

ROOT = Path(__file__).resolve().parents[1]
MAP_DIR = ROOT / "app" / "src" / "main" / "assets" / "map"

ROUTES: Tuple[str, ...] = ("7", "11", "18", "24", "50", "55", "62", "101", "114", "115")

# Перша зупинка рейсу — умовний час відправлення (год, хв) узгоджено з «ранковими» рейсами.
FIRST_STOP_CLOCK: Dict[str, Tuple[int, int]] = {
    "7": (6, 20),
    "11": (5, 45),
    "18": (6, 5),
    "24": (6, 10),
    "50": (5, 55),
    "55": (6, 8),
    "62": (6, 15),
    "101": (6, 0),
    "114": (5, 58),
    "115": (6, 12),
}

AVG_SPEED_KMH = 13.5  # середня комерційна швидкість автобуса у місті (узагальнено)
LEG_MIN_MINUTES = 2
LEG_MAX_MINUTES = 14
EXTRA_DWELL_MINUTES = 0.9  # посадка-висадка між зупинками


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    h = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(min(1.0, math.sqrt(h)))


def minutes_from_midnight(hour: int, minute: int) -> int:
    return max(0, min(23, hour)) * 60 + max(0, min(59, minute))


def fmt_hhmm(total_min: int) -> str:
    total_min %= 24 * 60
    return f"{total_min // 60:02d}:{total_min % 60:02d}"


def leg_duration_minutes(distance_km: float) -> int:
    drive = (distance_km / AVG_SPEED_KMH) * 60.0 if distance_km > 0 else LEG_MIN_MINUTES
    raw = drive + EXTRA_DWELL_MINUTES
    return int(round(max(LEG_MIN_MINUTES, min(LEG_MAX_MINUTES, raw))))


def annotate_file(route_ref: str) -> int:
    path = MAP_DIR / f"stops_{route_ref}_route.geojson"
    if not path.exists():
        print(f"skip missing {path.name}")
        return 0

    data = json.loads(path.read_text(encoding="utf-8"))
    features: List[Dict[str, Any]] = list(data.get("features") or [])
    points: List[Dict[str, Any]] = []
    for f in features:
        geom = f.get("geometry") or {}
        if geom.get("type") != "Point":
            continue
        coords = geom.get("coordinates") or []
        if len(coords) < 2:
            continue
        lon, lat = float(coords[0]), float(coords[1])
        props = dict(f.get("properties") or {})
        points.append({"feat": f, "lat": lat, "lon": lon, "props": props})

    if not points:
        print(f"warn no points {path.name}")
        return 0

    sh, sm = FIRST_STOP_CLOCK.get(route_ref, (6, 0))
    cursor = minutes_from_midnight(sh, sm)

    for i, item in enumerate(points):
        item["props"]["planned_time"] = fmt_hhmm(cursor)
        item["feat"]["properties"] = item["props"]
        if i + 1 < len(points):
            nxt = points[i + 1]
            d_km = haversine_km(item["lat"], item["lon"], nxt["lat"], nxt["lon"])
            cursor += leg_duration_minutes(d_km)

    data["features"] = features
    data["schedule_note"] = (
        "planned_time: illustrative cumulative schedule from distances · not official Kyivpastrans timetable"
    )
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"OK {path.name}: {len(points)} stops")
    return len(points)


def main() -> None:
    MAP_DIR.mkdir(parents=True, exist_ok=True)
    total = 0
    for ref in ROUTES:
        total += annotate_file(ref)
    print(f"Done. Annotated stops total: {total}")


if __name__ == "__main__":
    main()
