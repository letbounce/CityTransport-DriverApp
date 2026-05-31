# Контекст для написання звіту з бакалаврської роботи (RoutePulse)

> **Призначення файлу:** цей документ описує програмний продукт **RoutePulse** і всі 20 діаграм/схем у `docs/diagrams/`, щоб інша мовна модель могла без доступу до репозиторію підготувати текст звіту з ВБП (пояснювальна записка).  
> **Тема ВБП:** «Інформаційна система диспетчеризації міського пасажирського транспорту з використанням мобільних платформ».  
> **Об'єкт розробки:** інформаційна підсистема «Мобільний термінал водія» (Android-клієнт + REST API + MongoDB).  
> **Джерело діаграм:** SVG/PNG у `docs/diagrams/`; індекс — `docs/diagrams/README.md`.

---

## 1. Загальна характеристика системи

### 1.1. Призначення та контекст експлуатації

**RoutePulse** — клієнт-серверний прототип інформаційної системи диспетчеризації для **автотранспортного підприємства (АТП)** міського пасажирського транспорту. Система автоматизує робочі процеси **водія** під час виконання рейсу:

- авторизація в системі;
- отримання (створення) електронного **дорожнього листа** (waybill);
- ведення **активного рейсу** з фоновим GPS-трекінгом;
- **реєстрація інцидентів** (ДТП, поломка, затор тощо) з фотофіксацією та геоприв'язкою;
- **завершення або архівування** рейсу;
- перегляд **карти маршруту** та **журналу інцидентів**;
- передача **телеметрії** на сервер з підтримкою **offline-черги** (Room + WorkManager).

**Споживачі даних на стороні АТП** (не реалізовані окремим веб-клієнтом у прототипі, але відображені в організаційних та інформаційних схемах):

- диспетчерська служба (~5–6 АРМ + 1–2 великих моніторинг-дисплеї);
- аналітик (KPI, телеметрія);
- диспетчер з безпеки руху (інциденти).

**Масштаб (для звіту):** ~150 водіїв, ~150 мобільних терміналів, 5–6 диспетчерських АРМ.

### 1.2. Архітектурний підхід

| Рівень | Технології | Розташування в коді |
|--------|------------|---------------------|
| **Клієнт (Mobile Tier)** | Kotlin, Jetpack Compose, MVVM, Clean Architecture | `app/src/main/java/com/example/cityapp/` |
| **Сервер застосунків (Application Server Tier)** | Node.js 18+, Express 4, Mongoose 7 | `server/` |
| **Дані (Data Tier)** | MongoDB 6.x, 6 колекцій | `server/models/` |

**Патерни:** REST API, JWT-автентифікація, offline-first для телеметрії, Foreground Service для GPS, Repository + UseCase на клієнті.

### 1.3. Структура репозиторію

```
AndroidStudioProjects/
├── app/                    # Android-модуль RoutePulse
├── server/                 # Node.js API + Mongoose
├── docs/
│   ├── diagrams/           # 20 SVG + PNG (рисунки звіту)
│   ├── demo-script-5-7-min.md
│   ├── integration-smoke-test-protocol.md
│   ├── ux-ergonomics-checklist.md
│   └── wireframe-navigation-map.md
└── README.md
```

### 1.4. Запуск та демонстрація

**Бекенд:**
```bash
cd server && cp .env.example .env && npm install && npm run seed && npm start
```
URL за замовчуванням: `http://localhost:3000` (емулятор Android: `http://10.0.2.2:3000/`).

**Тестові облікові дані (seed):** `DRV-1042` / `password123`.

**Основні API (захищені JWT, крім login):**
- `POST /api/auth/login`
- `GET /api/routes`, `GET /api/vehicles`
- `POST /api/waybills`, `GET /api/waybills/active`, `PATCH /api/waybills/:id/complete`, `POST /api/waybills/:id/archive`
- `POST /api/incidents`, `GET /api/incidents`, `PATCH /api/incidents/:id`
- `POST /api/telemetry`
- `GET /api/map/live-trips`

---

## 2. Android-клієнт (детально)

### 2.1. Шари Clean Architecture

