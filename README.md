# 🚀 Network Speed Monitor for Android

A modern, lightweight, and flicker-free network speed monitor built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Fully optimized for **Samsung One UI** and modern Android (targeting Android 15 / SDK 35).

---

## ✨ Features

* **⚡ Real-Time Status Bar Speed:** Displays live download and upload speeds directly in the top status bar.
* **📊 Stacked Dual-Line & Single-Line Icons:** Choose between stacked `↓` / `↑` throughput rows or a large single-speed indicator.
* **🛡️ Flicker-Free Engine:** Uses dynamic time-delta division (`SystemClock.elapsedRealtime()`) and a configurable sliding-window moving average to eliminate socket batching flicker.
* **🎛️ Drawer Notification Toggle:** Hide the expanded notification card in your notification drawer while keeping the status bar icon running.
* **📱 Samsung One UI Optimized:** Direct intent navigation to Samsung Device Care battery whitelisting across One UI 3.x, 4.x, 5.x, 6.x, 7.x, and 8.x.
* **🔒 100% Private & Offline:** Zero analytics, zero ads, zero tracking, and no external network calls.

---

## 🛠️ Tech Stack & Architecture

* **UI:** Jetpack Compose + Material 3
* **Language:** Kotlin 2.0+
* **Concurrency:** Kotlin Coroutines & `StateFlow`
* **Target SDK:** 35 (Android 15) | **Min SDK:** 26 (Android 8.0)
* **Foreground Service Type:** `android:foregroundServiceType="dataSync"`
* **Architecture:** Unidirectional Data Flow (UDF) + Layered Architecture

---

## 📜 Privacy Policy

The Privacy Policy for this application can be found at:
* [Markdown Privacy Policy](PRIVACY_POLICY.md)
* [HTML Web Page](privacy-policy.html) / `https://aalokkumar0.github.io/net-speed/privacy-policy.html`

---

## 📄 License

```
Copyright (c) 2026 Aalok Kumar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
