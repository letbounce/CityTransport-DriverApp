# Server Setup

## Install

```bash
npm install
```

## Configure

```bash
cp .env.example .env
```

## Run

```bash
npm start
```

## Development

```bash
npm run dev
```

## Seed test data

```bash
npm run seed
```

## Smoke test (табл. 3.7 диплома — TC-01…TC-06, DRV-1042, маршрут №114)

**Підготовка (один раз):**

```bash
cp .env.example .env
# Запустіть MongoDB локально, потім:
npm run seed
```

**Варіант A — сервер уже запущений (`npm start` в іншому терміналі):**

```bash
npm run smoke
```

**Варіант B — автозапуск сервера + тести:**

```bash
npm run smoke:all
```

**Варіант C — пересід БД + сервер + тести:**

```bash
npm run smoke:seed
```

Очікуваний вивід: таблиця зі статусом «Пройдено» для TC-01…TC-06.  
Помилка `ECONNREFUSED` означає, що сервер не слухає порт 3000 — використайте `npm run smoke:all` або `npm start`.
