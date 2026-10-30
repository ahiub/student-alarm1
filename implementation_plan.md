# Implementation Plan: Comprehensive Student Alarm System Overhaul

A systematic plan to fix and enhance the existing native Android app across 5 targeted areas: dynamic theming without hard-coded colors, customizable study timer presets, named Room-backed custom modes, external audio ringtone selection with permission persistence, and complete icon library alignment.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following implementation decisions have been confirmed via Phase 1 clarifications:
> - **Pre-configured Custom Modes**: The app will initialize with pre-configured starter modes: **"Exam Mode"** (optimized for study blocks & exams) and **"Lecture Mode"** (focused on timetable & class reminders).
> - **Audio Fallback Strategy**: If an attached custom ringtone file is deleted or inaccessible when ringing, the foreground alarm service will safely fall back to **Vibrate only** without crashing or aborting the alarm.

---

## 1. Overview & Core Concept
The **Student Alarm (AH)** app provides offline academic schedule management, exact alarms, voice notes, study timer (Pomodoro), and class timetables. This update addresses runtime styling rigidity, timer flexibility, mode configuration persistence, audio playback customization, and iconography clarity.

---

## 2. User Experience & Visual Design

### Problem 1: Real Dynamic Theming & System Bars
- **Root `AppTheme` System**: Remove all hardcoded colors (`Color(0xFF...)`, `BrandBackground`, etc.) from components, cards, navigation bars, and dialogs.
- **Semantic M3 Token Mapping**:
  - `MaterialTheme.colorScheme.background` -> Screen scaffolds
  - `MaterialTheme.colorScheme.surface` / `surfaceVariant` -> Cards and dialogs
  - `MaterialTheme.colorScheme.primary` / `secondary` -> Primary action chips, indicators, and buttons
  - `MaterialTheme.colorScheme.onSurface` / `onBackground` -> Primary body text and icons
  - `MaterialTheme.colorScheme.outline` / `outlineVariant` -> Card borders and dividers
- **10 Real Palettes**:
  1. *Neon Glow*: `#070B1F` deep navy, `#2EC8FF` electric blue, `#8B5CF6` purple, `#FF9F43` orange.
  2. *Glass*: Frosted translucent surfaces (`#1E293B` at 70% opacity or translucent glass) with border glow and soft shadow; solid fallback for legacy devices.
  3. *Midnight Dark*: Pure AMOLED black (`#000000`), card `#0D0D0D`, `#38BDF8` soft blue accent.
  4. *Study Desk Light*: Cream paper background (`#FBF8EE`), sticky-note surfaces (`#FFFDF5`), warm brown typography (`#451A03`).
  5. *Sunrise*: Deep dark sunset background (`#200B17`) with glowing orange and yellow accents (`#FF7A00`, `#FFB800`).
  6. *Ocean*: Deep oceanic blue (`#061325`) with bright cyan & aqua teal accents (`#00E5FF`).
  7. *Forest*: Deep pine green (`#07170E`) with emerald and lime accents (`#10B981`, `#84CC16`).
  8. *Rose Sunset*: Velvet plum (`#17091A`) with rose pink and magenta accents (`#F43F5E`, `#C084FC`).
  9. *Minimal White*: High-contrast light canvas (`#FFFFFF`) with pure black typography (`#0F172A`).
  10. *Auto*: Day/Night time-aware theme (Study Desk Light from 6 AM to 7 PM; Neon Glow at night).
- **Settings Appearance Grid**: 2-column or horizontal preview cards displaying palette chips, card background, and an active checkmark. Changing applies instantly via DataStore Flow.
- **Edge-to-Edge System Bars**: Window status bar and navigation bar automatically tinted to match the active theme.

### Problem 2: Pomodoro Study Timer Customization & Presets
- **Expanded Timer Configuration**:
  - Focus Duration (1–180 minutes)
  - Short Break Duration (1–60 minutes)
  - Long Break Duration (1–60 minutes)
  - Sessions Before Long Break (e.g., every 4 sessions)
- **Input Controls**:
  - `+` / `-` increment buttons
  - Quick chips (`15m`, `25m`, `30m`, `45m`, `60m` focus; `5m`, `10m`, `15m` break)
  - Direct number entry modal with input validation (disallows 0, negative, or blank)
  - Reset to default (25/5/15) action
- **Named Presets**:
  - Store presets in Room/DataStore (e.g., "Exam Cram 50/10", "Quick Focus 20/5").
  - User can pick, save, rename, or delete presets.
  - Automatic stage transition: Focus -> Short Break -> Focus ... -> Long Break after N sessions, with sound + vibration on each period finish.

