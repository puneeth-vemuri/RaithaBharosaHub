# Raitha-Bharosa Hub
Smart Sowing Assistant for Karnataka Farmers

Raitha-Bharosa Hub is an offline-first Android app that helps Karnataka farmers make better sowing and crop-management decisions using local Room data, weather forecasts, sowing index logic, NPK recommendations, and seasonal history, with Kannada-first UI support for day-to-day field use.

## Tech Stack
- Kotlin
- Jetpack Compose
- MVVM
- Room DB
- Retrofit
- WorkManager
- Hilt
- Firebase
- Material Design 3

## Must-Have Features
- Language picker onboarding
- Farmer profile setup
- GPS plot pin setup
- Permissions onboarding
- Dashboard with circular sowing gauge and weather cards
- NPK input and recommendation flow
- 7-day Krishi calendar
- Season history tracking
- Settings for language, units, and notifications

## How to Build
1. Clone the repo.
2. Add `OPENWEATHER_API_KEY` and `DB_PASSPHRASE` to `local.properties`.
3. Add `google-services.json` to `app/`.
4. Run `./gradlew assembleRelease`.

Developed by Puneeth Vemuri · Journeymen G1 · MindMatrix VTU Internship Program · PRD #NO77