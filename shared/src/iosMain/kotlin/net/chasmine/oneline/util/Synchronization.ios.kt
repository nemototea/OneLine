package net.chasmine.oneline.util

import platform.Foundation.NSLock

private val lockMap = mutableMapOf<Any, NSLock>()
private val mapLock = NSLock()

actual fun <R> synchronized(lock: Any, block: () -> R): R {
    val nsLock = mapLock.let {
        it.lock()
        val result = lockMap.getOrPut(lock) { NSLock() }
        it.unlock()
        result
    }

    nsLock.lock()
    try {
        return block()
    } finally {
        nsLock.unlock()
    }
}
