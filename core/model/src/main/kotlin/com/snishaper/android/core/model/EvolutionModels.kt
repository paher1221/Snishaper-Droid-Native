package com.snishaper.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 演进模式（M5）：对齐 Go 侧 evolution 包的 TestTask / DomainTestResult /
 * StepResult / TempRule（snake_case，见 snishaper-core/evolution/result.go）。
 * delay 为 Go time.Duration（纳秒），展示时换算毫秒。
 */

@Serializable
data class EvolutionStep(
    @SerialName("step_name") val stepName: String = "",
    @SerialName("success") val success: Boolean = false,
    @SerialName("delay") val delay: Long = 0,
    @SerialName("error") val error: String? = null,
    @SerialName("timestamp") val timestamp: String = "",
)

@Serializable
data class EvolutionTempRule(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("domain") val domain: String = "",
    @SerialName("mode") val mode: String = "",
    @SerialName("sni_fake") val sniFake: String? = null,
    @SerialName("ech_enabled") val echEnabled: Boolean = false,
    @SerialName("method") val method: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("is_applied") val isApplied: Boolean = false,
)

@Serializable
data class EvolutionResult(
    @SerialName("domain") val domain: String = "",
    @SerialName("reachable") val reachable: Boolean = false,
    @SerialName("method") val method: String? = null,
    @SerialName("resolved_ips") val resolvedIps: List<String> = emptyList(),
    @SerialName("best_ip") val bestIp: String? = null,
    @SerialName("is_cloudflare") val isCloudflare: Boolean = false,
    @SerialName("delay") val delay: Long = 0,
    @SerialName("error") val error: String? = null,
    @SerialName("timestamp") val timestamp: String = "",
    @SerialName("step_results") val stepResults: List<EvolutionStep> = emptyList(),
    @SerialName("generated_rule") val generatedRule: EvolutionTempRule? = null,
) {
    /** 纳秒 → 毫秒（旧前端 Math.round(delay / 1e6) 同口径）。 */
    val delayMs: Long get() = delay / 1_000_000
}

@Serializable
data class EvolutionTask(
    @SerialName("id") val id: String = "",
    @SerialName("domains") val domains: List<String> = emptyList(),
    @SerialName("status") val status: String = "",
    @SerialName("results") val results: List<EvolutionResult> = emptyList(),
    @SerialName("progress") val progress: Int = 0,
    @SerialName("total") val total: Int = 0,
) {
    val isRunning: Boolean get() = status == "running" || status == "pending"
    val tempRules: List<EvolutionTempRule>
        get() = results.mapNotNull { it.generatedRule }

    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): EvolutionTask? =
            if (json.isBlank()) null
            else runCatching { parser.decodeFromString<EvolutionTask>(json) }.getOrNull()
    }
}
