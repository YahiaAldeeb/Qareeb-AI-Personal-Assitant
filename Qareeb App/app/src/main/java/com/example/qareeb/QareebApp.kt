package com.example.qareeb

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.qareeb.security.AppLifecycleObserver
// That's what makes AppLifecycleObserver start listening for background/foreground transitions as soon as the app process begins
// — independent of which Activity or screen is currently showing.
class QareebApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }
}