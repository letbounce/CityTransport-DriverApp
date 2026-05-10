# CityTransport DriverApp — Технічне завдання для Cursor
> Дипломна робота: «Інформаційна система диспетчеризації міського транспорту з використанням мобільних платформ»
> Автор: Коксюк Олег Віталійович, ІН-403

---

## 1. ЗАГАЛЬНИЙ ОПИС ПРОЄКТУ

Потрібно розробити **повну клієнт-серверну систему** — нативний Android-додаток для водіїв міського транспорту + локальний Node.js-бекенд з MongoDB. Додаток є цифровим терміналом водія, який замінює паперові дорожні листи та забезпечує диспетчеризацію в реальному часі.

**Репозиторій:** `CityTransport-DriverApp`
**Гілки:** `main` (стабільні релізи) / `develop` (активна розробка)

---

## 2. ТЕХНОЛОГІЧНИЙ СТЕК

### Android-клієнт (папка `/app`)
- **Мова:** Kotlin
- **UI-фреймворк:** Jetpack Compose (Material Design 3)
- **Архітектура:** MVVM + Clean Architecture (3 шари: UI → ViewModel → Repository)
- **Мережа:** Retrofit 2 + OkHttp (REST API, JSON)
- **Локальна БД (офлайн-кеш):** Room (SQLite)
- **Геолокація:** Android FusedLocationProviderClient (фоновий сервіс)
- **Фонові задачі:** WorkManager (відкладена синхронізація)
- **Автентифікація:** JWT-токени (зберігаються в EncryptedSharedPreferences)
- **DI:** Hilt (Dagger)
- **Асинхронність:** Kotlin Coroutines + Flow

### Бекенд (папка `/server`)
- **Платформа:** Node.js
- **Фреймворк:** Express.js
- **База даних:** MongoDB (локально, через mongoose ODM)
- **Автентифікація:** JWT (бібліотека `jsonwebtoken`)
- **Хешування паролів:** bcrypt
- **Валідація:** express-validator або Joi
- **Змінні середовища:** dotenv (файл `.env`)

### Структура папок проєкту
```
CityTransport-DriverApp/
├── app/                        # Android-додаток (Kotlin)
│   └── src/main/
│       ├── data/               # Repository, API, Room DAO
│       ├── domain/             # Use Cases, моделі
│       └── presentation/       # ViewModels, Compose Screens
├── server/                     # Node.js бекенд
│   ├── models/                 # Mongoose-схеми
│   ├── routes/                 # Express роутери
│   ├── middleware/             # JWT-auth middleware
│   └── index.js                # Точка входу
└── README.md
```

---

## 3. СЕРВЕРНА ЧАСТИНА (Node.js + MongoDB)

### 3.1 Запуск та налаштування

Файл `server/.env`:
```
PORT=3000
MONGO_URI=mongodb://localhost:27017/CityDispatchDB
JWT_SECRET=your_super_secret_key
JWT_EXPIRES_IN=8h
```

Запуск:
```bash
cd server
npm install
node index.js
# або: npm run dev (з nodemon)
```

MongoDB повинна бути запущена локально на порту 27017.

### 3.2 База даних: CityDispatchDB

**Колекція `drivers` — Водії:**
```json
{
  "_id": ObjectId,
  "driver_id": "DRV-1042",
  "full_name": "Коксюк О.В.",
  "phone": "+380991234567",
  "password_hash": "<bcrypt_hash>",
  "role": "driver",
  "is_active": true,
  "created_at": ISODate
}
```

