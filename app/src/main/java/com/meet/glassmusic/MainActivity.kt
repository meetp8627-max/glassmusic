package com.meet.glassmusic

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

val Accent = Color(0xFFFA2D48)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) {
                Surface(color = Color.Transparent, contentColor = Color.White) { App() }
            }
        }
    }
}

@Composable
fun App(vm: MusicVM = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    var full by remember { mutableStateOf(false) }
    BackHandler(full) { full = false }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A1020), Color(0xFF0B0B10), Color(0xFF0B0B10))))) {
        when (tab) { 0 -> Home(vm); 1 -> SearchScreen(vm); else -> Library(vm) }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vm.current?.let { MiniPlayer(vm) { full = true } }
            TabBar(tab) { tab = it }
        }
        AnimatedVisibility(full, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            NowPlaying(vm) { full = false }
        }
        vm.ytVideo?.let { v -> YtPlayer(v) { vm.closeYt() } }
    }
}

@Composable
fun Art(url: String, size: Int, radius: Int = 12) = AsyncImage(
    model = url, contentDescription = null, contentScale = ContentScale.Crop,
    modifier = Modifier.size(size.dp).clip(RoundedCornerShape(radius.dp)).background(Color(0xFF2A2A33))
)

@Composable
fun TrackRow(t: Track, vm: MusicVM, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Art(t.art, 54)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(t.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.artist, color = Color.White.copy(0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton({ vm.like(t) }) {
            Icon(if (vm.isLiked(t)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                tint = if (vm.isLiked(t)) Accent else Color.White.copy(0.6f))
        }
    }
}

@Composable
fun YtRow(v: YtVideo, vm: MusicVM, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(v.art, null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2A33)))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(v.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(v.channel, color = Color.White.copy(0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton({ vm.toggleYt(v) }) {
            Icon(if (vm.isSavedYt(v)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                tint = if (vm.isSavedYt(v)) Accent else Color.White.copy(0.6f))
        }
    }
}

@Composable
fun HeroCard(t: Track, onPlay: () -> Unit) {
    Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().aspectRatio(1f)
        .clip(RoundedCornerShape(28.dp)).background(Color(0xFF2A2A33)).clickable(onClick = onPlay)) {
        AsyncImage(t.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.75f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth(0.7f)) {
            Text("FEATURED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Accent)
            Text(t.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.artist, fontSize = 15.sp, color = Color.White.copy(0.7f), maxLines = 1)
        }
        Glass(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(58.dp), CircleShape) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.align(Alignment.Center).size(32.dp))
        }
    }
}

@Composable
fun Home(vm: MusicVM) {
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 220.dp)) {
        item { Text("Listen Now", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 14.dp)) }
        vm.error?.let { item { Text(it, color = Accent, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) } }
        vm.trending.firstOrNull()?.let { t -> item { HeroCard(t) { vm.play(vm.trending, 0) } } }
        item { Text("Trending this week", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 26.dp, bottom = 12.dp)) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(vm.trending.drop(1)) { i, t ->
                    Column(Modifier.width(150.dp).clickable { vm.play(vm.trending, i + 1) }) {
                        Art(t.art, 150, 20)
                        Text(t.title, Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = Color.White.copy(0.6f), fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        item { Text("Popular this month", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 26.dp, bottom = 6.dp)) }
        itemsIndexed(vm.fresh) { i, t -> TrackRow(t, vm) { vm.play(vm.fresh, i) } }
    }
}

