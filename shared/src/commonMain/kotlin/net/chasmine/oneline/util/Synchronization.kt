package net.chasmine.oneline.util

/**
 * プラットフォーム固有の同期処理を提供
 */
expect fun <R> synchronized(lock: Any, block: () -> R): R
