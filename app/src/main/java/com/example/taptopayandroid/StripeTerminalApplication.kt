package com.example.taptopayandroid

import android.app.Application
import android.util.Log
import com.stripe.stripeterminal.TerminalApplicationDelegate
import timber.log.Timber

class StripeTerminalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TerminalApplicationDelegate.onCreate(this)
        initLogger()
    }

    private fun initLogger() = if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    } else {
        Timber.plant(ReleaseTree())
    }
}

class ReleaseTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        // TODO Send log to a crash report manager (as Firebase Crashlytics)
    }
}