**Колекція `routes` — Маршрути:**
```json
{
  "_id": ObjectId,
  "route_number": "12",
  "route_name": "Центр - Вокзал",
  "vehicle_type": "bus",
  "stops": [
    { "stop_number": 1, "name": "Центральна площа", "planned_time": "08:10", "lat": 50.45, "lng": 30.52 },
    { "stop_number": 2, "name": "Проспект Миру",    "planned_time": "08:18", "lat": 50.46, "lng": 30.53 },
    { "stop_number": 3, "name": "Ринок",            "planned_time": "08:25", "lat": 50.47, "lng": 30.54 },
    { "stop_number": 4, "name": "Вокзал",           "planned_time": "08:33", "lat": 50.48, "lng": 30.55 }
  ],
  "is_active": true
}
```

**Колекція `waybills` — Електронні дорожні листи:**
```json
{
  "_id": ObjectId,
  "driver_id": "DRV-1042",
  "route_id": ObjectId,
  "route_number": "12",
  "status": "in_progress",   // enum: "assigned" | "in_progress" | "completed" | "cancelled"
  "started_at": ISODate,
  "completed_at": null,
  "vehicle_id": "BUS-007",
  "created_at": ISODate
}
```

**Колекція `telemetry` — GPS-треки (Bucket-патерн):**
```json
{
  "_id": ObjectId,
  "waybill_id": ObjectId,
  "driver_id": "DRV-1042",
  "bucket_start": ISODate,
  "locations": [
    { "lat": 50.4501, "lng": 30.5234, "speed_kmh": 35, "timestamp": ISODate },
    { "lat": 50.4512, "lng": 30.5245, "speed_kmh": 40, "timestamp": ISODate }
  ],
  "count": 2
}
```

**Колекція `incidents` — Журнал інцидентів:**
```json
{
  "_id": ObjectId,
  "waybill_id": ObjectId,
  "driver_id": "DRV-1042",
  "type": "breakdown",       // enum: "accident" | "breakdown" | "traffic_jam" | "other"
  "description": "Спустило колесо на вул. Хрещатик",
  "location": { "lat": 50.4501, "lng": 30.5234 },
  "photo_url": null,         // шлях до фото (якщо є)
  "reported_at": ISODate,
  "status": "open"           // "open" | "resolved"
}
```

### 3.3 REST API Endpoints

Базовий URL: `http://localhost:3000/api`

| Метод | URL | Опис | Auth |
|-------|-----|------|------|
| POST | `/auth/login` | Авторизація водія | Ні |
| GET | `/routes` | Список активних маршрутів | JWT |
| GET | `/routes/:id` | Деталі маршруту з зупинками | JWT |
| POST | `/waybills` | Створити дорожній лист (Start Trip) | JWT |
| PATCH | `/waybills/:id/complete` | Завершити рейс (End Trip) | JWT |
| GET | `/waybills/active` | Отримати активний рейс водія | JWT |
| POST | `/telemetry` | Відправити пакет GPS-координат | JWT |
| POST | `/incidents` | Зареєструвати інцидент | JWT |

**Приклад запиту авторизації:**
```json
POST /api/auth/login
{
  "driver_id": "DRV-1042",
  "password": "password123"
}

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "driver": { "driver_id": "DRV-1042", "full_name": "Коксюк О.В." }
}
```

**Усі захищені запити** надсилають заголовок:
```
Authorization: Bearer <jwt_token>
```

---

## 4. ANDROID-ДОДАТОК (Kotlin + Jetpack Compose)

### 4.1 Навігація між екранами

```
LoginScreen
    ↓ (успішна авторизація)
RouteDashboardScreen
    ↓ (натиснув "ОТРИМАТИ ДОРОЖНІЙ ЛИСТ")
ActiveTripScreen
    ↓ (натиснув "ІНЦИДЕНТ")
IncidentReportScreen
    ↓ (відправив звіт) → повернення на ActiveTripScreen
    ↓ (натиснув "ЗАВЕРШИТИ РЕЙС")
RouteDashboardScreen (з очищеним активним рейсом)
```

### 4.2 Екрани — детальний опис

#### ЕКРАН 1: LoginScreen
**Wireframe:** логотип системи зверху (120×120dp), поле Driver ID, поле Password, кнопка "УВІЙТИ"

