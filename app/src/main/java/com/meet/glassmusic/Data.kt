package com.meet.glassmusic

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class Track(val id: String, val title: String, val artist: String, val album: String,
                 val art: String, val url: String, val durationSec: Int)

object Jamendo {
    // Free key: https://devportal.jamendo.com
    private const val CLIENT_ID = "cdc39798f297b4e4defc441c59bac317"

    suspend fun tracks(order: String = "popularity_week", query: String? = null): List<Track> =
        withContext(Dispatchers.IO) {
            val u = StringBuilder("https://api.jamendo.com/v3.0/tracks/?client_id=$CLIENT_ID" +
                    "&format=json&limit=30&imagesize=500&audioformat=mp32&order=$order")
            if (query != null) u.append("&search=").append(URLEncoder.encode(query, "UTF-8"))
            val root = JSONObject(URL(u.toString()).readText()); val arr = root.getJSONArray("results"); if (arr.length() == 0) throw Exception("empty")
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                Track(o.getString("id"), o.getString("name"), o.getString("artist_name"),
                    o.getString("album_name"), o.optString("image"), o.getString("audio"), o.getInt("duration"))
            }
        }
}

class MusicVM(app: Application) : AndroidViewModel(app) {
    val player: ExoPlayer = ExoPlayer.Builder(app).build()
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
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onMediaItemTransition(m: MediaItem?, r: Int) {
                current = queue.getOrNull(player.currentMediaItemIndex)
            }
        })
        viewModelScope.launch { while (true) { pos = player.currentPosition; delay(400) } }
        viewModelScope.launch {
            try {
                trending = Jamendo.tracks("popularity_week")
                fresh = Jamendo.tracks("releasedate_desc")
            } catch (e: Exception) { error = "Couldn't load music. Check your Jamendo client ID and internet." }
        }
    }

    fun play(list: List<Track>, index: Int) {
        queue = list
        player.setMediaItems(list.map { MediaItem.fromUri(it.url) }, index, 0)
        player.prepare(); player.play(); current = list[index]
    }
    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun next() = player.seekToNextMediaItem()
    fun prev() { if (player.currentPosition > 3000) player.seekTo(0) else player.seekToPreviousMediaItem() }
    fun seek(ms: Long) { player.seekTo(ms); pos = ms }
    fun toggleShuffle() { shuffle = !shuffle; player.shuffleModeEnabled = shuffle }
    fun toggleRepeat() { repeat = !repeat; player.repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF }
    fun like(t: Track) { if (liked.any { it.id == t.id }) liked.removeAll { it.id == t.id } else liked.add(t) }
    fun isLiked(t: Track) = liked.any { it.id == t.id }
    fun search(q: String) = viewModelScope.launch {
        try { results = Jamendo.tracks("relevance", q) } catch (e: Exception) { error = "Search failed." }
    }
    override fun onCleared() { player.release() }
}
