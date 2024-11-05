package com.example.taptopayandroid

import android.content.Intent

/**
 * An `Activity` that should be notified when various navigation activities have been triggered
 */
interface NavigationListener {
    fun retrievePaymentIntent(intent: Intent?)

    fun retrievePaymentIntent(pi: String)

    fun onNavigateToPaymentDetails()
}
