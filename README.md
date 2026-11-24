# Curio Alarm

A minimal black-and-white Android clock that only stops ringing when you wake your
brain up: every alarm is dismissed by solving a **math**, **memory**, or **phrase** challenge.
Four sections via bottom navigation: **Alarms · Stopwatch · Timer · Settings**.

## Website

The download site is [`docs/index.html`](docs/index.html). The Download button serves one file, and only that file:

```
docs/downloads/CurioAlarm.apk
```

Put the APK at that path with that exact name. Do not rename it, and do not point the button at `dist/`. Replacing the file is enough — the link is `downloads/CurioAlarm.apk` relative to the site. `dist/CurioAlarm-debug.apk` is the CI copy; copy it into `docs/downloads/CurioAlarm.apk` when you want the site to serve the same build.

GitHub Pages: Settings → Pages → Deploy from a branch → `main` → `/docs`. The site is then `https://1-aftab.github.io/Curio2/` and the button downloads `https://1-aftab.github.io/Curio2/downloads/CurioAlarm.apk`.

## Features

- **Challenges to dismiss** — arithmetic, sequence memory, or exact phrase typing, each with
  Easy / Medium / Hard difficulty. Enable several types and one is picked at random per ring.
- **Reliable scheduling** — exact `setAlarmClock` triggers that survive Doze, reboot, app
  updates, and time/timezone changes; missed alarms are reported instead of silently dropped.
- **Custom sounds** — import any audio file (copied into app-private storage), preview it,
  and fade the volume in gradually. Each alarm (and the timer) can ring as **sound + vibration**,
  **sound only**, or **vibration only**.
- **Power button does not dismiss** — pressing power, Home, or swiping the notification turns
  the screen off at most for a moment. The tone keeps going and the challenge comes back.
  The only way out is solving the challenge (or tapping Dismiss on a timer). Force-stopping
  the app in system Settings is the one escape Android still allows.
- **Limited snooze** — off on alarms you already saved. New alarms and quick alarms can snooze
  5, 9, or 15 minutes, up to three times. Kiosk mode never snoozes. After the last one, only
  the challenge stops it.
- **Quick alarms** — 15 min, 30 min, or 1 hour from the home screen. One-shot, with a single
  9-minute snooze.
- **Wake streak** — consecutive mornings you actually solved the challenge.
- **First-run consent and tour** — permissions are explained before they are requested. The tour and consent screens are interactive; the checkbox still gates setup.
  Replay either from Settings. A short developer note sits under About.
- **Kiosk mode** — opens the challenge screen immediately and pins it where Android allows,
  for the truly heavy sleepers.
- **Stopwatch** — lap timing with fastest/slowest highlights; keeps correct time while the
  app is backgrounded and restores after process death (paused for review after a reboot).
- **Timer** — presets or custom h/m/s, optional label, pause / +1 min / repeat-last. The finish
  moment is registered with `setAlarmClock`, so it rings through the same reliable pipeline as
  alarms even with the app closed or the phone locked — dismissed with a single tap, no challenge.
- **Clean monochrome design** — a professional black-and-white look in both light and dark,
  switchable under **Settings → Appearance** (System / Light / Dark, applied everywhere
  including the ringing screen and system bars). Quiet sans-serif type, hairline borders,
  next-alarm countdown, day strips, and a full editor with dial *or* keyboard time entry
  and repeat presets. The footer scrolls with the content, clear of the "New alarm" button.

## Build

Requires JDK 17 and Gradle 8.9 (or use the CI workflows, which set both up):

```sh
gradle assembleDebug          # debug APK
gradle testDebugUnitTest      # unit tests
```

Signed release builds run in CI (`.github/workflows/build.yml`) using repository secrets;
pull requests are built and tested by `.github/workflows/pr-build.yml`. That workflow commits
`dist/CurioAlarm-debug.apk` onto the branch instead of uploading a GitHub Actions artifact.
Install that file. If Android says the package conflicts, uninstall the previous Curio Alarm
first — debug builds are not always signed with the same key.

## Permissions

The app asks only for what an alarm clock needs: notifications, exact alarms, full-screen
alerts, boot completion, and (optionally) ignoring battery optimizations. The home screen
shows a checklist with one-tap fixes when anything is missing.

---

made with curiosity by Aftab
