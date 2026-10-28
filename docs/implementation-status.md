# Pane Liquid — Implementation Status & Device QA

## Current release status

Pane is a native Kotlin + Jetpack Compose Android dialer prototype. The project is **not yet device-verified or ready to call production-ready**. GitHub Actions compiles the debug APK and uploads `pane-debug-apk`; it does not run device/emulator call tests.

## Implemented in source

| Area | Current status | Notes |
|---|---|---|
| Five-tab app shell | Done in source | Favourites, Recents, Contacts, Keypad, Settings |
| UI / typography refresh | Done in source | Floating translucent navigation, cool blue/teal glass palette, reusable glass material, animated press scaling, and specular-edge gradients |
| Contacts and favourites | Done in source | Uses Android Contacts Provider after permission |
| Recents | Done in source | All/Missed filter, latest-first call history, timestamp, account/SIM label where Android reports it, call details and dialer handoff |
| Search | Done in source | Filters names/numbers in the main lists |
| DTMF keypad tone | Done in source | Plays local keypad tone when the DTMF preference is enabled |
| Default launch tab | Done in source | Persists selected tab locally |
| Theme selection | Done in source | System / Dark / Light |
| Liquid Glass intensity | Done in source | Persisted 0–100% Appearance slider controls shared glass surface gradients and edge highlights; lightweight fallback avoids continuous GPU shaders |
| Adaptive app icon | Done in source | Pane Liquid adaptive launcher icon with a translucent blue/teal phone glyph and Android monochrome layer |
| Call buttons | Done in source | Classic answer/reject or slide-to-answer |
| Incoming call style | Done in source | Fullscreen or resized translucent banner window in code |
| Custom call background | Done in source | Persisted photo/video URI is displayed on the call screen |
| Contact photo on call | Done in source | Looks up contact photo when enabled and permission is granted |
| Call image memory use | Done in source | Downsamples selected background and caller photos before displaying them, reducing large-bitmap memory spikes |
| Caller announcement | Done in source | Android TextToSpeech during ringing, when enabled |
| Flash on call | Done in source | Camera2 torch while ringing, when enabled and supported |
| Fake call | Done in source | Clearly simulated; it does not create a real telecom call |
| SIM preference | Partial | Selected PhoneAccountHandle is passed to the system dialer; behavior varies by dialer/MIUI |
| Speed dial | Partial | Slots 1–5 save numbers and display tappable shortcuts on Keypad; no labels/contact picker yet |
| Call blocker | Partial | Saved list can be rejected by PaneInCallService only when Android routes the call through Pane as default phone app |
| Quick response | Partial | Opens the SMS composer with a saved response and rejects the incoming call; user must review/send the message |
| Sound & vibration | Done as a system handoff | Opens Android sound settings; Pane does not override system ringer policy |
| Accessibility | Done as a system handoff | Opens Android accessibility settings |
| Call Settings / Voicemail | Partial | Opens Android phone settings; carrier-specific voicemail setup is not automated |
| Language | Partial | Hindi localizes core navigation and settings labels; some explanatory copy and the call screen remain English |
| Call Back Screen | Implemented in source | After a real call disconnects, shows last caller/number with Call back and Close when enabled; device behavior still needs testing |
| Recents Edit | Partial | Toggles the edit presentation; does not delete or modify Android's call-log entries |
| Liquid Glass long-press | Partial | Main navigation, many list rows, keypad keys, settings selectors and in-call action buttons have haptic spring-sheet feedback. Full native-style real-time refraction is not claimed; blur behavior varies by Android version |
| Privacy Policy | Partial | In-app privacy explanation only; no published legal-policy URL |
| Ads / subscription / billing | Done | No ad, subscription, paywall, or billing integration is included |

## CI / build checks

