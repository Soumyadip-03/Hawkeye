# 🦅 Haykeye — Local-First Encrypted Urgent Message Notebook

**Haykeye** is a local, privacy-first Android application designed to intercept, classify, alert, and store urgent notifications from **WhatsApp**, **WhatsApp Business**, **Gmail**, and **Google Messages** into an encrypted, organized local notebook.

By operating 100% on-device without any external server or cloud dependency, Haykeye guarantees that your personal messages and notifications never leave your phone.

---

## 🌟 Key Features

### 📬 1. Notification Listening & Provider Parsing
- **Selective Interception:** Automatically filters incoming notifications from WhatsApp, WhatsApp Business, Gmail, and Google Messages, immediately discarding unsupported applications for privacy-by-design.
- **Provider Parsers:** Dedicated parsers (`WhatsAppParser`, `GmailParser`, `MessagesParser`) that handle `MessagingStyle` notifications, group messages, sender names, and subjectless emails safely.

### 🧠 2. Multilingual Rule-Based Urgency Engine
- **Multilingual Support:** Evaluates message text against rule-based keywords in **English, Bengali script (বাংলা), Hindi script (हिंदी), Banglish, and Hinglish**.
- **Word-Boundary Regex (`\b`):** Prevents accidental substring matches (e.g. *"urgent"* matches *"It is urgent!"* but **never** *"detergent"*).
- **VIP Lists:** Provider-scoped VIP Senders and VIP Groups that always trigger urgency alerts regardless of text content.
- **Text Heuristics:** Detects multiple exclamation marks (`!!`) and excessive uppercase text (>=70% capital letters).
- **Per-Group Noise Control:** Configure individual group policies: *Monitor Normally*, *Alert on @Mention Only*, or *Ignore Group Entirely*.

### 🔒 3. Military-Grade Database Encryption at Rest
- **SQLCipher 256-bit AES:** Local SQLite database encrypted at rest using **SQLCipher 4.19.1** (100% compliant with Android 15+ 16 KB memory page sizes).
- **Android Keystore Protection:** Passphrase generated with `SecureRandom` and protected using a 256-bit AES-GCM master key stored inside hardware-backed **Android Keystore**.
- **Cloud Backup Exclusion:** Database files and security preferences are excluded from Android Auto Backup to prevent unkeyable database restoration on new devices.

### 🔔 4. Urgent Alert System
- **Custom Ringtone Alerting:** Plays a distinct customizable alert ringtone (`RingtoneManager`) only when a *new* urgent message is inserted into the database.
- **High-Importance System Channel:** Posts neutral system notifications (*"A new urgent message was detected from WhatsApp"*).

### 📓 5. Premium Dark-First Material 3 Notebook UI
- **OLED Pitch Black Theme:** Designed with a pure pitch black (`#000000`) AMOLED theme by default, with customizable Warm Beige / Light theme support in Settings.
- **Provider Tabs:** Filter saved messages instantly across **All**, **WhatsApp**, **Gmail**, and **Messages**.
- **Real-Time Search:** Search messages by sender, group name, preview, or full text off the main thread with zero search logging.
- **Message Management:** Read/unread toggling, swipe-to-delete with undo capability, and bulk *"Clear All Read"* action.

### 🚀 6. 3-Tier Source App Navigation (Phase 10)
- **Priority 1 (Active Notification Action):** Invokes the active notification's `PendingIntent` to open the **exact chat thread / conversation / email** directly in the source app.
- **Priority 2 (Contact Phone Mapping / Deep Links):** Uses `ContactMappingRepository` or verified phone numbers/emails to open direct chat windows (`whatsapp://send?phone=...`, `smsto:`, `mailto:`).
- **Priority 3 (Safe App Fallback):** Launches the provider application's home screen safely if exact navigation is unavailable.

---

## 🛠️ Technology Stack & Dependencies

| Category | Component / Library | Version |
| :--- | :--- | :--- |
| **Language** | Kotlin | `2.2.10` |
| **Build Tool** | Android Gradle Plugin (AGP) | `9.3.3` |
| **Compiler Toolchain** | KSP (Kotlin Symbol Processing) | `2.3.9` |
| **UI Framework** | Jetpack Compose BOM / Material 3 | `2026.02.01` |
| **Database** | Room Runtime & KTX | `2.8.4` |
| **Encryption** | SQLCipher (`net.zetetic:sqlcipher-android`) | `4.19.1` |
| **Key Security** | Android Keystore (AES-256-GCM) | Native |
| **Background Tasks** | WorkManager KTX | `2.11.2` |
| **SDK Targets** | Min SDK: `26` (Android 8.0) \| Target SDK: `37` (Android 15) | - |
| **Application ID** | `com.soumyadip.haykeye` | - |

