# FitTrack — Personal Fitness Companion for Android

A personal fitness tracking app built for Android using Kotlin and Jetpack Compose. Designed around Indian lifestyle and diet, with optional AI features powered by the Claude API. Built entirely through conversational AI-assisted development with Claude (Coco).

---

## What Is This App?

FitTrack is a personal wellness companion that tracks workouts, nutrition (with a large Indian food database), weight, daily steps, rest days, walk days, and period days — all stored locally on your phone with no account or subscription required. It also features AI-powered weekly summaries, rest day advice, and exercise suggestions via the Claude API, daily reminder notifications, and full backup/restore.

---

## Tech Stack

| Tool | What It Does |
|---|---|
| **Kotlin** | The programming language. Android's official language since 2019. |
| **Jetpack Compose** | Google's modern UI toolkit. UI is written in Kotlin as composable functions — no XML layouts. |
| **Material 3** | Google's design system. Provides buttons, cards, bottom sheets, dialogs, and navigation — styled consistently. |
| **Room** | SQLite wrapper library. Tables are defined as Kotlin data classes; queries are annotated functions. |
| **StateFlow + combine()** | Reactive data streams. When the database changes, UI updates automatically with no manual refresh. |
| **ViewModel** | Holds screen logic and state. Survives screen rotation. Sits between UI and data. |
| **Repository** | Single access point for all data (database, API). ViewModels never talk to the database directly. |
| **OkHttp** | HTTP client used to call the Claude API. |
| **KSP** | Kotlin Symbol Processing — generates Room boilerplate (DAOs, database helpers) at build time. |
| **DataStore** | Jetpack Preferences DataStore for user profile (name, age, weight, goal, activity level). |
| **Jetpack Navigation Compose** | Type-safe screen routing with back stack management. |
| **WorkManager** | Schedules daily periodic reminder notifications (workout, lunch, dinner). |
| **BiometricPrompt** | Optional biometric-gated actions (via AppCompat + Biometric libraries). |
| **Health Connect** | Step sync integration point. |

---

## Architecture

```
UI (Compose Screens)
    ↕  collectAsState() / StateFlow
ViewModel  (one per screen)
    ↕  suspend functions / Flow
Repository  (single shared instance via FitTrackApp)
    ↕                         ↕
Room Database (v8)      Claude API (OkHttp + coroutines)
```

This is the **MVVM pattern** (Model–View–ViewModel), the standard Android architecture recommended by Google. All state is held in `StateFlow` and collected in Compose with `collectAsState()`. There is no dependency-injection framework (no Hilt) — `FitTrackApp` (the `Application` subclass) constructs the database and repository once and hands them to ViewModel factories.

---

## Project Structure

```
app/src/main/java/com/sunitha/fittrack/
├── ai/
│   └── ClaudeApiService.kt           ← All Claude API calls, grounding rules, hallucination guard, serialization
├── data/
│   ├── datastore/
│   │   └── UserProfileStore.kt       ← Name, age, height, weight goal, activity level; macro calculator
│   ├── db/
│   │   ├── dao/                      ← One DAO per table (WorkoutDao, FoodDao, WeightDao, etc.)
│   │   ├── entities/                 ← Room @Entity data classes for each table
│   │   └── FitTrackDatabase.kt       ← Room setup, version 8, all migrations
│   ├── local/
│   │   ├── ExerciseData.kt           ← 70+ exercise templates across 8 muscle groups
│   │   └── IndianFoodData.kt         ← 100+ Indian and international foods across 17 categories
│   └── repository/
│       └── FitTrackRepository.kt     ← Single data access layer used by all ViewModels
├── notifications/
│   ├── ReminderWorker.kt             ← WorkManager periodic reminders (workout, lunch, dinner)
│   └── NotificationHelper.kt         ← Notification channel setup + display
└── ui/
    ├── home/                         ← Home screen, ViewModel, MacroHistoryScreen + ViewModel
    ├── workout/                      ← Workout tab, WorkoutHistoryScreen + ViewModels
    ├── nutrition/                    ← Food logging screen + ViewModel
    ├── progress/                     ← Weight chart, strength progress + ViewModel
    ├── ai/                           ← AI Insights screen + ViewModel
    ├── settings/                     ← Settings, Backup/Restore, Import + ViewModels
    ├── onboarding/                   ← First-launch profile setup + ViewModel
    ├── navigation/                   ← AppNavigation (bottom nav + all routes)
    └── theme/                        ← Colors (green + rose palette), typography
```

