# Keep the glass renderer (it is called from native code) and the player stack intact.
-keep class io.github.neoruaa.glasmio.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn **
