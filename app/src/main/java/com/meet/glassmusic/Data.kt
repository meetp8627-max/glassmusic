package com.meet.glassmusic

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfo

data class Track(val id: String, val title: String, val artist: String, val album: String,
                 val art: String, val url: String, val durationSec: Int)

data class YtVideo(val id: String, val title: String, val channel: String, val art: String)

object Jamendo {
    private const val CLIENT_ID = "16f2e5c8"

    suspend fun tracks(order: String = "popularity_week", query: String? = null): List<Track> =
        withContext(Dispatchers.IO) {
            val u = "https://api.jamendo.com/v3.0/tracks/?client_id=$CLIENT_ID" +
                    "&format=json&limit=30&imagesize=500&audioformat=mp32&order=$order" +
                    (if (query != null) "&search=" + URLEncoder.encode(query, "UTF-8") else "")
            val arr = JSONObject(URL(u).readText()).getJSONArray("results")
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                Track(o.getString("id"), o.getString("name"), o.getString("artist_name"),
                    o.getString("album_name"), o.optString("image"), o.getString("audio"), o.getInt("duration"))
            }
        }
}

object YouTube {
    // Free key: console.cloud.google.com > enable "YouTube Data API v3" > Credentials > Create API key
    private const val KEY = "PASTE_YOUTUBE_KEY"
    val configured: Boolean get() = KEY != "PASTE_YOUTUBE_KEY"

    private fun clean(s: String) = s.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"")

    suspend fun search(q: String, live: Boolean = false): List<YtVideo> = withContext(Dispatchers.IO) {
        val filter = if (live) "&eventType=live" else "&videoCategoryId=10"
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video" +
                filter + "&maxResults=25&q=" + URLEncoder.encode(q, "UTF-8") + "&key=" + KEY
        val arr = JSONObject(URL(url).readText()).getJSONArray("items")
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            val s = o.getJSONObject("snippet")
            YtVideo(o.getJSONObject("id").getString("videoId"), clean(s.getString("title")),
                clean(s.getString("channelTitle")),
                s.getJSONObject("thumbnails").getJSONObject("medium").getString("url"))
        }
    }
}

object NewPipe {
    private val client = OkHttpClient()
    private val downloader = object : Downloader() {
        override fun execute(request: Request): Response {
            val rb = client.newCall(okhttp3.Request.Builder()
                .url(request.url())
                .apply { request.headers().forEach { (k, v) -> addHeader(k, v) } }
                .build()).execute()
            return Response(rb.code, rb.message, rb.headers.toMultimap(), rb.body?.string(), rb.request.url.toString())
        }
    }
    init { NewPipe.init(downloader) }

    suspend fun search(q: String): List<YtVideo> = withContext(Dispatchers.IO) {
        val service = ServiceList.YouTube
        val extractor = service.getSearchExtractor(q, listOf(), "")
        extractor.fetchPage()
        extractor.initialPage.items.map { item ->
            val info = item as? org.schabi.newpipe.extractor.search.SearchInfoItem
            YtVideo(info?.url?.substringAfter("watch?v=") ?: "", info?.name ?: "",
                info?.uploaderName ?: "", info?.thumbnailUrl ?: "")
        }
    }

    suspend fun audioUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(service = ServiceList.YouTube, url = videoUrl)
        info.audioStreams.maxByOrNull { it.averageBitrate }?.content
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
    var ytResults by mutableStateOf(listOf<YtVideo>())
    var ytVideo by mutableStateOf<YtVideo?>(null)
    var error by mutableStateOf<String?>(null)
    var searchError by mutableStateOf<String?>(null)
    var results by mutableStateOf(listOf<Track>())
    val savedYt = mutableStateListOf<YtVideo>()
    private val prefs = app.getSharedPreferences("library", Context.MODE_PRIVATE)

    private fun persist() {
        val a = JSONArray()
        liked.forEach { t -> a.put(JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist)
            .put("album", t.album).put("art", t.art).put("url", t.url).put("dur", t.durationSec)) }
        val b = JSONArray()
        savedYt.forEach { v -> b.put(JSONObject().put("id", v.id).put("title", v.title)
            .put("channel", v.channel).put("art", v.art)) }
        prefs.edit().putString("liked", a.toString()).putString("yt", b.toString()).apply()
    }

    private fun restore() {
        try {
            val a = JSONArray(prefs.getString("liked", "[]"))
            for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                liked.add(Track(o.getString("id"), o.getString("title"), o.getString("artist"),
                    o.getString("album"), o.getString("art"), o.getString("url"), o.getInt("dur"))) }
            val b = JSONArray(prefs.getString("yt", "[]"))
            for (i in 0 until b.length()) { val o = b.getJSONObject(i)
                savedYt.add(YtVideo(o.getString("id"), o.getString("title"), o.getString("channel"), o.getString("art"))) }
        } catch (e: Exception) { }
    }

    init {
        restore()
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
            try { fresh = Jamendo.tracks("popularity_month") } catch (e: Exception) { error = "Couldn't load more songs." }
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
    fun like(t: Track) { if (liked.any { it.id == t.id }) liked.removeAll { it.id == t.id } else liked.add(t); persist() }
    fun toggleYt(v: YtVideo) { if (savedYt.any { it.id == v.id }) savedYt.removeAll { it.id == v.id } else savedYt.add(v); persist() }
    fun isSavedYt(v: YtVideo) = savedYt.any { it.id == v.id }
    fun isLiked(t: Track) = liked.any { it.id == t.id }

    fun searchYt(q: String, live: Boolean = false) = viewModelScope.launch {
        if (!YouTube.configured) { searchError = "Add your YouTube API key in Data.kt"; return@launch }
        try { ytResults = YouTube.search(q, live); results = emptyList(); searchError = null }
        catch (e: Exception) { searchError = "YouTube search failed. Check your API key." }
    }
    fun searchJamendo(q: String) = viewModelScope.launch {
        try { results = Jamendo.tracks("relevance", q); ytResults = emptyList(); searchError = if (results.isEmpty()) "No songs found." else null }
        catch (e: Exception) { searchError = "Search failed. Check your connection." }
    }
    fun playYt(v: YtVideo) { player?.pause(); ytVideo = v }
    fun closeYt() { ytVideo = null }

    override fun onCleared() { controller?.release() }
}
