# budgetapp

The Weekly Budget Android app: https://play.google.com/store/apps/details?id=com.andrewovens.weeklybudget2

## Building

Requires JDK 17+ and an Android SDK with platform 37 installed. Point
`local.properties` at the SDK (`sdk.dir=...`); that file is machine-specific and
is not tracked.

```
./gradlew assembleDebug     # debug APK (applicationId gets a .debug suffix)
./gradlew test lintDebug    # unit tests and lint
./gradlew assembleRelease   # minified release APK
```

- `minSdk` 23 (Android 6.0), `targetSdk` 37 (Android 17).
- Release builds run R8 with resource shrinking. Keep rules live in
  `app/proguard-rules.pro`.

## Release signing

The keystore is deliberately **not** in the repository. Release builds pick it
up from Gradle properties (typically `~/.gradle/gradle.properties`) or the
matching environment variables, and fall back to producing an unsigned APK when
neither is set:

| Gradle property            | Environment variable        |
| -------------------------- | --------------------------- |
| `budgetapp.storeFile`      | `BUDGETAPP_STORE_FILE`      |
| `budgetapp.storePassword`  | `BUDGETAPP_STORE_PASSWORD`  |
| `budgetapp.keyAlias`       | `BUDGETAPP_KEY_ALIAS`       |
| `budgetapp.keyPassword`    | `BUDGETAPP_KEY_PASSWORD`    |
