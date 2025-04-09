package io.github.fate_grand_automata.ui.more

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.prefs.core.PrefsCore
import io.github.fate_grand_automata.util.StorageProvider
import io.github.fate_grand_automata.util.SupportImageExtractor
import io.github.fate_grand_automata.util.DisplayHelper
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MoreOptionsViewModel @Inject constructor(
    val storageProvider: StorageProvider,
    val prefsCore: PrefsCore,
    displayHelper: DisplayHelper
) : ViewModel() {
    val storageSummary: MutableState<String?> = mutableStateOf(null)
    val extractSummary: MutableState<String> = mutableStateOf("")

    // Expose screen resolution as state
    val screenResolution: MutableState<String> = mutableStateOf("")

    init {
        storageSummary.value = storageProvider.rootDirName
        // Set screen resolution string (consider landscape orientation)
        val metrics = displayHelper.metrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        screenResolution.value = "Screen: ${kotlin.math.max(w,h)}x${kotlin.math.min(w,h)} px" // Show as Landscape WxH
    }

    fun performSupportImageExtraction(context: Context) = viewModelScope.launch {
        // TODO: Translate
        extractSummary.value = "Extracting ..."

        val msg = try {
            SupportImageExtractor(context, storageProvider).extract()

            context.getString(R.string.support_imgs_extracted)
        } catch (e: Exception) {
            context.getString(R.string.support_imgs_extract_failed).also { msg ->
                Timber.e(e, msg)
            }
        }

        extractSummary.value = msg
    }

    fun pickedDirectory(uri: Uri?) {
        if (uri != null) {
            storageProvider.setRoot(uri)

            storageSummary.value = storageProvider.rootDirName
        }
    }
}