**UI-елементи:**
- Logo placeholder — Box 120×120dp, по центру
- OutlinedTextField "Driver ID" — height: 56dp, keyboardType: Number
- OutlinedTextField "Password" — height: 56dp, visualTransformation: PasswordVisual
- Button "УВІЙТИ" — height: 64dp, marginTop: 32dp, fillMaxWidth
- CircularProgressIndicator (видимий під час запиту)
- Текст помилки (якщо авторизація не вдалась)

**Логіка:**
- Викликає `POST /api/auth/login`
- Зберігає JWT-токен у EncryptedSharedPreferences
- Зберігає дані водія (driver_id, full_name) у DataStore або SharedPreferences
- Перенаправляє на RouteDashboardScreen при success
- Показує текст помилки при 401/failure

#### ЕКРАН 2: RouteDashboardScreen
**Wireframe:** заголовок "Route Dashboard", інфо-рядок юзера, картка маршруту, список зупинок, кнопка "ОТРИМАТИ ДОРОЖНІЙ ЛИСТ"

**UI-елементи:**
- TopAppBar з ім'ям водія та кнопкою logout
- Рядок: "User: DRV-1042 (Коксюк О.В.)"
- Card з маршрутом: "Route #12: Центр – Вокзал" (великий жирний текст)
- LazyColumn зі зупинками (height ~40dp кожна):
  - кожен рядок: "Stop N | ЧАС | НАЗВА ЗУПИНКИ"
- Button "ОТРИМАТИ ДОРОЖНІЙ ЛИСТ" — height: 80dp, fillMaxWidth, внизу екрану

**Логіка:**
- При відкритті: `GET /api/routes` → відображає список або конкретний призначений маршрут
- Якщо є активний рейс (`GET /api/waybills/active`) — одразу редирект на ActiveTripScreen
- По натисканню кнопки: `POST /api/waybills` → перехід на ActiveTripScreen
- Кешує маршрути в Room для офлайн-доступу

#### ЕКРАН 3: ActiveTripScreen
**Wireframe:** статус-бар зверху, великий заголовок "Рейс #12 В ДОРОЗІ", блок поточної зупинки, внизу дві великі кнопки

**UI-елементи:**
- LinearProgressIndicator або StatusBar (100% ширини) — показує прогрес рейсу
- Великий жирний текст: "Рейс #12 В ДОРОЗІ" — центр, розмір ~24sp
- Card "Trip Info":
  - "Поточна зупинка: [назва]"
  - "Наступна зупинка: [назва]"
  - "Орієнтовний час прибуття: ЧЧ:ХХ"
- Spacer (займає весь вільний простір — щоб кнопки були внизу)
- Button "ЗАВЕРШИТИ РЕЙС" — height: 80dp, fillMaxWidth, колір: основний
- Button "ІНЦИДЕНТ" — height: 80dp, fillMaxWidth, колір: попереджувальний (жовтий або червоний)

**Логіка (найважливіша частина!):**
- Запускає **Foreground Service** для збору GPS-координат
- **LocationService** (Android Service): кожні 10-15 секунд отримує поточні координати через FusedLocationProviderClient
- Координати накопичуються пакетами по 5-10 точок та надсилаються на сервер: `POST /api/telemetry`
- Якщо немає інтернету — координати зберігаються в Room (таблиця `pending_telemetry`)
- **WorkManager** виконує синхронізацію накопичених даних при відновленні мережі
- **WakeLock**: тримає пристрій увімкненим під час рейсу
- По натисканню "ЗАВЕРШИТИ РЕЙС":
  - `PATCH /api/waybills/:id/complete`
  - Зупиняє LocationService
  - Фінальна синхронізація телеметрії
  - Повернення на RouteDashboardScreen
- По натисканню "ІНЦИДЕНТ" → перехід на IncidentReportScreen

