package com.example.taptopayandroid.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.taptopayandroid.NavigationListener
import com.example.taptopayandroid.R
import com.example.taptopayandroid.databinding.FragmentConnectReaderBinding
import com.example.taptopayandroid.fragments.ConnectReaderState.ConnectToReader
import com.example.taptopayandroid.fragments.ConnectReaderState.DiscoverReader
import com.example.taptopayandroid.fragments.ConnectReaderState.InitializeTerminal
import com.example.taptopayandroid.fragments.ConnectReaderState.RequestPermission
import com.example.taptopayandroid.fragments.ConnectReaderState.RequestTerminalToken
import com.google.android.material.snackbar.Snackbar
import com.stripe.stripeterminal.external.models.Reader

@SuppressLint("MissingPermission")
class ConnectReaderFragment : Fragment(R.layout.fragment_connect_reader) {
    companion object {
        const val TAG = "com.example.taptopayandroid.fragments.ConnectReaderFragment"
        const val locationId = "tml_FyBbEQsL13YDbe"
    }

    private var binding: FragmentConnectReaderBinding? = null
    private val viewModel by lazy {
        ViewModelProvider(this)[ConnectReaderViewModel::class.java]
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onPermissionResult,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        super.onCreateView(inflater, container, savedInstanceState)
            ?.also { view -> binding = FragmentConnectReaderBinding.bind(view) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let {
            viewModel.requestLocation(it)
        }
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner, ::renderState)
    }

    private fun renderState(state: ConnectReaderState) = when (state) {
        ConnectReaderState.InitialState -> {}
        is RequestPermission.RequestLocation -> requestLocationPermission(state.permissions)
        RequestPermission.RequestLocationDone -> requestLocationPermissionDone()
        is RequestPermission.RequestBluetooth -> requestBluetoothPermission(state.permissions)
        RequestPermission.RequestBluetoothDone -> requestBluetoothPermissionDone()
        is RequestPermission.RequestNFC -> requestNFC(state.shouldRequest)

        RequestTerminalToken.Loading -> renderTerminalTokenLoading()
        is RequestTerminalToken.Success -> renderTerminalTokenSuccess(state.token)
        is RequestTerminalToken.Error -> renderTerminalTokenError(state.exception)

        InitializeTerminal.Loading -> renderInitializeTerminalLoading()
        InitializeTerminal.Success -> renderInitializeTerminalSuccess()
        is InitializeTerminal.Error -> renderInitializeTerminalError(state.exception)

        DiscoverReader.Loading -> renderDiscoverReaderLoading()
        is DiscoverReader.Success -> renderDiscoverReaderSuccess(state.reader)
        is DiscoverReader.Error -> renderDiscoverReaderError(state.exception)

        ConnectToReader.Loading -> renderConnectToReaderLoading()
        is ConnectToReader.Success -> renderConnectToReaderSuccess(state.reader)
        is ConnectToReader.Error -> renderConnectToReaderError(state.exception)
    }

    private fun renderLocationLoading() = binding?.run {
        locationText.visibility = View.VISIBLE
        locationIcon.visibility = View.GONE
        locationLoading.visibility = View.VISIBLE
    }

    private fun renderLocationNotAllowed() = binding?.run {
        locationIcon.visibility = View.VISIBLE
        locationIcon.setImageResource(R.drawable.ic_close_circle)
        locationLoading.visibility = View.GONE
    }

    private fun renderLocationAllowed() = binding?.run {
        locationIcon.visibility = View.VISIBLE
        locationIcon.setImageResource(R.drawable.ic_checked)
        locationLoading.visibility = View.GONE
    }

    private fun requestLocationPermission(permissions: Array<String>) {
        renderLocationLoading()
        requestPermissionLauncher.launch(permissions)
    }

    private fun requestLocationPermissionDone() {
        renderLocationAllowed()
        context?.let {
            viewModel.requestBluetooth(it)
        }
    }

    private fun renderBluetoothLoading() = binding?.run {
        bluetoothText.visibility = View.VISIBLE
        bluetoothIcon.visibility = View.GONE
        bluetoothLoading.visibility = View.VISIBLE
    }

    private fun renderBluetoothNotAllowed() = binding?.run {
        bluetoothIcon.visibility = View.VISIBLE
        bluetoothIcon.setImageResource(R.drawable.ic_close_circle)
        bluetoothLoading.visibility = View.GONE
    }

    private fun renderBluetoothAllowed() = binding?.run {
        bluetoothIcon.visibility = View.VISIBLE
        bluetoothIcon.setImageResource(R.drawable.ic_checked)
        bluetoothLoading.visibility = View.GONE
    }

    private fun requestBluetoothPermission(permissions: Array<String>) {
        renderBluetoothLoading()
        requestPermissionLauncher.launch(permissions)
    }

    private fun requestBluetoothPermissionDone() {
        renderBluetoothAllowed()
        enableBluetooth()
        context?.let {
            viewModel.requestNFC(it)
        }
    }