---

## Database Schema (v8)

| Table | What It Stores |
|---|---|
| `workout_sessions` | Each completed workout (muscle group, duration, sets, estimated calories, date) |
| `workout_sets` | Individual sets within a session (exercise name, reps, weight kg) |
| `food_entries` | Each food logged (name, serving description, quantity/servings, meal type, macros, date) |
| `weight_entries` | Weight logs with timestamps |
| `step_entries` | Daily step counts + `isWalkDay` flag (one row per day) |
| `rest_day_entries` | Days marked as rest days with optional notes |
| `period_entries` | Days marked as period days with optional notes |
| `ai_insights` | Cached AI responses (weekly summaries, rest day advice, exercise suggestions per muscle group) |
| `ai_food_cache` | Permanently cached AI food macro lookups; also synced from logged food history |

**Migration history:**
- v1 → v2: Added `step_entries`, `rest_day_entries`
- v2 → v3: Internal restructure (`fallbackToDestructiveMigration`)
- v3 → v4: Added `ai_food_cache`
- v4 → v5: Added `period_entries`
- v5 → v6: Added `isWalkDay` column to `step_entries`
- v6 → v7: Added `fiberG` column to `food_entries` and `ai_food_cache`
- v7 → v8: Added `servings` column to `food_entries` — enables quantity-driven macro recalculation on edit

---

## Features

### Home Screen
The home screen uses a **rose/blush pink** palette — warm and personal, distinct from the green workout screens.

- **Greeting banner** — deep rose gradient with your name, today's date, time-aware greeting (Good morning / afternoon / evening), and streak counter with flame icon. Period Day badge appears when today is logged as one.
- **Calorie ring** — animated arc with rose-to-blush gradient showing consumed vs personalised calorie goal
- **Macros card** — protein, carbs, fat with circular progress indicators and goals calculated from your profile (weight, activity level, target). "View All" opens the Macro History screen.
- **Quick Log buttons** — Log Workout, Log Food, Log Weight — navigate directly to the right screen
- **Steps card** — today's step count with rose progress bar toward your daily step goal
- **Last Activity card** — shows the most recent activity across all types (workout, rest day, walk day, period day). "View All" opens full Activity History.
- **Weight card** — current weight, goal, progress bar from starting weight to goal, "Log Weight" navigation
- **Pull-to-refresh** — swipe down to force data refresh

### Workouts Tab
Clean and focused — no clutter.

- **Last Activity card** — most recent activity across all types, with "View All" → full history
- **Today's Steps card** — step count with animated progress bar and "Update" dialog
- **Log Today As section:**
  - **Rest Day** — mark with optional note (e.g. "Recovery, felt tired"), remove anytime
  - **Walk Day** — mark if today was a walk day; steps sync from your step log
  - **Period Day** — log with optional note; feeds into AI context for tailored advice
- **+ Log Workout FAB** — starts the full workout flow

### Workout Logging Flow
1. **Muscle Group Selector** — 8 groups with color-coded cards: Chest & Triceps, Back & Biceps, Legs, Glutes, Shoulders, Full Body, Abs, Cardio
2. **Exercise Selector** — 70+ preset exercises; tap "Ask AI" for Claude-generated suggestions (cached 24h per group); add custom exercises by name
3. **Workout Logger** — reps/weight fields start empty (no pre-filled default); log sets with reps and weight stepper (±2.5 kg, 0–80 kg range); cardio exercises show duration instead of weight; "Finish Workout" saves duration + estimated calories

