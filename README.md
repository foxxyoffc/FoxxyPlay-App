<div align="center">

<img src="assets/branding/foxxyplay-neon.svg" width="150" alt="FoxxyPlay Neon Fox"/>

# FOXXYPLAY

### NEON AUDIO INTELLIGENCE

<img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=18&duration=2800&pause=900&color=B78AFF&center=true&vCenter=true&repeat=true&width=550&lines=YOUR+MUSIC.+YOUR+WORLD.;EXPERIENCE+THE+SOUND.;POWERED+BY+FOXXYOFFC." alt="Animated FoxxyPlay tagline"/>

**A modern Android music experience.**

**Developer: Foxxy Offc** <img src="assets/branding/verified-badge.svg" width="19" height="19" alt="Verified badge"/>

<br/>

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
<img src="https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"/>

<br/><br/>

[ FEATURES ](#-features) •
[ INSTALLATION ](#-installation) •
[ BUILD ](#-build) •
[ BRANDING ](#-branding)

</div>

---

## ◈ About FoxxyPlay

FoxxyPlay is a native Android music application developed by **FoxxyOffc**, built with Kotlin and Jetpack Compose.

Designed around a modern interface, music playback experience, and a dedicated developer edition.

## ◈ Features

- Modern native Android interface
- Music playback experience
- Library and playlist management
- Search and music discovery
- Favorites and playback queue
- Now Playing interface
- Public and Developer editions
- Developer API integration
- Maintenance and application management

*Feature availability depends on the configured services and application edition.*

## ◈ Editions

| Edition | Application ID | Description |
|---|---|---|
| Public | `com.foxxy.play` | Main FoxxyPlay application |
| Developer | `com.foxxy.play.developer` | Developer management edition |

## ◈ Technology

| Technology | Usage |
|---|---|
| Kotlin | Main programming language |
| Jetpack Compose | UI toolkit |
| Android SDK | Native Android platform |
| Gradle Kotlin DSL | Build configuration |
| OkHttp | HTTP networking |
| Firebase | Public edition service integration |

## ◈ Branding

Five visual directions for the FoxxyPlay identity.

<div align="center">

| Neon Fox | Cyber Orbit |
|:---:|:---:|
| <img src="assets/branding/foxxyplay-neon.svg" width="130"/> | <img src="assets/branding/foxxyplay-orbit.svg" width="130"/> |
| Audio Wave | Foxxy Monogram |
| <img src="assets/branding/foxxyplay-wave.svg" width="130"/> | <img src="assets/branding/foxxyplay-monogram.svg" width="130"/> |

**Holographic Disc**

<img src="assets/branding/foxxyplay-disc.svg" width="130"/>

</div>

All branding assets are available in `assets/branding/`.

## ◈ Installation

### Requirements

- Android Studio
- JDK 21
- Android SDK 36
- Android SDK Platform 26 or newer for the app

### Clone repository

```bash
git clone YOUR_REPOSITORY_URL
cd FoxxyPlay
```

### Configure local properties

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
sdk.dir=/your/android/sdk/path
musicapi.clientId=YOUR_MUSIC_CLIENT_ID
```

### Firebase configuration

For the Public edition, add your own Firebase configuration:

```text
app/src/public/google-services.json
```

Never commit private credentials or local configuration files.

## ◈ Build

### Public edition

```bash
./gradlew assemblePublicDebug
```

### Developer edition

```bash
./gradlew assembleDeveloperDebug
```

### Build both editions

```bash
./gradlew assemblePublicDebug assembleDeveloperDebug
```

APK output:

```text
app/build/outputs/apk/
├── public/debug/
└── developer/debug/
```

The Developer edition skips the Google Services processing task. The Public edition needs its own Firebase configuration for Firebase integration.

## ◈ Project Structure

```text
FoxxyPlay/
├── app/
│   └── src/
│       ├── main/
│       ├── public/
│       └── developer/
├── assets/
│   └── branding/
├── backup-v1.0.5/
├── gradle/
├── scripts/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties.example
└── README.md
```

## ◈ Security

- Local configuration is excluded from Git.
- Firebase configuration is excluded from Git.
- Signing credentials must never be committed.
- Use your own service credentials when building.
- Never put server-side private keys inside an Android APK.

## ◈ Developer

<div align="center">

<img src="assets/branding/foxxyplay-orbit.svg" width="85"/>

**FOXXYOFFC**

*Designing. Developing. Creating.*

</div>

---

<div align="center">

### FOXXYPLAY — MADE WITH PASSION.

**FoxxyOffc © 2026. All Rights Reserved.**

<sub>Built for music lovers. Powered by creativity.</sub>

</div>
