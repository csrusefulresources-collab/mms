# App Cloner — Kotlin Android Project

A native Kotlin Android app for finding installed apps and keeping a personal list of app shortcuts.

## Features

- Lists launchable apps available to the app on the device.
- Searches by app name or package name.
- Adds/removes shortcuts in **My Clones**.
- Opens the original app, Android App Info, or its Play Store page.
- Uses large controls, descriptive accessibility labels, and an announced result count.

## Important limitation

This app manages shortcuts; it does **not** create independent copies of other apps. It cannot duplicate another app's private data, login session, or package identity. Ordinary Android apps cannot silently clone arbitrary apps. Separate app instances require a supported Android work profile or a compatible virtualization solution, which has device and app limitations.

## Build the APK using GitHub on an Android phone

1. Download and extract this ZIP on your phone.
2. Open your GitHub repository in the browser or GitHub app.
3. Upload the **contents inside** the `AppCloner-Kotlin` folder. Do not upload the ZIP file itself.
4. If your Android project files currently live inside a repository folder named `cloner`, upload the Android project files (such as `app`, `build.gradle.kts`, and `settings.gradle.kts`) into that `cloner` folder and replace the old versions. Otherwise, upload them to the repository root.
5. Important: GitHub only detects workflow files inside the repository-root `.github/workflows/` folder. Copy this ZIP's `.github/workflows/build.yml` to that exact root location, replacing the old `build.yml` if appropriate. Do not leave the only workflow copy inside `cloner/.github/`.
6. Commit the uploaded files to the `main` branch.
7. Open the repository's **Actions** tab and select **Build APK**.
8. If it did not start automatically, tap **Run workflow** and confirm the `main` branch.
9. When the run finishes successfully, open that run and scroll to **Artifacts**.
10. Download **AppCloner-APK**. It contains `AppCloner-debug.apk`.

The workflow also places the APK in the repository's `APK` folder and commits it to the branch. For that commit to work, open repository **Settings → Actions → General → Workflow permissions** and select **Read and write permissions**.

## Build configuration

- Kotlin Android application
- Application ID: `com.shaheen.appcloner`
- Minimum Android: 6.0 (API 23)
- Compile/target SDK: 35
- Java: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin plugin: 2.0.21

## Privacy and package visibility

The app requests `QUERY_ALL_PACKAGES` to discover installed apps. Google Play restricts broad package visibility and may require a policy declaration and approval. The saved shortcut list is stored locally in this app's private preferences.

## Build status

The source and workflow have been statically reviewed and the previously reported Kotlin `singleLine` compile error is fixed using `setSingleLine(true)`. A full Android build still needs to run in GitHub Actions because this working environment does not have the Android SDK/Gradle build environment installed.
