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

data class Track(val id: String, val title: String, val artist: String, val album: String,
                 val art: String, val url: String, val durationSec: Int)

data class YtVideo(val id: String, val title: String, val channel: String, val art: String)

object Jamendo {
    private const val CLIENT_ID = "16f2e5c8"

    suspend fun tracks(order: String = "popularity_week", query: String? = null, artist: String? = null): List<Track> =
        withContext(Dispatchers.IO) {
            val u = "https://api.jamendo.com/v3.0/tracks/?client_id=$CLIENT_ID" +
                    "&format=json&limit=30&imagesize=500&audioformat=mp32&order=$order" +
                    (if (query != null) "&search=" + URLEncoder.encode(query, "UTF-8") else "") +
                    (if (artist != null) "&artist_name=" + URLEncoder.encode(artist, "UTF-8") else "")
            val arr = JSONObject(URL(u).readText()).getJSONArray("results")
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                Track(o.getString("id"), o.getString("name"), o.getString("artist_name"),
                    o.getString("album_name"), o.optString("image"), o.getString("audio"), o.getInt("duration"))
            }
        }
}

object RadioBrowser {
    private val hosts = listOf("de1", "de2", "fi1")

    suspend fun search(tag: String?, name: String?): List<Track> = withContext(Dispatchers.IO) {
        val q = StringBuilder("/json/stations/search?order=clickcount&reverse=true&limit=40&hidebroken=true&is_https=true")
        if (tag != null) q.append("&tag=").append(URLEncoder.encode(tag, "UTF-8"))
        if (!name.isNullOrBlank()) q.append("&name=").append(URLEncoder.encode(name, "UTF-8"))
        if (tag == null && name.isNullOrBlank()) {
            val cc = java.util.Locale.getDefault().country
            if (cc.isNotEmpty()) q.append("&countrycode=").append(cc)
        }
        var last: Exception? = null
        for (h in hosts) {
            try {
                val conn = URL("https://$h.api.radio-browser.info$q").openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "GlassMusic/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 10000
                val arr = JSONArray(conn.inputStream.bufferedReader().readText())
                return@withContext List(arr.length()) {
                    val o = arr.getJSONObject(it)
                    val tags = o.optString("tags").split(",").firstOrNull { t -> t.isNotBlank() }?.trim()
                    Track("radio:" + o.optString("stationuuid"), o.optString("name").trim(),
                        tags ?: o.optString("country"), o.optString("country"),
                        o.optString("favicon"), o.optString("url_resolved"), 0)
                }.filter { it.url.isNotEmpty() && it.title.isNotEmpty() }
            } catch (e: Exception) { last = e }
        }
        throw last ?: Exception("radio")
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
    var stations by mutableStateOf(listOf<Track>())
    var radioStatus by mutableStateOf<String?>(null)
    var artistName by mutableStateOf<String?>(null)
    var artistTracks by mutableStateOf(listOf<Track>())
    var artistLoading by mutableStateOf(false)
    var mixes by mutableStateOf(listOf<Pair<String, List<Track>>>())
    val recent = mutableStateListOf<Track>()
    private val prefs = app.getSharedPreferences("library", Context.MODE_PRIVATE)
    var ambient by mutableStateOf(prefs.getBoolean("ambient", true))
    var showRecent by mutableStateOf(prefs.getBoolean("showRecent", true))
    var sleepMinutes by mutableIntStateOf(0)
    var speed by mutableFloatStateOf(1f)
    private var sleepJob: Job? = null

    fun toggleAmbient(b: Boolean) { ambient = b; prefs.edit().putBoolean("ambient", b).apply() }
    fun toggleShowRecent(b: Boolean) { showRecent = b; prefs.edit().putBoolean("showRecent", b).apply() }
    fun changeSpeed(s: Float) { speed = s; player?.setPlaybackSpeed(s) }
    fun setSleep(min: Int) {
        sleepMinutes = min
        sleepJob?.cancel()
        if (min > 0) sleepJob = viewModelScope.launch {
            delay(min * 60_000L)
            player?.pause()
            sleepMinutes = 0
        }
    }
    fun playNext(t: Track) {
        val p = player ?: return
        if (queue.isEmpty()) { play(listOf(t), 0); return }
        val at = (p.currentMediaItemIndex + 1).coerceAtMost(queue.size)
        queue = queue.toMutableList().also { it.add(at, t) }
        p.addMediaItem(at, MediaItem.fromUri(t.url))
    }

    private fun persist() {
        val a = JSONArray()
        liked.forEach { t -> a.put(JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist)
            .put("album", t.album).put("art", t.art).put("url", t.url).put("dur", t.durationSec)) }
        val b = JSONArray()
        savedYt.forEach { v -> b.put(JSONObject().put("id", v.id).put("title", v.title)
            .put("channel", v.channel).put("art", v.art)) }
        prefs.edit().putString("liked", a.toString()).putString("yt", b.toString()).apply()
    }