@Composable
fun SearchScreen(vm: MusicVM) {
    var q by remember { mutableStateOf("") }
    var live by remember { mutableStateOf(false) }
    val genres = listOf("Lofi", "Pop", "Rock", "Electronic", "Jazz", "Classical", "Hip Hop", "Ambient")
    val tiles = listOf(
        Color(0xFFFA7333) to Color(0xFFBF1A4D), Color(0xFF3380E6) to Color(0xFF6633B3),
        Color(0xFF33BF8C) to Color(0xFF1A5980), Color(0xFFF2BF33) to Color(0xFFE65940),
        Color(0xFF994DE6) to Color(0xFFE64D99))
    val doSearch = { if (q.isNotBlank()) { if (YouTube.configured) vm.searchYt(q, live) else vm.searchJamendo(q) } }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 220.dp)) {
        item {
            Text("Search", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 14.dp))
            Glass(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), CircleShape) {
                TextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Artists, songs, albums") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            }
            if (YouTube.configured) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(false to "Songs", true to "Live").forEach { (isLive, label) ->
                        val on = live == isLive
                        Text(label, color = if (on) Color.White else Color.White.copy(0.7f), fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(CircleShape)
                                .background(if (on) Accent else Color.White.copy(0.12f))
                                .clickable { live = isLive; doSearch() }
                                .padding(horizontal = 18.dp, vertical = 8.dp))
                    }
                }
            } else Spacer(Modifier.height(16.dp))
            vm.searchError?.let { Text(it, color = Accent, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
        }
        if (vm.results.isEmpty() && vm.ytResults.isEmpty()) {
            item { Text("Browse categories", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)) }
            items(genres.chunked(2).size) { r ->
                Row(Modifier.padding(horizontal = 20.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    genres.chunked(2)[r].forEachIndexed { k, g ->
                        val (c1, c2) = tiles[(r * 2 + k) % tiles.size]
                        Box(Modifier.weight(1f).aspectRatio(1.6f).clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(c1, c2)))
                            .clickable { q = g; vm.searchJamendo(g) }) {
                            Text(g, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
                        }
                    }
                }
            }
        }
        itemsIndexed(vm.results) { i, t -> TrackRow(t, vm) { vm.play(vm.results, i) } }
        itemsIndexed(vm.ytResults) { _, v -> YtRow(v, vm) { vm.playYt(v) } }
    }
}

@Composable
fun Library(vm: MusicVM) {
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 220.dp)) {
        item { Text("Library", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp)) }
        if (vm.liked.isEmpty() && vm.savedYt.isEmpty()) item { Text("Tap the heart on a song or video to add it here.", color = Color.White.copy(0.6f), modifier = Modifier.padding(20.dp)) }
        if (vm.liked.isNotEmpty()) item { Text("Songs", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        itemsIndexed(vm.liked.toList()) { i, t -> TrackRow(t, vm) { vm.play(vm.liked.toList(), i) } }
        if (vm.savedYt.isNotEmpty()) item { Text("YouTube", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        itemsIndexed(vm.savedYt.toList()) { _, v -> YtRow(v, vm) { vm.playYt(v) } }
    }
}

@Composable
fun YtPlayer(v: YtVideo, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, null) }
            Text(v.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        key(v.id) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webChromeClient = WebChromeClient()
                        loadDataWithBaseURL("https://www.youtube.com", ytHtml(v.id), "text/html", "utf-8", null)
                    }
                },
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
        Text(v.channel, color = Color.White.copy(0.6f), modifier = Modifier.padding(16.dp))
    }
}

fun ytHtml(id: String) = """<html><body style="margin:0;background:#000;height:100%">
<iframe style="position:absolute;top:0;left:0;width:100%;height:100%" src="https://www.youtube.com/embed/$id?autoplay=1&playsinline=1"
frameborder="0" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></body></html>"""

@Composable
fun MiniPlayer(vm: MusicVM, open: () -> Unit) {
    val t = vm.current ?: return
    Glass(Modifier.fillMaxWidth().clickable(onClick = open), RoundedCornerShape(26.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(t.art, 46, 14)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(t.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, fontSize = 12.sp, color = Color.White.copy(0.6f), maxLines = 1)
            }
            IconButton({ vm.toggle() }) { Icon(if (vm.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(30.dp)) }
            IconButton({ vm.next() }) { Icon(Icons.Filled.SkipNext, null, Modifier.size(30.dp)) }
        }
    }
}

