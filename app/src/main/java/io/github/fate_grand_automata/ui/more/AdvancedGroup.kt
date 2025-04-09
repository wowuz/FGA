package io.github.fate_grand_automata.ui.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.prefs.core.GameAreaMode
import io.github.fate_grand_automata.prefs.core.Pref
import io.github.fate_grand_automata.prefs.core.PrefsCore
import io.github.fate_grand_automata.root.SuperUser
import io.github.fate_grand_automata.ui.Stepper
import io.github.fate_grand_automata.ui.icon
import io.github.fate_grand_automata.ui.prefs.ListPreference
import io.github.fate_grand_automata.ui.prefs.Preference
import io.github.fate_grand_automata.ui.prefs.PreferenceGroupHeader
import io.github.fate_grand_automata.ui.prefs.SwitchPreference
import io.github.fate_grand_automata.ui.prefs.remember
import io.github.fate_grand_automata.ui.prefs.EditTextPreference
import io.github.fate_grand_automata.ui.prefs.StepperPreference

import io.github.fate_grand_automata.util.stringRes

fun LazyListScope.advancedGroup(
    prefs: PrefsCore,
    goToFineTune: () -> Unit,
    screenResolution: String
) {
    item {
        Preference(
            title = stringResource(R.string.p_fine_tune),
            icon = icon(R.drawable.ic_tune),
            onClick = goToFineTune
        )
    }

    item {
        prefs.debugMode.SwitchPreference(
            title = stringResource(R.string.p_debug_mode),
            summary = stringResource(R.string.p_debug_mode_summary),
            icon = icon(R.drawable.ic_bug)
        )
    }

    item {
        val rootForScreenshots by prefs.useRootForScreenshots.remember()

        val enabled = !rootForScreenshots &&
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
        if (!enabled) {
            prefs.recordScreen.resetToDefault()
        }
        prefs.recordScreen.SwitchPreference(
            title = stringResource(R.string.p_record_screen),
            summary = stringResource(R.string.p_record_screen_summary),
            icon = icon(R.drawable.ic_video),
            enabled = enabled
        )
    }

    item {
        RootForScreenshots(prefs.useRootForScreenshots)
    }

    item {
        prefs.autoStartService.SwitchPreference(
            title = stringResource(R.string.p_auto_start_service),
            icon = icon(R.drawable.ic_launch)
        )
    }

    item {
        Column {
            prefs.gameAreaMode.ListPreference(
                title = stringResource(R.string.p_game_area_mode),
                icon = icon(Icons.Default.Fullscreen),
                entries = GameAreaMode.entries
                    .associateWith { stringResource(it.stringRes) }
            )

            val gameAreaMode by prefs.gameAreaMode.remember()

            AnimatedVisibility(gameAreaMode == GameAreaMode.Custom) {
                Card(
                    modifier = Modifier.padding(5.dp),
                    elevation = cardElevation(5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.scale(0.9f)
                    ) {
                        prefs.gameOffsetLeft.customOffset(stringResource(R.string.p_game_area_custom_left))
                        prefs.gameOffsetRight.customOffset(stringResource(R.string.p_game_area_custom_right))
                        prefs.gameOffsetTop.customOffset(stringResource(R.string.p_game_area_custom_top))
                        prefs.gameOffsetBottom.customOffset(stringResource(R.string.p_game_area_custom_bottom))
                    }
                }
            }
        }
    }

    item {
        prefs.stageCounterNew.SwitchPreference(
            title = stringResource(R.string.p_thresholded_stage_counter),
            icon = icon(R.drawable.ic_counter)
        )
    }

    item { PreferenceGroupHeader(title = "Auto Translate") } // TODO: Localize

    item {
        prefs.autoTranslateApiKey.EditTextPreference(
            title = "Gemini API Key", // TODO: Localize
            validate = { it.isNotBlank() },
            icon = icon(R.drawable.ic_key),
            singleLine = true
        )
    }

    item {
        // Consider making this a ListPreference with common language codes
        prefs.autoTranslateTargetLanguage.EditTextPreference(
            title = "Target Language Code", // TODO: Localize
            icon = icon(Icons.Default.Translate),
            singleLine = true
        )
    }

    item {
        Preference(
            // *** FIX: Wrap title string in Text composable ***
            title = { Text("OCR Region") }, // TODO: Localize
            summary = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prefs.autoTranslateOcrRegionX.StepperPreference(title = "X", valueRange = 0..3000)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prefs.autoTranslateOcrRegionY.StepperPreference(title = "Y", valueRange = 0..2000)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prefs.autoTranslateOcrRegionWidth.StepperPreference(title = "Width", valueRange = 10..3000)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prefs.autoTranslateOcrRegionHeight.StepperPreference(title = "Height", valueRange = 10..2000)
                    }
                    Text(
                        screenResolution, // Display the resolution hint
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "Coordinates are based on FGA's internal 1440p landscape system.", // TODO: Localize
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            icon = icon(R.drawable.ic_screenshot)
        )
    }
}

// Extracted Game Area logic for clarity
@Composable
private fun GameAreaGroup(prefs: PrefsCore) {
    Column {
        prefs.gameAreaMode.ListPreference(
            title = stringResource(R.string.p_game_area_mode),
            icon = icon(Icons.Default.Fullscreen),
            entries = GameAreaMode.entries
                .associateWith { stringResource(it.stringRes) }
        )

        val gameAreaMode by prefs.gameAreaMode.remember()

        AnimatedVisibility(gameAreaMode == GameAreaMode.Custom) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                elevation = cardElevation(2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, // Slightly different background
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .scale(0.95f) // Slightly smaller scale
                ) {
                    prefs.gameOffsetLeft.customOffset(stringResource(R.string.p_game_area_custom_left))
                    prefs.gameOffsetRight.customOffset(stringResource(R.string.p_game_area_custom_right))
                    prefs.gameOffsetTop.customOffset(stringResource(R.string.p_game_area_custom_top))
                    prefs.gameOffsetBottom.customOffset(stringResource(R.string.p_game_area_custom_bottom))
                }
            }
        }
    }
}

@Composable
private fun Pref<Int>.customOffset(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        var value by remember()

        Text(text)
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueRange = 0..999
        )
    }
}

private fun hasRootAccess() = try {
    SuperUser().close()
    true
} catch (e: Exception) {
    false
}

@Composable
private fun RootForScreenshots(
    pref: Pref<Boolean>
) {
    var state by pref.remember()
    var enabled by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    val action: (Boolean) -> Unit = {
        error = false
        enabled = false
        try {
            when {
                !it -> state = false
                hasRootAccess() -> state = true
                else -> error = true
            }
        } finally {
            enabled = true
        }
    }

    Column {
        Preference(
            title = stringResource(R.string.p_root_screenshot),
            summary = stringResource(if (error) R.string.root_failed else R.string.p_root_screenshot_summary),
            icon = icon(R.drawable.ic_key),
            enabled = enabled,
            onClick = { action(!state) },
        ) {
            Switch(
                checked = state,
                onCheckedChange = action,
                enabled = enabled
            )
        }
    }
}