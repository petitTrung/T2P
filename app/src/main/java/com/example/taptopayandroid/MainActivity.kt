package com.example.taptopayandroid

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.taptopayandroid.fragments.ConnectReaderFragment
import com.example.taptopayandroid.fragments.PaymentDetailsFragment
import timber.log.Timber

var SKIP_TIPPING: Boolean = true

// Retrieved from:
// http :1337/v1/payments/payment_xxx/psp-session psp=stripe selected_payment_method=card_present
var PAYMENT_INTENT_SECRET: String = "pi_..."


class MainActivity : AppCompatActivity(), NavigationListener {

    var currentPi: String? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateTo(ConnectReaderFragment.TAG, ConnectReaderFragment(), false)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.i("onNewIntent----")
        retrievePaymentIntent(intent)
    }

    override fun retrievePaymentIntent(intent: Intent?) {
        val paymentIntentSecret = intent?.data?.getQueryParameter("client_secret")
        val paymentId = intent?.data?.getQueryParameter("payment_id")

        Timber.i("intent", intent?.data)
        Timber.i("paymentIntentSecret", paymentIntentSecret)
        Timber.i("paymentId", paymentId)

        paymentIntentSecret?.let {
            retrievePaymentIntent(paymentIntentSecret)
        }
    }

    override fun retrievePaymentIntent(pi: String) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is PaymentDetailsFragment) {
            currentFragment.retrievePayment(pi)
        } else {
            currentPi = pi
        }
    }


    // Navigate to Fragment
    private fun navigateTo(
        tag: String,
        fragment: Fragment,
        replace: Boolean = true,
        addToBackStack: Boolean = false,
    ) {
        val frag = supportFragmentManager.findFragmentByTag(tag) ?: fragment
        supportFragmentManager
            .beginTransaction()
            .apply {
                if (replace) {
                    replace(R.id.container, frag, tag)
                } else {
                    add(R.id.container, frag, tag)
                }

                if (addToBackStack) {
                    addToBackStack(tag)
                }
            }
            .commitAllowingStateLoss()
    }


    override fun onNavigateToPaymentDetails() {
        // Navigate to the fragment that will show the payment details
        navigateTo(PaymentDetailsFragment.TAG, PaymentDetailsFragment(), true)
    }
}
