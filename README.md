# GlassMusic

A music player for Android with a clean glass-style interface. Stream free music, tune in to live radio and keep your favourites in one place.

<!-- Add screenshots here, for example: ![Home](docs/home.png) -->

## Download

**[Download GlassMusic.apk](https://github.com/meetp8627-max/glassmusic/releases/latest/download/GlassMusic.apk)** - Android 13 or newer

Older phone? Get **[GlassMusic-Classic.apk](https://github.com/meetp8627-max/glassmusic/releases/latest/download/GlassMusic-Classic.apk)** for Android 8.0 to 12.

Every version is listed on the [Releases](https://github.com/meetp8627-max/glassmusic/releases) page with release notes and checksums.

To install, download the APK, open it and allow installs from your browser or file manager if Android asks. To update, install the new APK over the old one.

## Features

- **Home** with a featured track, mixes, trending songs and recently played
- **Radio** with thousands of live stations, searchable by name or genre
- **Library** to save songs with one tap
- **Search** by artist, song or category
- **Now Playing** with a seek bar, volume, playback speed, a reorderable queue and a sleep timer
- **Background playback** with media controls
- **Artist pages** with top songs
- **Lite mode** that turns off heavy visual effects for smoother use on slower phones (automatic on low-memory devices)

## Releases

Releases are automatic. To publish a new version:

1. Add a section for it at the top of `CHANGELOG.md`. Its text becomes the public release notes. If there is no section, notes are written from the commit messages.
2. Raise the version number in the app's build file and push.

GitHub Actions then builds both signed APKs, names them `GlassMusic.apk` and `GlassMusic-Classic.apk`, writes the release notes, and publishes the release. You can also run **Actions > Release > Run workflow** by hand.

## Credits

Music comes from [Jamendo](https://www.jamendo.com) and live radio from [Radio Browser](https://www.radio-browser.info). Third-party libraries and their licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
