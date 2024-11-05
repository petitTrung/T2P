package com.example.taptopayandroid.data

data class CreatePspPayload(
    val pay_at_once: Boolean = false,
    val selected_payment_method: String = "card_present",
)

