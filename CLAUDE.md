# CLAUDE.md — Raitha-Bharosa Hub
## Smart Sowing Assistant · Android App · Karnataka Farmers
**PRD Reference:** #NO77 · Puneeth Vemuri · Journeymen G1 · MindMatrix VTU Internship

---

## 🧭 Your Role
You are a senior Android engineer building a production-grade app for Karnataka smallholder farmers.
Every decision must reflect:
- **Offline-first** design (Room DB is king, API is enhancement)
- **Strict MVVM + Clean Architecture** — no business logic inside Composables, ever
- **Kannada-first UI** — zero hardcoded English strings anywhere in the codebase
- **Mid-range device targeting** — Redmi 9A class (2 GB RAM, API 26+)

---

## 📦 Tech Stack (non-negotiable)

| Layer | Library / Tool |
|---|---|
| Language | Kotlin (latest stable) |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Clean Architecture (Domain / Data / Presentation) |
| Local DB | Room DB (with SQLCipher for personal data tables) |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp + Gson/Moshi |
| Background Work | WorkManager (PeriodicWorkRequest, 30-min interval) |
| Weather API | OpenWeatherMap `/forecast` (7-day, 3-hourly) |
| Auth | Firebase Phone OTP (Good-to-Have — scaffold but don't block v1) |
| Crash Reporting | Firebase Crashlytics |
| Charts | MPAndroidChart |
| Persistence (prefs) | DataStore (Preferences) |
| Build | Gradle KTS, R8 minification in release |
| Min SDK | 26 · Target SDK | Latest stable |

---

## 🏗️ Project Structure

```
app/
├── data/
│   ├── local/
│   │   ├── dao/          # FarmerDao, PlotDao, WeatherDao, NpkDao, SeasonDao
│   │   ├── entity/       # FarmerEntity, PlotEntity, WeatherEntity, NpkEntity, SeasonEntity
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── api/          # WeatherApiService.kt (Retrofit)
│   │   └── dto/          # WeatherResponseDto, ForecastDto
│   ├── mock/
│   │   └── mock_weather.json   # Asset file — offline fallback
│   ├── repository/
│   │   └── WeatherRepository.kt  # Decides API vs mock
│   └── generator/
│       └── DataGeneratorClass.kt  # Simulation: randomMoisture, randomTemperature
├── domain/
│   ├── model/            # Farmer, Plot, Weather, NpkEntry, Season (pure Kotlin, no Android deps)
│   ├── usecase/          # GetSowingIndexUseCase, GetWeatherForecastUseCase, SaveNpkEntryUseCase, etc.
│   └── calculator/
│       └── SowingIndexCalculator.kt  # Weighted formula — Moisture 40%, Temp 30%, Rain 30%
├── presentation/
│   ├── onboarding/
│   │   ├── OnboardingViewModel.kt
│   │   └── screens/      # LanguagePickerScreen, FarmerProfileScreen, PermissionsScreen
│   ├── dashboard/
│   │   ├── DashboardViewModel.kt
│   │   └── DashboardScreen.kt  # Circular gauge, data cards
│   ├── npk/
│   │   ├── NpkViewModel.kt
│   │   └── NpkScreen.kt
│   ├── calendar/
│   │   ├── KrishiCalendarViewModel.kt
│   │   └── KrishiCalendarScreen.kt
│   ├── history/
│   │   ├── SeasonHistoryViewModel.kt
│   │   └── SeasonHistoryScreen.kt
│   ├── settings/
│   │   └── SettingsScreen.kt
│   └── navigation/
│       └── AppNavGraph.kt  # Single-activity, Compose Navigation
├── di/
│   └── AppModule.kt      # Hilt modules — DB, Retrofit, Repository, WorkManager
├── worker/
│   └── WeatherRefreshWorker.kt  # WorkManager — 30-min periodic, Doze-safe
├── util/
│   └── NpkRecommendationEngine.kt
└── MainActivity.kt       # Single Activity
res/
├── values/strings.xml        # English strings
└── values-kn/strings.xml     # Kannada strings — MUST be 100% complete
assets/
└── mock_weather.json
```

---

## 🔑 Core Domain Rules

### SowingIndexCalculator
```kotlin
// In domain/calculator/SowingIndexCalculator.kt
// Formula: Moisture×0.4 + Temperature×0.3 + (1-RainProb)×0.3  → normalised to 0–100
// Thresholds:
//   Green  > 70  → "Sow Now"
//   Yellow 40–70 → "Caution"
//   Red    < 40  → "Wait"
// Crop-specific moisture thresholds:
//   Paddy  : optimal 25–35%
//   Ragi   : optimal 20–30%
//   Sugarcane: optimal 22–32%
// Guard: if (moisture > 30) → forcibly clamp index, show "Soil too wet to sow"
```

### DataGeneratorClass
```kotlin
// In data/generator/DataGeneratorClass.kt — injected via Hilt
// randomMoisture()    → Float in 10–40%
// randomTemperature() → Float in 18–35°C
// randomHumidity()    → Float in 40–90%
// Activated automatically by WeatherRepository when API call fails
```

### WeatherRepository Decision Logic
```kotlin
// 1. Try Retrofit → OpenWeatherMap /forecast
// 2. On failure  → load assets/mock_weather.json
// 3. Cache last success in Room WeatherEntity with timestamp
// 4. UI always reads from Room (single source of truth)
```

---

## 📋 Feature Build Order (follow this sequence)

1. **Room DB schema** — all entities + DAOs + migrations skeleton
2. **Hilt DI setup** — AppModule, DatabaseModule, NetworkModule
3. **DataGeneratorClass + SowingIndexCalculator** — with unit tests
4. **WeatherRepository** (API + mock fallback)
5. **Onboarding flow** — Language picker → Profile form → GPS plot pin → Permissions
6. **Dashboard screen** — Circular gauge + data cards + WorkManager refresh
7. **NPK Input Centre** — form + recommendation engine + Room persistence
8. **7-Day Krishi Calendar** — horizontal strip + storm-warning logic
9. **Season History** — log + MPAndroidChart bar chart
10. **Settings screen** — language toggle, unit, notifications
11. **(Good-to-Have last)** Firebase OTP, CSV export, Lottie animation, FCM push

---

## 🌐 API Configuration

```
// Store in local.properties — NEVER commit to Git
OPENWEATHER_API_KEY=your_key_here

// Access via BuildConfig
buildConfigField("String", "OWM_API_KEY", "\"${localProperties["OPENWEATHER_API_KEY"]}\"")

// Endpoint
GET https://api.openweathermap.org/data/2.5/forecast?lat={lat}&lon={lon}&appid={key}&units=metric&cnt=56
```

---

## 🗄️ Room DB Schema

```
FarmerEntity     : id (PK), name, mobile, primaryCrop, district, languagePref
PlotEntity       : id (PK), farmerId (FK), latitude, longitude, label
WeatherEntity    : id (PK), plotId (FK), date, rainMm, tempMax, humidity, fetchedAt
NpkEntity        : id (PK), plotId (FK), nitrogen, phosphorus, potassium, testDate, labName
SeasonEntity     : id (PK), plotId (FK), crop, sowDate, harvestDate, yieldKg, notes

// FK: FarmerEntity ↔ PlotEntity ↔ SeasonEntity (supports multiple plots per farmer)
// Encrypt personal tables (FarmerEntity, NpkEntity) via SQLCipher
```

---

## 🎨 UI / UX Rules

- **Primary brand color:** `Color(0.086275f, 0.639216f, 0.290196f, 1f)` — Karnataka agriculture green
- **Material Design 3** dynamic theming — seed from brand color
- Every screen must have both `strings.xml` (English) and `strings-kn.xml` (Kannada) entries
- **Zero** hardcoded English text in any Composable — use `stringResource()` only
- Every color-state indicator (Green/Yellow/Red gauge) MUST also carry a text label — never color-only (accessibility NFR-05)
- Touch targets ≥ 48 dp for every interactive element
- WCAG AA contrast on all text

---

## ⚙️ WorkManager Setup

```kotlin
// WeatherRefreshWorker
// Interval: 15 minutes (minimum allowed) — display updates every 30 min
// Constraints: NetworkType.CONNECTED (graceful fallback to mock if not)
// Backoff: EXPONENTIAL, min 15 min, max 6 h
// Must survive Doze mode: use setExpedited() for first run
// Battery budget: ≤ 1% drain/hour
```

---

## ✅ Non-Functional Checklist (verify before each PR)

- [ ] Dashboard cold-start < 2 s on API 26 emulator (Redmi 9A class)
- [ ] Sowing Index updates within 500 ms of data change
- [ ] All strings in `strings.xml` AND `strings-kn.xml` — run `./gradlew lint` to verify
- [ ] No API keys in source — only in `local.properties` + `BuildConfig`
- [ ] R8 minification enabled in `release` buildType
- [ ] APK size ≤ 20 MB (check with `./gradlew assembleRelease` + analyze APK)
- [ ] Unit tests for `SowingIndexCalculator` covering all three threshold bands
- [ ] Unit tests for `DataGeneratorClass` boundary values
- [ ] WorkManager refresh verified on both Doze and non-Doze in emulator

---

## 🚫 Hard Constraints — Never Violate

1. **No business logic inside Composable functions** — ViewModels only
2. **No hardcoded English strings** — `stringResource()` everywhere
3. **No API keys in source code** — `local.properties` + `BuildConfig` only
4. **Room DB is the single source of truth** — UI never reads directly from network response
5. **DataGenerator must activate automatically** when API is unavailable — not manually triggered
6. **Offline mode must cover:** Dashboard, NPK log, Season History — all must work with zero network
7. **Do NOT build** in v1.0: IoT sensor pairing, ML yield prediction, PM Kisan API, marketplace, multi-state

---

## 🧪 Testing Requirements

```
test/                              # JVM unit tests
├── SowingIndexCalculatorTest.kt   # Green/Yellow/Red bands, crop thresholds, wet-soil clamp
├── DataGeneratorClassTest.kt      # Boundary values for moisture, temp, humidity
├── NpkRecommendationEngineTest.kt # All three nutrients below/above/within optimal
└── WeatherRepositoryTest.kt       # API success, API fail → mock fallback

androidTest/                       # Instrumented
├── DashboardScreenTest.kt         # Gauge renders, color states, Kannada strings visible
└── OnboardingFlowTest.kt          # Full onboarding E2E
```

---

## 📌 Success Criteria (from PRD §06)

| Metric | Target |
|---|---|
| Dashboard load time | < 2 seconds |
| Kannada UI coverage | 100% |
| Crash-free rate | > 99% (Crashlytics) |
| Architecture | Strict MVVM — verified in code review |
| Play Store rating target | > 4.2 ★ |
| Sowing Index dynamic update | Verified via unit test on each data regeneration |

---

*CLAUDE.md last updated for PRD #NO77 · Puneeth Vemuri · MindMatrix VTU Internship*