| Шар | Пакет | Відповідальність |
|-----|-------|------------------|
| **presentation** | `presentation/` | Compose-екрани, ViewModel, навігація (`AppNavGraph.kt`) |
| **domain** | `domain/` | Моделі (`Models.kt`), інтерфейси репозиторіїв, UseCase |
| **data** | `data/` | Retrofit `ApiService`, DTO, `RepositoryImpl`, Room, DataStore |

**DI:** `ServiceLocator` (простий locator, без Hilt у прототипі).

### 2.2. Екрани та навігація

Маршрути (`Destinations` у `AppNavGraph.kt`):

| Екран | Route | Призначення |
|-------|-------|-------------|
| `LoginScreen` | `login` | Вхід водія |
| `HomeMenuScreen` | `home` | Головне меню після логіну |
| `RouteDashboardScreen` | `dashboard` | Список маршрутів, старт рейсу |
| `ActiveTripScreen` | `active_trip/{waybillId}` | Активний рейс, завершення |
| `IncidentReportScreen` | `incident/{waybillId}` | Новий інцидент |
| `IncidentsListScreen` | `incidents` | Журнал інцидентів |
| `IncidentEditScreen` | `incident_edit/{incidentId}` | Редагування з історією версій |
| `TripMapHubScreen` | `trip_map` | Вибір маршруту на карті |
| `TripMapRouteScreen` | `trip_map_route/{routeId}` | Карта конкретного маршруту |

**Стартовий екран:** `Login`. Після успішного логіну — `Home`.

### 2.3. Ключові доменні моделі (`Models.kt`)

- **Driver** — `driverId`, `fullName`
- **Route** — `id`, `routeNumber`, `routeName`, `stops: List<Stop>`
- **Stop** — `stopNumber`, `name`, `plannedTime`, `lat`, `lng`
- **Vehicle** — `vehicleId`, `label`, `plateNumber`
- **Waybill** — `id`, `routeId`, `routeNumber`, `status`, `vehicleId`, `startedAt`, `completedAt`, soft-delete поля
- **IncidentItem** — тип, опис, координати, `photoUrl`, `status`, `versionHistoryCount`, soft-delete
- **AuthSession** — `token`, `driver`
- **NewIncidentPayload** / **IncidentUpdatePayload** — тіла запитів, у т.ч. `photoBase64`
- **LiveTripMarker** — маркер для live-карти всіх рейсів

### 2.4. Фонові процеси та безпека

| Компонент | Файл / технологія | Роль |
|-----------|-------------------|------|
| **LocationTrackingService** | `service/`, FGS `location` | GPS кожні ~5 с під час активного рейсу |
| **TelemetrySyncWorker** | `work/`, WorkManager | Періодична відправка батчів (~15 хв, max 100 точок) |
| **LocalQueue (Room)** | Room DB | Offline-черга координат |
| **SessionStore / DataStore** | preferences | Збереження сесії |
| **EncryptedSharedPreferences** | security-crypto | JWT-токен |
| **TokenProvider + OkHttp Interceptor** | data layer | `Authorization: Bearer` на запити |

