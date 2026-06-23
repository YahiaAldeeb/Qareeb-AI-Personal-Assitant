package com.example.qareeb.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppLockManager {
    var isLocked = false
        private set

    // ✅ Track if app has been unlocked at least once
    var hasBeenUnlockedOnce = false
        private set

    fun unlock() {
        isLocked = false
        hasBeenUnlockedOnce = true
    }

    fun lock() {
        // ✅ Only lock if it was unlocked before — don't lock on first launch
        if (hasBeenUnlockedOnce) {
            isLocked = true
        }
    }
}

class AppLifecycleObserver : DefaultLifecycleObserver {

    private var lockJob: Job? = null

    override fun onStop(owner: LifecycleOwner) {
        lockJob = CoroutineScope(Dispatchers.Main).launch {
            delay(1000L)
            AppLockManager.lock()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        lockJob?.cancel()
    }
}