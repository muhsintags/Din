<div align="center">

# Scriptorium

**A personal digital library for sacred and classical texts — Torah, Bible, Quran, Sahih al-Bukhari, Talmud, Bhagavad Gita, and translation, all in one place.**

[![Build Status](https://github.com/muhsintags/Din/actions/workflows/build.yml/badge.svg)](https://github.com/muhsintags/Din/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Latest Release](https://img.shields.io/github/v/release/muhsintags/Din)](https://github.com/muhsintags/Din/releases/latest)

[Download](#download) ▸ [Features](#features) ▸ [Tech Stack](#tech-stack) ▸ [Build](#build--run) ▸ [Contributing](#contributing)

</div>

---

## About

**Scriptorium** *(formerly "Din Kütüphanesi")* is a native Android app that brings sacred and classical texts from multiple traditions together in a single, clean, offline-friendly library — built for reading, comparing, and translating scripture without the clutter.

## Features

- ▸ **Torah** — full text, downloadable for offline reading
- ▸ **Bible** — full text, downloadable for offline reading
- ▸ **Quran** — full text, downloadable for offline reading
- ▸ **Sahih al-Bukhari** — full text, downloadable for offline reading
- ▸ **Talmud** — full text, downloadable for offline reading
- ▸ **Bhagavad Gita** — full text, downloadable for offline reading
- ▸ **Translation** — integrated Google Translate support for cross-language reading
- ▸ **Offline after first download** — texts are fetched once and stored locally via Room; no connection needed afterward
- ▸ **Modern UI** — built entirely in Jetpack Compose with Material 3

## Download

Every push to `main` triggers an automatic build. You can grab an APK two ways:

| Option | What you get | Where |
|---|---|---|
| **Latest Release** | Stable, signed release APK | [Releases page](../../releases/latest) |
| **Dev Build** | Freshest debug build (may be unstable) | [Actions tab](../../actions) → latest run → Artifacts |

## Tech Stack

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

## Build & Run

**Locally / in Codespaces:**
```bash
./gradlew assembleDebug      # debug build
./gradlew assembleRelease    # signed release build (requires keystore secrets)
```

**Via GitHub Actions (recommended):**
Push to `main` → check the [Actions tab](../../actions) → download `app-debug` or `app-release` from the run's artifacts.

## Project Structure

```
Din/
├── app/                  # Main application module
├── gradle/               # Gradle wrapper & version catalog
├── .github/workflows/    # CI/CD build pipeline
└── Versions/             # Version history / notes
```

## Version History

| Version | Notes |
|---|---|
| v1.0 | Initial release |
| v1.1 – v1.9 | Bug fixes and refinements |
| v2.0 | 3 new texts planned: Guru Granth Sahib (Sikhism), Book of Mormon, Buddhist texts (Tripitaka / Sutta) |

## Roadmap

- [ ] Publish to APKPure
- [ ] Firebase cleanup (unused dependencies)
- [ ] Google Play release (pending developer account)
- [ ] Turkish commentary layer for texts

## Contributing

This is currently a solo personal project, but bug reports, suggestions, and feedback are always welcome — open an [Issue](../../issues) any time.

Feel free to fork the repo and build your own version too. Some ideas to get you started:

- ▸ Build a **philosophy library** — Stoic, Confucian, or other classical texts instead of religious ones
- ▸ **Translate the app into your own language** before official support arrives — a native speaker's translation is often more accurate than a machine one (e.g. Japanese, before it's officially supported)
- ▸ Add a **new sacred or classical text** not currently included
- ▸ Build a **comparative reading mode** across traditions or translations

## Built With AI Assistance

This project was developed with the help of AI tools:

- **Claude** (Anthropic)
- **ChatGPT** (OpenAI)
- **Google AI Studio**
- **Gemini**

## License

Licensed under the [MIT License](LICENSE).

---

<div align="center">
Made with a lot of coffee and GitHub Actions minutes.
</div>
