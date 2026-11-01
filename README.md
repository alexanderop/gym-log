# Gym Log

Offline Android app for logging gym workouts. Upper/Lower templates, set logging (kg or bodyweight, reps, RIR), history. No account and no network.

Package: `dev.alexop.gymlog`. UI is German. A workout that is still running is stored on the device and comes back after the app is killed.

## Install the APK

GitHub Actions builds `app-debug.apk` and uploads it as the `gymlog-debug` artifact.

On a phone, open the APK (allow installs from unknown sources). Or with a computer:

```
adb install -r app-debug.apk
```

## Build

JDK 17+ and Android SDK 36.

```
./gradlew assembleDebug testDebugUnitTest lint
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
