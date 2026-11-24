# Where the download file goes

The Download button on the site points at this folder. It does not look in `dist/`.

Put the Android package here and name it exactly:

```
docs/downloads/CurioAlarm.apk
```

Rules:

- Keep the name `CurioAlarm.apk`. Capital C, capital A. GitHub Pages is case-sensitive.
- Do not rename it to `app.apk`, `CurioAlarm-debug.apk`, or a versioned name. The button would 404.
- Replacing the file is enough. You do not edit `index.html`.
- Commit the file. `*.apk` is gitignored except this path.
- `dist/CurioAlarm-debug.apk` is the CI copy for installing from the repo. Copy that file here if you want the site to serve the same build.

Optional, next to the APK:

```
docs/downloads/CurioAlarm.apk.sha256
```

The install section reads that file and shows the hash. The pull-request workflow writes it when it publishes a build.

If you host the `docs` folder somewhere else, upload this `downloads` folder with `index.html`. The link is relative: `downloads/CurioAlarm.apk`.
