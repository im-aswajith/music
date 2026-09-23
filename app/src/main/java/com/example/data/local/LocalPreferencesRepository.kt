package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.EqualizerPreset
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.YouTubeVideo
import org.json.JSONArray
import org.json.JSONObject

class LocalPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tubemusic_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_SAVED_VIDEOS = "saved_videos"
        private const val KEY_RECENTLY_PLAYED = "recently_played"
        private const val KEY_PLAYLISTS = "custom_playlists"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_SHUFFLE = "shuffle_enabled"
        private const val KEY_EQUALIZER = "equalizer_preset"
        private const val KEY_THEME = "selected_theme"
        private const val KEY_USER_NAME = "user_profile_name"
        private const val KEY_USER_EMAIL = "user_profile_email"
        private const val MAX_HISTORY = 20
        private const val MAX_RECENTS = 40
    }

    fun getSelectedTheme(): String {
        return prefs.getString(KEY_THEME, "Dark") ?: "Dark"
    }

    fun setSelectedTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getUserName(): String {
        val saved = prefs.getString(KEY_USER_NAME, null)
        if (!saved.isNullOrBlank()) return saved

        // Automatic fallback based on device/user profile
        return "Aswajith"
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "2007aswajith@gmail.com") ?: "2007aswajith@gmail.com"
    }

    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getSearchHistory(): List<String> {
        val jsonStr = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val item = arr.getString(i)
                if (item.isNotBlank()) list.add(item)
            }
        } catch (_: Exception) {}
        return list
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getSearchHistory().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(MAX_HISTORY)

        val arr = JSONArray()
        limited.forEach { arr.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    fun getSavedVideos(): List<YouTubeVideo> {
        val jsonStr = prefs.getString(KEY_SAVED_VIDEOS, null) ?: return emptyList()
        return parseVideosJson(jsonStr)
    }

    fun toggleSavedVideo(video: YouTubeVideo): Boolean {
        val current = getSavedVideos().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == video.id }
        val isNowSaved = if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            false
        } else {
            current.add(0, video)
            true
        }
        saveVideosJson(KEY_SAVED_VIDEOS, current)
        return isNowSaved
    }

    fun isVideoSaved(videoId: String): Boolean {
        return getSavedVideos().any { it.id == videoId }
    }

    fun getRecentlyPlayed(): List<YouTubeVideo> {
        val jsonStr = prefs.getString(KEY_RECENTLY_PLAYED, null) ?: return emptyList()
        return parseVideosJson(jsonStr)
    }

    fun addRecentlyPlayed(video: YouTubeVideo) {
        val current = getRecentlyPlayed().toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        val limited = current.take(MAX_RECENTS)
        saveVideosJson(KEY_RECENTLY_PLAYED, limited)
    }

    fun clearRecentlyPlayed() {
        prefs.edit().remove(KEY_RECENTLY_PLAYED).apply()
    }

    // Playlists Management
    fun getPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString(KEY_PLAYLISTS, null) ?: return getDefaultPlaylists()
        val list = mutableListOf<Playlist>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val videoIdsArr = obj.optJSONArray("videoIds")
                val videoIds = mutableListOf<String>()
                if (videoIdsArr != null) {
                    for (j in 0 until videoIdsArr.length()) {
                        videoIds.add(videoIdsArr.getString(j))
                    }
                }
                list.add(
                    Playlist(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        coverUrl = obj.optString("coverUrl").ifBlank { null },
                        videoIds = videoIds,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return if (list.isEmpty()) getDefaultPlaylists() else list
    }

    private fun getDefaultPlaylists(): List<Playlist> {
        return listOf(
            Playlist(
                id = "pl_favorites",
                title = "Favorites & Liked Hits",
                description = "Your favorite music tracks and saved songs",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                videoIds = emptyList()
            ),
            Playlist(
                id = "pl_lofi",
                title = "Lo-Fi Study & Chill",
                description = "Mellow beats, ambient soundscapes, and relaxing vibes",
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600",
                videoIds = listOf("jfKfPfyJRdk", "4xDzrJKXOOY")
            ),
            Playlist(
                id = "pl_workout",
                title = "High Energy & Workout",
                description = "Bass heavy hype tracks and high-tempo bangers",
                coverUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600",
                videoIds = listOf("kJQP7kiw5Fk", "9bZkp7q19f0")
            )
        )
    }

    fun savePlaylist(playlist: Playlist) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlist.id }
        if (index >= 0) {
            current[index] = playlist
        } else {
            current.add(0, playlist)
        }
        persistPlaylists(current)
    }

    fun deletePlaylist(playlistId: String) {
        val current = getPlaylists().toMutableList()
        current.removeAll { it.id == playlistId }
        persistPlaylists(current)
    }

    fun addTrackToPlaylist(playlistId: String, videoId: String) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            if (!pl.videoIds.contains(videoId)) {
                val updatedIds = pl.videoIds.toMutableList().apply { add(videoId) }
                current[index] = pl.copy(videoIds = updatedIds)
                persistPlaylists(current)
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, videoId: String) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            val updatedIds = pl.videoIds.toMutableList().apply { remove(videoId) }
            current[index] = pl.copy(videoIds = updatedIds)
            persistPlaylists(current)
        }
    }

    private fun persistPlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("title", pl.title)
                put("description", pl.description)
                put("coverUrl", pl.coverUrl ?: "")
                put("createdAt", pl.createdAt)
                val idArr = JSONArray()
                pl.videoIds.forEach { idArr.put(it) }
                put("videoIds", idArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    // Settings
    fun getRepeatMode(): RepeatMode {
        val name = prefs.getString(KEY_REPEAT_MODE, RepeatMode.ALL.name) ?: RepeatMode.ALL.name
        return try { RepeatMode.valueOf(name) } catch (_: Exception) { RepeatMode.ALL }
    }

    fun setRepeatMode(mode: RepeatMode) {
        prefs.edit().putString(KEY_REPEAT_MODE, mode.name).apply()
    }

    fun isShuffleEnabled(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)

    fun setShuffleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
    }

    fun getEqualizerPreset(): EqualizerPreset {
        val name = prefs.getString(KEY_EQUALIZER, EqualizerPreset.NORMAL.name) ?: EqualizerPreset.NORMAL.name
        return try { EqualizerPreset.valueOf(name) } catch (_: Exception) { EqualizerPreset.NORMAL }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        prefs.edit().putString(KEY_EQUALIZER, preset.name).apply()
    }

    fun getEqBand(band: String, defaultVal: Float): Float {
        return prefs.getFloat("eq_band_$band", defaultVal)
    }

    fun setEqBand(band: String, value: Float) {
        prefs.edit().putFloat("eq_band_$band", value).apply()
    }

    fun isBassBoost(): Boolean = prefs.getBoolean("eq_bass_boost", false)

    fun setBassBoost(enabled: Boolean) {
        prefs.edit().putBoolean("eq_bass_boost", enabled).apply()
    }

    fun isVirtualizer(): Boolean = prefs.getBoolean("eq_virtualizer", false)

    fun setVirtualizer(enabled: Boolean) {
        prefs.edit().putBoolean("eq_virtualizer", enabled).apply()
    }

    // Helper JSON conversions
    private fun parseVideosJson(jsonStr: String): List<YouTubeVideo> {
        val list = mutableListOf<YouTubeVideo>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    YouTubeVideo(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        channelTitle = obj.getString("channelTitle"),
                        channelThumbnailUrl = obj.optString("channelThumbnailUrl").ifBlank { null },
                        thumbnailUrl = obj.getString("thumbnailUrl"),
                        duration = obj.optString("duration").ifBlank { null },
                        viewCountText = obj.optString("viewCountText").ifBlank { null },
                        publishedTimeText = obj.optString("publishedTimeText").ifBlank { null },
                        isLive = obj.optBoolean("isLive", false),
                        description = obj.optString("description").ifBlank { null }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveVideosJson(key: String, videos: List<YouTubeVideo>) {
        val arr = JSONArray()
        videos.forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("title", v.title)
                put("channelTitle", v.channelTitle)
                put("channelThumbnailUrl", v.channelThumbnailUrl ?: "")
                put("thumbnailUrl", v.thumbnailUrl)
                put("duration", v.duration ?: "")
                put("viewCountText", v.viewCountText ?: "")
                put("publishedTimeText", v.publishedTimeText ?: "")
                put("isLive", v.isLive)
                put("description", v.description ?: "")
            }
            arr.put(obj)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}

