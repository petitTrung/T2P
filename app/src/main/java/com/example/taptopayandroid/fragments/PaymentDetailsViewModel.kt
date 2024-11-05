package com.example.taptopayandroid.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taptopayandroid.ApiClient
import com.example.taptopayandroid.SKIP_TIPPING
import com.example.taptopayandroid.fragments.PaymentDetailsState.CollectPaymentState
import com.example.taptopayandroid.fragments.PaymentDetailsState.ConfirmPaymentState
import com.example.taptopayandroid.fragments.PaymentDetailsState.CreatePaymentState
import com.example.taptopayandroid.fragments.PaymentDetailsState.RetrievePaymentState
import com.example.taptopayandroid.utils.launch
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.CollectConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

sealed interface PaymentDetailsState {
    sealed interface CreatePaymentState : PaymentDetailsState {
        data object CreatePaymentInitial : CreatePaymentState
        data object CreatePaymentLoading : CreatePaymentState
        data class CreatePaymentSuccess(val pi: String) : CreatePaymentState
        data class CreatePaymentError(val exception: Exception) : CreatePaymentState
    }

    sealed interface RetrievePaymentState : PaymentDetailsState {
        data object RetrievePaymentLoading : RetrievePaymentState
        data object RetrievePaymentSuccess : RetrievePaymentState
        data class RetrievePaymentError(val exception: Exception) : RetrievePaymentState
    }

    sealed interface CollectPaymentState : PaymentDetailsState {
        data object CollectPaymentLoading : CollectPaymentState
        data object CollectPaymentSuccess : CollectPaymentState
        data class CollectPaymentError(val exception: Exception) : CollectPaymentState
    }

    sealed interface ConfirmPaymentState : PaymentDetailsState {
        data object ConfirmPaymentLoading : ConfirmPaymentState
        data object ConfirmPaymentSuccess : ConfirmPaymentState
        data class ConfirmPaymentError(val exception: Exception) : ConfirmPaymentState
    }
}

class PaymentDetailsViewModel : ViewModel() {
    // Backing property to avoid state updates from other classes
    private val _state = MutableLiveData<PaymentDetailsState>(CreatePaymentState.CreatePaymentInitial)
    val state: LiveData<PaymentDetailsState> = _state

    fun createPayment(amount: String) = launch {
        _state.postValue(CreatePaymentState.CreatePaymentLoading)
        withContext(Dispatchers.IO) {
            try {
                val pi = ApiClient.createPayment(amount.toLong() * 100)
                _state.postValue(CreatePaymentState.CreatePaymentSuccess(pi))
                retrievePayment(pi)
            } catch (e: Exception) {
                _state.postValue(CreatePaymentState.CreatePaymentError(e))
            }
        }
    }

    fun retrievePayment(pi: String) = launch {
        _state.postValue(RetrievePaymentState.RetrievePaymentLoading)
        Terminal.getInstance().retrievePaymentIntent(
            pi,
            object : PaymentIntentCallback {
                override fun onFailure(e: TerminalException) {
                    _state.postValue(RetrievePaymentState.RetrievePaymentError(e))
                }

                override fun onSuccess(paymentIntent: PaymentIntent) {
                    _state.postValue(RetrievePaymentState.RetrievePaymentSuccess)
                    collectPayment(paymentIntent)
                }
            }
        )
    }

    private fun collectPayment(paymentIntent: PaymentIntent) = launch {
        _state.postValue(CollectPaymentState.CollectPaymentLoading)

        val skipTipping = SKIP_TIPPING

        val collectConfig = CollectConfiguration.Builder()
            .skipTipping(skipTipping)
            .updatePaymentIntent(true)
            .build()

        Timber.i("collectPaymentMethod")
        Terminal.getInstance().collectPaymentMethod(
            paymentIntent, object : PaymentIntentCallback {
                override fun onFailure(e: TerminalException) {
                    _state.postValue(CollectPaymentState.CollectPaymentError(e))
                }

                override fun onSuccess(paymentIntent: PaymentIntent) {
                    _state.postValue(CollectPaymentState.CollectPaymentSuccess)
                    confirmPayment(paymentIntent.id)
                }
            }, collectConfig
        )
    }

    private fun confirmPayment(pi: String?) = launch {
        _state.postValue(ConfirmPaymentState.ConfirmPaymentLoading)
        withContext(Dispatchers.IO) {
            try {
                if (pi != null) {
                    ApiClient.confirmPaymentIntent(pi)
                    _state.postValue(ConfirmPaymentState.ConfirmPaymentSuccess)
                }
            } catch (e: Exception) {
                _state.postValue(ConfirmPaymentState.ConfirmPaymentError(e))
            }
        }
    }
}