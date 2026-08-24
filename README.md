# RULe-Cough — Android app

A Kotlin + Jetpack Compose (Material 3) app that records a cough with the phone
mic, sends it to your model server, and shows the classification with a Viridis
probability chart, an uncertainty flag, and the acoustic biomarkers.

**Includes:** an animated splash screen, a 3-tab bottom menu (Record · History ·
Settings), a **play button** to listen back to the recorded or uploaded clip, a
persistent **History** of past screenings, and **Light / Dark / System** theming
(toggle in Settings).

## Open & build

1. Install **Android Studio** (Ladybug 2024.2 or newer).
2. **File → Open** and select this `android/` folder.
3. Let Gradle sync. Android Studio will download the SDK components, AGP 8.7.3,
   Kotlin 2.0.21 and the libraries automatically (needs internet on first sync).
   If it offers to update AGP/Gradle, accepting is fine.
4. Plug in a phone (USB debugging on) or start an emulator, then press **Run ▶**.

> Requires `compileSdk 35` — install "Android 15 (API 35)" via the SDK Manager if
> Studio prompts. Minimum device: Android 8.0 (API 26).

## Two ways to run the model

The app has a **mode switch in Settings**:

* **On-device (offline)** — runs the exported `rule_cough.tflite` right on the phone,
  no server, no internet. Uses the **mel-only ResNet**; the audio front-end is baked
  into the model, so the app just feeds raw audio. To enable it, drop the two files
  from the notebook's *"Export on-device model"* cell into **`app/src/main/assets/`**:

  ```
  app/src/main/assets/rule_cough.tflite
  app/src/main/assets/labels.txt
  ```
  Then rebuild. (Uncertainty and the 27 acoustic biomarkers aren't available offline.)

* **Server** — sends the audio to your FastAPI backend for the full **multi-view**
  model with Monte-Carlo uncertainty and acoustic biomarkers.

## Point the app at your backend  (Server mode)

Start the backend first (see `../backend/README.md`), then in the app open
**Settings** and set the **Server URL**:

| How you run the backend | URL to enter |
|-------------------------|--------------|
| Android **emulator**, server on same PC | `http://10.0.2.2:8000` (default) |
| **Physical phone**, same Wi-Fi as PC    | `http://<your-PC-LAN-IP>:8000` e.g. `http://192.168.1.20:8000` |
| **ngrok** (from anywhere)               | `https://xxxx.ngrok-free.app` |

Tap **Save & test** — it should read *Connected · model ready*. Cleartext `http://`
to a LAN IP is already allowed via `res/xml/network_security_config.xml`.

## How it works

```
Mic (AudioRecord, 16 kHz PCM) ──► WAV file ──► multipart POST /predict
                                                      │
                                          FastAPI runs the exact
                                          notebook preprocessing + model
                                                      │
        Results screen ◄── JSON (class, probabilities, uncertainty, features)
```

Recording is captured as **16 kHz mono WAV** — the same sample rate the model was
trained on, and a format the server reads with no `ffmpeg` dependency.

## Project map

```
app/src/main/java/com/rulecough/app/
  MainActivity.kt        entry point + tab navigation + permissions
  MainViewModel.kt       UI state, recording, upload, connection test
  audio/WavRecorder.kt   AudioRecord → WAV writer
  net/                   Retrofit service, API client, JSON models
  data/Prefs.kt          stores the server URL
  ui/                    RecordScreen, ResultScreen, SettingsScreen
  ui/theme/              Viridis Material-3 theme (light + dark)
```

## Notes
- The app uses the platform default font so it builds with no bundled font files.
  To match the prototype exactly, add Sora / IBM Plex `.ttf` files to `res/font`
  and update `ui/theme/Type.kt`.
- Screening aid for a school project — **not** a medical device.
