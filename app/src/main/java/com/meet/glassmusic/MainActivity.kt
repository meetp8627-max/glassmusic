package com.meet.glassmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NewPipe.init(DownloaderImpl.instance)
        NewPipe.setupLocalization(Localization.DEFAULT)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black,
                contentColor = Color.White
            ) {
                App()
            }
        }
    }
}

@Composable
fun App() {
    var query by remember { mutableStateOf("lofi") }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search YouTube Music") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    songs = searchSongs(query)
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Loading..." else "Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(songs) { song ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = song.title, color = Color.White)
                    Text(text = song.artist, color = Color.Gray)
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
        }
    }
}
