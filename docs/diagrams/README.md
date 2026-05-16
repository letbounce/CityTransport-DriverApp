# Діаграми та схеми до дипломної роботи

**Тема ВБП:** «Інформаційна система диспетчеризації міського пасажирського транспорту з використанням мобільних платформ»

**Об'єкт реалізації:** мобільний додаток **RoutePulse** + Node.js/MongoDB бекенд.

## Стиль оформлення

Усі діаграми виконані у єдиному візуальному стилі (Enterprise Architect / Visual Paradigm-подібний):

- Шрифт: Segoe UI / Calibri / Arial
- Заливка фігур (потоки даних): світло-блакитна `#dfeaf7`, циліндри БД `#e8eef6`, джерела/споживачі — біла
- Заливка фігур (класи UML, sequence-клієнт): світло-кремова `#fdf6e3`, заголовок `#f5e8c8`, бордер `#b89a5c`
- Заливка фігур (sequence-сервер, виділені блоки): світло-блакитна `#dfeaf7`, бордер `#5a6b8a`
- Заливка фігур (ER-сутності): світло-блакитна `#eef3f9`, заголовок `#dfeaf7`, бордер `#5a6b8a`
- Embedded sub-documents (MongoDB): світло-зелена `#f0f6ec`, бордер `#7a9a7a`
- I/O в алгоритмах (паралелограми): світло-зелена `#eaf3eb`
- Error / помилкові гілки: рожевувата `#fdf3f3` + `#c97070`
- Стрілки: тонкі (1.2-1.4 px), композиція — closed diamond, залежність — open triangle (dashed), посилання — open triangle (dashed, червоніший)
- Кириличні підписи в UTF-8 без BOM

## Перелік артефактів

