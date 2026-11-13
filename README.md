# Baby Sensory Playground

A simple, silent Android sensory app inspired by the optical in-display fingerprint sensor on the Samsung Galaxy S20 FE 5G.

## Features

- Choose between 1 and 24 circular sensor lights.
- Full-screen, near-black / darkened wallpaper background.
- Subtle illuminated sensor rings while idle.
- Stronger pulse and expanding optical-style rings while a circle is pressed.
- No sound and no internet permission.
- Gentle haptic feedback on touch.

The app recreates the visual interaction only. It does not use or imitate the phone's actual fingerprint authentication hardware.

## Build locally with Android Studio

1. Install Android Studio and Android SDK Platform 35.
2. Clone this repository:
   ```bash
   git clone https://github.com/YOUR-USERNAME/BabySensoryPlayground.git
   ```
3. Open the cloned folder in Android Studio.
4. Wait for Gradle sync to finish.
5. Connect an Android phone with USB debugging enabled.
6. Click **Run** to install the app.

To build a debug APK from Android Studio, select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

The APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build on GitHub automatically

A GitHub Actions workflow is included at `.github/workflows/android.yml`.

After pushing this project to a GitHub repository, open the repository's **Actions** tab. The workflow builds the debug APK on pushes to `main`, pull requests to `main`, or when manually started with **Run workflow**.

Download the APK from the workflow run's **Artifacts** section.

## Wallpaper behaviour

The app uses the wallpaper Android exposes through its public wallpaper API and darkens it so the sensor rings remain visible. Android may not expose a separate lock-screen-only wallpaper to third-party apps on every device. If unavailable, the app uses a dark gradient fallback.

## Project details

- Language: Kotlin
- UI: Jetpack Compose
- Minimum Android version: Android 8.0 (API 26)
- Target / compile SDK: 35
- App version: 1.1

## Safety note

Designed as a short, supervised visual activity. Keep screen brightness and play sessions comfortable for your child.