    private fun onPermissionResult(permissions: Map<String, Boolean>) {
        // If none of the requested permissions were declined, start the discovery process.
        if (permissions.none { !it.value }) {
            context?.let {
                when (viewModel.state.value) {
                    is RequestPermission.RequestLocation -> {
                        requestLocationPermissionDone()
                    }

                    is RequestPermission.RequestBluetooth -> {
                        requestBluetoothPermissionDone()
                    }

                    else -> {}
                }
            }
        } else {
            context?.let {
                when (viewModel.state.value) {
                    is RequestPermission.RequestLocation -> {
                        renderLocationNotAllowed()
                    }

                    is RequestPermission.RequestBluetooth -> {
                        renderBluetoothNotAllowed()
                    }

                    else -> {}
                }
            }
            view?.let {
                Snackbar.make(it, "Please Complete allow above permissions", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_CONNECT
        ],
    )
    private fun enableBluetooth() {
        BluetoothAdapter.getDefaultAdapter()?.let { adapter ->
            if (!adapter.isEnabled) {
                adapter.enable()
            }
        }
    }

    private fun requestNFC(shouldRequest: Boolean) = binding?.run {
        nfcText.visibility = View.VISIBLE
        if (shouldRequest) {
            nfcIcon.visibility = View.GONE
            nfcLoading.visibility = View.GONE

            view?.let {
                Snackbar.make(it, "Allow NFC and restart App", Snackbar.LENGTH_LONG).show()
            }
        } else {
            nfcIcon.visibility = View.VISIBLE
            nfcIcon.setImageResource(R.drawable.ic_checked)
            nfcLoading.visibility = View.GONE
            viewModel.requestTerminalToken()
        }
    }

    private fun renderTerminalTokenLoading() = binding?.run {
        requestTokenText.visibility = View.VISIBLE
        requestTokenIcon.visibility = View.GONE
        requestTokenLoading.visibility = View.VISIBLE
    }

    private fun renderTerminalTokenSuccess(token: String) = binding?.run {
        requestTokenIcon.visibility = View.VISIBLE
        requestTokenIcon.setImageResource(R.drawable.ic_checked)
        requestTokenLoading.visibility = View.GONE

        context?.let {
            viewModel.initializeTerminal(it, token)
        }
    }

    private fun renderTerminalTokenError(exception: Exception) = binding?.run {
        requestTokenIcon.visibility = View.VISIBLE
        requestTokenIcon.setImageResource(R.drawable.ic_close_circle)
        requestTokenLoading.visibility = View.GONE

        view?.let {
            Snackbar.make(it, "Error ${exception.message}, restart App", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderInitializeTerminalLoading() = binding?.run {
        initializeTerminalText.visibility = View.VISIBLE
        initializeTerminalIcon.visibility = View.GONE
        initializeTerminalLoading.visibility = View.VISIBLE
    }

    private fun renderInitializeTerminalSuccess() = binding?.run {
        initializeTerminalIcon.visibility = View.VISIBLE
        initializeTerminalIcon.setImageResource(R.drawable.ic_checked)
        initializeTerminalLoading.visibility = View.GONE

        context?.let {
            viewModel.discoverReaders(it)
        }
    }

    private fun renderInitializeTerminalError(exception: Exception) = binding?.run {
        initializeTerminalIcon.visibility = View.VISIBLE
        initializeTerminalIcon.setImageResource(R.drawable.ic_close_circle)
        initializeTerminalLoading.visibility = View.GONE

        view?.let {
            Snackbar.make(it, "Error ${exception.message}, restart App", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderDiscoverReaderLoading() = binding?.run {
        initializeReaderText.visibility = View.VISIBLE
        initializeReaderIcon.visibility = View.GONE
        initializeReaderLoading.visibility = View.VISIBLE
    }

    private fun renderDiscoverReaderSuccess(reader: Reader) = binding?.run {
        initializeReaderIcon.visibility = View.VISIBLE
        initializeReaderIcon.setImageResource(R.drawable.ic_checked)
        initializeReaderLoading.visibility = View.GONE

        context?.let {
            viewModel.connectToReader(reader, locationId)
        }
    }

    private fun renderDiscoverReaderError(exception: Exception) = binding?.run {
        initializeReaderIcon.visibility = View.VISIBLE
        initializeReaderIcon.setImageResource(R.drawable.ic_close_circle)
        initializeReaderLoading.visibility = View.GONE

        view?.let {
            Snackbar.make(it, "Error ${exception.message}, restart App", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderConnectToReaderLoading() = binding?.run {
        connectReaderText.visibility = View.VISIBLE
        connectReaderIcon.visibility = View.GONE
        connectReaderLoading.visibility = View.VISIBLE
    }

    private fun renderConnectToReaderSuccess(reader: Reader) = binding?.run {
        connectReaderIcon.visibility = View.VISIBLE
        connectReaderIcon.setImageResource(R.drawable.ic_checked)
        connectReaderLoading.visibility = View.GONE

        activity?.let {
            (activity as NavigationListener).onNavigateToPaymentDetails()
        }
    }

    private fun renderConnectToReaderError(exception: Exception) = binding?.run {
        connectReaderIcon.visibility = View.VISIBLE
        connectReaderIcon.setImageResource(R.drawable.ic_close_circle)
        connectReaderLoading.visibility = View.GONE

        view?.let {
            Snackbar.make(it, "Error ${exception.message}, restart App", Snackbar.LENGTH_LONG).show()
        }
    }
}