package com.snishaper.android.core.common

/**
 * 仓库层统一返回封装：引擎调用失败不抛到 UI 层，由 ViewModel 决定如何呈现。
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>

    companion object {
        inline fun <T> of(block: () -> T): AppResult<T> =
            try {
                Success(block())
            } catch (t: Throwable) {
                Failure(t.message ?: "unknown error", t)
            }
    }
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}