#### ЕКРАН 4: IncidentReportScreen
**Wireframe:** заголовок "Incident Report", dropdown типу інциденту, textarea опис, зона камери, кнопка "ВІДПРАВИТИ ЗВІТ"

**UI-елементи:**
- TopAppBar "Реєстрація інциденту" з кнопкою "Назад"
- Text "Тип інциденту" + ExposedDropdownMenu з варіантами:
  - "ДТП (Аварія)"
  - "Технічна несправність"
  - "Затор на дорозі"
  - "Інше"
  - Висота: 56dp
- Text "Опис інциденту" + OutlinedTextField multiline — minHeight: 120dp
- Box 150×150dp — кнопка виклику камери (Camera Intent):
  - Показує іконку камери або прев'ю знятого фото
  - По натисканню: `Intent(MediaStore.ACTION_IMAGE_CAPTURE)`
- Spacer
- Button "ВІДПРАВИТИ ЗВІТ" — height: 72dp, fillMaxWidth

**Логіка:**
- Автоматично отримує поточні GPS-координати при відкритті екрану
- По натисканню "ВІДПРАВИТИ ЗВІТ": `POST /api/incidents` з усіма даними
- Після успішної відправки → Toast "Звіт надіслано" → повернення на ActiveTripScreen

---

## 5. АРХІТЕКТУРА (Clean Architecture + MVVM)

### Структура пакетів Android-проєкту:
```
com.citytransport.driverapp/
├── di/                          # Hilt модулі (NetworkModule, DatabaseModule)
├── data/
│   ├── remote/
│   │   ├── api/ApiService.kt    # Retrofit інтерфейс
│   │   └── dto/                 # Data Transfer Objects (JSON моделі)
│   ├── local/
│   │   ├── AppDatabase.kt       # Room Database
│   │   ├── dao/                 # DAO інтерфейси
│   │   └── entity/              # Room Entity класи
│   └── repository/              # Реалізації репозиторіїв
├── domain/
│   ├── model/                   # Доменні моделі (чисті Kotlin data class)
│   ├── repository/              # Інтерфейси репозиторіїв
│   └── usecase/                 # Use Cases (бізнес-логіка)
│       ├── LoginUseCase.kt
│       ├── GetActiveRouteUseCase.kt
│       ├── StartTripUseCase.kt
│       ├── EndTripUseCase.kt
│       └── ReportIncidentUseCase.kt
├── presentation/
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── route/
│   │   ├── RouteDashboardScreen.kt
│   │   └── RouteDashboardViewModel.kt
│   ├── trip/
│   │   ├── ActiveTripScreen.kt
│   │   └── ActiveTripViewModel.kt
│   └── incident/
│       ├── IncidentReportScreen.kt
│       └── IncidentReportViewModel.kt
├── service/
│   └── LocationTrackingService.kt  # Foreground Service для GPS
└── MainActivity.kt
```

### ViewModel → UI через StateFlow:
```kotlin
// Приклад для LoginViewModel
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
}
```

---

## 6. ФОНОВИЙ GPS-СЕРВІС (LocationTrackingService.kt)

Це **Foreground Service** — найважливіший компонент:

```
Запускається → показує постійне сповіщення у статус-барі ("Рейс #12 активний")
↓
FusedLocationProviderClient отримує координати кожні 10 сек
↓
Накопичує пакет з 5 точок
↓
Спроба надіслати на сервер (POST /api/telemetry)
  ├─ Успіх → очистити локальний кеш
  └─ Невдача (немає мережі) → зберегти в Room (pending_telemetry)
↓
WorkManager при поверненні мережі → синхронізація всіх pending
```

