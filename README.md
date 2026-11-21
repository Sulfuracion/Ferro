# Ferro

[![Android Build & Check](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Status: Work in Progress](https://img.shields.io/badge/Status-Work%20in%20Progress-orange.svg)]()

> ⚠️ **Project Status: Under Active Development (Work in Progress)**  
> Ferro is currently not finished and is being actively built. APIs, database schemas, and UI components may change as new features and refinements are introduced.

---

## Overview

**Ferro** is an **offline-first**, privacy-focused native Android workout tracking application built for serious gym athletes and strength trainers.

Every rep, set, routine, and personal record is stored directly on your device with **zero mandatory account creation, zero tracking, and zero subscription paywalls**.

---

## Features

### 🏋️ Active Workout Tracking
- **Set Types**: Warm-up (`W`), Normal, Failure (`F`), and Drop sets (`D`).
- **Flexible Logging Types**: Weight + Reps, Bodyweight + Reps, Weighted Bodyweight, Assisted Bodyweight, Duration, and Distance + Duration.
- **Live Autosave**: Workout progress is committed to local storage on every single change, ensuring no progress is lost if the app is killed or battery dies.
- **Previous Session Comparison**: Displays historical performance side-by-side during active sets.
- **Session Notes & RPE**: Optional RPE (Rate of Perceived Exertion) logging and exercise notes.

### ⏱️ Bulletproof Background Rest Timer
- **Special-Use Foreground Service (`ActiveWorkoutService`)**: Maintains an ongoing chronometer notification with quick `+15s` and `Skip` actions.
- **Accurate Background Alerts**: Combines `AlarmManager` (`RTC_WAKEUP`) with dedicated audio and haptic notification channels (`VibratorManager`) to wake up and alert the user even when the screen is locked, in deep sleep (Doze), or under aggressive OEM battery optimizations.

### 📋 Routines & Workouts
- **Predefined Starter Templates**: Full Body (3-day), Upper / Lower (4-day), Push / Pull / Legs (6-day), and Strength 5×5.
- **Custom Routine Builder**: Organize exercises, configure target sets/reps, set default rest intervals, and link supersets.
- **Folder Organization**: Group routines cleanly.

### 📚 Exercise Library
- Seeded database with **120+ standard gym exercises** covering major and secondary muscle groups, required equipment, and instructions.
- Full support for adding and editing custom exercises.
- Muscle and equipment filtering with instant search.

### 🏆 Personal Records (PRs)
- Automated detection and celebration of PRs upon workout completion:
  - Estimated 1RM (Epley formula)
  - Heaviest Weight
  - Best Volume (Weight × Reps)
  - Repetition Maxes at specific weights

### 🎨 Design & Localization
- **Design System (`FerroTheme`)**: Industrial, distraction-free aesthetic with Charcoal/Slate tones and an Ember (`brasa`) accent. Built strictly with Jetpack Compose Material 3 tokens.
- **True Offline-First Storage**: Room Database + DataStore Preferences.
- **Canonical Units**: Weights stored strictly in kg internally, converted seamlessly between kg and lb based on user preference.
- **Bilingual**: Complete English and Spanish support with per-app language selection (`locales_config`).

---

## Roadmap / Planned Features

- [ ] Interactive volume distribution and strength progress charts.
- [ ] Body weight and measurement tracking over time.
- [ ] Encrypted local JSON/archive backup and restore.
- [ ] Workout history calendar view and export features (CSV/JSON).
- [ ] Wear OS companion app.

---

## Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Architecture**: MVVM + Clean Architecture / Repository pattern with unidirectional data flow
- **Concurrency & Reactivity**: Kotlin Coroutines & Flow
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Local Persistence**: [Room](https://developer.android.com/training/data-storage/room) SQLite & Preferences DataStore
- **Navigation**: Jetpack Navigation Suite with type-safe routes (`@Serializable`)
- **Code Quality**: Ktlint, Android Lint, JUnit 4, Kotlinx Coroutines Test, Turbine

---

## Project Structure

```text
Ferro/
├── app/
│   ├── src/main/java/com/davidguijo/ferro/
│   │   ├── core/
│   │   │   ├── data/             # Room DB, DAOs, repositories, migrations
│   │   │   ├── designsystem/     # FerroTheme, color tokens, typography
│   │   │   └── domain/           # Pure domain models, calculations (1RM, units)
│   │   ├── feature/
│   │   │   ├── workout/          # Active workout screen & foreground service
│   │   │   ├── routines/         # Routine manager and editor
│   │   │   ├── exercises/        # Exercise library & details
│   │   │   ├── history/          # Past sessions and workout logs
│   │   │   ├── progress/         # Analytics and records
│   │   │   ├── settings/         # App preferences, theme, units, language
│   │   │   ├── body/             # Body measurements
│   │   │   └── backup/           # Data backup & restore
│   │   └── navigation/           # Type-safe bottom bar and route definitions
│   └── src/test/                 # Unit tests (Room migrations, calculations, repositories)
├── docs/
│   ├── SPEC.md                   # Full product specification
│   └── design/                   # Brand and design guidelines
└── README.md
```

---

## Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer recommended
- **JDK**: 17+
- **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 35 (Android 15)

### Build & Run
1. Clone this repository:
   ```bash
   git clone https://github.com/Sulfuracion/Ferro.git
   cd Ferro
   ```

2. Run tests and lint checks:
   ```bash
   ./gradlew check
   ```

3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Contributing & Development Guidelines

For details on project architecture, code conventions, and guidelines, please review:
- [`AGENTS.md`](AGENTS.md) — Code style, Room migration rules, and design guidelines.
- [`docs/SPEC.md`](docs/SPEC.md) — Complete technical and functional specification.
- [`docs/design/DESIGN.md`](docs/design/DESIGN.md) — Design tokens and brand rules.

---

## License

This project is licensed under the terms of the MIT License. See [LICENSE](LICENSE) for details.
