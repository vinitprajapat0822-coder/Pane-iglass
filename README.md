# Pane — Aurora Glass

Pane is a native Android phone/dialer project built with Kotlin and Jetpack Compose. V1 is local-first and has no account or backend.

## Implemented in source

- Aurora Glass dark UI with Favorites, Recents, Contacts, and Keypad.
- Real contacts and starred contacts from Android's Contacts Provider, behind runtime permission.
- Recent calls from Android's Call Log Provider, behind runtime permission.
- Search by contact name or number.
- Keypad/contact actions hand off to Android's system dialer for user confirmation.
- Telecom-bound `InCallService` and an incoming/active call screen with answer, reject, end, mute, and speaker-route controls.
- A user-approved default-phone-app role request helper.
- GitHub Actions workflow intended to compile and upload a debug APK artifact.

## Important limitations

- The new source has been committed, but compilation and physical-device behavior are **not yet verified**. Check the Actions run before installing.
- Default-dialer eligibility and call behavior can vary by Android/MIUI version. Test role selection, incoming calls, outgoing calls, lock-screen behavior, audio routing, and permission denial on a real device.
- Call controls operate only on `Call` objects supplied by Android Telecom; the app must be selected as the default phone app and have its service bound.
- Call-log and contacts permissions are permission-gated. Google Play distribution may require policy review for call-log access.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21
- compileSdk 35, minSdk 29 (Android 10)
- JDK 17 / Gradle 8.9 in CI

## Build and verify

Open in Android Studio with JDK 17 and Android SDK 35, sync Gradle, and build the debug variant. GitHub Actions uploads `app-debug.apk` as `pane-debug-apk` if compilation succeeds. Do not treat the source as device-verified until calls are tested on the Redmi Note 7S.
