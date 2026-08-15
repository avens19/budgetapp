# budgetapp

The Weekly Budget Android app: https://play.google.com/store/apps/details?id=com.andrewovens.weeklybudget2

The backend and the web app (the iPhone answer) live in the sibling
`budgetappweb` repository, and the two share a design language — the CSS tokens
in `BudgetAppWeb/Content/app.css` are the same values as `res/values/colors.xml`
here.

## Building

Requires JDK 17+ and an Android SDK with platform 37 installed. Point
`local.properties` at the SDK (`sdk.dir=...`); that file is machine-specific and
is not tracked.

```
./gradlew assembleDebug     # debug APK (applicationId gets a .debug suffix)
./gradlew test lintDebug    # unit tests and lint
./gradlew assembleRelease   # minified release APK
./gradlew bundleRelease     # app bundle, which is what Play takes
```

- `minSdk` 23 (Android 6.0), `targetSdk` 37 (Android 17).
- Release builds run R8 with resource shrinking. Keep rules live in
  `app/proguard-rules.pro`.
- Lint runs with `warningsAsErrors`, so a new warning fails the build.

## UI

Material 3 (`Theme.Material3.DayNight.NoActionBar`), light and dark, with the
colour roles defined once in `res/values/colors.xml` and
`res/values-night/colors.xml` and wired to theme attributes in `themes.xml`.
Screens read those attributes rather than naming colours directly.

Navigation is a `BottomNavigationView` with three destinations — Week, Month,
Categories — installed by `Navigation`. The two category screens (by week, by
month) share `activity_categories.xml` and `CategoryChartActivity`; a segmented
control switches between them and "manage categories" sits in that screen's
toolbar. `WeekActivity` still owns the back stack: the other screens hand a
`GOTO_*` code back to it through `ScreenSwitcher` and finish.

`BaseActivity` owns the window insets for every screen from one layout
contract: `@id/toolbar` takes the status bar, `@id/bottom_nav` takes the
navigation bar (and the IME on screens without one), the root takes the
horizontal insets.

## Talking to the API

`API` reads its base URL from `BuildConfig.API_BASE_URL`. Release builds point
at production; debug builds point at `http://10.0.2.2:8099/api/` — the
emulator's route to a mock server on the host — so the sync can be exercised
without touching live data. Override it per build:

```
./gradlew assembleDebug -Pbudgetapp.debugApiUrl=https://budget.andrewovens.com/api/
```

Debug builds carry a `network-security-config` that permits cleartext to
`10.0.2.2` and `localhost` only; release builds still require TLS everywhere.

### The sync watermark

`Sync` pushes local changes, then pulls everything the server has changed since
its stored watermark. That watermark is **the server's clock, never the
device's**: both change-feed endpoints return an `X-Watermark` header, and the
client stores the earlier of the two (they are separate requests, so the later
one would skip anything written to the other collection in between).

If the header is absent the watermark is left alone, which costs a redundant
re-sync but cannot lose data. The previous implementation stamped the device's
own clock, so a device running fast silently skipped every change another
device made inside the skew.