**AndroidManifest.xml** потребує дозволів:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.CAMERA"/>
```

---

## 7. UI/UX ВИМОГИ ДО ІНТЕРФЕЙСУ

Інтерфейс розроблений для водіїв під час руху — **ергономіка є критичною вимогою**:

- **Мінімальна висота кнопок:** 64dp для звичайних, 80dp для критичних (Start/End trip, ІНЦИДЕНТ)
- **Шрифти:** великий, читабельний текст (не менше 16sp для контенту, 20-24sp для заголовків)
- **Контраст:** висококонтрастний інтерфейс — чорний текст на білому або навпаки
- **Мінімалізм:** на кожному екрані лише необхідна інформація, ніяких декоративних елементів
- **Лінійна навігація:** не більше 3 кроків від логіну до активного рейсу
- **Material Design 3:** використовувати стандартні компоненти з `androidx.compose.material3`
- **Кольорова схема:** нейтральна (сірий/білий фон), акцентний колір для основних кнопок, червоний/жовтий для кнопки "ІНЦИДЕНТ"

---

## 8. ТЕСТОВІ ДАНІ (Mock Data для першого запуску)

При запуску сервера автоматично заповнити БД тестовими даними (файл `server/seed.js`):

**Водії:**
- driver_id: `DRV-1042`, password: `password123`, name: `Коксюк О.В.`
- driver_id: `DRV-2001`, password: `password123`, name: `Іваненко П.С.`

**Маршрути:**
- Route #12: Центр – Вокзал (4 зупинки, як у wireframe)
- Route #7: Аеропорт – Університет (5 зупинок)

---

## 9. ПОРЯДОК РОЗРОБКИ (рекомендована послідовність)

1. **Спочатку — бекенд:**
   - `server/index.js` + підключення до MongoDB
   - Mongoose-моделі (5 колекцій)
   - JWT middleware
   - Всі API-endpoints
   - `seed.js` — наповнення тестовими даними
   - Тестування через Postman або curl

2. **Потім — Android:**
   - Налаштування Retrofit (базовий URL: `http://10.0.2.2:3000/api` для емулятора, або IP комп'ютера для реального пристрою)
   - Room Database + DAO
   - Repository шар
   - ViewModels + Use Cases
   - Compose UI екрани (в порядку: Login → RouteDashboard → ActiveTrip → IncidentReport)
   - LocationTrackingService
   - WorkManager для синхронізації

3. **Останнє — інтеграція та тест офлайн-режиму**

---

## 10. ВАЖЛИВІ ТЕХНІЧНІ ДЕТАЛІ

- **Retrofit baseUrl** на емуляторі: `http://10.0.2.2:3000/` (10.0.2.2 = localhost хост-машини)
- **Retrofit baseUrl** на реальному пристрої: `http://[IP_КОМП'ЮТЕРА]:3000/`
- **JWT токен** додається через OkHttp Interceptor автоматично до всіх запитів
- **Room** зберігає: маршрути (для офлайн), незінхронізовану телеметрію, поточний активний рейс
- **Корутини** скрізь — ніяких callback-ів, використовувати `viewModelScope.launch`
- **Hilt** для dependency injection — всі репозиторії та use cases ін'єктуються через конструктор
- **StateFlow** (не LiveData) для стану UI
- **Navigation Compose** (`androidx.navigation:navigation-compose`) для навігації між екранами

---

## 11. МІНІМАЛЬНА ВЕРСІЯ ДЛЯ ЗАХИСТУ ДИПЛОМУ

Якщо необхідно зосередитись на MVP — реалізувати обов'язково:
1. ✅ LoginScreen + JWT авторизація
2. ✅ RouteDashboardScreen + завантаження маршруту з сервера
3. ✅ ActiveTripScreen + кнопки Start/End Trip + зміна статусу waybill у MongoDB
4. ✅ IncidentReportScreen + збереження інциденту в MongoDB
5. ✅ Фонова відправка GPS (хоча б базова, без Bucket-патерну)

Як додаткові фічі (якщо залишиться час):
- Офлайн-режим з Room
- WorkManager синхронізація
- Фото-фіксація інцидентів
- Day/Night Mode
