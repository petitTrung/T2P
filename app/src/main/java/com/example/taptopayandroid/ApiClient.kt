package com.example.taptopayandroid

import com.example.taptopayandroid.BuildConfig
import com.example.taptopayandroid.PaymentIntentCreationResponse
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.PaymentMethod
import okhttp3.OkHttpClient
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The `ApiClient` is a singleton object used to make calls to our backend and return their results
 */
object ApiClient {

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.EXAMPLE_BACKEND_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service: BackendService = retrofit.create(BackendService::class.java)

    @Throws(ConnectionTokenException::class)
    internal fun createConnectionToken(): String {
        try {
            val result = service.getConnectionToken().execute()
            if (result.isSuccessful && result.body() != null) {
                return result.body()!!.secret
            } else {
                throw ConnectionTokenException("Creating connection token failed")
            }
        } catch (e: IOException) {
            throw ConnectionTokenException("Creating connection token failed", e)
        }
    }

    internal fun confirmPaymentIntent(id: String) {
        val result = service.confirmPaymentIntent(id).execute()
        if (!result.isSuccessful) {
            throw Exception("something happened :-/")
        }
    }

    internal fun saveCard(paymentId: String, paymentMethodId: String) {
        // paymentId can be extracted from paymentIntent.metadata
        val req = SaveCardRequest(card = CardPspInfoSchema(
            provider = "stripe", pspDetails = StripeCardSchema(paymentMethodId)
        ))
        val result = service.saveCard(paymentId, req).execute()
        if (!result.isSuccessful) {
            throw Exception("something happened :-/")
        }
    }
}
