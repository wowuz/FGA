package io.github.fate_grand_automata.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.fate_grand_automata.scripts.prefs.IPreferences

@Composable
fun translateLauncher(
    prefs: IPreferences, // Pass prefs if needed for config
    modifier: Modifier = Modifier
): ScriptLauncherResponseBuilder {
    // TODO: Add UI elements here to configure the AutoTranslate script
    // - Target Language selection (Dropdown/ListPreference)
    // - OCR Region configuration (Could be complex: maybe predefined options or a visual editor?)
    // - Enable/Disable switch?
    // For now, just show a placeholder text.

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "Auto Translate Settings", // Replace with localized string
            style = MaterialTheme.typography.titleLarge
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Configure OCR region and target language in preferences (Not implemented yet).") // Placeholder
        // Add actual settings UI here using prefs
    }

    // This builder currently doesn't take any config, update as needed
    return ScriptLauncherResponseBuilder(
        canBuild = { true }, // Always enabled for now
        build = { ScriptLauncherResponse.Translate } // New response type needed
    )
}