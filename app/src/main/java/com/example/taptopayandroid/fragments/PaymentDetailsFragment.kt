package com.example.taptopayandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.taptopayandroid.MainActivity
import com.example.taptopayandroid.R
import com.example.taptopayandroid.databinding.FragmentPaymentDetailsBinding
import com.example.taptopayandroid.fragments.PaymentDetailsState.CreatePaymentState
import com.google.android.material.snackbar.Snackbar

class PaymentDetailsFragment : Fragment(R.layout.fragment_payment_details) {
    companion object {
        const val TAG = "com.example.taptopayandroid.fragments.PaymentDetailsFragment"
    }

    private var binding: FragmentPaymentDetailsBinding? = null

    private val viewModel by lazy {
        ViewModelProvider(this)[PaymentDetailsViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        super.onCreateView(inflater, container, savedInstanceState)?.also { view -> binding = FragmentPaymentDetailsBinding.bind(view) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.run {
            collectPaymentButton.setOnClickListener {
                viewModel.createPayment(priceInput.text.toString())
            }
        }

        observeViewModel()
        if ((requireActivity() as MainActivity).currentPi != null) {
            retrievePayment((requireActivity() as MainActivity).currentPi!!)
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner, ::renderState)
    }

    fun retrievePayment(pi: String) {
        viewModel.retrievePayment(pi)
    }

    private fun renderState(state: PaymentDetailsState) = when (state) {
        CreatePaymentState.CreatePaymentInitial -> {}
        CreatePaymentState.CreatePaymentLoading -> renderLoading()
        is CreatePaymentState.CreatePaymentSuccess -> {}
        is CreatePaymentState.CreatePaymentError -> renderError(state.exception)

        is PaymentDetailsState.RetrievePaymentState.RetrievePaymentError -> renderError(state.exception)
        PaymentDetailsState.RetrievePaymentState.RetrievePaymentLoading -> {}
        PaymentDetailsState.RetrievePaymentState.RetrievePaymentSuccess -> {}

        is PaymentDetailsState.CollectPaymentState.CollectPaymentError -> renderError(state.exception)
        PaymentDetailsState.CollectPaymentState.CollectPaymentLoading -> {}
        PaymentDetailsState.CollectPaymentState.CollectPaymentSuccess -> {}

        is PaymentDetailsState.ConfirmPaymentState.ConfirmPaymentError -> renderError(state.exception)
        PaymentDetailsState.ConfirmPaymentState.ConfirmPaymentLoading -> {}
        PaymentDetailsState.ConfirmPaymentState.ConfirmPaymentSuccess -> renderConfirmSuccess()
    }

    private fun renderLoading() = binding?.run {
        collectPaymentButton.isEnabled = false
        collectPaymentLoader.visibility = View.VISIBLE
    }

    private fun renderError(exception: Exception) = binding?.run {
        collectPaymentButton.isEnabled = true
        collectPaymentLoader.visibility = View.GONE

        view?.let {
            val snackbar = Snackbar.make(it, "Error ${exception.message}", Snackbar.LENGTH_LONG).apply {
                setAction("Try again", object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        viewModel.createPayment(priceInput.text.toString())
                    }
                })
            }
            snackbar.show()
        }
    }

    private fun renderConfirmSuccess() = binding?.run {
        collectPaymentButton.isEnabled = true
        collectPaymentLoader.visibility = View.GONE

        view?.let {
            Snackbar.make(it, "Payment Success", 5000).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}