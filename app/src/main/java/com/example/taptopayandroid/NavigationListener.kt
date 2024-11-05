package com.example.taptopayandroid

/**
 * An `Activity` that should be notified when various navigation activities have been triggered
 */
interface NavigationListener {
    fun onCollectPayment(
        amount: Long,
        currency: String,
        skipTipping: Boolean,
        extendedAuth: Boolean,
        incrementalAuth: Boolean
    )

    fun onNavigateToPaymentDetails()

    fun onCancel()
}