### Activity History Screen
Full chronological history of all activity types in one place:
- Workouts (green, with duration/sets/calories pills)
- Rest Days (purple, with note)
- Walk Days (blue, with step count)
- Period Days (rose, with note)

All styled as polished cards with colored icon boxes, stat pills, and type badges. Accessible from "View All" on both the Home screen and Workouts tab.

### Nutrition Screen
- **Daily summary** — calories and macros with progress bars, goals personalised from your profile
- **Meal sections** — food entries grouped by: Pre Breakfast, Breakfast, Lunch, Dinner, Snack, Post Dinner. Meal chips are horizontally scrollable — no labels cut off.
- **Food search** — searches 100+ Indian and international foods instantly, plus previously AI-looked-up foods from cache
- **Add food** — select food, choose meal, adjust quantity, live macro preview before confirming
- **Edit food entries** — tap the edit icon; change the **Quantity** field and calories/protein/carbs/fat/fiber recompute automatically from the food's per-serving base — no manual macro math required
- **AI food lookup** — type a food not in the database and tap "Ask AI" → Claude returns accurate macros, permanently cached so you never pay for the same lookup twice
- **Food DB sync** — on startup, food entries you've previously logged are synced back into the search cache so they appear in future searches without hitting the API again

**Food categories:** South Indian staples (idli, dosa, sambar, kootu, poriyal, adai, puttu, appam, etc.), Grains, Dal & Legumes, Dairy, Fats & Oils (ghee by tsp/tbsp), Eggs, Non-Veg, Vegetables, Breads & Cereals, International (pasta, noodles), Beverages, Nuts & Seeds, Fruits, Junk Food, Biscuits, Sweets, Protein supplements

### Macro History
Table view of the last 30 days — Date, Calories, Protein, Carbs, Fat — with a pinned goals row at the top. Values are color-coded by how close you got to goal (full color = ≥80%, dimmed = below target). Tap any day to expand it in place: see every food entry logged that day, edit or delete any of them, or **add a forgotten entry** directly against that day — all using the same quantity-driven macro recalculation as the Nutrition screen.

### Progress Screen
- **Weight chart** — line chart with gradient fill, filterable by 7D / 30D / 90D; plotted from all logged entries
- **Current weight header** — large display with goal and weekly trend
- **Weight history** — all entries listed with date, each deletable
- **Strength Progress chart** — exercise picker dropdown; line chart of max weight lifted per session; stat row showing First / Latest / Change / Progress%

### AI Insights Screen

**Weekly Summary tab:**
- Generate a full analysis of the past 7 days (workouts, avg calories/protein/carbs/fat vs goals, current weight, period day context)
- **Regenerate** at any time — clears previous and fetches fresh
- **Last 5 insights** — collapsible history cards with 2-line preview; expand to read full text; copy any insight to clipboard
- **Action chips** — after each summary, Claude generates 3 suggested follow-up questions as tappable chips
- **Follow-up answers** — tap a chip to get a focused 2–3 sentence answer; tap again to deselect; tapping a chip with an existing answer is a no-op (no duplicate calls)

**Rest Day Advisor tab:**
- Enter symptoms (sore, tired, heavy legs, etc.)
- Get a recommendation: Full Rest / Active Recovery / Train (with a specific muscle group suggestion)
- Result is copyable and saved to history

**Grounding & anti-hallucination:** every prompt sent to Claude carries a shared grounding rule — never estimate, round speculatively, or invent a number, date, or name that isn't in the data provided. Responses are additionally scanned for calendar dates that don't trace back to the input; if one shows up anyway, a visible warning is appended rather than letting an unverifiable claim pass silently.

### Daily Reminders
WorkManager periodic notifications: a workout reminder plus lunch and dinner meal-logging reminders, once per day, surviving app restarts and device reboots.

### Settings Screen
- **Edit Profile** — name, age, height, current weight, goal weight, activity level; recalculates macros dynamically
- **Backup** — exports all data as JSON to `Android/data/com.sunitha.fittrack/files/Backups/` — no permissions required on any Android version
- **Restore** — in-app list of all backup files with Restore and Delete buttons per file; confirmation dialog before any action; "Pick from another location" fallback for cross-device restore
- **Import** — import workout history from external sources