| № | Файл (SVG + PNG) | Опис | Розділ ВБП | Статус |
|---|------------------|------|------------|--------|
| 01 | `01-information-model` | Інформаційна модель розв'язання задачі (РД 50-34.698-90) | 2.2.1 | OK |
| 02 | `02-use-case-diagram` | Діаграма прецедентів (UML) — водій / серверна підсистема | 2.3 | OK |
| 03 | `03-class-diagram` | Діаграма класів UML 3-tier (Data / App Server / Client) | 2.3 | OK |
| 04 | `04-sequence-diagram` | Sequence — «Створення нового дорожнього листа» | 2.3 | OK |
| 05 | `05-sequence-login` | Sequence — «Авторизація водія (Login → JWT)» | 2.3 | OK |
| 06 | `06-sequence-incident-photo` | Sequence — «Реєстрація інциденту з фотофіксацією» | 2.3 | OK |
| 07 | `07-sequence-complete-waybill` | Sequence — «Завершення / архівування дорожнього листа» | 2.3 | OK |
| 08 | `08-sequence-telemetry-sync` | Sequence — «Передача GPS-телеметрії з offline-чергою (WorkManager)» | 2.3 | OK |
| 09 | `09-er-infological` | Інфологічна модель БД (ER, Crow's foot) | 3.1 | OK |
| 10 | `10-er-datalogical` | Даталогічна модель БД (фізична схема колекцій MongoDB) | 3.1 | OK |
| 11 | `11-org-structure` | Організаційна структура АТП (виділення диспетчерської служби) | 1.1 | OK |
| 12 | `12-activity-parallel` | Activity-діаграма — паралельні процеси активного рейсу | 2.3 | OK |
| 14 | `14-algorithm-incident` | Схема алгоритму обробки інциденту (ДСТУ 19.701-90) | 2.2.2 | OK |
| 13 | _чекає_ | Діаграма трасування вимог | 2.3 | TODO |
| 15 | _чекає_ | Загальна схема інформаційного забезпечення | 3.1 | TODO |
| 16 | _чекає_ | Загальна схема автоматизації (Android - API - MongoDB) | 3.2 | TODO |
| 17 | _чекає_ | Структура комплексу технічних засобів (КТЗ) | 3.2 | TODO |
| 18 | _чекає_ | Схема АРМ водія | 3.2 | TODO |
| 19 | _чекає_ | Схема мережі передачі даних | 3.2 | TODO |
| 20 | _чекає_ | Структура програмного забезпечення | 3.3 | TODO |

## Підписи рисунків для Word

| Файл | Підпис |
|------|--------|
| `11-org-structure.png` | **Рис. 1.1.** Організаційна структура АТП з виділенням диспетчерської служби як об'єкта впровадження |
| `01-information-model.png` | **Рис. 2.1.** Інформаційна модель розв'язання задачі диспетчеризації міського пасажирського транспорту |
| `02-use-case-diagram.png` | **Рис. 2.2.** Діаграма прецедентів інформаційної підсистеми «Мобільний термінал водія» |
| `14-algorithm-incident.png` | **Рис. 2.3.** Схема алгоритму обробки інциденту (за ДСТУ 19.701-90) |
| `03-class-diagram.png` | **Рис. 2.4.** Діаграма класів інформаційної підсистеми (3-tier архітектура) |
| `04-sequence-diagram.png` | **Рис. 2.5.** Діаграма послідовності для сценарію «Створення нового дорожнього листа» |
| `05-sequence-login.png` | **Рис. 2.6.** Діаграма послідовності для сценарію «Авторизація водія (Login → JWT)» |
| `06-sequence-incident-photo.png` | **Рис. 2.7.** Діаграма послідовності для сценарію «Реєстрація інциденту з фотофіксацією» |
| `07-sequence-complete-waybill.png` | **Рис. 2.8.** Діаграма послідовності для сценарію «Завершення / архівування дорожнього листа» |
| `08-sequence-telemetry-sync.png` | **Рис. 2.9.** Діаграма послідовності для сценарію «Передача GPS-телеметрії з offline-чергою (WorkManager)» |
| `12-activity-parallel.png` | **Рис. 2.10.** Діаграма діяльності — паралельні процеси під час виконання активного дорожнього листа |
| `09-er-infological.png` | **Рис. 3.1.** Інфологічна модель бази даних інформаційної підсистеми (нотація Crow's foot) |
| `10-er-datalogical.png` | **Рис. 3.2.** Даталогічна модель бази даних (фізична схема колекцій MongoDB) |

## Формати

- `.svg` — векторний оригінал, легко редагується (Inkscape, Edge/Chrome, draw.io). Word 2016+ підтримує вставку SVG напряму.
- `.png` — растровий експорт для Word.

## Перерендер SVG → PNG (Chrome headless)

```powershell
$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$folder = "G:\AndroidStudioProjects\docs\diagrams"

function Render-Svg($name, $w, $h) {
    $svg = Join-Path $folder "$name.svg"
    $png = Join-Path $folder "$name.png"
    Remove-Item $png -Force -ErrorAction SilentlyContinue
    $url = "file:///" + ((Resolve-Path $svg).Path -replace '\\','/')
    Start-Process -FilePath $chrome -ArgumentList @("--headless=new","--disable-gpu","--hide-scrollbars","--window-size=$w,$h","--screenshot=$png",$url) -Wait -NoNewWindow
}

Render-Svg "01-information-model" 1200 620
Render-Svg "02-use-case-diagram" 1000 870
Render-Svg "03-class-diagram" 1200 920
Render-Svg "04-sequence-diagram" 1200 680
Render-Svg "05-sequence-login" 1200 720
Render-Svg "06-sequence-incident-photo" 1200 780
Render-Svg "07-sequence-complete-waybill" 1200 720
Render-Svg "08-sequence-telemetry-sync" 1280 820
Render-Svg "09-er-infological" 1280 860
Render-Svg "10-er-datalogical" 1280 1020
Render-Svg "11-org-structure" 1280 720
Render-Svg "12-activity-parallel" 1280 1020
Render-Svg "14-algorithm-incident" 1000 1080
```

## Вставка у Word

1. Insert -> Pictures -> From this device -> обрати потрібний `.png`.
2. Підпис рисунка (стиль «Caption / Підпис»).
3. Рекомендована ширина у документі: 16 см (приблизно 100% від ширини сторінки A4 з полями 30/15 мм).

## Узгодженість артефактів

Усі діаграми побудовані на єдиному наборі сутностей і ролей RoutePulse, тому в звіті їх можна посилатися перехресно:

- **Driver** як актор: фігурує у use case (02), class (03), всіх sequence (04–08), Activity (12), org chart (11 — як «Водії» виконавчого рівня)
- **WAYBILL** як ключова сутність: 01 (вхідні/вихідні), 03, 04, 07, 09, 10
- **INCIDENT**: 01, 03, 06, 09, 10, 14 (алгоритм)
- **TELEMETRY**: 01, 03, 08, 09, 10, 12 (Loop B)
- **Диспетчерська служба** як споживач: 01 (вихідні), 11 (виділена), 09, 10
- **Шари клієнта (Screen/VM/Repository/ApiService)**: 03 (3-tier), всі sequence (04-08)
- **Шари сервера (Controller/MongoDB)**: 03, всі sequence (04-08), 10

## Редагування діаграми

- Просте редагування тексту: відкрити `.svg` у будь-якому текстовому редакторі (UTF-8) та виправити теги `<text>`.
- Візуальне редагування: завантажити у https://www.drawio.com/ або у безкоштовний **Inkscape**.

## Зауваги

- НЕ використовуйте інструмент StrReplace для редагування цих SVG — він пошкоджує UTF-8 кирилицю в `<text>`. Краще робити перезапис файлу повністю.
- Після збереження варто пропустити файл через очистку керуючих байтів (див. скрипт нижче).

### Очистка SVG від керуючих байтів

```powershell
function Clean-Svg($path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $cleaned = $bytes | Where-Object { $_ -ge 0x20 -or $_ -eq 0x09 -or $_ -eq 0x0A -or $_ -eq 0x0D }
    [System.IO.File]::WriteAllBytes($path, $cleaned)
}
```
