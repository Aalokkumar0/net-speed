# Privacy Policy for Network Speed Monitor

**Last Updated:** August 11, 2026

**Application Name:** Network Speed Monitor  
**Package Name:** `com.netspeed.monitor`  
**Developer:** Aalok Kumar  
**Contact Email:** aalokkumar0@gmail.com  

---

## 1. Overview & Privacy Commitment

**Network Speed Monitor** ("we", "our", or "the app") is committed to respecting and protecting your privacy. This Privacy Policy explains how our application operates and confirms our strict policy regarding user data.

**Key Principle:** Network Speed Monitor is a **100% privacy-focused, offline utility application**. We do **NOT** collect, store, transmit, sell, or share any personal information, browsing activity, or network traffic data with anyone.

---

## 2. Information Collection and Processing

### A. Network Traffic & Speed Statistics
* **How It Works:** Network Speed Monitor measures your download and upload speeds by reading local operating system kernel counters provided by Android’s public API (`android.net.TrafficStats.getTotalRxBytes()` and `android.net.TrafficStats.getTotalTxBytes()`).
* **On-Device Only:** All throughput calculations, moving average smoothing, and session totals are processed strictly in real-time on your device's local memory (RAM).
* **Zero Payload Inspection:** The app only reads the total number of bytes transferred. It **never** inspects, captures, logs, or intercepts the contents of your network packets, URLs, messages, files, or browsing history.
* **No Remote Transmission:** None of your speed or usage statistics are ever sent to external servers or cloud services.

### B. Personal Information
* We do **not** collect your name, email address, phone number, device IMEI, Android ID, IP address, or location.
* The app does not require account creation, login credentials, or user registration.

---

## 3. Permissions Used and Why

Network Speed Monitor requests only the minimum permissions required for core functionality:

| Permission | Purpose |
| :--- | :--- |
| **`android.permission.POST_NOTIFICATIONS`** | Required on Android 13+ (API 33+) to display the real-time speed number in the status bar and notification shade. |
| **`android.permission.FOREGROUND_SERVICE`** | Required to run the continuous speed monitoring service in the background. |
| **`android.permission.FOREGROUND_SERVICE_DATA_SYNC`** | Required on Android 14+ (API 34+) and Android 15 (API 35) to maintain continuous foreground service execution for periodic data synchronization. |
| **`android.permission.ACCESS_NETWORK_STATE`** | Used to detect whether your device is connected to Wi-Fi, Cellular (5G/4G/LTE), or disconnected. |
| **`android.permission.RECEIVE_BOOT_COMPLETED`** | Allows the app to automatically resume the speed monitor service after you restart your device (only if "Auto-start on boot" is enabled in settings). |
| **`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** | Used to open Samsung One UI Device Care / Android battery settings so you can exempt the app from background sleep restrictions. |

---

## 4. Third-Party Services, SDKs & Analytics

* **No Advertising SDKs:** We do not display ads and do not integrate advertising networks (e.g., Google AdMob, Unity Ads, AppLovin).
* **No Tracking or Analytics SDKs:** We do not use third-party analytics or crash-reporting services (e.g., Firebase Analytics, Google Analytics, Facebook SDK, Mixpanel).
* **No Cookies or Trackers:** The app does not employ cookies, web beacons, or tracking identifiers.

---

## 5. Data Storage and Retention

* **Local Preferences:** Your settings (such as chosen speed unit format, status bar icon preferences, and boot options) are saved locally on your device via standard Android `SharedPreferences`.
* **Complete Removal:** Uninstalling the application completely removes all locally stored preferences and session counters from your device.

---

## 6. Children's Privacy

Network Speed Monitor does not address anyone under the age of 13, nor does it collect personally identifiable information from children. It is safe for all age groups.

---

## 7. Security

Because all network throughput calculations and settings remain entirely on your local device, your data is not subject to remote transmission vulnerabilities. We adhere to modern Android security and sandboxing standards.

---

## 8. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any updates will be posted on this page with a revised "Last Updated" date.

---

## 9. Contact Us

If you have any questions, feedback, or concerns regarding this Privacy Policy or the app's privacy practices, please contact:

* **Email:** aalokkumar0@gmail.com  
* **Developer:** Aalok Kumar