**Дозволи (`AndroidManifest.xml`):** `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `CAMERA`, `INTERNET`.

**Зовнішні залежності:** OSMdroid (карти), Play Services Location, Retrofit, Coil, GeoJSON у `assets/map/`.

### 2.5. Обробка фото інциденту

1. Камера: `ACTION_IMAGE_CAPTURE` → `Bitmap`.
2. Клієнт: JPEG → **Base64** у полі `photo_base64` JSON.
3. Сервер: `saveBase64ToJpeg` → файл `/server/uploads/<uuid>.jpg`, у БД — `photo_url`.

*(У звіті можна зазначити, що це JSON+Base64, а не multipart/form-data — відповідає sequence-діаграмі 06.)*

---

## 3. Серверна частина (детально)

### 3.1. Точка входу (`server/index.js`)

- Middleware: `cors`, `express.json({ limit: "12mb" })` (для Base64-фото).
- Статика: `/uploads` — збережені JPEG інцидентів.
- Маршрути з `requireAuth` (JWT): routes, waybills, telemetry, incidents, map, vehicles.
- `GET /health` — перевірка живості.

### 3.2. Контролери

| Контролер | Основні операції |
|-----------|------------------|
| `authController` | login, bcrypt, jwt.sign (HS256, 8h) |
| `waybillController` | start, active, complete, archive, list |
| `incidentController` | create (фото), update, archive, version_history |
| `telemetryController` | batch insert у колекцію `telemetry` |
| `routeController` | список маршрутів з embedded stops |
| `mapController` | `live-trips` — маркери активних рейсів |
| `vehicleController` | довідник ТЗ |

### 3.3. MongoDB — 6 колекцій

База: **`routepulse_db`**.

| Колекція | Ключові поля |
|----------|--------------|
| `drivers` | `driver_id` (unique), `password_hash`, `role`, `is_active` |
| `vehicles` | `vehicle_id` (unique), `plate_number` |
| `routes` | `route_number`, **embedded** `stops[]` |
| `waybills` | `driver_id`, `route_id` (ObjectId ref), `status` enum, soft-delete |
| `incidents` | `waybill_id`, `type` enum, `location` embedded, `photo_url`, `version_history[]` |
| `telemetry` | `waybill_id`, `bucket_start`, `locations[]` (lat, lng, speed_kmh, timestamp) |

**Статуси waybill:** `assigned`, `in_progress`, `completed`, `cancelled`.  
**Типи incident:** `accident`, `breakdown`, `traffic_jam`, `other`.  
**Коди архівації (5 значень):** `error`, `duplicate`, `wrong_data`, `equipment_issue`, `other` — `server/constants/archiveReasons.js`.

---

## 4. Узгодженість артефактів (для перехресних посилань у звіті)

Усі діаграми побудовані на **єдиному словнику сутностей**:

| Поняття | Де зустрічається |
|---------|------------------|
| Водій (~150) | 02, 04–08, 11, 12, 13, 17, 18, 19 |
| Диспетчерська служба | 01, 11, 15, 17, 19 |
| Waybill | 01, 03, 04, 07, 09, 10, 13, 15, 16 |
| Incident | 01, 03, 06, 09, 10, 13, 14, 15 |
| Telemetry / offline | 01, 03, 08, 09, 10, 12, 13, 15, 19 |
| JWT / HTTPS / EncryptedPrefs | 05, 13, 16, 18, 19, 20 |
| 6 колекцій MongoDB | 03, 04–08, 09, 10, 15, 16, 19, 20 |

**Нумерація рисунків для Word** — див. таблицю в `docs/diagrams/README.md` (розділи 1.1, 2.x, 3.x).

---

## 5. Детальний опис кожної діаграми

Для кожного рисунка нижче: **файл**, **підпис у Word**, **зміст SVG**, **текст для пояснювального розділу**.

---

### Рис. 1.1 — `11-org-structure.svg` / `.png`

**Підпис:** Організаційна структура АТП з виділенням диспетчерської служби як об'єкта впровадження.

**Що зображено:**
- Ієрархія АТП: **Директор** → заступники (експлуатація, технічні питання, головний бухгалтер).
- Під заступником з експлуатації: **Служба перевезень**, **ДИСПЕТЧЕРСЬКА СЛУЖБА** (виділена жирною рамкою — основний користувач RoutePulse), **Технічна служба**.
- Під диспетчерською: старший диспетчер зміни, диспетчер-маршрутник, диспетчер з безпеки руху, аналітик.
- Під службою перевезень: маршрутні майстри, **Водії (~150)** — користувачі мобільного терміналу.
- Блок **«Інформаційні потоки навколо RoutePulse»**: входи (GPS, дорожні листи, інциденти з фото, облікові дані, маршрути) та виходи (live-карта, журнал інцидентів, звіт за зміну, KPI, архів).

**Як використовувати у звіті:** розділ 1.1 «Характеристика об'єкта проєктування» — обґрунтування вибору диспетчерської служби та водіїв як зацікавлених сторін.

---

### Рис. 2.1 — `01-information-model.svg`

**Підпис:** Інформаційна модель розв'язання задачі диспетчеризації міського пасажирського транспорту (РД 50-34.698-90).

**Що зображено (потоки даних):**

**Входи в підсистему RoutePulse (центральний блок):**
- З **ІС «Кадри»** → БД «Водії» → `driver_id`, credentials.
- З **ІС «Диспетчерська»** → БД «Маршрути, зупинки, ТЗ» → `route_number`, stops, GeoJSON.
- З **GPS-модуля смартфона** → «Поточні геодані» → `lat`, lng, speed, timestamp.

**Функції RoutePulse (перелік у блоці):** авторизація/JWT, ведення дорожнього листа, GPS-трекінг, реєстрація інцидентів, карта маршрутів; технології: Kotlin/Compose, REST, WorkManager, DataStore, OSMdroid, FileProvider, FGS.

**Виходи:**
- «Звіт про інцидент» → **Екран диспетчера** (тип, опис, фото).
- БД «Виконані рейси» (waybills) → **Серверна БД MongoDB** (`route`, `status`, `started_at`).
- БД «Журнал телеметрії» → **Підсистема аналітики** (треки, KPI).

**Текст для звіту:** пояснити інформаційні об'єкти та напрямки обміну між зовнішніми джерелами, мобільним терміналом і центральним сховищем.

---

### Рис. 2.2 — `02-use-case-diagram.svg`

**Підпис:** Діаграма прецедентів інформаційної підсистеми «Мобільний термінал водія».

**Актори:**
- **Водій** (ліворуч) — ініціює більшість сценаріїв.
- **Серверна підсистема** (праворуч) — відповідає на запити API.

**Прецеденти (всередині межі «Мобільний термінал водія»):**
1. Авторизуватися в системі
2. Отримати дорожній лист
3. Виконувати активний рейс
4. **Передавати GPS-телеметрію** — зв'язок **`«include»`** від прецеденту 3 (телеметрія обов'язково супроводжує активний рейс)
5. Зареєструвати інцидент
6. Завершити / архівувати рейс

**Зв'язки:** водій → 1,2,3,5,6; сервер → 1,2,4,5,6 (прецедент 3 — переважно локальний UX на пристрої).

**Відповідність UC у трасуванні (рис. 2.12):** UC-01…UC-06.

---

### Рис. 2.3 — `14-algorithm-incident.svg`

**Підпис:** Схема алгоритму обробки інциденту (за ДСТУ 19.701-90).

**Нотація:** овал — початок/кінець; прямокутник — процес; ромб — рішення; паралелограм — ввід/вивід; D-форма — носій даних.

**Алгоритм (кроки):**
1. **Початок** → **Ввід:** тип, опис, координати, час, зупинка, фото.
2. **Робити фото?** [так] → камера `ACTION_IMAGE_CAPTURE` → Bitmap→JPEG→Base64; [ні] — далі.
3. **description.trim() не порожній?** [ні] → вивід помилки «Заповніть опис інциденту».
4. **selectedStop обрано?** [ні] → «Оберіть зупинку маршруту».
5. Сформувати **NewIncidentPayload** → **POST /api/incidents** (HTTPS+JWT).
6. **HTTP 201?** [ні] → «Помилка зв'язку з сервером» → **Кінець (помилка)**.
7. [так] → Server `Incident.create()` → MongoDB; оновити VM `submitted=true`; `popBack()` на ActiveTripScreen → **Кінець (успіх)**.

**Текст для звіту:** розділ 2.2.2 — алгоритмічне забезпечення обробки інциденту.

---

### Рис. 2.4 — `03-class-diagram.svg`

**Підпис:** Діаграма класів інформаційної підсистеми (3-tier архітектура).

**Три пакети (рамки):**

**«Data Tier»** — доменні класи з атрибутами та методами:
- `Driver`, `Route`, `Waybill`, `Stop`, `IncidentItem`
- **Композиції (закритий ромб):** Driver 1 — 0..* Waybill; Route 1 — 0..* Waybill; Waybill 1 — 0..* IncidentItem; Route 1 — 0..* Stop

**«Application Server Tier»** — контролери (кола): AuthController, WaybillController, TelemetryController, IncidentController, RouteController.

**«Client Tier» (Android · Jetpack Compose)** — екрани: LoginScreen, RouteDashboardScreen, ActiveTripScreen, IncidentReportScreen, TripMapScreen.

**Залежності (пунктир, відкрита стрілка):** екрани → контролери → доменні класи.

**Текст для звіту:** логічна архітектура ПЗ; узгодження з ER-моделями та sequence-діаграмами.

---

### Рис. 2.5 — `04-sequence-diagram.svg`

**Підпис:** Діаграма послідовності — «Створення нового дорожнього листа».

**Учасники:** Водій → RouteDashboardScreen → DashboardViewModel → WaybillRepository → ApiService → WaybillController → MongoDB.

**Повідомлення (1–12):**
1. tap «Отримати дорожній лист»
2. `onStartTrip(routeId, vehicleId)`
3. `startWaybill(...)`
4. `POST /api/waybills`
5. HTTPS POST {JWT, body}
6. `insertOne(waybill)`
7. `{_id, status:"in_progress"}`
8. 201 Created WaybillDto → domain Waybill → Result.Success → emit state → **navigateTo(ActiveTripScreen)**

---

### Рис. 2.6 — `05-sequence-login.svg`

**Підпис:** Авторизація водія (Login → JWT).

**Учасники:** Водій → LoginScreen → LoginViewModel → AuthRepository → ApiService → AuthController → MongoDB (drivers).

**Ключові кроки:** POST login → findOne driver → **bcrypt.compare** → **jwt.sign (8h)** → AuthSession → `sessionStore.save`, `tokenProvider.setToken()` → navigate HomeMenu.

**Гілка помилки (alt):** невірні credentials / !is_active → 401 → помилка «Помилка авторизації», без навігації.

---

### Рис. 2.7 — `06-sequence-incident-photo.svg`

**Підпис:** Реєстрація інциденту з фотофіксацією.

**Фаза А:** tap «Зробити фото» → ACTION_IMAGE_CAPTURE → setPhoto → Base64.

**Фаза Б:** «ВІДПРАВИТИ ЗВІТ» → submit → POST /api/incidents → сервер `saveBase64ToJpeg` → `Incident.create` → 201 → popBack.

**Alt:** порожній опис / зупинка / пошкоджений Base64 — валідація на VM; 400 від Controller.

---

### Рис. 2.8 — `07-sequence-complete-waybill.svg`

**Підпис:** Завершення / архівування дорожнього листа.

**Сценарій завершення:** підтвердження → PATCH `/api/waybills/{id}/complete` → status=completed, completed_at → stopLocationTrackingService → HomeMenu.

**Alt (примітка на діаграмі):** архівування — POST `/archive` з `reason_code`, `reason_note`; soft-delete поля; валідація проти 5 кодів.

---

### Рис. 2.9 — `08-sequence-telemetry-sync.svg`

**Підпис:** Передача GPS-телеметрії з offline-чергою (WorkManager).

**Фаза А:** активний рейс → startForegroundService → FusedLocation **кожні 5 с** → insert у **LocalQueue (Room)**.

**Фаза Б (кожні 15 хв):** TelemetrySyncWorker → getPendingBatch(max=100) → POST /api/telemetry → Telemetry.create → deleteSynced.

**Alt:** немає мережі / 5xx → `Result.retry()` з exponential backoff (мін. 30 с), черга не очищається.

---

### Рис. 2.10 — `12-activity-parallel.svg`

**Підпис:** Діаграма діяльності — паралельні процеси під час активного дорожнього листа.

**Після створення waybill (in_progress):** **fork** на три **loop** (поки status=in_progress):

| Потік | Зміст |
|-------|--------|
| **Loop A** | LocationTrackingService: GPS → LocalQueue |
| **Loop B** | WorkManager 15 хв: batch → POST telemetry → delete або retry |
| **Loop C** | UI ActiveTripScreen: дії водія — [Інцидент] → IncidentReportScreen; [Завершити] → підтвердження |

**join** → PATCH complete → зупинити LocationTrackingService.

**Текст для звіту:** паралельність фонового трекінгу, синхронізації та UI — обґрунтування Activity-діаграми замість одного лінійного потоку.

---

### Рис. 2.12 — `13-traceability.svg`

**Підпис:** Діаграма трасування вимог до системних компонентів.

**Три стовпці:** requirements → use cases → components.

**Функціональні вимоги (FR-01…FR-10):**
- FR-01 Авторизація → UC-01 → C1 (LoginScreen+VM+AuthRepository)
- FR-02 Маршрути → UC-02 → C2 (Dashboard+Waybill+Route repos)
- FR-03 Створення waybill → UC-02
- FR-04 Завершення рейсу → UC-06 → C3 (ActiveTrip)
- FR-05 Архівування з причиною → UC-06
- FR-06 Інцидент з фото → UC-05 → C4
- FR-07 Редагування інциденту → C4 (IncidentEdit)
- FR-08 Карта маршруту → C5 (TripMap+OSMdroid+GeoJSON)
- FR-09 GPS-трекінг → UC-04 → C6 (LocationTrackingService)
- FR-10 Live-карта → C5 + C9 (map API)

**Нефункціональні (NFR-01…04):**
- NFR-01 Offline sync → C7 (Worker+LocalQueue)
- NFR-02 JWT, HTTPS, EncryptedStorage → C8, C9
- NFR-03 Ергономіка UI (16sp+, 64dp+) → C1–C5
- NFR-04 Продуктивність (GPS 5с, batch 15хв) → C6, C7

**Компоненти C8–C9:** ApiService+TokenProvider; Node.js+Express+Mongoose+MongoDB.

**Легенда:** «satisfy» (FR→UC), «realize» (UC→C), «constrain» (NFR→C, cross-cutting).

---

### Рис. 3.1 — `09-er-infological.svg`

**Підпис:** Інфологічна модель БД (Crow's foot).

**Сутності та зв'язки:**
- DRIVER — «виконує» → WAYBILL (1:N)
- ROUTE — «обслуговує» → WAYBILL; «містить» → STOP (1:N, embedded логічно)
- VEHICLE — «експлуатується» → WAYBILL
- WAYBILL — «має» → INCIDENT (1:N)
- WAYBILL — «генерує треки» → TELEMETRY (1:N)

**Позначення:** PK жирний, FK курсив; Crow's foot для 1..N.

---

### Рис. 3.2 — `10-er-datalogical.svg`

**Підпис:** Даталогічна модель — фізична схема MongoDB.

**6 колекцій** з BSON-типами, індексами [PK][UQ][IX][ref→], embedded масивами (зелені блоки):
- `routes.stops[]` без власного `_id`
- `incidents.version_history[]` зі snapshot
- `telemetry.locations[]` — бакет ~100 точок синхронізації

**Примітки:** bcrypt cost 10; enum-валідатори Mongoose; посилання ObjectId на waybills/routes.

---

### Рис. 3.3 — `15-info-support-scheme.svg`

**Підпис:** Загальна схема інформаційного забезпечення (РД 50-34.698-90, розд. 5).

**Структура зверху вниз:**
1. **Джерела:** водії (~150 Android), адмін/диспетчер (seed, Compass), GPS+OSM.
2. **Засоби збору:** Compose-екрани, admin CLI, LocationService+OSMdroid+Room.
3. **Перетворення та контроль:** клієнтська/серверна валідація, JWT, класифікатори ID та enum.
4. **Сховище:** MongoDB `routepulse_db` (6 колекцій) + `/uploads` + Room + EncryptedPrefs.
5. **Засоби видачі:** REST GET, PdfExportWriter, Live-карта, IncidentsList.
6. **Споживачі:** диспетчери, аналітик, безпека руху, СТО.

**Бічний блок «Класифікатори»:** driver_id DRV-NNNN, vehicle KP-NNNN, статуси, типи інцидентів, archive_reason_code (5 значень).

---

### Рис. 3.4 — `16-automation-scheme.svg`

**Підпис:** Загальна схема автоматизації (Android · API · MongoDB).

**Три tier-и:**
- **mobile tier:** UI → ViewModels/Repos → Data (Retrofit, Room, DataStore) → Background (FGS, Worker) → Hardware.
- **application server tier:** Express routes → Middleware → Controllers → Mongoose → File storage.
- **data tier:** routepulse_db, 6 колекцій, WiredTiger, mongodump.

**Зовнішні сервіси:** GPS, OSM tiles.

**Зв'язки:** HTTPS REST+JWT; TCP 27017 Mongoose/BSON.

**Примітка внизу:** узгодженість з рис. 2.4, 3.1–3.2, 2.5–2.9.

---

### Рис. 3.5 — `17-ktz-structure.svg`

**Підпис:** Структура комплексу технічних засобів (КТЗ).

**Блоки:**
- **Серверна інфраструктура:** App Server (4 vCPU, 8 GB), DB Server (16 GB, RAID), File storage uploads, Backup NAS.
- **АРМ диспетчерів:** ПК i5/16GB/24″, 5–6 АРМ, 55″ 4K монітор, принтер, UPS, гарнітура.
- **Мобільні термінали:** Android 7+, GPS, камера, ~150+15 резерв, аксесуари в кабіні, MDM опційно.
- **Мережа:** FTTH 100 Mbps, firewall, L2 switch, WiFi 6 AP у депо.
- **Інженерне забезпечення:** UPS серверної, дизель-генератор, кондиціонування 24°C.

**Підсумок:** 2 сервера + NAS + 5–6 АРМ + ~165 смартфонів + мережа.

---

### Рис. 3.6 — `18-driver-workstation.svg`

**Підпис:** Схема АРМ водія.

**Контекст кабіни ТЗ:** водій, утримувач на лобовому склі, зарядка 12V→USB, кабель, бортова мережа, гарнітура (опц.).

**Смартфон:** апаратні модулі (екран, GPS L1, камера, АКБ, CPU/RAM, WiFi/LTE/BT) + програмний стек RoutePulse (екрани, FGS, Worker, бібліотеки, Android OS, локальні дані).

**Зовнішні канали:** GPS, 4G/LTE, API server, OSM, WiFi депо, MDM.

**Висновок для звіту:** АРМ водія — переносний комплекс «смартфон + кріплення + живлення + RoutePulse».

---

### Рис. 3.7 — `19-network-scheme.svg`

**Підпис:** Схема мережі передачі даних.

**Зона 1 — Internet:** ~150 смартфонів → 4G/LTE (оператори UA) → Internet (TLS, OSM, FCM).

**Зона 2 — DMZ/Edge:** ISP FTTH 100 Mbps → firewall (NAT 443, TLS, VPN WireGuard) → WiFi AP «RoutePulse-AT» (WPA2-Enterprise).

**Зона 3 — LAN 192.168.10.0/24:** L2 switch VLAN 10 → app server :3000, db :27017 (internal), 5–6 АРМ, NAS, принтер.

**Об'ємно-часові характеристики (бічний блок):**
- GPS: 1 точка/5 с, ~5760/зміну/водій, батч 100, ~28 MB/добу телеметрії на fleet.
- Транзакції: ~150 login, ~200 waybill, ~30–50 інцидентів/добу.
- Фото: ~30–50 MB/добу.

**ACL:** MongoDB 27017 не виставлено в Internet; SSH лише з admin VPN.

---

### Рис. 3.8 — `20-software-structure.svg`

**Підпис:** Структура програмного забезпечення RoutePulse.

**4 групи:**
1. **Системне ПЗ:** Ubuntu, Node.js, MongoDB, PM2, nginx; Android ART, Play Services; UFW, Fail2ban, Prometheus/Grafana, mongodump cron.
2. **RAD/CASE:** Android Studio, Cursor/VS Code, Gradle, npm; PlantUML/draw.io/Inkscape/Compass; JUnit/Espresso/smoke.js; Git.
3. **Прикладне Android:** RoutePulse (Compose, Clean Arch, UseCases, FGS, Worker); бібліотеки Retrofit, Room, OSMdroid…; 9 екранів; APK/AAB.
4. **Прикладне Backend + документація:** Express, Mongoose, bcrypt, JWT, controllers; seed/smoke scripts; README, docs/*.md, diagrams, KDoc.

---

## 6. Рекомендована структура звіту ВБП (зв'язок з артефактами)

| Розділ звіту | Рисунки | Що описувати |
|--------------|---------|--------------|
| 1.1 Об'єкт проєктування | 1.1 | АТП, диспетчерська, водії |
| 2.2.1 Інф. модель задачі | 2.1 | Потоки даних RoutePulse |
| 2.2.2 Алгоритми | 2.3 | Алгоритм інциденту |
| 2.3 Проєктування ПЗ | 2.2, 2.4, 2.5–2.9, 2.10, 2.12 | UML + трасування |
| 3.1 Інф. забезпечення | 3.1, 3.2, 3.3 | БД + ІЗ |
| 3.2 Технічне / автоматизація | 3.4, 3.5, 3.6, 3.7 | КТЗ, АРМ, мережа |
| 3.3 Програмне забезпечення | 3.8 | Структура ПЗ |
| Практична частина | скріншоти APK + 2.5–2.9 | Демо-сценарій, контрольний приклад |

---

## 7. Контрольний приклад та тестування (для розділу «Перевірка»)

**Сценарій демо (5–7 хв, `docs/demo-script-5-7-min.md`):**
1. Login DRV-1042
2. Dashboard → старт waybill
3. Інцидент з фото
4. Завершення рейсу
5. Перевірка записів у MongoDB

**Smoke:** `npm run smoke`, `scripts/smoke-curl.sh` — див. `docs/integration-smoke-test-protocol.md`.

**UX/NFR:** `docs/ux-ergonomics-checklist.md` — шрифт ≥16 sp, кнопки ≥64 dp (відповідає NFR-03 на рис. 2.12).

---

## 8. Стиль діаграм (для узгодженості підписів у тексті)

- Шрифт: Segoe UI / Calibri / Arial.
- Потоки даних: `#dfeaf7`; БД: `#e8eef6`; UML-класи/клієнт: `#fdf6e3`; сервер sequence: `#eef3f9`; embedded/сховище: `#f0f6ec`; помилки/DMZ: `#fdf3f3`.
- Редагування SVG: **не** використовувати частковий search-replace кирилиці — лише повний перезапис UTF-8 (див. README diagrams).

