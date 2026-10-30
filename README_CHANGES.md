# Student Alarm - changes in this version

## Startup speed
- `res/drawable/ic_ah_logo.png`: 1254x1254 / 1.87 MB -> 384x384 / ~170 KB (was decoded at full size on 4 screens).
- Removed the duplicate 1.87 MB logo from `public/assets/aistudio/`.
- `StudentAlarmApplication`: database and DataStore are now `by lazy` (not opened in onCreate).
- `MainActivity`: AndroidX SplashScreen; saved theme + mode are read on a background thread (max 500 ms) before the first frame, so no theme flash and no jump to Mode Selection.
- `AlarmViewModel`: initial theme / mode / "mode chosen" come from that startup snapshot.
- `HomeScreen`, `MainActivity`: `collectAsStateWithLifecycle`.
- `app/build.gradle.kts`: release `isMinifyEnabled = true`, `isShrinkResources = true`; added `core-splashscreen` and `profileinstaller`.
- `app/src/main/baseline-prof.txt`: hand-written baseline profile for the startup path.
- `proguard-rules.pro`: keep rules for enums (stored by name) and data classes.

## Logo
- Splash: `Theme.App.Starting` (themes.xml), `ic_splash_logo.png`, `splash_background` color; set on MainActivity in the manifest.
- Home header already shows the logo; it now also shows the active mode chip + alarm icon + "Next: h:mm a".

## Alarm icon in the status bar
- `AlarmScheduler`: the main alarm uses `setAlarmClock(AlarmClockInfo)`; Android shows its own clock icon while an alarm is set. Smart reminders and snooze still use exact alarms.
- BUG FIX: `cancelPendingIntent` built an Intent without the action, so Android never matched it and old alarms were NOT really cancelled. It now uses the same action.
- New `ic_stat_alarm.xml` (white vector) is the notification small icon.

## Database safety
- `AppDatabase`: `fallbackToDestructiveMigration()` -> `fallbackToDestructiveMigrationFrom(1, 2)`.
  Data is no longer wiped silently on future updates. IMPORTANT: if you ever change a table (version 3 -> 4),
  a Room `Migration` is required, otherwise the app will crash on start. Tell AI Studio: "add a Room Migration".

## Not changed
- There is no blur effect in the Glass theme (it is already a gradient), so nothing to optimise there.
- Not compiled/tested here (no Gradle in this environment). Build in Android Studio and send any error.

## Stopwatch (new)
- New bottom-bar tab "Stopwatch" (all modes): Start / Pause, Lap, Reset, lap list with best (green) and worst (red) lap.
- `ui/StopwatchViewModel.kt`: uses SystemClock.elapsedRealtime(), so it stays exact when you switch tabs or lock the screen.
- `ui/screens/StopwatchScreen.kt`: keeps the screen on while running; only created when the tab is opened.
- `MainActivity.kt`: `Screen.Stopwatch`, the tab, and the screen branch.
