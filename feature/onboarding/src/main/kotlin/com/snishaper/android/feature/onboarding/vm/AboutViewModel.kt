package com.snishaper.android.feature.onboarding.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.BuildInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AboutUiState(
    val version: String = "",
    val buildInfo: BuildInfo? = null,
)

/** 关于页：应用版本 + 引擎构建信息（对应旧桥接契约 GetAppVersion / GetBuildInfo）。 */
class AboutViewModel : ViewModel() {

    companion object {
        const val URL_WEBSITE = "https://jetcpp.ccwu.cc"
        const val URL_GITHUB = "https://github.com/SnishaperTeam/Snishaper-Droid"
        const val URL_RELEASES = "https://github.com/SnishaperTeam/Snishaper-Droid/releases"
    }

    private val appInfoRepo = DataGraph.appInfoRepository

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = appInfoRepo.buildInfo()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(version = r.value.version, buildInfo = r.value)
                }
                is AppResult.Failure -> Unit
            }
        }
    }
}
