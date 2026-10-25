# Apna AI Assistant — Android starter

This is a native Android Kotlin starter for your Vivo T4x 5G.

## What it does
- Tap-to-speak voice input (Punjabi/Hindi/English language options)
- Spoken replies using Android TextToSpeech
- Tells date/time, opens YouTube, and searches the web
- Typed-command fallback
- Microphone permission is requested transparently

## Important limits
- This is **not** an always-listening wake-word assistant. Android restricts background microphone use; reliable always-on wake-word detection requires a carefully designed foreground service, persistent notification, battery handling, and testing on the actual phone.
- It does not read SMS/call history or send messages/calls.
- Speech recognition may use the speech service configured on your phone.
- This is an unsigned debug APK, suitable for testing, not Play Store release.

## Build APK for free with GitHub Actions
1. Create a new GitHub repository named `apna-ai-assistant` and upload all files from this folder.
2. Ensure the default branch is `main` or `master`.
3. Open **Actions** in the repository and enable workflows if asked.
4. Run **Build Android APK** from Actions (or push a commit).
5. Open the completed workflow run, scroll to **Artifacts**, and download `apna-ai-assistant-debug-apk`.
6. Extract the ZIP and install `app-debug.apk` on your phone. Android may ask you to allow installs from that browser/file app.

## Local build
With Java 17 and Gradle installed: `gradle assembleDebug`
