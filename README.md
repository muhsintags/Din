<div align="center">

# 📖 Scriptorium

**A personal digital library for sacred and classical texts — Torah, Bible, Quran, Sahih al-Bukhari, Talmud, Bhagavad Gita, and translation, all in one place.**

[![Build Status](https://github.com/muhsintags/Din/actions/workflows/build.yml/badge.svg)](https://github.com/muhsintags/Din/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Latest Release](https://img.shields.io/github/v/release/muhsintags/Din)](https://github.com/muhsintags/Din/releases/latest)

[Download](#-download) • [Features](#-features) • [Tech Stack](#-tech-stack) • [Build](#-build--run) • [Contributing](#-contributing)

</div>

---

## About

**Scriptorium** *(formerly "Din Kütüphanesi")* is a native Android app that brings sacred and classical texts from multiple traditions together in a single, clean, offline-friendly library — built for reading, comparing, and translating scripture without the clutter.

## ✨ Features

- 📖 **Torah** — full text, offline-ready
- 📖 **Bible** — full text, offline-ready
- 📖 **Quran** — full text, offline-ready
- 📖 **Sahih al-Bukhari** — full text, offline-ready
- 📖 **Talmud** — full text, offline-ready
- 📖 **Bhagavad Gita** — full text, offline-ready
- 🌍 **Translation** — integrated Google Translate support for cross-language reading
- 💾 **Offline-first** — texts are stored locally via Room, no connection needed after first load
- 🎨 **Modern UI** — built entirely in Jetpack Compose with Material 3

## 📱 Download

Every push to `main` triggers an automatic build. You can grab an APK two ways:

| Option | What you get | Where |
|---|---|---|
| **Latest Release** | Stable, signed release APK | [Releases page](../../releases/latest) |
| **Dev Build** | Freshest debug build (may be unstable) | [Actions tab](../../actions) → latest run → Artifacts |

## 🛠 Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM |
| Local storage | Room |
| Networking | Retrofit + OkHttp |
| Async | Kotlin Coroutines |
| CI/CD | GitHub Actions |
| Dev environment | GitHub Codespaces |

> This project is built entirely in the cloud — no local Android Studio setup required. Every build, test, and release runs through GitHub Actions and Codespaces.

## 🏗 Build & Run

**Locally / in Codespaces:**
```bash
./gradlew assembleDebug      # debug build
./gradlew assembleRelease    # signed release build (requires keystore secrets)
```

**Via GitHub Actions (recommended):**
Push to `main` → check the [Actions tab](../../actions) → download `app-debug` or `app-release` from the run's artifacts.

## 📂 Project Structure

```
Din/
├── app/                  # Main application module
├── gradle/               # Gradle wrapper & version catalog
├── .github/workflows/    # CI/CD build pipeline
└── Versions/             # Version history / notes
```

## 🗺 Roadmap

- [ ] Publish to APKPure
- [ ] Firebase cleanup (unused dependencies)
- [ ] Google Play release (pending developer account)
- [ ] Turkish commentary layer for texts

## 🤝 Contributing

This is currently a solo personal project, but bug reports, suggestions, and feedback are always welcome — open an [Issue](../../issues) any time.

Feel free to fork the repo and build your own version too — add texts, languages, or features that matter to you.

## 🤖 Built With AI Assistance

This project was developed with the help of AI tools:

- **Claude** (Anthropic)
- **Google AI Studio**
- **Gemini**
- **ChatGPT**

## 📄 License

Licensed under the [MIT License](LICENSE).

---

<div align="center">
Made with ☕ and a lot of GitHub Actions minutes.
</div>