    private fun persistRecent() {
        val a = JSONArray()
        recent.forEach { t -> a.put(JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist)
            .put("album", t.album).put("art", t.art).put("url", t.url).put("dur", t.durationSec)) }
        prefs.edit().putString("recent", a.toString()).apply()
    }

    private fun pushRecent(t: Track) {
        recent.removeAll { it.id == t.id }
        recent.add(0, t)
        while (recent.size > 20) recent.removeAt(recent.size - 1)
        persistRecent()
    }

    private fun restore() {
        try {
            val a = JSONArray(prefs.getString("liked", "[]"))
            for (i in 0 until a.length()) { val o = a.getJSONObject(i)
                liked.add(Track(o.getString("id"), o.getString("title"), o.getString("artist"),
                    o.getString("album"), o.getString("art"), o.getString("url"), o.getInt("dur"))) }
            val r = JSONArray(prefs.getString("recent", "[]"))
            for (i in 0 until r.length()) { val o = r.getJSONObject(i)
                recent.add(Track(o.getString("id"), o.getString("title"), o.getString("artist"),
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
                    current?.let { pushRecent(it) }
                }
            })
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch { while (true) { pos = player?.currentPosition ?: 0L; delay(400) } }

        load()
    }

    fun load() {
        error = null
        viewModelScope.launch {
            try { trending = Jamendo.tracks("popularity_week") } catch (e: Exception) { error = "Couldn't load music. Tap to retry." }
        }
        viewModelScope.launch {
            try { fresh = Jamendo.tracks("popularity_month") } catch (e: Exception) { error = "Couldn't load music. Tap to retry." }
        }
        viewModelScope.launch {
            try {
                mixes = listOf("Chill Mix" to "chill", "Focus Mix" to "focus", "Energy Mix" to "energy")
                    .map { (n, q) -> n to Jamendo.tracks("popularity_month", q) }
                    .filter { it.second.isNotEmpty() }
            } catch (e: Exception) { }
        }
    }

    fun loadRadio(tag: String? = null, name: String? = null) {
        radioStatus = "Loading…"
        viewModelScope.launch {
            try {
                val r = RadioBrowser.search(tag, name)
                stations = r
                radioStatus = if (r.isEmpty()) "No stations found." else null
            } catch (e: Exception) { radioStatus = "Couldn't load stations. Pick a tag to retry." }
        }
    }

    fun openArtist(n: String) {
        artistName = n; artistTracks = emptyList(); artistLoading = true
        viewModelScope.launch {
            try { artistTracks = Jamendo.tracks("popularity_total", null, n) } catch (e: Exception) { }
            artistLoading = false
        }
    }
    fun closeArtist() { artistName = null }

    fun playAt(i: Int) {
        val p = player ?: return
        p.seekTo(i, 0); p.play(); current = queue.getOrNull(i)
    }

    fun play(list: List<Track>, index: Int) {
        val p = player ?: return
        queue = list
        p.setMediaItems(list.map { MediaItem.fromUri(it.url) }, index, 0)
        p.prepare(); p.play(); current = list[index]; pushRecent(list[index])
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