**Вставка в Word:** PNG з `docs/diagrams/`, ширина ~16 см; підписи — з таблиці в `docs/diagrams/README.md`.

---

## 9. Обмеження прототипу (чесно вказати у звіті)

- Окремий **веб-кабінет диспетчера** не реалізовано — моніторинг передбачено через API/Compass/майбутній веб-клієнт (на схемах — «екран диспетчера»).
- Пакет Android у коді: `com.example.cityapp` (історична назва CityTransport-DriverApp); бренд UI — **RoutePulse**.
- `usesCleartextTraffic=true` у manifest — для локальної розробки HTTP; у продакшені — лише HTTPS (на мережевій схемі — TLS 1.2+).
- Телеметрія: реалізовані FGS + Room + Worker; точні інтервали на діаграмах (5 с / 15 хв) — цільові параметрі проєкту.

---

## 10. Швидкий індекс файлів діаграм

| № | Базове ім'я | Розділ ВБП |
|---|-------------|------------|
| 01 | `01-information-model` | 2.2.1 |
| 02 | `02-use-case-diagram` | 2.3 |
| 03 | `03-class-diagram` | 2.3 |
| 04 | `04-sequence-diagram` | 2.3 |
| 05 | `05-sequence-login` | 2.3 |
| 06 | `06-sequence-incident-photo` | 2.3 |
| 07 | `07-sequence-complete-waybill` | 2.3 |
| 08 | `08-sequence-telemetry-sync` | 2.3 |
| 09 | `09-er-infological` | 3.1 |
| 10 | `10-er-datalogical` | 3.1 |
| 11 | `11-org-structure` | 1.1 |
| 12 | `12-activity-parallel` | 2.3 |
| 13 | `13-traceability` | 2.3 |
| 14 | `14-algorithm-incident` | 2.2.2 |
| 15 | `15-info-support-scheme` | 3.1 |
| 16 | `16-automation-scheme` | 3.2 |
| 17 | `17-ktz-structure` | 3.2 |
| 18 | `18-driver-workstation` | 3.2 |
| 19 | `19-network-scheme` | 3.2 |
| 20 | `20-software-structure` | 3.3 |

---

*Документ згенеровано для супроводу бакалаврського звіту. При розбіжності між цим текстом і кодом пріоритет має **фактична реалізація** в репозиторії та вміст SVG у `docs/diagrams/`.*
