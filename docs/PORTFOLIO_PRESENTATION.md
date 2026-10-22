# 💼 Developer Portfolio & Resume Presentation

This document contains materials for showcasing **Haykeye** in a developer portfolio, on GitHub, and in technical resumes.

---

## 📌 GitHub Repository Short Description

> **Haykeye** — A local, privacy-first Android notification notebook built with Kotlin, Jetpack Compose, Room, and SQLCipher 256-bit AES encryption. Intercepts, classifies, alerts, and organizes urgent messages from WhatsApp, Gmail, and Google Messages 100% on-device.

---

## 📝 Portfolio Project Summary

**Haykeye** solves notification overload by acting as an intelligent, local-first safety net for urgent communications. By leveraging Android's `NotificationListenerService` and a rule-based multilingual classification engine (supporting English, Bengali, Hindi, Banglish, and Hinglish), Haykeye detects high-priority messages from WhatsApp, Gmail, and Google Messages.

Key technical highlights include:
- **Local-First & Serverless:** Executes 100% on-device without cloud dependencies, network permissions, or data tracking.
- **Hardware Key Encryption:** Encrypts local SQLite database storage using **SQLCipher 256-bit AES** with master key protection inside **Android Keystore**.
- **OLED Pitch Black UI:** Modern Jetpack Compose & Material 3 UI with dynamic theme toggles, real-time search, provider tabs, and WorkManager background cleanup.

---

## 🚀 Key Technical Achievements

1. **Rule-Based Multilingual NLP Pipeline:** Implemented word-boundary regex (`\b`) matching across 5 languages/scripts (English, Bengali, Hindi, Banglish, Hinglish) to eliminate false-positive substring matches (*"urgent"* vs *"detergent"*).
2. **Encrypted Local Storage at Rest:** Integrated SQLCipher 4.19.1 with Room 2 via `SupportOpenHelperFactory`, generating 256-bit passphrases encrypted via hardware-backed Android Keystore (AES-GCM).
3. **Safe Plaintext-to-Encrypted Database Migration:** Designed an in-app database migration pipeline that converts legacy SQLite databases to SQLCipher using `sqlcipher_export`, verifying schema integrity before swapping database files.
4. **3-Tier Smart Source App Navigation:** Developed an intent routing strategy that fires active notification `PendingIntent`s to launch WhatsApp, Gmail, and Messages straight into exact conversation threads.
5. **Background Resilience & OEM Hardening:** Built a `BOOT_COMPLETED` broadcast receiver and WorkManager auto-retention cleanup worker with realme UI / ColorOS battery optimization diagnostics.

---

## 📄 Resume Bullet Points

### **Android Developer / Kotlin Software Engineer**

- **Engineered Haykeye**, a privacy-first Android notification notebook in **Kotlin & Jetpack Compose** that intercepts, classifies, and alerts urgent notifications from WhatsApp, Gmail, and SMS 100% on-device.
- **Implemented 256-bit AES database encryption at rest** using **SQLCipher and Android Keystore**, protecting sensitive messages and designing an automated plaintext-to-encrypted database migration pipeline.
- **Developed a rule-based multilingual urgency engine** supporting 5 languages/scripts (English, Bengali, Hindi, Banglish, Hinglish) with word-boundary regex matching and provider-scoped VIP filtering.
- **Built an intelligent 3-tier deep linking strategy** leveraging system `PendingIntent`s to open exact chat threads and email conversations directly from saved notebook items.
- **Architected background data lifecycle tasks** using **WorkManager** for automated database retention cleanup, handling Android OS reboot recovery and OEM battery optimization rules.

---

## 🌿 Recommended Git Commit Strategy

If committing your project history to Git, use clean feature commits:

```bash
git init
git add .
git commit -m "feat: complete Haykeye local notification notebook with SQLCipher encryption, multilingual classifier, and Compose UI"
git branch -M main
git remote add origin https://github.com/your-username/Haykeye.git
git push -u origin main
```
