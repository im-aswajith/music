package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PlayerDisplayMode
import com.example.data.model.YouTubeVideo
import com.example.ui.viewmodel.VideoSearchViewModel

class MusicJsBridge(
    private val onStateChange: (Int) -> Unit,
    private val onTimeUpdate: (Float, Float) -> Unit,
    private val onPlayerReady: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPlayerStateChange(state: Int) {
        mainHandler.post { onStateChange(state) }
    }

    @JavascriptInterface
    fun onProgress(current: Float, duration: Float) {
        mainHandler.post { onTimeUpdate(current, duration) }
    }

    @JavascriptInterface
    fun onReady() {
        mainHandler.post { onPlayerReady() }
    }
}

object GlobalMusicPlayerHolder {
    var webViewInstance: WebView? = null
    var currentLoadedId: String? = null
    var isPlayerReady: Boolean = false
    var intendedPlaying: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        onStateChange: (Int) -> Unit,
        onTimeUpdate: (Float, Float) -> Unit,
        onReady: () -> Unit
    ): WebView {
        if (webViewInstance != null) {
            return webViewInstance!!
        }

        val webView = WebView(context.applicationContext).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            }
            setBackgroundColor(android.graphics.Color.BLACK)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }

            addJavascriptInterface(
                MusicJsBridge(
                    onStateChange = onStateChange,
                    onTimeUpdate = onTimeUpdate,
                    onPlayerReady = {
                        isPlayerReady = true
                        onReady()
                    }
                ),
                "MusicBridge"
            )
        }

        webViewInstance = webView
        return webView
    }

    fun loadTrack(webView: WebView, videoId: String) {
        if (currentLoadedId == videoId && isPlayerReady) {
            play(webView)
            return
        }
        currentLoadedId = videoId
        intendedPlaying = true
        val htmlContent = buildPlayerHtml(videoId)
        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
    }

    fun playDirectly() {
        webViewInstance?.let { play(it) }
    }

    fun pauseDirectly() {
        webViewInstance?.let { pause(it) }
    }

    fun togglePlayDirectly() {
        if (intendedPlaying) {
            pauseDirectly()
        } else {
            playDirectly()
        }
    }

    fun play(webView: WebView) {
        intendedPlaying = true
        webView.evaluateJavascript("window.shouldBePlaying = true; if(window.player && player.playVideo){ player.playVideo(); } if(window.resumeAudioDsp){ window.resumeAudioDsp(); }", null)
    }

    fun pause(webView: WebView) {
        intendedPlaying = false
        webView.evaluateJavascript("window.shouldBePlaying = false; if(window.player && player.pauseVideo){ player.pauseVideo(); }", null)
    }

    fun seekTo(webView: WebView, seconds: Float) {
        webView.evaluateJavascript("if(window.player && player.seekTo){ player.seekTo($seconds, true); }", null)
    }

    fun setPlaybackRate(webView: WebView, rate: Float) {
        webView.evaluateJavascript("if(window.player && player.setPlaybackRate){ player.setPlaybackRate($rate); }", null)
    }

    fun applyEqualizer(
        webView: WebView,
        band60: Float,
        band230: Float,
        band910: Float,
        band3k: Float,
        band14k: Float,
        bassBoost: Boolean,
        virtualizer: Boolean
    ) {
        // Convert normalized 0.0..1.0 values to decibels (-12dB to +12dB)
        val g60 = (band60 - 0.5f) * 24f
        val g230 = (band230 - 0.5f) * 24f
        val g910 = (band910 - 0.5f) * 24f
        val g3k = (band3k - 0.5f) * 24f
        val g14k = (band14k - 0.5f) * 24f

        val script = "if(window.applyEqualizer){ window.applyEqualizer($g60, $g230, $g910, $g3k, $g14k, $bassBoost, $virtualizer); }"
        webView.evaluateJavascript(script, null)
    }

    private fun buildPlayerHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }
                    html, body { width: 100%; height: 100%; overflow: hidden; }
                    #player { width: 100%; height: 100%; }
                </style>
                <script>
                    // Keep visibility active in background and lock screen
                    try {
                        Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                        Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                        document.addEventListener('visibilitychange', function(e) {
                            e.stopImmediatePropagation();
                        }, true);
                    } catch(e) {}
                </script>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var shouldBePlaying = true;
                    var player;
                    var progressInterval;
                    var audioCtx, eq60, eq230, eq910, eq3k, eq14k, bassBoostFilter, virtualizerFilter;

                    function initAudioDsp() {
                        try {
                            if (!audioCtx) {
                                var AudioContext = window.AudioContext || window.webkitAudioContext;
                                audioCtx = new AudioContext();
                            }
                            if (audioCtx.state === 'suspended') {
                                audioCtx.resume();
                            }

                            // Keep audio pipeline alive in background
                            var keepAliveOsc = audioCtx.createOscillator();
                            var keepAliveGain = audioCtx.createGain();
                            keepAliveGain.gain.value = 0.00001;
                            keepAliveOsc.connect(keepAliveGain);
                            keepAliveGain.connect(audioCtx.destination);
                            keepAliveOsc.start();

                            // 5-Band Biquad Filters
                            eq60 = audioCtx.createBiquadFilter();
                            eq60.type = 'lowshelf';
                            eq60.frequency.value = 60;
                            eq60.gain.value = 0;

                            eq230 = audioCtx.createBiquadFilter();
                            eq230.type = 'peaking';
                            eq230.frequency.value = 230;
                            eq230.Q.value = 1.0;
                            eq230.gain.value = 0;

                            eq910 = audioCtx.createBiquadFilter();
                            eq910.type = 'peaking';
                            eq910.frequency.value = 910;
                            eq910.Q.value = 1.0;
                            eq910.gain.value = 0;

                            eq3k = audioCtx.createBiquadFilter();
                            eq3k.type = 'peaking';
                            eq3k.frequency.value = 3600;
                            eq3k.Q.value = 1.0;
                            eq3k.gain.value = 0;

                            eq14k = audioCtx.createBiquadFilter();
                            eq14k.type = 'highshelf';
                            eq14k.frequency.value = 14000;
                            eq14k.gain.value = 0;

                            bassBoostFilter = audioCtx.createBiquadFilter();
                            bassBoostFilter.type = 'lowshelf';
                            bassBoostFilter.frequency.value = 90;
                            bassBoostFilter.gain.value = 0;

                            virtualizerFilter = audioCtx.createBiquadFilter();
                            virtualizerFilter.type = 'peaking';
                            virtualizerFilter.frequency.value = 2500;
                            virtualizerFilter.Q.value = 0.7;
                            virtualizerFilter.gain.value = 0;

                            eq60.connect(eq230);
                            eq230.connect(eq910);
                            eq910.connect(eq3k);
                            eq3k.connect(eq14k);
                            eq14k.connect(bassBoostFilter);
                            bassBoostFilter.connect(virtualizerFilter);
                            virtualizerFilter.connect(audioCtx.destination);
                        } catch(e) {}
                    }

                    window.resumeAudioDsp = function() {
                        if (audioCtx && audioCtx.state === 'suspended') {
                            audioCtx.resume();
                        }
                    };

                    window.applyEqualizer = function(g60, g230, g910, g3k, g14k, bassBoostOn, virtualizerOn) {
                        try {
                            if (!audioCtx) initAudioDsp();
                            if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();
                            if (eq60) eq60.gain.setValueAtTime(g60, audioCtx.currentTime);
                            if (eq230) eq230.gain.setValueAtTime(g230, audioCtx.currentTime);
                            if (eq910) eq910.gain.setValueAtTime(g910, audioCtx.currentTime);
                            if (eq3k) eq3k.gain.setValueAtTime(g3k, audioCtx.currentTime);
                            if (eq14k) eq14k.gain.setValueAtTime(g14k, audioCtx.currentTime);
                            if (bassBoostFilter) bassBoostFilter.gain.setValueAtTime(bassBoostOn ? 8.0 : 0.0, audioCtx.currentTime);
                            if (virtualizerFilter) virtualizerFilter.gain.setValueAtTime(virtualizerOn ? 4.5 : 0.0, audioCtx.currentTime);
                        } catch(e) {}
                    };

                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'controls': 0,
                                'rel': 0,
                                'modestbranding': 1,
                                'enablejsapi': 1,
                                'cc_load_policy': 1,
                                'cc_lang_pref': 'en',
                                'origin': 'https://www.youtube-nocookie.com'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        try {
                            initAudioDsp();
                            window.MusicBridge.onReady();
                            event.target.playVideo();
                            startProgressLoop();
                        } catch(e) {}
                    }

                    function onPlayerStateChange(event) {
                        try {
                            // Resume if interrupted by screen-off
                            if (event.data === 2 && shouldBePlaying) {
                                setTimeout(function() {
                                    if (shouldBePlaying && player && player.playVideo) {
                                        player.playVideo();
                                    }
                                }, 80);
                            }
                            window.MusicBridge.onPlayerStateChange(event.data);
                        } catch(e) {}
                    }

                    function startProgressLoop() {
                        if (progressInterval) clearInterval(progressInterval);
                        progressInterval = setInterval(function() {
                            try {
                                if (player && player.getCurrentTime && player.getDuration) {
                                    var cur = player.getCurrentTime() || 0;
                                    var dur = player.getDuration() || 0;
                                    window.MusicBridge.onProgress(cur, dur);
                                }
                            } catch(e) {}
                        }, 800);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}

