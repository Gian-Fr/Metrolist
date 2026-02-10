/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.GridMenu
import com.metrolist.music.ui.component.GridMenuItem
import com.metrolist.shazamkit.models.RecognitionResult

@Composable
fun RecognitionResultMenu(
    result: RecognitionResult,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp
        )
    ) {
        // Play on app
        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play_on_app
        ) {
            val searchQuery = "${result.title} ${result.artist}"
            navController.navigate("search/${java.net.URLEncoder.encode(searchQuery, "UTF-8")}")
            onDismiss()
        }
        
        // View details
        GridMenuItem(
            icon = R.drawable.info,
            title = R.string.details
        ) {
            // Show details dialog or navigate to details
            onDismiss()
        }
        
        // View lyrics if available
        if (!result.lyrics.isNullOrEmpty()) {
            GridMenuItem(
                icon = R.drawable.lyrics,
                title = R.string.lyrics
            ) {
                // Show lyrics
                onDismiss()
            }
        }
        
        // Share
        GridMenuItem(
            icon = R.drawable.share,
            title = R.string.share
        ) {
            val shareText = buildString {
                append("🎵 ")
                append(result.title)
                append(" - ")
                append(result.artist)
                result.album?.let {
                    append("\n📀 ")
                    append(it)
                }
                result.shazamUrl?.let {
                    append("\n🔗 ")
                    append(it)
                }
                append("\n\nRecognized with Metrolist")
            }
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }
        
        // Open in Shazam if URL available
        result.shazamUrl?.let { url ->
            GridMenuItem(
                icon = R.drawable.language,
                title = R.string.view_on_shazam
            ) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
                onDismiss()
            }
        }
        
        // Open in Apple Music if URL available
        result.appleMusicUrl?.let { url ->
            GridMenuItem(
                icon = R.drawable.language,
                title = R.string.view_on_apple_music
            ) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
                onDismiss()
            }
        }
        
        // Open in Spotify if URL available
        result.spotifyUrl?.let { url ->
            GridMenuItem(
                icon = R.drawable.language,
                title = R.string.view_on_spotify
            ) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
                onDismiss()
            }
        }
    }
}
