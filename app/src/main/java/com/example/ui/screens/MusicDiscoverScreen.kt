package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.YouTubeVideo
import com.example.ui.components.MusicTrackRow
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.DarkCharcoal
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.AlbumDetail
import com.example.ui.viewmodel.ArtistProfile
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel
import java.util.Calendar

/**
 * Screen 1: Home / Discover Screen matching designv2-example.png
 */
@Composable
fun MusicDiscoverScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val featuredTracks = if (uiState.quickPicks.isNotEmpty()) uiState.quickPicks else uiState.searchResults
    val heroTrack = featuredTracks.firstOrNull() ?: uiState.currentTrack

    // Calculate dynamic time of day greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..22 -> "Good evening,"
            else -> "Night vibes,"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .testTag("music_discover_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Top Greeting Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MediumGray
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.userName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianBlack,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎸",
                            fontSize = 20.sp
                        )
                    }
                    Text(
                        text = "Let the music move you.",
                        fontSize = 13.sp,
                        color = MediumGray,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Notification Bell Icon
                IconButton(
                    onClick = { viewModel.showNotificationsDialog(true) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SoftSurfaceGray)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Search Shortcut Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .clickable { viewModel.onSelectTab(1) }
                    .testTag("home_search_bar"),
                shape = RoundedCornerShape(25.dp),
                color = SoftSurfaceGray
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MediumGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search songs, artists, albums...",
                        color = MediumGray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 3. Featured Hero Card ("Midnight Echoes")
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        if (featuredTracks.isNotEmpty()) {
                            viewModel.playTrack(featuredTracks.first(), featuredTracks)
                        }
                    }
                    .testTag("hero_album_card"),
                shape = RoundedCornerShape(24.dp),
                color = ObsidianBlack
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Image / Gradient
                    if (heroTrack != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(heroTrack.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = heroTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ObsidianBlack, Color(0x99121214), Color(0x33000000))
                                    )
                                )
                        )
                    }

                    // Scrim Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ObsidianBlack.copy(alpha = 0.95f),
                                        ObsidianBlack.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Content inside Hero Card
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Midnight Echoes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New Album • 12 tracks",
                                fontSize = 13.sp,
                                color = Color(0xFFD1D5DB),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Experience raw emotion and midnight basslines.",
                                fontSize = 11.sp,
                                color = Color(0xFFA1A1AA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Circular Solid Play Button
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (featuredTracks.isNotEmpty()) {
                                        viewModel.playTrack(featuredTracks.first(), featuredTracks)
                                    }
                                },
                            shape = CircleShape,
                            color = PureWhite
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Recently Played Carousel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Played",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "See all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediumGray,
                        modifier = Modifier.clickable { viewModel.onSelectTab(2) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val recentList = if (uiState.recentlyPlayed.isNotEmpty()) {
                    uiState.recentlyPlayed
                } else if (featuredTracks.size > 1) {
                    featuredTracks.drop(1).take(6)
                } else {
                    featuredTracks
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentList) { track ->
                        RecentlyPlayedCard(
                            track = track,
                            onClick = { viewModel.playTrack(track, recentList) }
                        )
                    }
                }
            }
        }

        // 5. Quick Mixes Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Mixes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.quickMixes) { (name, tag) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.onCategorySelected(name)
                                    viewModel.onSelectTab(1)
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = SoftSurfaceGray
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ObsidianBlack
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = MediumGray,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Top Tracks & Popular List
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trending Now",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack,
                letterSpacing = (-0.3).sp
            )
        }

        if (uiState.isLoading && featuredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ObsidianBlack, strokeWidth = 2.5.dp)
                }
            }
        } else {
            itemsIndexed(featuredTracks.take(12)) { index, track ->
                MusicTrackRow(
                    track = track,
                    index = index,
                    isCurrentPlaying = uiState.currentTrack?.id == track.id,
                    isPlaying = uiState.isPlaying,
                    isSaved = viewModel.isVideoSaved(track.id),
                    onTrackClick = { viewModel.playTrack(track, featuredTracks) },
                    onPlayNext = { viewModel.playNextInQueue(track) },
                    onAddToQueue = { viewModel.addToQueue(track) },
                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                    onToggleSave = { viewModel.toggleSaveVideo(track) }
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedCard(
    track: YouTubeVideo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color(0x1A000000))
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ObsidianBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.displayArtist,
            fontSize = 11.sp,
            color = MediumGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
