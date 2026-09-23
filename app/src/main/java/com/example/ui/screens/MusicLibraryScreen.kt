package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.data.model.Playlist
import com.example.data.model.YouTubeVideo
import com.example.ui.components.MusicTrackRow
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

/**
 * Screen 11 & 14: Library & Liked Songs matching designv2-example.png
 */
@Composable
fun MusicLibraryScreen(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Playlists") }
    val filters = listOf("Playlists", "Albums", "Artists", "Downloads")

    if (uiState.isLikedSongsScreenOpen) {
        // Liked Songs Screen (Screen 14)
        LikedSongsView(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.openLikedSongsScreen(false) }
        )
        return
    }

    if (uiState.selectedPlaylist != null) {
        // Playlist Detail View
        PlaylistDetailView(
            playlist = uiState.selectedPlaylist,
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.selectPlaylist(null) }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .testTag("music_library_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: "Your Library" & Action Icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ObsidianBlack,
                    letterSpacing = (-0.5).sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.onSelectTab(1) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Library",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showCreatePlaylistDialog(true) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create Playlist",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. Filter Chips: Playlists, Albums, Artists, Downloads
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) ObsidianBlack else SoftSurfaceGray,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) PureWhite else ObsidianBlack,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. System Playlists: Liked Songs & Recently Added
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Liked Songs Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.openLikedSongsScreen(true) }
                        .testTag("liked_songs_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Liked",
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Liked Songs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Text(
                            text = "${uiState.savedVideos.size} songs",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                    }
                }

                // Downloads / Recently Added Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.openLikedSongsScreen(true) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CanvasBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Downloads",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Downloads",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        Text(
                            text = "34 tracks",
                            fontSize = 12.sp,
                            color = MediumGray
                        )
                    }
                }
            }
        }

        // 4. Playlists Header & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack,
                    letterSpacing = (-0.3).sp
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.showCreatePlaylistDialog(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
                    }
                }
            }
        }

        // Playlists Items
        if (uiState.playlists.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftSurfaceGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showCreatePlaylistDialog(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CanvasBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MediumGray)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Create your first playlist", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Save your favorite songs in custom lists", color = MediumGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(uiState.playlists) { playlist ->
                PlaylistRowItem(
                    playlist = playlist,
                    onClick = { viewModel.selectPlaylist(playlist) },
                    onDelete = { viewModel.deletePlaylist(playlist.id) }
                )
            }
        }
    }
}

@Composable
fun LikedSongsView(
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ObsidianBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Liked Songs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            }
        }

        // Shuffle Play Pill Button
        item {
            Button(
                onClick = {
                    if (uiState.savedVideos.isNotEmpty()) {
                        viewModel.toggleShuffle()
                        viewModel.playTrack(uiState.savedVideos.random(), uiState.savedVideos)
                    }
                },
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianBlack,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle Play (${uiState.savedVideos.size} songs)", fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.savedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No liked songs yet. Tap heart on any track to save it!", color = MediumGray)
                }
            }
        } else {
            itemsIndexed(uiState.savedVideos) { index, track ->
                MusicTrackRow(
                    track = track,
                    index = index,
                    isCurrentPlaying = uiState.currentTrack?.id == track.id,
                    isPlaying = uiState.isPlaying,
                    isSaved = true,
                    onTrackClick = { viewModel.playTrack(track, uiState.savedVideos) },
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
fun PlaylistDetailView(
    playlist: Playlist,
    uiState: MusicPlayerUiState,
    viewModel: VideoSearchViewModel,
    onBack: () -> Unit
) {
    val playlistVideos = uiState.savedVideos.filter { playlist.videoIds.contains(it.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ObsidianBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playlist.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (playlistVideos.isNotEmpty()) {
                        viewModel.playTrack(playlistVideos.first(), playlistVideos)
                    }
                },
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianBlack,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play (${playlistVideos.size} songs)", fontWeight = FontWeight.Bold)
            }
        }

        if (playlistVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tracks in this playlist yet.", color = MediumGray)
                }
            }
        } else {
            itemsIndexed(playlistVideos) { index, track ->
                MusicTrackRow(
                    track = track,
                    index = index,
                    isCurrentPlaying = uiState.currentTrack?.id == track.id,
                    isPlaying = uiState.isPlaying,
                    isSaved = viewModel.isVideoSaved(track.id),
                    onTrackClick = { viewModel.playTrack(track, playlistVideos) },
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
fun PlaylistRowItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftSurfaceGray,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CanvasBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = ObsidianBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = playlist.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.videoIds.size} songs",
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }
            }

            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Options",
                        tint = MediumGray
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = Color(0xFFDC2626)) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626)) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
