**Design Overview**

- **Purpose:** Central reference for redesigning the app's UI. Lists every screen, responsibilities, important components, accessibility/localization rules, and implementation constraints so you can implement a new Jetpack Compose Material3 UI consistently.
- **Audience:** Android engineers, UI designers, and QA working on Raitha Bharosa Hub.

**Guiding Principles**

- Offline-first: Room is the single source of truth. UI reads from ViewModels which expose Room-backed state.
- Architecture: MVVM + Clean Architecture. No business logic in Composables.
- Kannada-first: All visible text must come from `res/values/strings.xml` and `res/values-kn/strings.xml`.
- Target devices: Mid-range (e.g., Redmi 9A). Min SDK 26. Optimize for 1080×2400 layouts.
- UI tech: Jetpack Compose + Material3 (use Compose for new screens; keep compatibility wrappers for existing screens where necessary).

**Global Design Tokens**

- Primary color: Karnataka green (brand): Color(0.086275f, 0.639216f, 0.290196f, 1f) — expose as `colorPrimary` in theme.
- Typography: Title 20–24sp, Body 14–16sp, Labels 12–14sp.
- Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 dp.
- Touch targets: >= 48dp for interactive elements.
- Contrast: meet WCAG AA for all text-on-background combinations.

**Navigation & App Shell**

- Single-Activity app using Compose Navigation and `AppNavGraph`.
- Top App Bar: left back affordance (contentDescription set), centered title, optional right action (Filter). All titles use `stringResource()`.
- Bottom sheet / dialogs: use Material3 `ModalBottomSheet` for contextual actions.

**Screens (by area)**

- **Onboarding**
  - `LanguagePickerScreen` (file: presentation/onboarding/screens) — choose Kannada/English preference; persists in DataStore.
  - `FarmerProfileScreen` — collect farmer name, mobile, primary crop, district. Validations for phone number.
  - `PermissionsScreen` — explain and request location/storage permissions.

- **Dashboard**
  - `DashboardScreen` — circular gauge (Sowing Index), data cards for latest weather, NPK quick access, last season summary.
  - Components: `CircularGauge`, `DataCard`, `RefreshButton` (triggers WorkManager refresh indicator).

- **NPK**
  - `NpkScreen` / `NpkInputCenter` — NPK input form, recommendation engine output, history link. Persist entries to Room (NpkEntity).

- **Calendar**
  - `KrishiCalendarScreen` (7-day) — horizontal day strip with icons, storm-warning logic, weather forecast summaries.

- **Seasons & History**
  - `AddSeasonScreen` / `Add New Season` — form: crop, sow date, harvest date, yield, notes, Save action.
    - IDs: `title_add_season`, `input_crop`, `input_sow_date`, `input_harvest_date`, `input_yield`, `input_notes`, `button_save_season`, `stats_total_seasons`, `stats_completed`, `stats_avg_yield`.
    - Validation: crop required, sow date required. Save persists a `SeasonEntity` in Room.
  - `SeasonHistoryScreen` — list of seasons, quick stats row (Total Seasons, Completed, Avg Yield). Tap a season to view/edit.

- **Settings**
  - `SettingsScreen` — language toggle, units, notifications, Data export (CSV), debug options (clear cache).

- **Shared / Utility Screens**
  - `Loading` / `EmptyState` / `Error` Composables — consistent visual language and copy.
  - `LocationPicker` (for plot pin) — small map or lat/lon entry fallback.

**Component Library**

- Create a Compose component set (package: presentation/components):
  - `AppTopBar(title, navIcon, actions)`
  - `FormTextField(id, labelRes, value, onValue)` — supports single/multi-line, hint via strings.
  - `PrimaryButton(textRes, onClick)` — uses colorPrimary and minHeight 48dp.
  - `StatRow(item1, item2, item3)` — used in Season screen.
  - `Gauge` — Sowing Index gauge with color + textual label (Green/Yellow/Red + text)

**Strings & Localization**

- All copy must be keys in `res/values/strings.xml` and Kannada in `res/values-kn/strings.xml`.
- Provide consistent keys across screens (e.g., `add_season_title`, `label_crop_hint`, `button_save_season`).

**Accessibility**

- Provide `contentDescription` for icons/actions (e.g., Back, Filter, Save). Use `contentDescription` string resources.
- Color must never be the only way to convey state — always add a text label for status indicators.
- Ensure focus order and large touch targets.

**Data Flow & ViewModel Responsibilities**

- ViewModels: expose state flows (StateFlow / LiveData) with UI models derived from domain use-cases.
- No business logic in Composables: use `GetWeatherForecastUseCase`, `GetSowingIndexUseCase`, `SaveSeasonUseCase` from domain layer.
- UI should display loading/error states and retry affordances.

**Offline Behavior**

- UI always reads from Room. Network triggers repository refresh; on failure the repository uses asset `mock_weather.json` and DataGenerator.

**Testing**

- Unit tests for `SowingIndexCalculator` and `DataGeneratorClass` (JVM tests).
- Compose UI tests for key screens: `DashboardScreen`, `OnboardingFlow`, `AddSeasonScreen` (instrumented/composable tests).

**Implementation Checklist for Rebuild**

1. Create Compose theme with Material3 tokens (colors, typography, shapes).
2. Implement component library (`presentation/components`).
3. Migrate each screen one-by-one to Compose, keeping old screens available until parity tested.
4. Update `AppNavGraph` to use new Compose destinations.
5. Add strings in English + Kannada and run lint to confirm coverage.

**Prototyping**

- Use a rapid prototyping tool (the user suggested: https://stitch.withgoogle.com/) for quick visual flows. Export assets and tokens, then implement in Compose.

**Files & Screens Map (quick reference)**

- Onboarding: `presentation/onboarding/LanguagePickerScreen`, `OnboardingViewModel`
- Dashboard: `presentation/dashboard/DashboardScreen`, `DashboardViewModel`
- NPK: `presentation/npk/NpkScreen`, `NpkViewModel`
- Calendar: `presentation/calendar/KrishiCalendarScreen`, `KrishiCalendarViewModel`
- Seasons: `presentation/history/SeasonHistoryScreen`, `AddSeasonScreen` (Add/Edit)
- Settings: `presentation/settings/SettingsScreen`

---

If you want, I can now:

- generate a starter Compose component library and a single `AddSeasonScreen` Compose implementation (small PR), or
- produce a checklist-style migration plan with per-screen acceptance criteria.

Tell me which you'd prefer next.
