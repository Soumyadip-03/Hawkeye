# 🏗️ Haykeye Architecture & System Design

Haykeye follows a reactive, local-first architecture built with modern Android development practices, Jetpack Compose, Room, SQLCipher, and Kotlin Coroutines.

---

## 📐 System Architecture Diagram

```mermaid
flowchart TD
    subgraph Android_OS ["Android OS & System Events"]
        SBN["StatusBarNotification"]
        BOOT["Intent.ACTION_BOOT_COMPLETED"]
    end

    subgraph Listeners_Receivers ["Background Services & Receivers"]
        NL["NotificationListenerService<br/>(NotificationListener.kt)"]
        BR["BootReceiver.kt"]
    end

    subgraph Security_Layer ["Security & Key Management"]
        KS["KeystoreManager.kt<br/>(Android KeyStore AES-256-GCM)"]
        KM["DatabaseKeyManager.kt<br/>(haykeye_security_prefs)"]
        DM["DatabaseMigrator.kt<br/>(Plaintext to SQLCipher)"]
    end

    subgraph Parsing_Layer ["Provider Parsers"]
        WP["WhatsAppParser.kt"]
        GP["GmailParser.kt"]
        MP["MessagesParser.kt"]
    end

    subgraph Classification_Engine ["Urgency Classifier Engine"]
        UC["UrgencyClassifier.kt"]
        CR["ClassifierConfigRepository.kt"]
    end

    subgraph Storage_Layer ["Encrypted Room Storage"]
        DB[("AppDatabase<br/>SQLCipher 256-bit AES")]
        DAO["UrgentMessageDao.kt"]
    end

    subgraph Alerts_and_Retention ["Alerts & WorkManager"]
        AM["AlertManager.kt<br/>(Ringtone & Notification)"]
        CW["CleanupWorker.kt<br/>(WorkManager Auto-Retention)"]
    end

    subgraph UI_Layer ["Jetpack Compose UI"]
        VM["NotebookViewModel.kt"]
        NS["NotebookScreen.kt<br/>(Notebook UI)"]
        SS["SettingsScreen.kt<br/>(Settings UI)"]
        ML["MessageLauncher.kt<br/>(Phase 10 Navigation)"]
    end

    %% Flow Connections
    SBN -->|Intercept| NL
    BOOT -->|Reschedule Worker| BR
    BR -->|Schedule| CW

    NL -->|Package Filtering| WP & GP & MP
    WP & GP & MP -->|Parsed Messages| UC

    CR -->|Config & Policies| UC
    UC -->|Classify Urgency| NL

    KM -->|256-bit Passphrase| Security_Layer
    KS -->|Encrypt/Decrypt Key| KM
    DM -->|Migrate Plaintext DB| DB

    NL -->|If Urgent: Insert| DAO
    DAO -->|Persist Message| DB
    DAO -->|On New Row Inserted| AM

    DB -->|Flow Stream| VM
    VM -->|Observe State| NS
    NS -->|Manage Settings| SS
    NS -->|Launch App| ML
    CW -->|Periodic Delete| DAO
```

---

## 🧩 Component Responsibilities

### 1. Notification Listener (`NotificationListener.kt`)
- Extends `NotificationListenerService`.
- Filters notifications from supported packages (`com.whatsapp`, `com.whatsapp.w4b`, `com.google.android.gm`, `com.google.android.apps.messaging`).
- Tracks connection lifecycle (`onListenerConnected()`, `onListenerDisconnected()`, `requestRebind()`).

### 2. Provider Parsers (`WhatsAppParser`, `GmailParser`, `MessagesParser`)
- **`WhatsAppParser.kt`**: Extracts sender names, group names, timestamps, and message text from `MessagingStyle` notifications. Ignores batch group summary notifications.
- **`GmailParser.kt`**: Extracts email senders, subjects, and previews, gracefully fallback-handling subjectless emails.
- **`MessagesParser.kt`**: Extracts SMS/MMS/RCS text messages from Google Messages.

### 3. Urgency Classifier (`UrgencyClassifier.kt`)
- Pure, stateless classification engine.
- Evaluates urgency against **Default Multilingual Keywords** (English, Bengali, Hindi, Banglish, Hinglish) using word boundary regex (`\b`).
- Checks provider-scoped VIP Senders, VIP Groups, `@mention` metadata, text heuristics (`!!` & 70%+ uppercase text), and Per-Group Noise Control policies.

### 4. Security & Encryption (`com.soumyadip.haykeye.security`)
- **`KeystoreManager.kt`**: Generates a 256-bit AES master key in `AndroidKeyStore` and performs hardware-backed encryption/decryption.
- **`DatabaseKeyManager.kt`**: Generates a 256-bit `SecureRandom` passphrase, encrypts it via `KeystoreManager`, and stores the ciphertext in private app preferences.
- **`DatabaseMigrator.kt`**: Automatically migrates legacy plaintext databases into SQLCipher encrypted databases using `sqlcipher_export`.

### 5. Source App Navigation (`MessageLauncher.kt`)
- Implements 3-tier navigation priority:
  1. **Priority 1 (Active Notification Action):** Invokes `pendingIntent.send(context, 0, null)` from `Activity` context to open exact chat/email thread.
  2. **Priority 2 (Contact Phone Mapping / Deep Links):** Uses `ContactMappingRepository` or verified phone/email to launch direct chat windows (`whatsapp://`, `smsto:`, `mailto:`).
  3. **Priority 3 (Safe App Fallback):** Launches provider app home screen safely if exact navigation is unavailable.
