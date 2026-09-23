package com.example.data.model

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class PlayerDisplayMode {
    AUDIO_ARTWORK,
    VIDEO_STREAM
}

enum class EqualizerPreset(val label: String, val description: String) {
    NORMAL("Normal", "Balanced, studio-grade sound reproduction"),
    BASS_BOOST("Bass Boost", "Deep enhanced low-end frequencies for punchy beats"),
    VOCAL_BOOST("Vocal Boost", "Crisp midrange clarity for vocals and podcasts"),
    ACOUSTIC("Acoustic", "Warm acoustic tone with open acoustic treble"),
    EDM_CLUB("EDM & Club", "High dynamic range with elevated bass and highs"),
    ROCK("Rock", "Aggressive guitars with punchy drums and bass"),
    LOFI_CHILL("Lo-Fi Chill", "Smooth vintage tape warmth with soft roll-off")
}

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val videoIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
