package net.chasmine.oneline.util

actual fun <R> synchronized(lock: Any, block: () -> R): R {
    return kotlin.synchronized(lock, block)
}
