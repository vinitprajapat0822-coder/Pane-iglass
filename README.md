# Pane — Aurora Glass

A native Android dialer UI prototype built with Kotlin and Jetpack Compose.

## Current state
- Aurora Glass-inspired dark interface
- Favorites, Recents, Contacts and Keypad navigation
- Search across demo contacts
- Demo contact list and keypad input
- **Native calling, real contacts, call history, permissions and default-dialer role are not connected yet**
- Build has not yet been verified in an Android build environment

## Stack
- Kotlin
- Jetpack Compose / Material 3
- minSdk 29 (Android 10)
- compileSdk 35
- Local-first v1; no backend or account required

## Design direction
- Midnight navy base with translucent-style panels and thin glass borders
- Lavender-blue accent and green call affordance
- Glass styling is limited to navigation and interactive surfaces to preserve readability
- Keep blur and animation lightweight for older Android devices

## Build
Open this repository in Android Studio with a compatible JDK 17 and Android SDK 35 installed, then sync Gradle and run the `app` configuration.

## Next implementation stages
1. Verify Gradle sync and compile; fix any errors
2. Add runtime permission rationale and contacts integration
3. Add native calling through Android Telecom APIs and the required default-dialer flow
4. Add real call-log integration, favorites persistence, and accessibility checks
5. Add light/system themes and UI/device testing

This project is a work in progress and is not yet a production-ready replacement for the system dialer.
