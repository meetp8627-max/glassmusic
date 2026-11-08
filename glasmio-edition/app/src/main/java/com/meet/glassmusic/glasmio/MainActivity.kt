package com.meet.glassmusic.glasmio

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.neoruaa.glasmio.core.*
import io.github.neoruaa.glasmio.glass.*
import io.github.neoruaa.glasmio.icons.*
import io.github.neoruaa.glasmio.preference.*
import io.github.neoruaa.glasmio.scaffold.*
import io.github.neoruaa.glasmio.ui.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlasmioTheme { GlasmioApp() } }
    }
}

@Composable
fun GlasmioApp(vm: MusicVM = viewModel()) {
    val colors = GlasmioTheme.colors
    GlasmioBackdropHost(
        modifier = Modifier.fillMaxSize(),
        sampledContent = { Box(Modifier.fillMaxSize().background(colors.groupedBackground)) },
        overlayContent = { AppShell(vm) },
    )
}

@Composable
fun AppShell(vm: MusicVM) {
    var tab by remember { mutableIntStateOf(0) }
    var nowPlaying by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    val compact = remember { mutableFloatStateOf(0f) }
    val tabs = remember {
        listOf(
            GlasmioTabItem(0, "Home", SfSymbol.HouseFilled),
            GlasmioTabItem(1, "Radio", SfSymbol.RadioWaves),
            GlasmioTabItem(2, "Library", SfSymbol.MusicNoteList),
            GlasmioTabItem(3, "Search", SfSymbol.Search),
        )
    }
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = tab, label = "tab") { t ->
            when (t) {
                0 -> HomePage(vm) { settings = true }
                1 -> RadioPage(vm)
                2 -> LibraryPage(vm)
                else -> SearchPage(vm)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vm.current?.let { MiniBar(vm, it) { nowPlaying = true } }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlasmioNavigationBar(
                    items = tabs,
                    selectedKey = tab,
                    onSelected = { tab = it },
                    onExpand = {},
                    compactProgress = compact,
                    compactSize = 48.dp,
                )
            }
        }
        GlasmioBottomSheet(
            visible = nowPlaying,
            skipPartiallyExpanded = true,
            onDismissRequest = { nowPlaying = false },
            title = "Now Playing",
        ) { NowPlayingContent(vm) }
        GlasmioBottomSheet(
            visible = settings,
            skipPartiallyExpanded = false,
            onDismissRequest = { settings = false },
            title = "Settings",
        ) { SettingsContent(vm) }
        GlasmioBottomSheet(
            visible = vm.artistName != null,
            skipPartiallyExpanded = false,
            onDismissRequest = { vm.closeArtist() },
            title = "Artist",
        ) { ArtistContent(vm) }
    }
}

// ---------- small building blocks ----------

@Composable
fun Cover(url: String, size: Dp, radius: Dp = 10.dp) {
    AsyncImage(
        model = url, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(GlasmioTheme.colors.separator),
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text, style = GlasmioTheme.textStyles.title3, color = GlasmioTheme.colors.content,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp),
    )
}

@Composable
fun TrackItem(t: Track, vm: MusicVM, first: Boolean, artistLink: Boolean = true, onPlay: () -> Unit) {
    val colors = GlasmioTheme.colors
    var menu by remember { mutableStateOf(false) }
    val isRadio = t.id.startsWith("radio:")
    val liked = vm.isLiked(t)
    GlasmioListRow(
        title = t.title,
        subtitle = t.artist,
        showTopSeparator = !first,
        leading = { Cover(t.art, 48.dp) },
        onClick = onPlay,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlasmioIconButton(onClick = { vm.like(t) }) {
                    SfIcon(
                        SfSymbol.Heart,
                        if (liked) "Remove from Library" else "Add to Library",
                        tint = if (liked) colors.accent else colors.secondaryContent,
                    )
                }
                GlasmioMenu(
                    expanded = menu,
                    onExpandedChange = { menu = it },
                    itemCount = if (isRadio || !artistLink) 2 else 3,
                    anchor = { open -> GlasmioIconButton(onClick = open) { SfIcon(SfSymbol.Ellipsis, "More") } },
                ) { backdrop, close ->
                    GlasmioMenuItem("Play next", { vm.playNext(t); close() }, backdrop = backdrop)
                    GlasmioMenuItem(
                        if (liked) "Remove from Library" else "Add to Library",
                        { vm.like(t); close() }, backdrop = backdrop,
                    )
                    if (!isRadio && artistLink) {
                        GlasmioMenuItem("Go to artist", { vm.openArtist(t.artist); close() }, backdrop = backdrop)
                    }
                }
            }
        },
    )
}