@Composable
fun BackgroundMusicEngine(
    currentTrack: YouTubeVideo?,
    isPlaying: Boolean,
    playbackSpeed: Float,
    seekToMs: Long,
    displayMode: PlayerDisplayMode,
    isSheetOpen: Boolean,
    viewModel: VideoSearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        GlobalMusicPlayerHolder.getOrCreateWebView(
            context = context,
            onStateChange = { state ->
                // YT.PlayerState: -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (video cued)
                when (state) {
                    0 -> viewModel.onTrackEnded()
                    1 -> viewModel.setPlayerStateFromBridge(isPlaying = true, isBuffering = false)
                    2 -> viewModel.setPlayerStateFromBridge(isPlaying = false, isBuffering = false)
                    3 -> viewModel.setPlayerStateFromBridge(isPlaying = true, isBuffering = true)
                }
            },
            onTimeUpdate = { cur, dur ->
                viewModel.updatePlaybackProgress(cur, dur)
            },
            onReady = {
                viewModel.setPlayerStateFromBridge(isPlaying = true, isBuffering = false)
            }
        )
    }

    // Load track whenever currentTrack changes
    LaunchedEffect(currentTrack?.id) {
        if (currentTrack != null) {
            GlobalMusicPlayerHolder.loadTrack(webView, currentTrack.id)
        }
    }

    // React to play/pause
    LaunchedEffect(isPlaying) {
        if (currentTrack != null) {
            if (isPlaying) {
                GlobalMusicPlayerHolder.play(webView)
            } else {
                GlobalMusicPlayerHolder.pause(webView)
            }
        }
    }

    // React to playback speed
    LaunchedEffect(playbackSpeed) {
        GlobalMusicPlayerHolder.setPlaybackRate(webView, playbackSpeed)
    }

    // React to seek
    LaunchedEffect(seekToMs) {
        if (seekToMs >= 0) {
            GlobalMusicPlayerHolder.seekTo(webView, seekToMs / 1000f)
        }
    }

    // React to Equalizer updates in real time
    val uiState = viewModel.uiState.value
    LaunchedEffect(
        uiState.band60Hz,
        uiState.band230Hz,
        uiState.band910Hz,
        uiState.band3600Hz,
        uiState.band14000Hz,
        uiState.isBassBoost,
        uiState.isVirtualizer
    ) {
        GlobalMusicPlayerHolder.applyEqualizer(
            webView = webView,
            band60 = uiState.band60Hz,
            band230 = uiState.band230Hz,
            band910 = uiState.band910Hz,
            band3k = uiState.band3600Hz,
            band14k = uiState.band14000Hz,
            bassBoost = uiState.isBassBoost,
            virtualizer = uiState.isVirtualizer
        )
    }

    val isVideoVisible = isSheetOpen && displayMode == PlayerDisplayMode.VIDEO_STREAM

    if (isVideoVisible) {
        AndroidView(
            factory = {
                (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                webView
            },
            modifier = modifier
        )
    } else {
        // Keep in background view hierarchy (off-screen / 1dp) so audio never halts
        Box(
            modifier = Modifier
                .size(1.dp)
                .background(Color.Transparent)
        ) {
            AndroidView(
                factory = {
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
