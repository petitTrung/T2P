package com.example.taptopayandroid

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.taptopayandroid.fragments.ConnectReaderFragment
import com.example.taptopayandroid.fragments.PaymentDetails
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.CollectConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.TerminalException
import timber.log.Timber

var SKIP_TIPPING: Boolean = true

// Retrieved from:
// http :1337/v1/payments/payment_xxx/psp-session psp=stripe selected_payment_method=card_present
var PAYMENT_INTENT_SECRET: String = "pi_..."


class MainActivity : AppCompatActivity(), NavigationListener {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateTo(ConnectReaderFragment.TAG, ConnectReaderFragment(), false)
    }

    private fun collectPayment(
        amount: Long,
        currency: String,
        skipTipping: Boolean,
        extendedAuth: Boolean,
        incrementalAuth: Boolean
    ) {
        Timber.i("collectPayment")
        SKIP_TIPPING = skipTipping

        val params = PaymentIntentParameters.Builder()
            .setAmount(amount)
            .setCurrency(currency)
            .build()
        Terminal.getInstance().createPaymentIntent(
            params,
            createPaymentIntentCallback
        )

//        Terminal.getInstance().retrievePaymentIntent(
//            PAYMENT_INTENT_SECRET,
//            createPaymentIntentCallback
//        )
    }

    private val createPaymentIntentCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                val skipTipping = SKIP_TIPPING

                val collectConfig = CollectConfiguration.Builder()
                    .skipTipping(skipTipping)
                    .updatePaymentIntent(true)
                    .build()

                Timber.i("collectPaymentMethod")
                Terminal.getInstance().collectPaymentMethod(
                    paymentIntent, collectPaymentMethodCallback, collectConfig
                )
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        }
    }

    private val collectPaymentMethodCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                Timber.i("scoring------")
                val pm = paymentIntent.paymentMethod
                val card = pm?.cardPresentDetails ?: pm?.interacPresentDetails
                // Placeholder for business logic on card before confirming paymentIntent
                Timber.i("pm : $pm")
                Timber.i("card : $card")


                Timber.i("processPayment")
                // 🚨AUTHENTICATION: I don't know when the PIN will be asked for,
                // so we may have to confirm in either the frontend (processPayment)
                // or the backend (ApiClient.confirmPaymentIntent)
                //Terminal.getInstance().processPayment(paymentIntent, processPaymentCallback)


                try {
                    if (paymentIntent.id != null) {
                        ApiClient.confirmPaymentIntent(paymentIntent.id!!)
                    }
                    //navigateTo(PaymentDetails.TAG, PaymentDetails(), true)
                } catch (e: Exception) {
                    Timber.i("processPayment failed")
                }
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        }
    }

    private val processPaymentCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                Timber.i("capturePaymentIntent")
                //ApiClient.capturePaymentIntent(paymentIntent.id)

                //TODO : Return to previous Screen
                navigateTo(PaymentDetails.TAG, PaymentDetails(), true)
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
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


    override fun onCollectPayment(
        amount: Long,
        currency: String,
        skipTipping: Boolean,
        extendedAuth: Boolean,
        incrementalAuth: Boolean
    ) {
        collectPayment(amount, currency, skipTipping, extendedAuth, incrementalAuth)
    }

    override fun onNavigateToPaymentDetails() {
        // Navigate to the fragment that will show the payment details
        navigateTo(PaymentDetails.TAG, PaymentDetails(), true)
    }

    override fun onCancel() {
        navigateTo(ConnectReaderFragment.TAG, ConnectReaderFragment(), true)
    }
}
