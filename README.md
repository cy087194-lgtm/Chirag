# Lia AI

Voice-first Android AI companion powered by **Gemini Live API**.

## Features

- Real-time voice conversation with Gemini 3.1 Flash Live
- Text chat with Gemini 2.0 Flash
- Beautiful animated 3D orb UI
- Offline-safe: API key stored locally
- Two flavors: `direct` (full features) and `play` (Play Store safe)

## Requirements

- Android 8.0+ (API 26)
- Gemini API key — get one at [Google AI Studio](https://aistudio.google.com)

## Setup

1. Clone the repo
2. Open in Android Studio
3. Build `directDebug` or `directRelease`
4. Add your Gemini API key in Settings inside the app

## Tech Stack

- Kotlin + Jetpack Compose (Material3)
- Gemini Live API (WebSocket BidiGenerateContent)
- OkHttp, Coroutines, StateFlow
- compileSdk 36, minSdk 26

## Security

- No API keys in source code
- No Firebase or Play licensing in `play` flavor
- Keys stored in local SharedPreferences only
