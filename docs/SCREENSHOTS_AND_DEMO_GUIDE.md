# 📸 Screenshots & Video Demonstration Guide

This guide provides a checklist for capturing screenshots and recording a video demonstration of Haykeye for GitHub and portfolio presentations.

---

## 📸 Recommended Screenshots Checklist

Place captured screenshots under `docs/screenshots/` with the file names below:

| File Name | View Description | Key Highlights to Show |
| :--- | :--- | :--- |
| `01_notebook_home.png` | Main Notebook Screen | Dark theme, provider tabs (**All**, **WhatsApp**, **Gmail**, **Messages**), status badge (`🟢 Active`), and urgent cards. |
| `02_search_and_filter.png` | Search Field Active | Real-time search query filtering urgent messages by sender, group, or keyword. |
| `03_expanded_message.png` | Card Expansion | Expanded card showing full message text, timestamp, and **"Open WhatsApp"** action button. |
| `04_settings_main.png` | Settings Screen | **App Appearance & Theme** Segmented Capsule Toggle (`Dark`, `Light`, `System`). |
| `05_security_storage.png` | Database Security Card | **Encrypted at Rest (SQLCipher 256-bit AES)** status & storage size display (`Storage Used: 0 KB (0 messages)`). |
| `06_system_health.png` | OEM Diagnostics Card | `Notification Listener: Connected` & realme UI / ColorOS reliability instructions. |

> [!IMPORTANT]
> **Privacy Note:** Ensure screenshots do not contain real phone numbers, personal email addresses, or confidential message content.

---

## 🎬 Video Demonstration Script (1-Minute Portfolio Demo)

### Demo Sequence (60 Seconds):

1. **0:00 - 0:10 | Setup & Status Check**
   - Open Haykeye. Show top header bar with title, status badge (`🟢 Active`), search bar, and provider tabs.

2. **0:10 - 0:25 | Incoming Urgent Notification Capture & Ringtone Alert**
   - Send an urgent WhatsApp message from another phone: *"Emergency! Please call me ASAP!!"*.
   - Show Haykeye playing the custom alert ringtone and adding the new message card to the notebook list.

3. **0:25 - 0:40 | Card Expansion, Read Status & Search**
   - Tap the card to expand full text.
   - Click **Read** to toggle read status (unread dot disappears).
   - Type `"Emergency"` into the top search bar — the list filters instantly in real time.

4. **0:40 - 0:50 | Source App Navigation**
   - Tap the **"Open WhatsApp"** button.
   - Watch Haykeye invoke the active notification `PendingIntent` and launch WhatsApp straight into that chat!

5. **0:50 - 1:00 | Settings & Encryption Status**
   - Tap **⚙️ Settings**.
   - Show the **Segmented Theme Toggle** (`Dark`, `Light`, `System`), **SQLCipher 256-bit AES Encryption Status**, and **Auto-Retention Settings**.
