# Pane — Aurora Glass

Pane is a native Android phone/dialer project built with Kotlin and Jetpack Compose. V1 is local-first and does not require an account or backend.

## Current implementation

- Aurora Glass-inspired dark UI with Favorites, Recents, Contacts, and Keypad tabs.
- Contacts are queried from Android's Contacts Provider after runtime permission is granted.
- Favorites show contacts marked as starred in the phone's Contacts app.
- Recents are queried from Android's Call Log Provider after runtime permission is granted.
- Search filters real contacts and recent-call rows by name or number.
- Keypad and contact call actions open Android's system dialer with the selected number, so the user can confirm the call there.
- Runtime permission denial is handled with a useful empty state rather than demo contact data.
- GitHub Actions builds a debug APK and uploads it as an artifact when a build succeeds.

## Important current limits

- This is not yet a full default-dialer replacement. The app currently hands off to Android's system dialer; it does not yet own the active/incoming call UI.
- Android's default-dialer role, InCallService, native answer/reject controls, call-state lifecycle, and device-level call testing remain future work.
- A successful CI build is required before considering the current source build-verified. A CI build does not replace testing on a physical phone.
- Call-log and contacts access are permission-gated. Distribution through Google Play may require policy review for call-log permissions.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk 35, minSdk 29 (Android 10)
- JDK 17 / Gradle 8.9 in CI

## Build

Open the project in Android Studio with JDK 17 and Android SDK 35 installed, sync Gradle, then run the app configuration. GitHub Actions runs the debug build and publishes app-debug.apk as the pane-debug-apk workflow artifact when successful.

## Roadmap

1. Verify CI build and resolve compile errors.
2. Implement default-dialer role eligibility and request flow.
3. Add InCallService and real incoming/active call controls.
4. Add tests for data permissions, call state, and empty/error states.
5. Test on Android 10 / Redmi Note 7S and document verified behavior.
6. Prepare a signed release build only after device testing.
