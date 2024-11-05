package com.example.taptopayandroid.data

data class CreatePaymentPayload(
    val payment: PaymentPayload,
)

data class PaymentPayload(
    val purchase_amount: Long,
    val installments_count: Int?,
)
