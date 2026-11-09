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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
fun appVersion(): String {
    val ctx = LocalContext.current
    return remember {
        try {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        } catch (e: Exception) { "?" }
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
    var confirm by remember { mutableIntStateOf(0) }
    val homeState = rememberLazyListState()
    val radioState = rememberLazyListState()
    val libraryState = rememberLazyListState()
    val searchState = rememberLazyListState()
    val activeState = when (tab) { 0 -> homeState; 1 -> radioState; 2 -> libraryState; else -> searchState }
    val navScroll = rememberGlasmioNavigationScrollState(listState = activeState)
    val tabs = remember {
        listOf(
            GlasmioTabItem(0, "Home", SfSymbol.HouseFilled),
            GlasmioTabItem(1, "Radio", SfSymbol.RadioWaves),
            GlasmioTabItem(2, "Library", SfSymbol.MusicNoteList),
            GlasmioTabItem(3, "Search", SfSymbol.Search),
        )
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().nestedScroll(navScroll.nestedScrollConnection)) {
            Crossfade(targetState = tab, label = "tab") { t ->
                when (t) {
                    0 -> HomePage(vm, homeState) { settings = true }
                    1 -> RadioPage(vm, radioState)
                    2 -> LibraryPage(vm, libraryState)
                    else -> SearchPage(vm, searchState)
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 12.dp).padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vm.current?.let { MiniBar(vm, it) { nowPlaying = true } }
            Box(
                Modifier.fillMaxWidth()
                    .glasmioNavigationScrollHint(navScroll.scrollHintProgress)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                GlasmioNavigationBar(
                    items = tabs,
                    selectedKey = tab,
                    onSelected = { tab = it },
                    onExpand = { navScroll.expand() },
                    compactProgress = navScroll.compactProgress,
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
        ) { SettingsContent(vm) { confirm = it } }
        GlasmioBottomSheet(
            visible = vm.artistName != null,
            skipPartiallyExpanded = false,
            onDismissRequest = { vm.closeArtist() },
            title = "Artist",
        ) { ArtistContent(vm) }
        GlasmioAlertDialog(
            onDismissRequest = { confirm = 0 },
            title = if (confirm == 1) "Clear Recently Played?" else "Clear Library?",
            message = if (confirm == 1) "Your listening history on this device will be removed."
                      else "All songs you added with the heart will be removed.",
            buttons = listOf(
                GlasmioAlertButtonSpec("Cancel", { confirm = 0 }, GlasmioAlertButtonRole.Cancel),
                GlasmioAlertButtonSpec(
                    "Clear",
                    { if (confirm == 1) vm.clearRecent() else vm.clearLibrary(); confirm = 0 },
                    GlasmioAlertButtonRole.Destructive,
                ),
            ),
            visible = confirm != 0,
        )
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

val MixPalette = listOf(
    Color(0xFFFA7333) to Color(0xFFBF1A4D),
    Color(0xFF3380E6) to Color(0xFF6633B3),
    Color(0xFF33BF8C) to Color(0xFF1A5980),
    Color(0xFFF2BF33) to Color(0xFFE65940),
)

@Composable
fun MixCard(name: String, art: String, index: Int, onClick: () -> Unit) {
    val ts = GlasmioTheme.textStyles
    val (c1, c2) = MixPalette[index % MixPalette.size]
    Box(
        Modifier.size(width = 200.dp, height = 124.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c1, c2))).clickable(onClick = onClick),
    ) {
        if (art.isNotEmpty()) {
            AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(Color.Transparent))
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1.copy(alpha = 0.82f), c2.copy(alpha = 0.9f)))))
        }
        Text("MADE FOR YOU", style = ts.callout, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(14.dp))
        Text(name, style = ts.title3, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
    }
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
fun HomePage(vm: MusicVM, listState: LazyListState, onSettings: () -> Unit) {
    val colors = GlasmioTheme.colors
    val ts = GlasmioTheme.textStyles
    val version = appVersion()
    GlasmioPinnedListPage(
        title = "Home",
        subtitle = "GlassMusic $version - Glasmio",
        listState = listState,
        bottomPadding = 190.dp,
        actions = { GlasmioIconButton(onClick = onSettings) { SfIcon(SfSymbol.Gearshape, "Settings") } },
    ) {
        if (vm.displayName.isNotBlank()) {
            item { Text("Hi, ${vm.displayName}", style = ts.title2, color = colors.content, modifier = Modifier.padding(4.dp)) }
        }
        vm.error?.let { e ->
            item { Text(e, color = colors.destructive, modifier = Modifier.clickable { vm.load() }.padding(4.dp)) }
        }
        vm.trending.firstOrNull()?.let { t -> item { HeroCard(t) { vm.play(vm.trending, 0) } } }
        if (vm.mixes.isNotEmpty()) {
            item { SectionHeader("Made for You") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    itemsIndexed(vm.mixes) { i, mix ->
                        MixCard(mix.first, mix.second.firstOrNull()?.art ?: "", i) { vm.play(mix.second, 0) }
                    }
                }
            }
        }
        if (vm.trending.size > 1) {
            item { SectionHeader("Trending this week") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                    itemsIndexed(vm.trending.drop(1)) { i, t ->
                        Column(Modifier.width(140.dp).clickable { vm.play(vm.trending, i + 1) }) {
                            Cover(t.art, 140.dp, 20.dp)
                            Text(t.title, style = ts.callout, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp))
                            Text(t.artist, style = ts.callout, color = colors.secondaryContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (vm.showRecent && vm.recent.isNotEmpty()) {
            item { SectionHeader("Recently Played") }
            item {
                GlasmioGroupedList {
                    val list = vm.recent.toList().take(8)
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
fun RadioPage(vm: MusicVM, listState: LazyListState) {
    val colors = GlasmioTheme.colors
    var sel by remember { mutableIntStateOf(0) }
    val tags = remember { listOf<String?>(null, "hindi", "pop", "lofi") }
    val labels = remember { listOf("Top", "Hindi", "Pop", "Lofi") }
    LaunchedEffect(Unit) { if (vm.stations.isEmpty()) vm.loadRadio() }
    GlasmioPinnedListPage(title = "Radio", listState = listState, bottomPadding = 190.dp) {
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
fun LibraryPage(vm: MusicVM, listState: LazyListState) {
    val colors = GlasmioTheme.colors
    GlasmioPinnedListPage(title = "Library", listState = listState, bottomPadding = 190.dp) {
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
fun SearchPage(vm: MusicVM, listState: LazyListState) {
    val colors = GlasmioTheme.colors
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var active by remember { mutableStateOf(false) }
    val genres = remember { listOf("Lofi", "Pop", "Rock", "Electronic", "Jazz", "Classical", "Hip Hop", "Ambient") }
    GlasmioPinnedListPage(title = "Search", listState = listState, bottomPadding = 190.dp) {
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
            Text("Drag the handle to reorder", style = ts.callout, color = colors.secondaryContent)
            GlasmioReorderableGroup(
                items = vm.queue,
                onReorder = { vm.reorderQueue(it) },
                dragHandleContentDescription = "Reorder",
                moveUpLabel = "Move up",
                moveDownLabel = "Move down",
                modifier = Modifier.fillMaxWidth(),
            ) { q ->
                Row(
                    Modifier.weight(1f).clickable { vm.playAt(vm.queue.indexOfFirst { it.id == q.id }.coerceAtLeast(0)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Cover(q.art, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            q.title, style = ts.body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (q.id == t.id) colors.accent else colors.content,
                        )
                        Text(q.artist, style = ts.callout, color = colors.secondaryContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
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
fun ColumnScope.SettingsContent(vm: MusicVM, askConfirm: (Int) -> Unit) {
    val colors = GlasmioTheme.colors
    val ts = GlasmioTheme.textStyles
    val version = appVersion()
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlasmioPreferenceGroupTitle("Profile")
        GlasmioGroupedList {
            GlasmioEditTextPreference(
                title = { Text("Display name") },
                value = vm.displayName,
                onValueChange = { vm.changeDisplayName(it) },
                isInputValid = { true },
                showTopSeparator = false,
            )
        }
        GlasmioPreferenceGroupTitle("Home")
        GlasmioGroupedList {
            GlasmioSwitchPreference(
                title = { Text("Show Recently Played") },
                checked = vm.showRecent,
                onCheckedChange = { vm.toggleShowRecent(it) },
                showTopSeparator = false,
            )
        }
        GlasmioPreferenceGroupTitle("Playback")
        GlasmioGroupedList {
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
        GlasmioPreferenceGroupTitle("Data")
        GlasmioGroupedList {
            GlasmioListRow(title = "Clear Recently Played", showTopSeparator = false, onClick = { askConfirm(1) })
            GlasmioListRow(title = "Clear Library", onClick = { askConfirm(2) })
        }
        GlasmioPreferenceGroupTitle("About")
        GlasmioGroupedList {
            GlasmioListRow(title = "Version", detail = version, showTopSeparator = false)
            GlasmioListRow(title = "Edition", detail = "Glasmio")
        }
        Text(
            "Built with Glasmio (Apache-2.0).",
            style = ts.callout, color = colors.secondaryContent, modifier = Modifier.padding(4.dp),
        )
        Spacer(Modifier.height(16.dp))
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