---

## 🏗️ Architecture Overview

Haykeye follows a clean, reactive, unidirectional data flow architecture:

```
[System Notification] ──► [NotificationListener]
                               │ (Filter Package)
                               ▼
                        [Provider Parsers]
                         (WhatsApp/Gmail/Messages)
                               │
                               ▼
                       [UrgencyClassifier]
                    (Keywords/VIPs/Heuristics/Group Policy)
                               │
                       Is Urgent? ──► No (Discarded)
                               │
                              Yes
                               ▼
                      [AppDatabase (Room + SQLCipher)]
                               │
                      Row Inserted Successfully?
                               │
                              Yes
                               ▼
                    [AlertManager] ──► Ringtone & System Notification
                               │
                               ▼
                      [NotebookViewModel] ──► [NotebookScreen UI]
```

Detailed architecture diagrams and data flow documentation are available in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 📲 Installation and Setup Guide

### 1. Prerequisites
- **Android Studio:** Ladybug or newer.
- **JDK:** Java 11 or Java 17.
- **Android SDK:** API Level 37 installed via SDK Manager.

### 2. Clone & Build
```bash
# Clone repository
git clone https://github.com/your-username/Haykeye.git
cd Haykeye

# Build Debug APK
.\gradlew.bat :app:assembleDebug
```

### 3. Deploy to Device / Emulator
1. Connect an Android phone or launch an Android Emulator (API Level 26+).
2. Click **Run 'app'** in Android Studio.

### 4. Enable Required Permissions
1. **Notification Access:** Upon launching Haykeye, tap **Enable** on the top banner or open **Settings ➔ Apps ➔ Special App Access ➔ Notification Access** and toggle **Haykeye ON**.
2. **Post Notifications Permission (Android 13+):** Tap **Grant** on the permission banner to allow alert notifications.

---

## 🔒 Privacy & Security Blueprint

- **100% On-Device:** Zero telemetry, zero internet networking permissions required, and zero cloud uploads.
- **Passphrase Encryption:** Database passphrase is generated using `SecureRandom` (256-bit) and encrypted using an AES-256-GCM key inside Android's hardware Keystore module.
- **Data Retention:** Configurable auto-cleanup (7, 30, 60, 90 days, or Never) executed in the background by `WorkManager` (`CleanupWorker`).
- **Data Loss Consequences:** Uninstalling the app or clearing app data erases the hardware Keystore key. By design, encrypted database files cannot be decrypted without the matching hardware key.

---

## 🧪 Testing Verification

### Automated Unit & Integration Tests

Run host JVM unit tests:
```bash
.\gradlew.bat :app:testDebugUnitTest
```
**Test Result:** **`14 PASSED, 0 FAILED`**
- Case-insensitive keyword matching.
- Multilingual keyword matching (Bengali, Hindi, Banglish, Hinglish).
- Substring match prevention (*"detergent"* vs *"urgent"*).
- Provider-scoped VIP sender & group matching.
- Mention signal & text heuristics.
- Urgent followed by ordinary message in same conversation.
- Updated `MessagingStyle` buffer handling.
- Repeated notification update deduplication.

### Instrumented Android On-Device Tests

Run instrumentation tests on a connected device:
```bash
.\gradlew.bat :app:connectedDebugAndroidTest
```
**Verified on realme RMX5030 (`HA7L65PV6DYHPZMB`):**
- Room DAO CRUD operations.
- File-backed SQLite database persistence.
- Room schema migration v1 to v2.
- WhatsApp, Gmail, and Google Messages parsing.
- Navigation package mapping and safe launch fallback.
- WorkManager retention rescheduling on boot.
- Plaintext-to-SQLCipher database migration (`DatabaseMigrator.kt` canonical `sqlcipher_export` with schema version transfer).

---

## ⚠️ Limitations & Future Roadmap

### Current Limitations
- **Notification Truncation:** Notification payloads exceeding 100 characters are truncated into previews by Android OS.
- **Provider Deep Link Restrictions:** When a notification is dismissed, WhatsApp and Gmail do not allow external apps to jump directly into a specific message ID without an active status bar `PendingIntent`.

### Future Enhancement Roadmap
- **Machine Learning Urgency Classifier:** On-device TensorFlow Lite / MobileBERT model for context-aware message intent classification.
- **Biometric App Lock:** Fingerprint / Face Unlock protection when opening Haykeye.
