package com.example.qareeb.security
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object AppLockManager {

    // Becomes true whenever app goes to background
    var isLocked = true
        private set

    fun unlock() {
        isLocked = false
    }

    fun lock() {
        isLocked = true
    }
}

class AppLifecycleObserver : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        // App went to background → lock it
        AppLockManager.lock()
    }
}