### Onboarding
First-launch profile setup (name, age, height, weight, goal, activity level) used to personalise calorie and macro goals throughout the app.

### Streak Calculation
Counts consecutive calendar days where you logged *anything* — a workout, rest day, walk day, food entry, or weight entry. If today hasn't been logged yet, yesterday's streak is preserved (doesn't reset mid-day). Day-boundary arithmetic uses `Calendar.add(DAY_OF_YEAR, -1)`, so it holds up across month and year rollovers, not just fixed millisecond subtraction.

---

## AI Caching Strategy

| Feature | Storage | Lifetime |
|---|---|---|
| Exercise suggestions | `ai_insights` table | 24 hours per muscle group |
| Food macro lookup | `ai_food_cache` table | Permanent |
| Weekly insights | `ai_insights` table | Until manually regenerated |
| Rest day advice | `ai_insights` table | Until manually regenerated |
| Action chips | ViewModel memory | Current session only (no DB schema change needed) |

---

## Design

### Color Palette
- **Home screen** — Rose/blush palette (`#D4608A` dusty rose, `#8B1A4A` deep cherry-rose, `#ECA4BE` soft blush)
- **Workout, Progress** — Green (`#4CAF50` primary, `#1B5E20` dark, `#81C784` light)
- **Nutrition** — Orange (`#FF7043`)
- **Rest Day** — Purple (`#6A1B9A`)
- **Walk Day** — Blue (`#0277BD`)
- **Period Day** — Rose (`#C2185B`)
- **Glutes** — Magenta (`#AD1457`)

### Navigation
5-tab bottom navigation bar: **Home | Workouts | Nutrition | Progress | AI**

Full-screen routes (no bottom bar): Onboarding, Settings, Import, Macro History, Activity History

---

## Offline Capability

All core features work with zero internet:
- Logging workouts, food, weight, steps, rest days, walk days, period days
- Viewing history, charts, streaks, macro history
- Searching the food database (including previously AI-looked-up foods)

Internet is only needed for:
- AI exercise suggestions (first request per muscle group per day — cached after)
- AI food macro lookup (first time only — cached permanently)
- Weekly insights generation (on demand)
- Rest day advice (on demand)
- Follow-up question answers (on demand, per question)

---

## Build Configuration

```
AGP (Android Gradle Plugin): 9.2.1
Kotlin:                       2.2.10
KSP:                          2.3.2
Compose BOM:                  2026.02.01
Min SDK:                      26 (Android 8.0 Oreo)
Target SDK:                   36
Compile SDK:                  36
Room:                         2.7.1 (schema v8)
Version:                      1.5 (versionCode 6)
```

---

## Setup

1. Clone the repo and open it in Android Studio.
2. Get a Claude API key from [console.anthropic.com](https://console.anthropic.com).
3. Add it to your **local** `local.properties` file (this file is gitignored and never committed):
   ```
   CLAUDE_API_KEY=sk-ant-api03-your-key-here
   ```
4. Sync Gradle and run. The key is exposed to the app at build time via `BuildConfig.CLAUDE_API_KEY` — it is never hardcoded in source.

If `CLAUDE_API_KEY` is left unset, the app still runs fully offline; AI-powered features (exercise suggestions, food lookup, weekly insights, rest day advice) will simply fail their API calls until a key is provided.

---

## What Could Come Next

- Step counting from the phone's built-in pedometer sensor (currently manual entry)
- Exact-time (not just periodic) workout reminders
- Workout plan / program scheduling (structured training cycles)
- Barcode scanning for packaged food nutrition labels
- Export data to CSV or share as PDF summary
- Menstrual cycle tracking with pattern insights
- Dark mode / light mode toggle in Settings
- Widget for home screen showing today's calorie ring
- iCloud / Google Drive sync for cross-device backup
