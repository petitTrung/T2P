package com.example.taptopayandroid

import com.example.taptopayandroid.ConnectionToken
import com.example.taptopayandroid.PaymentIntentCreationResponse
import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path


data class StripeCardSchema(
    @JsonProperty("payment_method_id") val paymentMethodId: String
)

data class CardPspInfoSchema(
    @JsonProperty("provider") val provider: String,
    @JsonProperty("psp_details") val pspDetails: StripeCardSchema
)

data class SaveCardRequest(
    @JsonProperty("card") val card: CardPspInfoSchema
)



/**
 * The `BackendService` interface handles the two simple calls we need to make to our backend.
 */
interface BackendService {

    /**
     * Get a connection token string from the backend
     */
    @POST("v1/psp/stripe/terminal/connection-token")
    fun getConnectionToken(): Call<ConnectionToken>

    /**
     * Register payment method
     *
     * card {provider=stripe psp_details={payment_method_id=xxx}}
     */
    @POST("/v1/checkout/payments/{payment_external_id}/payment_methods")
    fun saveCard(@Path("payment_external_id") paymentId: String, @Body saveCardRequest: SaveCardRequest): Call<Void>

    @POST("v1/psp/stripe/terminal/confirm-payment-intent/{payment_intent_id}")
    fun confirmPaymentIntent(@Path("payment_intent_id") id: String): Call<Void>
}