- GitHub Actions workflow: `.github/workflows/android.yml`
- Checks: JDK 17 + Android SDK 35 + Gradle compile of `:app:assembleDebug`
- Output: `app-debug.apk`, uploaded as artifact `pane-debug-apk`
- The workflow is a **compile/build check only**, not a full automated test suite.
- No claim of physical-device verification is made from a green CI build.
- Latest verified application-source build before this documentation sync: commit `6871b2600f89701ffd10dae4f3218ccfad1b4b81`; GitHub Actions run: [40458083724](https://github.com/vinitprajapat0822-coder/Pane-iglass/actions/runs/40458083724). The `pane-debug-apk` artifact was uploaded successfully.

## Liquid Glass upgrade

- App display name updated to Pane Liquid; package ID remains `com.vinit.pane`.
- The Compose main screen now uses reusable `LiquidGlassMaterial`, `LiquidGlassTabBar`, `LiquidGlassTab`, `LiquidGlassKey`, `LiquidGlassSearchBar`, and `LiquidGlassIntensityController` components.
- Appearance intensity persists locally in `pane_settings` and is supplied through a CompositionLocal so surface styling updates globally in the main dialer.
- Android adaptive launcher resources are used for the icon. The default phone-app role remains an explicit Android system confirmation and is requested after runtime permission handling when not already held.
- Optical refraction is represented with lightweight layered translucent gradients and specular borders, not true live background refraction. Device-specific blur and touch behavior still require testing.

## Recent build log

- UI refresh: revised app header, five-tab floating translucent navigation, typography, settings icon tiles/dividers, keypad sizing and recent-filter contrast.
- Phone utility actions: DTMF key tones, preferred SIM handoff, persisted launch tab, saved speed-dial shortcuts, local call-block list, system accessibility/call/sound settings handoffs, and user-confirmed quick-response SMS composer.
- Call screen: photo/video background rendering, contact-photo lookup, downsampled image decoding, compact Banner/Fullscreen layout mode, post-call callback screen, and haptic glass overlay for call action buttons.
- Language: Hindi localization for core navigation and settings labels, with immediate state refresh. Other explanatory strings remain English.
- Build fixes recorded: corrected Material3 `LocalTextStyle` import, replaced unsupported Android call-settings constant with an intent action string, removed unsupported XML `windowDimAmount`, imported delegated Compose state setter, and reset full-screen window bounds after Banner mode.
- Latest source commit listed above compiled successfully and uploaded the debug APK. Automated checks have not exercised real phone calls or MIUI-specific behavior.

## Redmi Note 7S manual test plan

Run these after installing the latest debug APK. Record Pass / Fail and the Android version/MIUI build for each:

1. Grant and deny Contacts, Call Log, Phone State and Camera permissions; confirm the app remains usable without them.
2. Select Pane as Default Phone App; verify it can be returned to the previous dialer.
3. Test an incoming call with screen on, screen locked, Fullscreen style and Banner style.
4. Answer, reject, end, mute and speaker-toggle a real call; test both button styles.
5. Test outgoing call handoff and preferred SIM on each SIM slot. Verify Android's confirmation is still shown.
6. Test Caller Announcer, Flash On Call and DTMF tone after explicitly enabling each setting.
7. Select image and video call backgrounds; test startup, rotation, lock screen, call end and repeat calls.
8. Enable Contact Photo on Call and test saved/unsaved callers and a number with no image.
9. Add a known test number to Call Blocker; verify auto-rejection only while Pane is the active default dialer. Remove the number afterward.
10. Save speed-dial slots 1–5, relaunch, and tap each keypad shortcut.
11. Trigger Quick Response; confirm the SMS composer contains the selected message and does not silently send an SMS.
12. Long-press nav tabs, recents rows/filters, keypad keys, settings rows and style selectors. Check dismissal, haptics, clipping and animation smoothness.
13. Test light/dark/system theme, Hindi labels, long contact names, long phone numbers, empty contacts/recents and screen rotation.
14. Watch for crashes, leaked flashlight state, a stuck call screen, clipped banner content, poor text contrast or UI jank.

## Release gate

Do not call this production-ready until the above device tests are recorded, all critical failures are fixed, and a release build (not just the debug APK) has been prepared and reviewed.
