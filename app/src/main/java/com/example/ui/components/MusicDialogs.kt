package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EqualizerPreset
import com.example.data.model.YouTubeVideo
import com.example.ui.theme.ActivePillBg
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.LightBorderGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurfaceGray
import com.example.ui.viewmodel.MusicPlayerUiState
import com.example.ui.viewmodel.VideoSearchViewModel

/**
 * Screen 16: Sleep Timer Dialog
 */
@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val timerOptions = listOf(
        Pair("Off", null),
        Pair("15 minutes", 15),
        Pair("30 minutes", 30),
        Pair("45 minutes", 45),
        Pair("1 hour", 60),
        Pair("End of track", 5)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep Timer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                timerOptions.forEach { (label, minutes) ->
                    val isSelected = currentMinutes == minutes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMinutes(minutes) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = ObsidianBlack
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMinutes(minutes) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ObsidianBlack,
                                unselectedColor = MediumGray
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 17: Equalizer Dialog matching designv2-example.png
 */
@Composable
fun EqualizerDialog(
    currentPreset: EqualizerPreset,
    band60: Float,
    band230: Float,
    band910: Float,
    band3600: Float,
    band14000: Float,
    isBassBoost: Boolean,
    isVirtualizer: Boolean,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onToggleBassBoost: () -> Unit,
    onToggleVirtualizer: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Equalizer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EqualizerPreset.values()) { preset ->
                        val isSel = currentPreset == preset
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) ObsidianBlack else SoftSurfaceGray,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectPreset(preset) }
                        ) {
                            Text(
                                text = preset.label,
                                color = if (isSel) PureWhite else ObsidianBlack,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5-Band Graphic Sliders
                Text("Frequency Bands", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MediumGray)
                Spacer(modifier = Modifier.height(8.dp))

                FrequencyBandRow(label = "60 Hz (Sub Bass)", value = band60, onValueChange = { onBandChange(0, it) })
                FrequencyBandRow(label = "230 Hz (Bass)", value = band230, onValueChange = { onBandChange(1, it) })
                FrequencyBandRow(label = "910 Hz (Midrange)", value = band910, onValueChange = { onBandChange(2, it) })
                FrequencyBandRow(label = "3.6 kHz (Presence)", value = band3600, onValueChange = { onBandChange(3, it) })
                FrequencyBandRow(label = "14 kHz (Brilliance)", value = band14000, onValueChange = { onBandChange(4, it) })

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = LightBorderGray, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Bass Boost & Virtualizer Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bass Boost", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                    Switch(
                        checked = isBassBoost,
                        onCheckedChange = { onToggleBassBoost() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = ObsidianBlack
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("3D Surround Virtualizer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                    Switch(
                        checked = isVirtualizer,
                        onCheckedChange = { onToggleVirtualizer() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = ObsidianBlack
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FrequencyBandRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = ObsidianBlack,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(110.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = ObsidianBlack,
                activeTrackColor = ObsidianBlack,
                inactiveTrackColor = LightBorderGray
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Screen 18: Playback Settings Dialog matching designv2-example.png
 */
@Composable
fun PlaybackSettingsDialog(
    crossfadeSeconds: Int,
    isGapless: Boolean,
    isAutoplay: Boolean,
    isNormalize: Boolean,
    isPlayInOrder: Boolean,
    onSetCrossfade: (Int) -> Unit,
    onToggleGapless: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleNormalize: () -> Unit,
    onTogglePlayInOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Crossfade
                Text(
                    text = "Crossfade (${crossfadeSeconds}s)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianBlack
                )
                Slider(
                    value = crossfadeSeconds.toFloat(),
                    onValueChange = { onSetCrossfade(it.toInt()) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = ObsidianBlack,
                        activeTrackColor = ObsidianBlack,
                        inactiveTrackColor = LightBorderGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Gapless Playback", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                        Text("Seamless transition between tracks", fontSize = 11.sp, color = MediumGray)
                    }
                    Switch(
                        checked = isGapless,
                        onCheckedChange = { onToggleGapless() },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = ObsidianBlack)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Autoplay Similar Songs", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                        Text("Keep playing when your queue ends", fontSize = 11.sp, color = MediumGray)
                    }
                    Switch(
                        checked = isAutoplay,
                        onCheckedChange = { onToggleAutoplay() },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = ObsidianBlack)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Normalize Volume", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                        Text("Set same volume level for all tracks", fontSize = 11.sp, color = MediumGray)
                    }
                    Switch(
                        checked = isNormalize,
                        onCheckedChange = { onToggleNormalize() },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = ObsidianBlack)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Play in Order", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                        Text("Keep default track sequence", fontSize = 11.sp, color = MediumGray)
                    }
                    Switch(
                        checked = isPlayInOrder,
                        onCheckedChange = { onTogglePlayInOrder() },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = ObsidianBlack)
                    )
                }
            }
        }
    }
}

