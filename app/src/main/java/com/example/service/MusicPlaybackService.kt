package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.MusicApp
import com.example.R
import com.example.data.model.YouTubeVideo
import com.example.ui.components.GlobalMusicPlayerHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var mediaSession: MediaSession? = null

    companion object {
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_TRACK = "com.example.service.PLAY_TRACK"
        const val ACTION_PLAY = "com.example.service.PLAY"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.example.service.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.service.NEXT"
        const val ACTION_PREVIOUS = "com.example.service.PREVIOUS"
        const val ACTION_STOP = "com.example.service.STOP"

        private val _serviceState = MutableStateFlow(ServicePlaybackState())
        val serviceState: StateFlow<ServicePlaybackState> = _serviceState.asStateFlow()

        // Callbacks from UI or background player controller
        var onNextCallback: (() -> Unit)? = null
        var onPrevCallback: (() -> Unit)? = null
        var onTogglePlayCallback: (() -> Unit)? = null
        var onStopCallback: (() -> Unit)? = null

        fun startServiceForTrack(context: Context, video: YouTubeVideo, isPlaying: Boolean = true) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_PLAY_TRACK
                putExtra("video_id", video.id)
                putExtra("video_title", video.title)
                putExtra("channel_title", video.channelTitle)
                putExtra("thumbnail_url", video.thumbnailUrl)
                putExtra("is_playing", isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updatePlaybackStatus(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PLAY else ACTION_PAUSE
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TubeMusic:PlaybackWakeLock")

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "TubeMusic:WifiLock")

        initMediaSession()
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSession(this, "TubeMusicSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        if (onTogglePlayCallback != null) {
                            onTogglePlayCallback?.invoke()
                        } else {
                            GlobalMusicPlayerHolder.playDirectly()
                            _serviceState.value = _serviceState.value.copy(isPlaying = true)
                            val current = _serviceState.value
                            updateMediaSessionState(true, current.title, current.artist)
                            showForegroundNotification(current.title, current.artist, current.thumbnailUrl, true)
                        }
                    }

                    override fun onPause() {
                        if (onTogglePlayCallback != null) {
                            onTogglePlayCallback?.invoke()
                        } else {
                            GlobalMusicPlayerHolder.pauseDirectly()
                            _serviceState.value = _serviceState.value.copy(isPlaying = false)
                            val current = _serviceState.value
                            updateMediaSessionState(false, current.title, current.artist)
                            showForegroundNotification(current.title, current.artist, current.thumbnailUrl, false)
                        }
                    }

                    override fun onSkipToNext() {
                        onNextCallback?.invoke()
                    }

                    override fun onSkipToPrevious() {
                        onPrevCallback?.invoke()
                    }

                    override fun onStop() {
                        GlobalMusicPlayerHolder.pauseDirectly()
                        onStopCallback?.invoke()
                        stopForeground(true)
                        stopSelf()
                    }
                })
                isActive = true
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_PLAY_TRACK -> {
                val videoId = intent.getStringExtra("video_id") ?: ""
                val title = intent.getStringExtra("video_title") ?: "Music Track"
                val artist = intent.getStringExtra("channel_title") ?: "Artist"
                val thumb = intent.getStringExtra("thumbnail_url") ?: ""
                val isPlaying = intent.getBooleanExtra("is_playing", true)

                _serviceState.value = ServicePlaybackState(
                    currentVideoId = videoId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumb,
                    isPlaying = isPlaying,
                    isActive = true
                )
                acquireWakeLock()
                updateMediaSessionState(isPlaying, title, artist)
                showForegroundNotification(title, artist, thumb, isPlaying)
            }
            ACTION_PLAY -> {
                _serviceState.value = _serviceState.value.copy(isPlaying = true)
                acquireWakeLock()
                val current = _serviceState.value
                GlobalMusicPlayerHolder.playDirectly()
                updateMediaSessionState(true, current.title, current.artist)
                showForegroundNotification(current.title, current.artist, current.thumbnailUrl, true)
            }
            ACTION_PAUSE -> {
                _serviceState.value = _serviceState.value.copy(isPlaying = false)
                releaseWakeLock()
                val current = _serviceState.value
                GlobalMusicPlayerHolder.pauseDirectly()
                updateMediaSessionState(false, current.title, current.artist)
                showForegroundNotification(current.title, current.artist, current.thumbnailUrl, false)
            }
            ACTION_TOGGLE_PLAY -> {
                if (onTogglePlayCallback != null) {
                    onTogglePlayCallback?.invoke()
                } else {
                    GlobalMusicPlayerHolder.togglePlayDirectly()
                    val newPlaying = !_serviceState.value.isPlaying
                    _serviceState.value = _serviceState.value.copy(isPlaying = newPlaying)
                    val current = _serviceState.value
                    updateMediaSessionState(newPlaying, current.title, current.artist)
                    showForegroundNotification(current.title, current.artist, current.thumbnailUrl, newPlaying)
                }
            }
            ACTION_NEXT -> {
                onNextCallback?.invoke()
            }
            ACTION_PREVIOUS -> {
                onPrevCallback?.invoke()
            }
            ACTION_STOP -> {
                _serviceState.value = ServicePlaybackState()
                releaseWakeLock()
                GlobalMusicPlayerHolder.pauseDirectly()
                onStopCallback?.invoke()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = _serviceState.value
        // If track is active, continue running foreground service in background even if app is closed/swiped from recents
        if (current.isActive) {
            acquireWakeLock()
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun updateMediaSessionState(isPlaying: Boolean, title: String, artist: String) {
        try {
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val actions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP

            mediaSession?.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build()
            )

            mediaSession?.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, artist)
                    .build()
            )
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours timeout
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun showForegroundNotification(title: String, artist: String, thumbnailUrl: String, isPlaying: Boolean) {
        serviceScope.launch {
            val bitmap = loadThumbnailBitmap(thumbnailUrl)
            val notification = buildNotification(title, artist, bitmap, isPlaying)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun loadThumbnailBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun buildNotification(
        title: String,
        artist: String,
        largeIcon: Bitmap?,
        isPlaying: Boolean
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, MusicApp.MUSIC_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("TubeMusic Background Player")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseTitle, toggleIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$artist\nNow streaming in lossless audio")
                    .setBigContentTitle(title)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

data class ServicePlaybackState(
    val currentVideoId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val isPlaying: Boolean = false,
    val isActive: Boolean = false
)
