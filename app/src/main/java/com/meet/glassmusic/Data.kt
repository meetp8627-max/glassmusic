package com.meet.glassmusic

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class Track(val id: String, val title: String, val artist: String, val album: String,
                 val art: String, val url: String, val durationSec: Int)

object Jamendo {
    private const val CLIENT_ID = "16f2e5c8"

    suspend fun tracks(order: String = "popularity_week", query: String? = null): List<Track> =
        withContext(Dispatchers.IO) {
            val u = StringBuilder("https://api.jamendo.com/v3.0/tracks/?client_id=$CLIENT_ID" +
                    "&format=json&limit=30&imagesize=500&audioformat=mp32&order=$order")
            if (query != null) u.append("&search=").append(URLEncoder.encode(query, "UTF-8"))
            val arr = JSONObject(URL(u.toString()).readText()).getJSONArray("results")
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                Track(o.getString("id"), o.getString("name"), o.getString("artist_name"),
                    o.getString("album_name"), o.optString("image"), o.getString("audio"), o.getInt("duration"))
            }
        }
}

class MusicVM(private val app: Application) : AndroidViewModel(app) {
    private var controller: MediaController? = null
    val player: Player? get() = controller

    var queue by mutableStateOf(listOf<Track>())
    var current by mutableStateOf<Track?>(null)
    var playing by mutableStateOf(false)
    var pos by mutableLongStateOf(0L)
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(false)
    val liked = mutableStateListOf<Track>()
    var trending by mutableStateOf(listOf<Track>())
    var fresh by mutableStateOf(listOf<Track>())
    var results by mutableStateOf(listOf<Track>())
    var error by mutableStateOf<String?>(null)

    init {
        // Connect to the background playback service
        val token = SessionToken(app, ComponentName(app, MusicService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            playing = c.isPlaying
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                override fun onMediaItemTransition(m: MediaItem?, r: Int) {
                    current = queue.getOrNull(c.currentMediaItemIndex)
                }
            })
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch { while (true) { pos = player?.currentPosition ?: 0L; delay(400) } }

        viewModelScope.launch {
            try { trending = Jamendo.tracks("popularity_week") } catch (e: Exception) { error = "Couldn't load music. Check your connection." }
        }
        viewModelScope.launch {
            try { fresh = Jamendo.tracks("releasedate") } catch (e: Exception) { error = "Couldn't load new releases." }
        }
    }

    fun play(list: List<Track>, index: Int) {
        val p = player ?: return
        queue = list
        p.setMediaItems(list.map { MediaItem.fromUri(it.url) }, index, 0)
        p.prepare(); p.play(); current = list[index]
    }
    fun toggle() { player?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { player?.seekToNextMediaItem() }
    fun prev() { player?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seek(ms: Long) { player?.seekTo(ms); pos = ms }
    fun toggleShuffle() { shuffle = !shuffle; player?.shuffleModeEnabled = shuffle }
    fun toggleRepeat() { repeat = !repeat; player?.repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF }
    fun like(t: Track) { if (liked.any { it.id == t.id }) liked.removeAll { it.id == t.id } else liked.add(t) }
    fun isLiked(t: Track) = liked.any { it.id == t.id }
    fun search(q: String) = viewModelScope.launch {
        try { results = Jamendo.tracks("relevance", q) } catch (e: Exception) { error = "Search failed." }
    }
    override fun onCleared() { controller?.release() }
}
