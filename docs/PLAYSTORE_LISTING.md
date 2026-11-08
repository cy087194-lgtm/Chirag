# Lia AI — Play Store Listing

## Artifact to Upload
- **AAB file**: `app/build/outputs/bundle/playRelease/app-play-release.aab`
- Build with: `./gradlew bundlePlayRelease`

---

## Short Description (80 chars)
Your voice-first AI companion — talk naturally, get things done.

## Full Description (4000 chars max)

**Lia AI — Your Intelligent Voice Companion**

Meet Lia, a real-time AI companion that listens, understands and acts — in Hindi, Hinglish or English. Just talk naturally and Lia responds instantly, opens apps, helps you draft messages and even builds websites on command.

**Real-time Voice Conversations**
Talk with Lia over a full-duplex live connection — barge in, interrupt or change topic any time. No waiting for replies.

**Speaks Your Language**
Switch freely between Hindi, Hinglish and English. Lia detects your language automatically and answers in the same tongue.

**10 Personalities, Your Way**
Choose from Normal, Friendly, Professional, Funny, Companion, Teacher, Developer, Hungry, Nautanki and more. Change Lia's name to whatever you like.

**Website Forge**
Say "Lia, ek cafe ki website banao" and watch a live 3D build scene construct an animated, Three.js-powered website in seconds.

**Phone Actions**
- Open any app by name
- Start a call to a contact
- Open the message composer for SMS or WhatsApp

**Premium 3D Design**
A GPU-shaded 3D orb reacts to your voice and tilts with your phone. Deep indigo night sky, one warm marigold accent. Looks stunning in dark and light themes.

**Typed Chat**
Prefer typing? The chat screen uses the same personality and even detects website build requests.

**Privacy First**
Your Gemini API key is stored only on your device — never sent to our servers. No audio is logged or stored. Microphone is used only during active voice sessions.

---

## Data Safety Answers

| Category | Collected | Shared | Required |
|----------|-----------|--------|----------|
| Microphone audio | No (processed on-device) | No | Yes |
| Contacts (name + phone) | No | No | Optional |
| Approximate location | No | No | No |
| Device or other IDs | No | No | No |

**Encryption**: All API calls use HTTPS/WSS.
**Deletion**: Users can clear the API key and conversation history from Settings.

---

## Permission Justifications

### RECORD_AUDIO — Foreground microphone
Required for the core voice conversation feature. Audio is streamed to the Gemini Live API in real time and never stored on device.

### READ_CONTACTS — Optional
Used only when the user asks Lia to call or message a contact by name. Requested at the moment of first use.

### CALL_PHONE — Optional
Used to place calls when the user explicitly asks ("Mummy ko call karo"). Requires user confirmation.

### FOREGROUND_SERVICE (microphone type)
Keeps the voice session alive while the user switches apps. A persistent notification is shown with a Stop button.

### POST_NOTIFICATIONS
Required on Android 13+ to display the ongoing voice session notification.

---

## Submission Checklist

- [ ] `app-play-release.aab` generated with `./gradlew bundlePlayRelease`
- [ ] Signed with release keystore (keystore.properties configured, git-ignored)
- [ ] Proguard applied: `isMinifyEnabled = true`, `isShrinkResources = true`
- [ ] `versionCode` and `versionName` updated in `app/build.gradle.kts`
- [ ] No secrets in repository (API keys, keystore passwords)
- [ ] Accessibility policy: play flavor contains no AccessibilityService
- [ ] No SEND_SMS permission in play manifest
- [ ] Privacy policy URL added in Play Console
- [ ] Content rating completed (Everyone)
- [ ] Data safety form filled (see above)
- [ ] App icon 512×512 PNG uploaded
- [ ] Feature graphic 1024×500 PNG uploaded
- [ ] At least 2 screenshots per form factor
- [ ] Short and full descriptions reviewed
- [ ] Target API level 36 (Android 16)
