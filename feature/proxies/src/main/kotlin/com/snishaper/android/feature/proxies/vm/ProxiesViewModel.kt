package com.snishaper.android.feature.proxies.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.EchProfile
import com.snishaper.android.core.model.Nat64Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProxiesUiState(
    val echProfiles: List<EchProfile> = emptyList(),
    val nat64Profiles: List<Nat64Profile> = emptyList(),
    val migrationServer: String = "",
    val migrationInput: String = "",
    /** NAT64 前缀测试结果（档案 id → 延迟毫秒描述，失败为错误文本）。 */
    val nat64TestResults: Map<String, String> = emptyMap(),
    val migrationMessage: String? = null,
    val migrationOk: Boolean? = null,
    val savingMigration: Boolean = false,
    val loaded: Boolean = false,
)

/** 代理页：迁移服务器 + ECH 档案 + NAT64 档案（与旧版 Proxies 页一致）。 */
class ProxiesViewModel : ViewModel() {

    private val echRepo = DataGraph.echRepository
    private val nat64Repo = DataGraph.nat64Repository
    private val migrationRepo = DataGraph.migrationRepository

    private val _uiState = MutableStateFlow(ProxiesUiState())
    val uiState: StateFlow<ProxiesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val ech = echRepo.profiles()
            val nat64 = nat64Repo.profiles()
            val server = migrationRepo.server()
            _uiState.update {
                it.copy(
                    echProfiles = (ech as? AppResult.Success)?.value ?: it.echProfiles,
                    nat64Profiles = (nat64 as? AppResult.Success)?.value ?: it.nat64Profiles,
                    migrationServer = (server as? AppResult.Success)?.value ?: it.migrationServer,
                    migrationInput = (server as? AppResult.Success)?.value ?: it.migrationInput,
                    loaded = true,
                )
            }
        }
    }

    // ---- 迁移服务器 ----

    fun migrationInputChanged(value: String) {
        _uiState.update { it.copy(migrationInput = value) }
    }

    fun saveMigration() {
        viewModelScope.launch {
            _uiState.update { it.copy(savingMigration = true) }
            val result = migrationRepo.setServer(_uiState.value.migrationInput.trim())
            _uiState.update {
                it.copy(
                    savingMigration = false,
                    migrationServer = if (result is AppResult.Success) _uiState.value.migrationInput.trim() else it.migrationServer,
                    migrationOk = result is AppResult.Success,
                    migrationMessage = if (result is AppResult.Failure) result.message else null,
                )
            }
        }
    }

    fun testMigration() {
        val server = _uiState.value.migrationInput.trim().ifBlank { _uiState.value.migrationServer }
        if (server.isBlank()) {
            _uiState.update { it.copy(migrationOk = false, migrationMessage = "server required") }
            return
        }
        viewModelScope.launch {
            when (val result = migrationRepo.test(server)) {
                is AppResult.Success -> _uiState.update { it.copy(migrationOk = true, migrationMessage = result.value) }
                is AppResult.Failure -> _uiState.update { it.copy(migrationOk = false, migrationMessage = result.message) }
            }
        }
    }

    fun consumeMigrationMessage() {
        _uiState.update { it.copy(migrationMessage = null) }
    }

    // ---- ECH 档案 ----

    fun saveEch(profile: EchProfile, onDone: () -> Unit) {
        viewModelScope.launch {
            echRepo.upsert(profile)
            refresh()
            onDone()
        }
    }

    fun deleteEch(id: String) {
        viewModelScope.launch {
            echRepo.delete(id)
            refresh()
        }
    }

    // ---- NAT64 档案 ----

    fun saveNat64(profile: Nat64Profile, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (isNew) nat64Repo.add(profile) else nat64Repo.update(profile)
            refresh()
            onDone()
        }
    }

    fun deleteNat64(id: String) {
        viewModelScope.launch {
            nat64Repo.delete(id)
            refresh()
        }
    }

    fun testNat64(profile: Nat64Profile) {
        viewModelScope.launch {
            when (val result = nat64Repo.test(profile.prefix)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(nat64TestResults = it.nat64TestResults + (profile.id to result.value))
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(nat64TestResults = it.nat64TestResults + (profile.id to result.message))
                }
            }
        }
    }
}
