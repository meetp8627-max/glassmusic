# GlassMusic

A music player for Android with a clean glass-style interface. Stream free music, tune in to live radio and keep your favourites in one place.

<!-- Add screenshots here, for example: ![Home](docs/home.png) -->

## Features

- **Home** with a featured track, mixes, trending songs and recently played
- **Radio** with thousands of live stations, searchable by name or genre
- **Library** to save songs with one tap
- **Search** by artist, song or category
- **Now Playing** with a seek bar, volume, playback speed, a reorderable queue and a sleep timer
- **Background playback** with media controls
- **Artist pages** with top songs
- **Lite mode** that turns off heavy visual effects for smoother use on slower phones (automatic on low-memory devices)

## Download

Get the latest APK from the [Releases](../../releases) page.

| Edition | Version | Android |
|---|---|---|
| GlassMusic | 2.x | 13 or newer |
| GlassMusic Classic | 1.5.x | 8.0 or newer |

To install, download the APK, open it and allow installs from your browser or file manager if Android asks.

## Build from source

You need JDK 21 and Gradle 9.6 or newer.

```bash
# GlassMusic 2.x
gradle -p glasmio-edition :app:assembleDebug

# GlassMusic Classic
gradle :app:assembleDebug
```

The debug APK is written to `build/outputs/apk/debug/` inside the module folder.

## Project layout

- `glasmio-edition/` - GlassMusic 2.x
- `app/` and `prismal/` - GlassMusic Classic
- `.github/workflows/` - automated builds and releases

## Credits

Music comes from [Jamendo](https://www.jamendo.com) and live radio from [Radio Browser](https://www.radio-browser.info). Third-party libraries and their licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
