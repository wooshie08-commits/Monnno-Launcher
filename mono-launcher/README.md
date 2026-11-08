# MONO Launcher 1.0.0

A minimalist, monochrome, touch-first home screen for Android 6.0+ (built for the Xiaomi Redmi 3S Prime).
It is a normal app, not a ROM. It does not touch the bootloader, recovery or partitions.

## Status: source complete, NOT yet compiled or tested
The sandbox this was written in has no Android SDK, no network and no Java compiler.
Nothing has been built, installed or run. Expect a few compile errors on the first build.
No APK and no checksum exist yet. The GitHub Actions workflow produces both.

## Get the APK (no Android Studio needed)
1. Create a new GitHub repository and push this folder to it.
2. Open the repo's Actions tab, run "Build APK" (it also runs on every push).
3. Download the `MONO-Launcher-apk` artifact: the APK and its `.sha256` file.
4. On the phone: allow installing from unknown sources, install the APK.
5. Settings > Apps > Default apps > Home app (or press Home) and choose MONO Launcher.

Local build: JDK 17, Android SDK platform 34, Gradle 8.7, then `gradle assembleDebug`
(or open the folder in Android Studio). Output: `app/build/outputs/apk/debug/app-debug.apk`.
The APK is debug-signed, which is fine for installing on your own phone.

## Design choices
- Java, framework APIs only, zero dependencies: nothing newer than Android 6 is required.
- minSdk 23, targetSdk 30. All UI is built in code.
- Only permission: RECEIVE_BOOT_COMPLETED (re-arms reminders after a reboot). No internet permission.
- Notes and reminders are JSON files in the app's private storage, written atomically.

## Features
Implemented (written, unverified):
- Home: split clock (HH | MM), date, greeting, battery %, pending reminder count, vertical list
- App drawer with live search; Enter opens the first match
- Long-press an app: App info, Uninstall, Pin/Unpin on home
- Notes: create, edit, autosave, delete, persistent
- Reminders: date + time picker, notification at that time, re-armed after reboot
- Music (opens installed music player), Files (tries known file managers, then a system picker), Settings (Android)
- Launcher settings: light/dark, 12/24-hour, greeting, font (thin sans / serif / mono), default-home shortcut
- Swipe up on home opens the drawer; "‹ Home" on every screen; Back never leaves the launcher

Not implemented:
- Page dots / multiple home pages, notification count (needs notification access permission)
- Notes search, folders, export; recurring reminders
- Release signing, app icon art beyond a simple vector
- Any E-Ink-specific refresh handling (your screen is an LCD)

Access: long-press "Settings" on home, or tap "Launcher" in the drawer, for launcher settings.
