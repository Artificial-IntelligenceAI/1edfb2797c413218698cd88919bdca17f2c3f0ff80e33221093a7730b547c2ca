# LingLongDingDong

A fake incoming-call app for Android, designed for the Nothing Phone (3a) Pro.

Schedule a call from anyone you like. It rings like a real call: a Pixel / Google Phone-style call screen, your ringtone, vibration, and Nothing's Glyph lights on the back. It can wake a locked phone. Answer it and the "caller" can talk to you through the earpiece.

## Features

- **Caller:** name, number, label (Mobile, Work, Home) and a photo you pick from your storage and crop to a circle
- **Timing:** ring now, or after 10 s, 30 s, 1 min, 5 min or a custom delay. Works with the screen locked or the app closed.
- **Ringing:** your default ringtone or one you pick, plus vibration. It follows your ring, vibrate and silent switch.
- **Glyph lights** (Phone (3a) / (3a) Pro): a light spins around the Glyph strips like a loading icon, speeding up over 20 seconds, then stays solid
- **Call screen:** swipe up to answer or down to decline, with a Pixel-style in-call screen (timer, keypad with DTMF tones, mute, speaker, hold)
- **Voice:** when you answer, the caller says your text (text-to-speech) or plays a clip you recorded, through the earpiece. Speaker switches it to loudspeaker.
- **Missed calls:** an unanswered call rings for 2 minutes, then leaves a missed-call notification
- **Saved callers:** keep presets like "Mom" or "Boss"

## Build

Requirements: JDK 17+ and the Android SDK (platform 37, build-tools). Point `local.properties` at your SDK:

```properties
sdk.dir=/path/to/android/sdk
```

Then:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The first build downloads Nothing's [Glyph Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) AAR from a pinned commit and checks its SHA-256. It isn't committed here, because Nothing publishes it without a license.

## Glyph lights

The app targets Android 16, so Nothing doesn't require an API key. Nothing only lets an app drive the Glyphs while the app is on screen. That's the case when a call wakes a locked phone, but not when the call arrives as a banner while you're using the phone.

To test the Glyphs on a phone without a production key, turn on Nothing's debug mode. It switches off again after 48 hours.

```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```

## Permissions

| Permission | Why |
|---|---|
| Notifications | The incoming call is a call-style notification |
| Full-screen intent | Lets the call take over the lock screen |
| Exact alarms | Rings at the exact time you chose, even in Doze |
| Foreground service (phone call) and manage own calls | Keeps the call alive while it rings and while you're on it |
| Microphone | Only when you record a voice clip |
| Glyph (`com.nothing.ketchum.permission.ENABLE`) | Controls the lights |

## Credits

- [Doto](https://github.com/oliverlalan/Doto) dot-matrix font, SIL Open Font License 1.1 (see [licenses/Doto-OFL.txt](licenses/Doto-OFL.txt))
- Material icon shapes, Apache License 2.0

## License

[MIT](LICENSE)
