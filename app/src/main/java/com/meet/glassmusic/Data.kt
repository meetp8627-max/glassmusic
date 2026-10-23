package com.meet.glassmusic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

data class Song(
    val title: String,
    val artist: String,
    val thumbnail: String,
    val streamUrl: String,
    val duration: Long = 0
)

suspend fun searchSongs(query: String, limit: Int = 20): List<Song> = withContext(Dispatchers.IO) {
    try {
        val service = ServiceList.YouTube
        val searchExtractor = service.getSearchExtractor(query, listOf("music_songs"), "")
        searchExtractor.fetchPage()

        val items = searchExtractor.initialPage.items.take(limit)

        items.mapNotNull { item ->
            try {
                val info = StreamInfo.getInfo(item.url)
                val bestAudio = info.audioStreams.maxByOrNull { it.averageBitrate }

                if (bestAudio != null) {
                    Song(
                        title = info.name,
                        artist = info.uploaderName,
                        thumbnail = info.thumbnails.firstOrNull()?.url ?: "",
                        streamUrl = bestAudio.url,
                        duration = info.duration
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}
