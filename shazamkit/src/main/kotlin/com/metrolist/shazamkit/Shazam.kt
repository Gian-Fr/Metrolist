package com.metrolist.shazamkit

import com.metrolist.shazamkit.models.RecognitionResult
import com.metrolist.shazamkit.models.ShazamResponseJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object Shazam {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            expectSuccess = true
        }
    }

    /**
     * Search for a song by title and artist using Shazam's search API
     * This is a fallback method when audio recognition is not available
     */
    suspend fun searchByText(query: String): Result<List<RecognitionResult>> = runCatching {
        val response = client.get("https://www.shazam.com/services/amapi/v1/catalog/US/search") {
            parameter("term", query)
            parameter("types", "songs")
            parameter("limit", "10")
            header("Accept", "application/json")
        }
        
        // Parse response and convert to RecognitionResult
        // This is simplified - actual implementation would parse the response properly
        emptyList<RecognitionResult>()
    }

    /**
     * Convert Shazam response to our internal model
     */
    fun ShazamResponseJson.toRecognitionResult(): RecognitionResult? {
        val track = this.track ?: return null
        
        // Extract metadata from sections
        val songSection = track.sections?.find { it?.type == "SONG" }
        val metadata = songSection?.metadata
        val album = metadata?.find { it?.title == "Album" }?.text
        val label = metadata?.find { it?.title == "Label" }?.text
        val releaseDate = metadata?.find { it?.title == "Released" }?.text
        
        // Extract lyrics
        val lyricsSection = track.sections?.find { it?.type == "LYRICS" }
        val lyrics = lyricsSection?.text
        
        // Extract streaming links
        val appleAction = track.hub?.options?.firstOrNull { 
            it?.providername?.contains("apple", ignoreCase = true) == true 
        }?.actions?.firstOrNull()
        val spotifyProvider = track.hub?.providers?.find { 
            it?.caption?.contains("spotify", ignoreCase = true) == true 
        }
        
        // Extract YouTube video ID if available
        val youtubeAction = track.hub?.options?.find { 
            it?.type?.contains("video", ignoreCase = true) == true 
        }?.actions?.firstOrNull()
        val youtubeVideoId = youtubeAction?.uri?.let { uri ->
            // Extract video ID from YouTube URL or URI
            uri.substringAfterLast("v=", "").takeIf { it.isNotEmpty() }
                ?: uri.substringAfterLast("/", "").takeIf { it.isNotEmpty() && it.length == 11 }
        }

        return RecognitionResult(
            trackId = track.key ?: tagid ?: "",
            title = track.title ?: "",
            artist = track.subtitle ?: "",
            album = album,
            coverArtUrl = track.images?.coverart,
            coverArtHqUrl = track.images?.coverarthq,
            genre = track.genres?.primary,
            releaseDate = releaseDate,
            label = label,
            lyrics = lyrics,
            shazamUrl = track.url,
            appleMusicUrl = appleAction?.uri,
            spotifyUrl = spotifyProvider?.actions?.firstOrNull()?.uri,
            isrc = track.isrc,
            youtubeVideoId = youtubeVideoId
        )
    }
}