/**
 * Screen 19: Audio Quality Dialog
 */
@Composable
fun AudioQualityDialog(
    streamingQuality: String,
    downloadQuality: String,
    onSetStreamingQuality: (String) -> Unit,
    onSetDownloadQuality: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val streamingOptions = listOf("Auto", "High (320 kbps)", "Medium (192 kbps)", "Low (128 kbps)")
    val downloadOptions = listOf("High (320 kbps)", "Medium (192 kbps)", "Low (128 kbps)")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Quality",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Streaming Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MediumGray)
                Spacer(modifier = Modifier.height(6.dp))
                streamingOptions.forEach { opt ->
                    val isSel = streamingQuality == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSetStreamingQuality(opt) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = opt, fontSize = 14.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = ObsidianBlack)
                        RadioButton(
                            selected = isSel,
                            onClick = { onSetStreamingQuality(opt) },
                            colors = RadioButtonDefaults.colors(selectedColor = ObsidianBlack, unselectedColor = MediumGray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = LightBorderGray, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Download Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MediumGray)
                Spacer(modifier = Modifier.height(6.dp))
                downloadOptions.forEach { opt ->
                    val isSel = downloadQuality == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSetDownloadQuality(opt) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = opt, fontSize = 14.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = ObsidianBlack)
                        RadioButton(
                            selected = isSel,
                            onClick = { onSetDownloadQuality(opt) },
                            colors = RadioButtonDefaults.colors(selectedColor = ObsidianBlack, unselectedColor = MediumGray)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 20: Notifications Activity Dialog
 */
@Composable
fun NotificationsDialog(
    onDismiss: () -> Unit
) {
    val notificationList = listOf(
        Pair("New Release: Billie Eilish", "New single is now streaming in Lossless HiFi • 2h ago"),
        Pair("Playlist Updated", "\"Chill Vibes\" was updated with 4 new tracks • 1d ago"),
        Pair("Followed Artist", "Luna Vale released a new album \"Fragments\" • 2d ago"),
        Pair("Download Complete", "34 offline tracks synchronized successfully • 3d ago"),
        Pair("Weekly Music Recap", "You listened to 18 hours of music this week • 5d ago")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                notificationList.forEach { (title, desc) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
                        Text(text = desc, fontSize = 12.sp, color = MediumGray)
                    }
                    HorizontalDivider(color = LightBorderGray.copy(alpha = 0.5f), thickness = 0.8.dp)
                }
            }
        }
    }
}

/**
 * Screen 22: Appearance Dialog
 */
@Composable
fun AppearanceDialog(
    currentTheme: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf("Light", "Dark", "System Default")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Appearance Theme",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                themes.forEach { themeName ->
                    val isSel = currentTheme.equals(themeName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTheme(themeName) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = themeName,
                            fontSize = 15.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = ObsidianBlack
                        )
                        RadioButton(
                            selected = isSel,
                            onClick = { onSelectTheme(themeName) },
                            colors = RadioButtonDefaults.colors(selectedColor = ObsidianBlack, unselectedColor = MediumGray)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Playback Speed Dialog
 */
@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback Speed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                speeds.forEach { sp ->
                    val isSelected = currentSpeed == sp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectSpeed(sp) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${sp}x",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = ObsidianBlack
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectSpeed(sp) },
                            colors = RadioButtonDefaults.colors(selectedColor = ObsidianBlack, unselectedColor = MediumGray)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Create Playlist Dialog
 */
@Composable
fun CreatePlaylistDialog(
    onCreatePlaylist: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "New Playlist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ObsidianBlack,
                        focusedLabelColor = ObsidianBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ObsidianBlack,
                        focusedLabelColor = ObsidianBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MediumGray, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreatePlaylist(title, description)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianBlack,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

/**
 * Add Track to Playlist Dialog
 */
@Composable
fun AddToPlaylistDialog(
    track: YouTubeVideo,
    playlists: List<com.example.data.model.Playlist>,
    onSelectPlaylist: (String) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add to Playlist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
                Text(
                    text = track.title,
                    fontSize = 12.sp,
                    color = MediumGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCreateNew() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoftSurfaceGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ObsidianBlack)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("New Playlist", fontWeight = FontWeight.Bold, color = ObsidianBlack)
                }

                HorizontalDivider(color = LightBorderGray, thickness = 0.8.dp)

                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPlaylist(pl.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftSurfaceGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(pl.title, fontWeight = FontWeight.SemiBold, color = ObsidianBlack)
                            Text("${pl.videoIds.size} tracks", fontSize = 11.sp, color = MediumGray)
                        }
                    }
                }
            }
        }
    }
}
