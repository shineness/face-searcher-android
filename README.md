# Face Searcher Android

A simple Android app for searching similar faces using image hashing (pHash algorithm).

## Features

- 🎯 Select target face image
- 📂 Scan folder for images
- 🔍 Find similar faces using perceptual hash
- 📝 Error logging for debugging
- 🔧 GitHub Actions CI/CD

## Tech Stack

- Kotlin
- Jetpack Compose
- Coil (Image loading)
- Retrofit (API calls)
- pHash (Perceptual Hash)

## Build

```bash
cd face-searcher-android
./gradlew assembleDebug
```

## Error Logs

The app saves logs to internal storage. Access them through the app's log button in the top bar.

## API Configuration

Edit `ApiClient.kt` to configure a backend API for more accurate face recognition.