@Composable
fun HeroCard(t: Track, onPlay: () -> Unit) {
    val colors = GlasmioTheme.colors
    val ts = GlasmioTheme.textStyles
    GlasmioCard(modifier = Modifier.fillMaxWidth().aspectRatio(1.1f), onClick = onPlay) {
        AsyncImage(t.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("FEATURED", style = ts.callout, color = colors.accent)
            Text(t.title, style = ts.title2, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.artist, style = ts.body, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

@Composable
fun MiniBar(vm: MusicVM, t: Track, onOpen: () -> Unit) {
    val colors = GlasmioTheme.colors
    val ts = GlasmioTheme.textStyles
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(32.dp),
        onClick = onOpen,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Cover(t.art, 44.dp, 12.dp)
            Column(Modifier.weight(1f)) {
                Text(t.title, style = ts.headline, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, style = ts.callout, color = colors.secondaryContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlasmioIconButton(onClick = { vm.toggle() }) {
                SfIcon(if (vm.playing) SfSymbol.PauseFilled else SfSymbol.PlayFilled, if (vm.playing) "Pause" else "Play")
            }
            GlasmioIconButton(onClick = { vm.next() }) { SfIcon(SfSymbol.ForwardFilled, "Next") }
        }
    }
}

// ---------- pages ----------

@Composable
fun HomePage(vm: MusicVM, onSettings: () -> Unit) {
    val colors = GlasmioTheme.colors
    GlasmioPinnedListPage(
        title = "Home",
        bottomPadding = 190.dp,
        actions = { GlasmioIconButton(onClick = onSettings) { SfIcon(SfSymbol.Gearshape, "Settings") } },
    ) {
        vm.error?.let { e ->
            item { Text(e, color = colors.destructive, modifier = Modifier.clickable { vm.load() }.padding(4.dp)) }
        }
        vm.trending.firstOrNull()?.let { t -> item { HeroCard(t) { vm.play(vm.trending, 0) } } }
        if (vm.mixes.isNotEmpty()) {
            item { SectionHeader("Made for You") }
            item {
                GlasmioGroupedList {
                    vm.mixes.forEachIndexed { i, mix ->
                        GlasmioListRow(
                            title = mix.first, subtitle = "${mix.second.size} songs",
                            showTopSeparator = i > 0, onClick = { vm.play(mix.second, 0) },
                        )
                    }
                }
            }
        }
        item { SectionHeader("Trending this week") }
        item {
            GlasmioGroupedList {
                vm.trending.forEachIndexed { i, t -> TrackItem(t, vm, i == 0) { vm.play(vm.trending, i) } }
            }
        }
        if (vm.showRecent && vm.recent.isNotEmpty()) {
            item { SectionHeader("Recently Played") }
            item {
                GlasmioGroupedList {
                    val list = vm.recent.toList()
                    list.forEachIndexed { i, t -> TrackItem(t, vm, i == 0) { vm.play(list, i) } }
                }
            }
        }
        item { SectionHeader("Popular this month") }
        item {
            GlasmioGroupedList {
                vm.fresh.forEachIndexed { i, t -> TrackItem(t, vm, i == 0) { vm.play(vm.fresh, i) } }
            }
        }
    }
}

@Composable
fun RadioPage(vm: MusicVM) {
    val colors = GlasmioTheme.colors
    var sel by remember { mutableIntStateOf(0) }
    val tags = remember { listOf<String?>(null, "hindi", "pop", "lofi") }
    val labels = remember { listOf("Top", "Hindi", "Pop", "Lofi") }
    LaunchedEffect(Unit) { if (vm.stations.isEmpty()) vm.loadRadio() }
    GlasmioPinnedListPage(title = "Radio", bottomPadding = 190.dp) {
        item {
            GlasmioSegmentedControl(
                items = labels.mapIndexed { i, l -> i to l },
                selected = sel,
                onSelected = { sel = it; vm.loadRadio(tag = tags[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        vm.radioStatus?.let { s ->
            item { Text(s, color = colors.secondaryContent, modifier = Modifier.padding(4.dp)) }
        }
        item {
            GlasmioGroupedList {
                vm.stations.forEachIndexed { i, s -> TrackItem(s, vm, i == 0) { vm.play(vm.stations, i) } }
            }
        }
    }
}

@Composable
fun LibraryPage(vm: MusicVM) {
    val colors = GlasmioTheme.colors
    GlasmioPinnedListPage(title = "Library", bottomPadding = 190.dp) {
        if (vm.liked.isEmpty()) {
            item {
                Text(
                    "Tap the heart on a song to add it here.",
                    style = GlasmioTheme.textStyles.body, color = colors.secondaryContent,
                    modifier = Modifier.padding(4.dp),
                )
            }
        } else {
            item {
                GlasmioGroupedList {
                    val list = vm.liked.toList()
                    list.forEachIndexed { i, t -> TrackItem(t, vm, i == 0) { vm.play(list, i) } }
                }
            }
        }
    }
}

@Composable
fun SearchPage(vm: MusicVM) {
    val colors = GlasmioTheme.colors
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var active by remember { mutableStateOf(false) }
    val genres = remember { listOf("Lofi", "Pop", "Rock", "Electronic", "Jazz", "Classical", "Hip Hop", "Ambient") }
    GlasmioPinnedListPage(title = "Search", bottomPadding = 190.dp) {
        item {
            GlasmioSearchBar(
                query = query,
                onQueryChange = { query = it },
                active = active,
                onActiveChange = { active = it },
                onClose = { active = false; query = TextFieldValue(""); vm.clearSearch() },
                placeholder = "Artists, songs, albums",
                closeContentDescription = "Close search",
                onSearch = { vm.searchJamendo(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        vm.searchError?.let { e -> item { Text(e, color = colors.destructive, modifier = Modifier.padding(4.dp)) } }
        if (vm.results.isEmpty()) {
            item { SectionHeader("Browse categories") }
            item {
                GlasmioGroupedList {
                    genres.forEachIndexed { i, g ->
                        GlasmioListRow(
                            title = g, showTopSeparator = i > 0,
                            onClick = { query = TextFieldValue(g); vm.searchJamendo(g) },
                        )
                    }
                }
            }
        } else {
            item {
                GlasmioGroupedList {
                    vm.results.forEachIndexed { i, t -> TrackItem(t, vm, i == 0) { vm.play(vm.results, i) } }
                }
            }
        }
    }
}

// ---------- sheets ----------

@Composable
fun ColumnScope.NowPlayingContent(vm: MusicVM) {
    val colors = GlasmioTheme.colors
    val ts = GlasmioTheme.textStyles
    val t = vm.current
    if (t == null) {
        Text("Nothing playing", style = ts.body, color = colors.secondaryContent, modifier = Modifier.padding(32.dp))
        return
    }
    val isRadio = t.id.startsWith("radio:")
    var mode by remember { mutableIntStateOf(0) }
    val dur = ((vm.player?.duration ?: 0L).takeIf { it > 0 } ?: (t.durationSec * 1000L)).coerceAtLeast(1L)
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember {
        mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
    }
    val speeds = remember { listOf(0.75f, 1f, 1.25f, 1.5f, 2f) }

    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GlasmioSegmentedControl(
            items = listOf(0 to "Now Playing", 1 to "Queue"),
            selected = mode,
            onSelected = { mode = it },
            modifier = Modifier.fillMaxWidth(),
        )
        if (mode == 0) {
            AsyncImage(
                t.art, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(colors.separator),
            )
        } else {
            GlasmioGroupedList {
                vm.queue.take(60).forEachIndexed { i, q ->
                    GlasmioListRow(
                        title = q.title, subtitle = q.artist, showTopSeparator = i > 0,
                        leading = { Cover(q.art, 44.dp) },
                        titleColor = if (q.id == t.id) colors.accent else Color.Unspecified,
                        onClick = { vm.playAt(i) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.title, style = ts.title2, color = colors.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                t.artist, style = ts.body, color = colors.accent, maxLines = 1,
                modifier = if (isRadio) Modifier else Modifier.clickable { vm.openArtist(t.artist) },
            )
        }
        if (t.durationSec > 0) {
            GlasmioSlider(
                value = (vm.pos.toFloat() / dur).coerceIn(0f, 1f),
                onValueChange = { vm.seek((it * dur).toLong()) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(vm.pos), style = ts.callout, color = colors.secondaryContent)
                Text("-" + fmt(dur - vm.pos), style = ts.callout, color = colors.secondaryContent)
            }
        } else {
            Text("LIVE", style = ts.headline, color = colors.accent)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlasmioIconButton(onClick = { vm.prev() }) { SfIcon(SfSymbol.BackwardFilled, "Previous") }
            GlasmioIconButton(onClick = { vm.toggle() }, size = 72.dp) {
                SfIcon(
                    if (vm.playing) SfSymbol.PauseFilled else SfSymbol.PlayFilled,
                    if (vm.playing) "Pause" else "Play", size = 34.dp,
                )
            }
            GlasmioIconButton(onClick = { vm.next() }) { SfIcon(SfSymbol.ForwardFilled, "Next") }
        }
        GlasmioVolumeSlider(
            value = volume,
            onValueChange = {
                volume = it
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVol).roundToInt(), 0)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Playback speed", style = ts.callout, color = colors.secondaryContent)
        GlasmioSegmentedControl(
            items = speeds.mapIndexed { i, s -> i to (if (s == 1f) "1x" else "${s}x") },
            selected = speeds.indexOf(vm.speed).coerceAtLeast(0),
            onSelected = { vm.changeSpeed(speeds[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ColumnScope.SettingsContent(vm: MusicVM) {
    val colors = GlasmioTheme.colors
    val ts = GlasmioTheme.textStyles
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = ts.title2, color = colors.content)
        GlasmioGroupedList {
            GlasmioSwitchPreference(
                title = { Text("Show Recently Played") },
                checked = vm.showRecent,
                onCheckedChange = { vm.toggleShowRecent(it) },
                showTopSeparator = false,
            )
            GlasmioListRow(
                title = "Sleep timer",
                detail = if (vm.sleepMinutes == 0) "Off" else "${vm.sleepMinutes} min",
                trailing = {
                    GlasmioStepper(
                        value = vm.sleepMinutes / 5,
                        onValueChange = { vm.setSleep(it * 5) },
                        range = 0..24,
                    )
                },
            )
        }
        Text("GlassMusic - Glasmio edition", style = ts.callout, color = colors.secondaryContent)
    }
}

@Composable
fun ColumnScope.ArtistContent(vm: MusicVM) {
    val ts = GlasmioTheme.textStyles
    val colors = GlasmioTheme.colors
    val name = vm.artistName ?: ""
    val tracks = vm.artistTracks
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(name, style = ts.title1, color = colors.content, maxLines = 2)
        Text(
            if (vm.artistLoading) "Loading..." else "${tracks.size} songs",
            style = ts.callout, color = colors.secondaryContent,
        )
        GlasmioButton(onClick = { if (tracks.isNotEmpty()) vm.play(tracks, 0) }) { Text("Play") }
        GlasmioGroupedList {
            tracks.forEachIndexed { i, t -> TrackItem(t, vm, i == 0, artistLink = false) { vm.play(tracks, i) } }
        }
        Spacer(Modifier.height(16.dp))
    }
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
