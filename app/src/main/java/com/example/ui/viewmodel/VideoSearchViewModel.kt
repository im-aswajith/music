package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalPreferencesRepository
import com.example.data.model.EqualizerPreset
import com.example.data.model.PlayerDisplayMode
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.SearchFilterType
import com.example.data.model.YouTubeVideo
import com.example.data.network.YouTubeSearchRepository
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistProfile(
    val name: String,
    val imageUrl: String,
    val followersText: String = "2.4M followers • 12 following",
    val isFollowing: Boolean = false,
    val bio: String = "Alternative pop and electronic artist exploring atmospheric soundscapes and deep emotional lyricism."
)

data class AlbumDetail(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val year: String = "2024",
    val tracksCount: Int = 10,
    val tracks: List<YouTubeVideo> = emptyList()
)

data class MusicPlayerUiState(
    // User Profile
    val userName: String = "Aswajith",
    val userEmail: String = "2007aswajith@gmail.com",
    val shouldQuitApp: Boolean = false,

    // Browsing & Search
    val query: String = "",
    val activeFilter: SearchFilterType = SearchFilterType.ALL,
    val activeSearchCategory: String = "Songs", // Songs, Artists, Albums, Playlists
    val isLoading: Boolean = false,
    val isSuggestionsLoading: Boolean = false,
    val liveSuggestions: List<String> = emptyList(),
    val searchResults: List<YouTubeVideo> = emptyList(),
    val quickPicks: List<YouTubeVideo> = emptyList(),
    val trendingTracks: List<YouTubeVideo> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val savedVideos: List<YouTubeVideo> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val recentlyPlayed: List<YouTubeVideo> = emptyList(),
    val selectedCategory: String = "Trending Music",
    val errorMessage: String? = null,
    val currentTab: Int = 0, // 0: Home, 1: Search, 2: Library, 3: Profile

    // Music Playback State
    val currentTrack: YouTubeVideo? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<YouTubeVideo> = emptyList(),
    val queueIndex: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val isShuffleEnabled: Boolean = false,
    val playerDisplayMode: PlayerDisplayMode = PlayerDisplayMode.AUDIO_ARTWORK,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutesRemaining: Int? = null,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.NORMAL,

    // Audio & Settings State (from designv2 screens)
    val selectedTheme: String = "Dark", // Light, Dark, System
    val streamingQuality: String = "Auto",
    val downloadQuality: String = "High (320 kbps)",
    val crossfadeSeconds: Int = 3,
    val isGaplessPlayback: Boolean = true,
    val isAutoplay: Boolean = true,
    val isNormalizeVolume: Boolean = true,
    val isPlayInOrder: Boolean = false,
    val band60Hz: Float = 0.5f,
    val band230Hz: Float = 0.5f,
    val band910Hz: Float = 0.5f,
    val band3600Hz: Float = 0.5f,
    val band14000Hz: Float = 0.5f,
    val isBassBoost: Boolean = false,
    val isVirtualizer: Boolean = false,

    // Modals, Sheets & Sub-screens
    val isNowPlayingSheetOpen: Boolean = false,
    val isLyricsSheetOpen: Boolean = false,
    val isQueueSheetOpen: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val trackForPlaylistSelection: YouTubeVideo? = null,
    val showSleepTimerDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showSpeedDialog: Boolean = false,
    val showPlaybackSettingsDialog: Boolean = false,
    val showAudioQualityDialog: Boolean = false,
    val showNotificationsDialog: Boolean = false,
    val showAppearanceDialog: Boolean = false,
    val selectedArtistDetail: ArtistProfile? = null,
    val selectedAlbumDetail: AlbumDetail? = null,
    val isLikedSongsScreenOpen: Boolean = false
)

class VideoSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchRepo = YouTubeSearchRepository()
    private val localRepo = LocalPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        MusicPlayerUiState(
            userName = localRepo.getUserName(),
            userEmail = localRepo.getUserEmail(),
            selectedTheme = localRepo.getSelectedTheme(),
            searchHistory = localRepo.getSearchHistory(),
            savedVideos = localRepo.getSavedVideos(),
            playlists = localRepo.getPlaylists(),
            recentlyPlayed = localRepo.getRecentlyPlayed(),
            repeatMode = localRepo.getRepeatMode(),
            isShuffleEnabled = localRepo.isShuffleEnabled(),
            equalizerPreset = localRepo.getEqualizerPreset(),
            band60Hz = localRepo.getEqBand("60", 0.5f),
            band230Hz = localRepo.getEqBand("230", 0.5f),
            band910Hz = localRepo.getEqBand("910", 0.5f),
            band3600Hz = localRepo.getEqBand("3600", 0.5f),
            band14000Hz = localRepo.getEqBand("14000", 0.5f),
            isBassBoost = localRepo.isBassBoost(),
            isVirtualizer = localRepo.isVirtualizer()
        )
    )
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var progressTickerJob: Job? = null

    val trendingSearches = listOf(
        "ocean eyes",
        "billie eilish",
        "the weeknd",
        "chill vibes",
        "tame impala",
        "lofi beats",
        "arctic monkeys",
        "indie rock"
    )

    val quickMixes = listOf(
        Pair("Chill Mix", "#waves"),
        Pair("Focus Mix", "#study"),
        Pair("Workout Mix", "#energy"),
        Pair("Late Night", "#jazz"),
        Pair("Indie Vibe", "#acoustic"),
        Pair("Lo-Fi Beats", "#peaceful")
    )

    init {
        // Wire service callbacks for lockscreen/notification background controls
        MusicPlaybackService.onTogglePlayCallback = { togglePlayPause() }
        MusicPlaybackService.onNextCallback = { playNextTrack() }
        MusicPlaybackService.onPrevCallback = { playPreviousTrack() }
        MusicPlaybackService.onStopCallback = { pausePlayback() }

        // Initial load with default trending music tracks
        loadMusicCategory("Trending Music")
        loadQuickPicks()
    }

    private fun loadQuickPicks() {
        viewModelScope.launch {
            try {
                val picks = searchRepo.searchVideos("popular hits songs 2026", SearchFilterType.ALL)
                val finalPicks = if (picks.isNotEmpty()) picks else searchRepo.getCuratedFallbackVideos("music")
                _uiState.update { it.copy(quickPicks = finalPicks) }
            } catch (_: Exception) {
                _uiState.update { it.copy(quickPicks = searchRepo.getCuratedFallbackVideos("music")) }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, errorMessage = null) }

        suggestionJob?.cancel()
        if (newQuery.trim().isEmpty()) {
            _uiState.update { it.copy(liveSuggestions = emptyList(), isSuggestionsLoading = false) }
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(200) // Debounce typing
            _uiState.update { it.copy(isSuggestionsLoading = true) }
            val suggestions = searchRepo.getLiveSuggestions(newQuery)
            _uiState.update {
                it.copy(
                    liveSuggestions = suggestions,
                    isSuggestionsLoading = false
                )
            }
        }
    }

    /**
     * Executed when the user submits a search query (e.g. clicks Keyboard Search IME button)
     */
    fun onSearchSubmitted(query: String = _uiState.value.query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _uiState.update {
            it.copy(
                query = trimmed,
                liveSuggestions = emptyList(),
                currentTab = 1, // Ensure on Search tab
                isLoading = true,
                errorMessage = null
            )
        }

        localRepo.addSearchQuery(trimmed)
        refreshHistory()

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val results = searchRepo.searchVideos(trimmed, _uiState.value.activeFilter)
                val finalResults = if (results.isNotEmpty()) results else searchRepo.getCuratedFallbackVideos(trimmed)
                _uiState.update {
                    it.copy(
                        searchResults = finalResults,
                        isLoading = false,
                        errorMessage = if (finalResults.isEmpty()) "No songs found for \"$trimmed\"" else null
                    )
                }
            } catch (e: Exception) {
                val fallback = searchRepo.getCuratedFallbackVideos(trimmed)
                _uiState.update {
                    it.copy(
                        searchResults = fallback,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onSearchCategoryChanged(category: String) {
        _uiState.update { it.copy(activeSearchCategory = category) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            onSearchSubmitted(currentQuery)
        }
    }

    fun onFilterChanged(filter: SearchFilterType) {
        if (_uiState.value.activeFilter == filter) return
        _uiState.update { it.copy(activeFilter = filter) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            onSearchSubmitted(currentQuery)
        } else {
            loadMusicCategory(_uiState.value.selectedCategory)
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                activeFilter = if (category.contains("Live", ignoreCase = true)) SearchFilterType.LIVE else SearchFilterType.ALL
            )
        }
        loadMusicCategory(category)
    }

    private fun loadMusicCategory(category: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchQuery = when (category) {
                "Trending Music" -> "top music hits 2026 official"
                "Chill Mix" -> "chill vibes acoustic lo-fi relax"
                "Focus Mix" -> "deep focus ambient study music"
                "Workout Mix" -> "workout gym high energy bass music"
                "Late Night" -> "late night jazz piano atmospheric"
                "Indie Vibe" -> "indie acoustic guitar alternative"
                "Lo-Fi Beats" -> "lofi hip hop chill beats"
                else -> "$category songs"
            }
            val filter = _uiState.value.activeFilter

            try {
                val results = searchRepo.searchVideos(searchQuery, filter)
                val finalResults = if (results.isNotEmpty()) results else searchRepo.getCuratedFallbackVideos(searchQuery)
                _uiState.update {
                    it.copy(
                        searchResults = finalResults,
                        trendingTracks = finalResults,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val fallback = searchRepo.getCuratedFallbackVideos(searchQuery)
                _uiState.update {
                    it.copy(
                        searchResults = fallback,
                        trendingTracks = fallback,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSelectTab(tabIndex: Int) {
        _uiState.update {
            it.copy(
                currentTab = tabIndex,
                isLikedSongsScreenOpen = false,
                selectedArtistDetail = null,
                selectedAlbumDetail = null
            )
        }
        if (tabIndex == 2) {
            refreshSavedVideos()
            refreshPlaylists()
        }
    }

    // ==========================================
    // MUSIC PLAYBACK ENGINE CONTROLS
    // ==========================================

    fun playTrack(track: YouTubeVideo, contextQueue: List<YouTubeVideo> = emptyList()) {
        val newQueue = if (contextQueue.isNotEmpty()) {
            contextQueue
        } else if (_uiState.value.queue.isNotEmpty()) {
            _uiState.value.queue
        } else {
            listOf(track)
        }

        val idx = newQueue.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }

        _uiState.update {
            it.copy(
                currentTrack = track,
                queue = newQueue,
                queueIndex = idx,
                isPlaying = true,
                isBuffering = true,
                playbackPositionMs = 0L,
                durationMs = track.durationSeconds * 1000L
            )
        }

        // Save to recently played
        localRepo.addRecentlyPlayed(track)
        refreshRecentlyPlayed()

        // Start background media service
        MusicPlaybackService.startServiceForTrack(
            getApplication<Application>().applicationContext,
            track,
            isPlaying = true
        )

        startProgressTicker()
    }

    fun togglePlayPause() {
        val current = _uiState.value.currentTrack ?: return
        val newPlayState = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = newPlayState) }

        MusicPlaybackService.updatePlaybackStatus(
            getApplication<Application>().applicationContext,
            newPlayState
        )

        if (newPlayState) {
            startProgressTicker()
        } else {
            progressTickerJob?.cancel()
        }
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        MusicPlaybackService.updatePlaybackStatus(
            getApplication<Application>().applicationContext,
            false
        )
        progressTickerJob?.cancel()
    }

    fun resumePlayback() {
        if (_uiState.value.currentTrack != null) {
            _uiState.update { it.copy(isPlaying = true) }
            MusicPlaybackService.updatePlaybackStatus(
                getApplication<Application>().applicationContext,
                true
            )
            startProgressTicker()
        }
    }

    fun playNextTrack() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled) {
            (state.queue.indices).filter { it != state.queueIndex }.randomOrNull() ?: 0
        } else {
            (state.queueIndex + 1) % state.queue.size
        }

        if (nextIndex < state.queue.size) {
            val nextTrack = state.queue[nextIndex]
            _uiState.update {
                it.copy(
                    currentTrack = nextTrack,
                    queueIndex = nextIndex,
                    isPlaying = true,
                    isBuffering = true,
                    playbackPositionMs = 0L,
                    durationMs = nextTrack.durationSeconds * 1000L
                )
            }
            localRepo.addRecentlyPlayed(nextTrack)
            refreshRecentlyPlayed()

            MusicPlaybackService.startServiceForTrack(
                getApplication<Application>().applicationContext,
                nextTrack,
                isPlaying = true
            )
        }
    }

    fun playPreviousTrack() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (state.playbackPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else state.queue.size - 1
        if (prevIndex in state.queue.indices) {
            val prevTrack = state.queue[prevIndex]
            _uiState.update {
                it.copy(
                    currentTrack = prevTrack,
                    queueIndex = prevIndex,
                    isPlaying = true,
                    isBuffering = true,
                    playbackPositionMs = 0L,
                    durationMs = prevTrack.durationSeconds * 1000L
                )
            }
            localRepo.addRecentlyPlayed(prevTrack)
            refreshRecentlyPlayed()

            MusicPlaybackService.startServiceForTrack(
                getApplication<Application>().applicationContext,
                prevTrack,
                isPlaying = true
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = _uiState.value.durationMs
        val clamped = if (dur > 0) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        _uiState.update { it.copy(playbackPositionMs = clamped) }
    }

    fun onTrackEnded() {
        val state = _uiState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                _uiState.update { it.copy(isPlaying = true) }
            }
            RepeatMode.ALL -> {
                playNextTrack()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    playNextTrack()
                } else {
                    _uiState.update { it.copy(isPlaying = false) }
                    MusicPlaybackService.updatePlaybackStatus(
                        getApplication<Application>().applicationContext,
                        false
                    )
                }
            }
        }
    }

    fun updatePlaybackProgress(positionSec: Float, durationSec: Float) {
        val posMs = (positionSec * 1000).toLong()
        val durMs = if (durationSec > 0) (durationSec * 1000).toLong() else _uiState.value.durationMs
        _uiState.update {
            it.copy(
                playbackPositionMs = posMs,
                durationMs = if (durMs > 0) durMs else it.durationMs,
                isBuffering = false
            )
        }
    }

    fun setPlayerStateFromBridge(isPlaying: Boolean, isBuffering: Boolean) {
        _uiState.update {
            it.copy(isPlaying = isPlaying, isBuffering = isBuffering)
        }
        MusicPlaybackService.updatePlaybackStatus(
            getApplication<Application>().applicationContext,
            isPlaying
        )
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                delay(1000)
                val current = _uiState.value
                if (current.isPlaying && current.durationMs > 0) {
                    val nextPos = (current.playbackPositionMs + (1000 * current.playbackSpeed).toLong())
                    if (nextPos >= current.durationMs) {
                        onTrackEnded()
                    } else {
                        _uiState.update { it.copy(playbackPositionMs = nextPos) }
                    }
                }
            }
        }
    }

    // Controls & Settings
    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffleEnabled
        localRepo.setShuffleEnabled(newShuffle)
        _uiState.update { it.copy(isShuffleEnabled = newShuffle) }
    }

    fun toggleRepeatMode() {
        val current = _uiState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        localRepo.setRepeatMode(next)
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed, showSpeedDialog = false) }
    }

    fun setPlayerDisplayMode(mode: PlayerDisplayMode) {
        _uiState.update { it.copy(playerDisplayMode = mode) }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        localRepo.setEqualizerPreset(preset)
        val (b60, b230, b910, b3600, b14000) = when (preset) {
            EqualizerPreset.NORMAL -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
            EqualizerPreset.BASS_BOOST -> listOf(0.85f, 0.70f, 0.50f, 0.45f, 0.45f)
            EqualizerPreset.VOCAL_BOOST -> listOf(0.40f, 0.55f, 0.85f, 0.75f, 0.60f)
            EqualizerPreset.ACOUSTIC -> listOf(0.60f, 0.55f, 0.65f, 0.70f, 0.75f)
            EqualizerPreset.EDM_CLUB -> listOf(0.90f, 0.65f, 0.45f, 0.75f, 0.85f)
            EqualizerPreset.ROCK -> listOf(0.75f, 0.60f, 0.45f, 0.70f, 0.80f)
            EqualizerPreset.LOFI_CHILL -> listOf(0.70f, 0.60f, 0.50f, 0.40f, 0.35f)
        }
        localRepo.setEqBand("60", b60)
        localRepo.setEqBand("230", b230)
        localRepo.setEqBand("910", b910)
        localRepo.setEqBand("3600", b3600)
        localRepo.setEqBand("14000", b14000)

        _uiState.update {
            it.copy(
                equalizerPreset = preset,
                band60Hz = b60,
                band230Hz = b230,
                band910Hz = b910,
                band3600Hz = b3600,
                band14000Hz = b14000
            )
        }
    }

    fun setFrequencyBand(bandIndex: Int, value: Float) {
        val clamped = value.coerceIn(0.0f, 1.0f)
        when (bandIndex) {
            0 -> {
                localRepo.setEqBand("60", clamped)
                _uiState.update { it.copy(band60Hz = clamped) }
            }
            1 -> {
                localRepo.setEqBand("230", clamped)
                _uiState.update { it.copy(band230Hz = clamped) }
            }
            2 -> {
                localRepo.setEqBand("910", clamped)
                _uiState.update { it.copy(band910Hz = clamped) }
            }
            3 -> {
                localRepo.setEqBand("3600", clamped)
                _uiState.update { it.copy(band3600Hz = clamped) }
            }
            4 -> {
                localRepo.setEqBand("14000", clamped)
                _uiState.update { it.copy(band14000Hz = clamped) }
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null, showSleepTimerDialog = false) }
            return
        }

        _uiState.update { it.copy(sleepTimerMinutesRemaining = minutes, showSleepTimerDialog = false) }
        sleepTimerJob = viewModelScope.launch {
            var rem = minutes
            while (rem > 0) {
                delay(60_000L)
                rem--
                _uiState.update { it.copy(sleepTimerMinutesRemaining = if (rem > 0) rem else null) }
            }
            // Sleep timer reached: stop the song and quit the app
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    sleepTimerMinutesRemaining = null,
                    shouldQuitApp = true
                )
            }
            MusicPlaybackService.stopService(getApplication<Application>().applicationContext)
        }
    }

    fun acknowledgeQuitApp() {
        _uiState.update { it.copy(shouldQuitApp = false) }
    }

    fun setTheme(theme: String) {
        localRepo.setSelectedTheme(theme)
        _uiState.update { it.copy(selectedTheme = theme, showAppearanceDialog = false) }
    }

    fun setUserName(name: String) {
        localRepo.setUserName(name)
        _uiState.update { it.copy(userName = name) }
    }

    fun setStreamingQuality(quality: String) {
        _uiState.update { it.copy(streamingQuality = quality) }
    }

    fun setDownloadQuality(quality: String) {
        _uiState.update { it.copy(downloadQuality = quality) }
    }

    fun setCrossfade(seconds: Int) {
        _uiState.update { it.copy(crossfadeSeconds = seconds) }
    }

    fun toggleGapless() {
        _uiState.update { it.copy(isGaplessPlayback = !it.isGaplessPlayback) }
    }

    fun toggleAutoplay() {
        _uiState.update { it.copy(isAutoplay = !it.isAutoplay) }
    }

    fun toggleNormalizeVolume() {
        _uiState.update { it.copy(isNormalizeVolume = !it.isNormalizeVolume) }
    }

    fun togglePlayInOrder() {
        _uiState.update { it.copy(isPlayInOrder = !it.isPlayInOrder) }
    }

    fun toggleBassBoost() {
        val next = !_uiState.value.isBassBoost
        localRepo.setBassBoost(next)
        _uiState.update { it.copy(isBassBoost = next) }
    }

    fun toggleVirtualizer() {
        val next = !_uiState.value.isVirtualizer
        localRepo.setVirtualizer(next)
        _uiState.update { it.copy(isVirtualizer = next) }
    }

    // Queue Management
    fun addToQueue(track: YouTubeVideo) {
        val current = _uiState.value.queue.toMutableList()
        current.add(track)
        _uiState.update { it.copy(queue = current) }
    }

    fun playNextInQueue(track: YouTubeVideo) {
        val current = _uiState.value.queue.toMutableList()
        val insertIndex = (_uiState.value.queueIndex + 1).coerceAtMost(current.size)
        current.add(insertIndex, track)
        _uiState.update { it.copy(queue = current) }
    }

    fun removeFromQueue(index: Int) {
        val current = _uiState.value.queue.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val newIdx = if (index < _uiState.value.queueIndex) _uiState.value.queueIndex - 1 else _uiState.value.queueIndex
            _uiState.update { it.copy(queue = current, queueIndex = newIdx.coerceAtLeast(0)) }
        }
    }

    fun clearQueue() {
        val current = _uiState.value.currentTrack
        _uiState.update {
            it.copy(
                queue = if (current != null) listOf(current) else emptyList(),
                queueIndex = 0
            )
        }
    }

    // Sheets & Dialog visibility triggers
    fun openNowPlayingSheet(open: Boolean) {
        _uiState.update { it.copy(isNowPlayingSheetOpen = open) }
    }

    fun openLyricsSheet(open: Boolean) {
        _uiState.update { it.copy(isLyricsSheetOpen = open) }
    }

    fun openQueueSheet(open: Boolean) {
        _uiState.update { it.copy(isQueueSheetOpen = open) }
    }

    fun openLikedSongsScreen(open: Boolean) {
        _uiState.update { it.copy(isLikedSongsScreenOpen = open) }
    }

    fun openArtistDetail(artist: ArtistProfile?) {
        _uiState.update { it.copy(selectedArtistDetail = artist) }
    }

    fun openAlbumDetail(album: AlbumDetail?) {
        _uiState.update { it.copy(selectedAlbumDetail = album) }
    }

    fun showCreatePlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showCreatePlaylistDialog = show) }
    }

    fun showAddToPlaylistDialog(track: YouTubeVideo?) {
        _uiState.update { it.copy(trackForPlaylistSelection = track) }
    }

    fun showSleepTimerDialog(show: Boolean) {
        _uiState.update { it.copy(showSleepTimerDialog = show) }
    }

    fun showEqualizerDialog(show: Boolean) {
        _uiState.update { it.copy(showEqualizerDialog = show) }
    }

    fun showSpeedDialog(show: Boolean) {
        _uiState.update { it.copy(showSpeedDialog = show) }
    }

    fun showPlaybackSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showPlaybackSettingsDialog = show) }
    }

    fun showAudioQualityDialog(show: Boolean) {
        _uiState.update { it.copy(showAudioQualityDialog = show) }
    }

    fun showNotificationsDialog(show: Boolean) {
        _uiState.update { it.copy(showNotificationsDialog = show) }
    }

    fun showAppearanceDialog(show: Boolean) {
        _uiState.update { it.copy(showAppearanceDialog = show) }
    }

    // Playlist Management
    fun createPlaylist(title: String, description: String) {
        val id = "pl_" + System.currentTimeMillis()
        val newPl = Playlist(
            id = id,
            title = title.ifBlank { "My Playlist" },
            description = description,
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600"
        )
        localRepo.savePlaylist(newPl)
        refreshPlaylists()
        showCreatePlaylistDialog(false)
    }

    fun deletePlaylist(playlistId: String) {
        localRepo.deletePlaylist(playlistId)
        if (_uiState.value.selectedPlaylist?.id == playlistId) {
            _uiState.update { it.copy(selectedPlaylist = null) }
        }
        refreshPlaylists()
    }

    fun addTrackToPlaylist(playlistId: String, video: YouTubeVideo) {
        localRepo.addTrackToPlaylist(playlistId, video.id)
        refreshPlaylists()
        showAddToPlaylistDialog(null)
    }

    fun removeTrackFromPlaylist(playlistId: String, videoId: String) {
        localRepo.removeTrackFromPlaylist(playlistId, videoId)
        refreshPlaylists()
        _uiState.value.selectedPlaylist?.let { pl ->
            if (pl.id == playlistId) {
                _uiState.update {
                    it.copy(selectedPlaylist = pl.copy(videoIds = pl.videoIds.filter { id -> id != videoId }))
                }
            }
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
    }

    // Liked & History
    fun toggleSaveVideo(video: YouTubeVideo) {
        localRepo.toggleSavedVideo(video)
        refreshSavedVideos()
    }

    fun isVideoSaved(videoId: String): Boolean {
        return localRepo.isVideoSaved(videoId)
    }

    fun removeHistoryItem(item: String) {
        localRepo.removeSearchQuery(item)
        refreshHistory()
    }

    fun clearAllHistory() {
        localRepo.clearSearchHistory()
        refreshHistory()
    }

    private fun refreshHistory() {
        _uiState.update { it.copy(searchHistory = localRepo.getSearchHistory()) }
    }

    private fun refreshSavedVideos() {
        _uiState.update { it.copy(savedVideos = localRepo.getSavedVideos()) }
    }

    private fun refreshPlaylists() {
        _uiState.update { it.copy(playlists = localRepo.getPlaylists()) }
    }

    private fun refreshRecentlyPlayed() {
        _uiState.update { it.copy(recentlyPlayed = localRepo.getRecentlyPlayed()) }
    }
}