@Composable
fun TabBar(sel: Int, onSel: (Int) -> Unit) {
    val items = listOf(Icons.Filled.PlayCircle to "Listen Now", Icons.Filled.Search to "Search", Icons.Filled.LibraryMusic to "Library")
    Glass(Modifier.fillMaxWidth(), CircleShape) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEachIndexed { i, (icon, label) ->
                val on = i == sel
                Column(
                    Modifier.weight(1f).clip(CircleShape)
                        .background(if (on) Color.White.copy(0.14f) else Color.Transparent)
                        .clickable { onSel(i) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, label, tint = if (on) Accent else Color.White.copy(0.7f))
                    Text(label, fontSize = 11.sp, color = if (on) Accent else Color.White.copy(0.7f))
                }
            }
        }
    }
}

@Composable
fun NowPlaying(vm: MusicVM, close: () -> Unit) {
    val t = vm.current ?: return
    val dur = ((vm.player?.duration ?: 0L).takeIf { it > 0 } ?: (t.durationSec * 1000L)).coerceAtLeast(1)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val glow = listOf(Color(0xFFFA7333), Color(0xFF3380E6), Color(0xFF33BF8C), Color(0xFFF2BF33), Color(0xFF994DE6))
        val g1 = glow[Math.abs(t.id.hashCode()) % glow.size]
        val g2 = glow[(Math.abs(t.id.hashCode()) + 1) % glow.size]
        val g3 = glow[(Math.abs(t.id.hashCode()) + 4) % glow.size]
        Box(Modifier.offset(x = (-90).dp, y = 40.dp).size(420.dp).background(Brush.radialGradient(listOf(g1.copy(alpha = 0.55f), Color.Transparent))))
        Box(Modifier.offset(x = 110.dp, y = 330.dp).size(400.dp).background(Brush.radialGradient(listOf(g2.copy(alpha = 0.5f), Color.Transparent))))
        Box(Modifier.offset(x = (-60).dp, y = 520.dp).size(380.dp).background(Brush.radialGradient(listOf(g3.copy(alpha = 0.45f), Color.Transparent))))
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 12.dp).width(42.dp).height(5.dp).clip(CircleShape)
                .background(Color.White.copy(0.5f)).clickable(onClick = close))
            Spacer(Modifier.weight(1f))
            AsyncImage(t.art, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).shadow(30.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)))
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, fontSize = 18.sp, color = Color.White.copy(0.65f), maxLines = 1)
                }
                IconButton({ vm.like(t) }) {
                    Icon(if (vm.isLiked(t)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                        tint = if (vm.isLiked(t)) Accent else Color.White)
                }
            }
            Slider(value = vm.pos.toFloat().coerceIn(0f, dur.toFloat()), onValueChange = { vm.seek(it.toLong()) },
                valueRange = 0f..dur.toFloat(),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.25f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(vm.pos), fontSize = 12.sp, color = Color.White.copy(0.6f))
                Text("-" + fmt(dur - vm.pos), fontSize = 12.sp, color = Color.White.copy(0.6f))
            }
            Spacer(Modifier.height(16.dp))
            Glass(Modifier.fillMaxWidth(), CircleShape) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ vm.toggleShuffle() }) { Icon(Icons.Filled.Shuffle, null, tint = if (vm.shuffle) Accent else Color.White) }
                    IconButton({ vm.prev() }) { Icon(Icons.Filled.SkipPrevious, null, Modifier.size(40.dp)) }
                    IconButton({ vm.toggle() }, Modifier.size(68.dp)) { Icon(if (vm.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(54.dp)) }
                    IconButton({ vm.next() }) { Icon(Icons.Filled.SkipNext, null, Modifier.size(40.dp)) }
                    IconButton({ vm.toggleRepeat() }) { Icon(Icons.Filled.Repeat, null, tint = if (vm.repeat) Accent else Color.White) }
                }
            }
            Spacer(Modifier.weight(1.2f))
        }
    }
}

fun fmt(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }
