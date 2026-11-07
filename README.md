# Curio Alarm

A minimal, dark-by-design Android alarm clock that only stops ringing when you wake your
brain up: every alarm is dismissed by solving a **math**, **memory**, or **phrase** challenge.

## Features

- **Challenges to dismiss** — arithmetic, sequence memory, or exact phrase typing, each with
  Easy / Medium / Hard difficulty. Enable several types and one is picked at random per ring.
- **Reliable scheduling** — exact `setAlarmClock` triggers that survive Doze, reboot, app
  updates, and time/timezone changes; missed alarms are reported instead of silently dropped.
- **Custom sounds** — import any audio file (copied into app-private storage), preview it,
  fade the volume in gradually, and add vibration.
- **Kiosk mode** — opens the challenge screen immediately and pins it where Android allows,
  for the truly heavy sleepers.
- **Nightstand UI** — deep-ink theme with a champagne accent, serif display type, next-alarm
  countdown, day strips, and a full editor with dial *or* keyboard time entry and repeat presets.

## Build

Requires JDK 17 and Gradle 8.9 (or use the CI workflows, which set both up):

```sh
gradle assembleDebug          # debug APK
gradle testDebugUnitTest      # unit tests
```

Signed release builds run in CI (`.github/workflows/build.yml`) using repository secrets;
pull requests are built and tested by `.github/workflows/pr-build.yml`.

## Permissions

The app asks only for what an alarm clock needs: notifications, exact alarms, full-screen
alerts, boot completion, and (optionally) ignoring battery optimizations. The home screen
shows a checklist with one-tap fixes when anything is missing.

---

made with curiosity by Aftab