### Problem 3: Named Custom Modes (Room Database)
- **Room Entity `CustomMode`**:
  - `id: Long`, `name: String`, `enabledFeatures: String` (comma-separated feature keys: `subjects`, `alarm_types`, `smart_reminders`, `timetable`, `study_timer`, `streak`), `theme: String?`, `alarmToneUri: String?`.
  - Default starter modes: **"Exam Mode"** and **"Lecture Mode"**.
- **Management UI**:
  - Modal or dedicated sheet to create, select, rename, duplicate, and delete custom modes with confirmation.
  - Active mode name displayed as an interactive chip at the top of the Home screen and in Settings.
  - Switching mode is purely a presentation filter and **never deletes alarms or user data**.

### Problem 4: Custom Audio Ringtone & System Tones
- **Tune Picker Sheet**:
  - Built-in tones: System Default, Gentle Chime, Academic Bell, Digital Beep.
  - System Ringtones: Query Android `RingtoneManager` for available alarm and ringtone options.
  - Phone Storage: `ACTION_OPEN_DOCUMENT` with `audio/*`, granting persistable URI permissions via `contentResolver.takePersistableUriPermission`.
- **Audition Controls**: Play/Preview button with live playback progress and Stop button.
- **Assignment Scope**: Set per-alarm and default per custom mode.
- **Playback & Safe Fallback**: In `AlarmRingingService`, if the custom audio URI is missing or fails to decode, fall back to **Vibrate only** without crashing.

### Problem 5: Icon Alignment & Accessibility
- Ensure `androidx.compose.material:material-icons-extended` dependency is enabled.
- Audit all icon usages across all screens:
  - Navigation: `Icons.Default.Alarm`, `Icons.Default.CalendarToday`, `Icons.Default.School`, `Icons.Default.Timer`, `Icons.Default.Settings`.
  - Alarm Types: `Icons.Default.Alarm`, `Icons.Default.Assignment`, `Icons.Default.School`, `Icons.Default.Class`.
  - Timer: `Icons.Default.PlayArrow`, `Icons.Default.Pause`, `Icons.Default.Refresh`, `Icons.Default.LocalFireDepartment`.
- Touch target standard: minimum 48dp with accessible `contentDescription` and tint bound to `LocalContentColor.current` or `MaterialTheme.colorScheme.onSurface`.
- Verify adaptive mipmap launcher icons display correctly without resource conflicts.

---

## 3. Technical Architecture & System Flow

```
┌─────────────────────────────────────────────────────────────┐
│                       MainActivity                          │
│                             │                               │
│              Collects Theme from DataStore                  │
│                             ▼                               │
│                   StudentAlarmTheme                         │
│       (Dynamically sets M3 ColorScheme & System Bars)       │
└─────────────────────────────┬───────────────────────────────┘
                              │
       ┌──────────────────────┼──────────────────────┐
       ▼                      ▼                      ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  Home View   │       │ Timetable &  │       │  Study Timer │
│  Active Mode │       │  Calendar    │       │ Presets &    │
│  Chip & Hero │       │  Classes &   │       │ Custom Cycles│
│  Alarm Card  │       │  Smart Dates │       │              │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │
       └──────────────────────┼──────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         Data Layer                          │
│  ┌──────────────────────┐        ┌───────────────────────┐  │
│  │     AppDatabase      │        │      DataStore        │  │
│  │ ───────────────────  │        │ ────────────────────  │  │
│  │  • AlarmEntity       │        │  • Selected Theme     │  │
│  │  • SubjectEntity     │        │  • Active Mode ID     │  │
│  │  • TimetableEntry    │        │  • App Language       │  │
│  │  • CustomMode        │        │  • Default Snooze     │  │
│  │  • PomodoroPreset    │        │  • Streak Data        │  │
│  └──────────────────────┘        └───────────────────────┘  │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│            AlarmScheduler & AlarmRingingService             │
│  • Exact Alarms & Smart Advance Reminders (1d, 3h, 30m)     │
│  • Custom Ringtone URI Playback (Fallback: Vibrate Only)    │
│  • Full-Screen Lockscreen UI & Notification Actions         │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Verification Plan
- **Theme Testing**: Switch through all 10 themes in Settings > Appearance, verifying background, card surface, text readability, button contrast, and system status bar transitions.
- **Timer Validation**: Test custom durations (e.g. 50/10), custom named preset creation, countdown completion audio/vibrate trigger, and session count increment.
- **Custom Mode Lifecycle**: Create "Exam Mode", verify toggle-based UI filtering, rename/duplicate, delete with confirmation, and ensure no existing alarms or subjects are lost.
- **Audio & Fallback**: Select system ringtone and external audio file, preview in settings, test ring in alarm service, and simulate missing URI to verify graceful vibration fallback.
- **Build Verification**: Run `compile_applet` and local unit tests (`gradle :app:testDebugUnitTest`) to ensure clean compilation.
