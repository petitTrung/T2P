package com.example.taptopayandroid.fragments

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.nfc.NfcManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taptopayandroid.ApiClient
import com.example.taptopayandroid.TerminalEventListener
import com.example.taptopayandroid.fragments.ConnectReaderState.ConnectToReader
import com.example.taptopayandroid.fragments.ConnectReaderState.DiscoverReader
import com.example.taptopayandroid.fragments.ConnectReaderState.InitializeTerminal
import com.example.taptopayandroid.fragments.ConnectReaderState.RequestPermission
import com.example.taptopayandroid.fragments.ConnectReaderState.RequestTerminalToken
import com.example.taptopayandroid.utils.isGranted
import com.example.taptopayandroid.utils.launch
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber


sealed interface ConnectReaderState {
    data object InitialState : ConnectReaderState

    sealed interface RequestPermission : ConnectReaderState {
        data class RequestLocation(val permissions: Array<String>) : RequestPermission
        data object RequestLocationDone : RequestPermission
        data class RequestBluetooth(val permissions: Array<String>) : RequestPermission
        data object RequestBluetoothDone : RequestPermission
        data class RequestNFC(val shouldRequest: Boolean) : RequestPermission
    }

    sealed interface RequestTerminalToken : ConnectReaderState {
        data object Loading : RequestTerminalToken
        data class Success(val token: String) : RequestTerminalToken
        data class Error(val exception: Exception) : RequestTerminalToken
    }

    sealed interface InitializeTerminal : ConnectReaderState {
        data object Loading : InitializeTerminal
        data object Success : InitializeTerminal
        data class Error(val exception: Exception) : InitializeTerminal
    }

    sealed interface DiscoverReader : ConnectReaderState {
        data object Loading : DiscoverReader
        data class Success(val reader: Reader) : DiscoverReader
        data class Error(val exception: Exception) : DiscoverReader
    }

    sealed interface ConnectToReader : ConnectReaderState {
        data object Loading : ConnectToReader
        data class Success(val reader: Reader) : ConnectToReader
        data class Error(val exception: Exception) : ConnectToReader
    }
}

class ConnectReaderViewModel : ViewModel() {
    // Backing property to avoid state updates from other classes
    private val _state = MutableLiveData<ConnectReaderState>(ConnectReaderState.InitialState)
    val state: LiveData<ConnectReaderState> = _state

    private var discoveryTask: Cancelable? = null

    fun requestLocation(context: Context) = launch {
        val hasGpsModule = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        val locationPermission = if (hasGpsModule) {
            Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            Manifest.permission.ACCESS_COARSE_LOCATION
        }

        val unGrantedPermissions = buildList {
            if (!context.isGranted(locationPermission)) add(locationPermission)
        }.toTypedArray()

        delay(200)
        if (unGrantedPermissions.isNotEmpty()) {
            _state.postValue(RequestPermission.RequestLocation(unGrantedPermissions))
        } else {
            _state.postValue(RequestPermission.RequestLocationDone)
        }
    }

    fun requestBluetooth(context: Context) = launch {
        val unGrantedPermissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!context.isGranted(Manifest.permission.BLUETOOTH_SCAN)) add(Manifest.permission.BLUETOOTH_SCAN)
                if (!context.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()

        delay(200)
        if (unGrantedPermissions.isNotEmpty()) {
            _state.postValue(RequestPermission.RequestBluetooth(unGrantedPermissions))
        } else {
            _state.postValue(RequestPermission.RequestBluetoothDone)
        }
    }

    fun requestNFC(context: Context) = launch {
        val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        val adapter = manager.defaultAdapter

        delay(200)
        if (adapter != null && adapter.isEnabled) {
            // adapter exists and is enabled.
            _state.postValue(RequestPermission.RequestNFC(shouldRequest = false))
        } else {
            _state.postValue(RequestPermission.RequestNFC(shouldRequest = true))
        }
    }

    fun requestTerminalToken() = launch {
        _state.postValue(RequestTerminalToken.Loading)
        withContext(Dispatchers.IO) {
            try {
                val token = ApiClient.createConnectionToken()
                _state.postValue(RequestTerminalToken.Success(token))
            } catch (e: Exception) {
                _state.postValue(RequestTerminalToken.Error(e))
            }
        }
    }

    fun initializeTerminal(context: Context, token: String) = launch {
        _state.postValue(InitializeTerminal.Loading)
        delay(200)
        try {
            if (!Terminal.isInitialized()) {
                Terminal.initTerminal(
                    context,
                    LogLevel.VERBOSE,
                    object : ConnectionTokenProvider {
                        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                            callback.onSuccess(token)
                            _state.postValue(InitializeTerminal.Success)
                        }
                    },
                    TerminalEventListener()
                )

            } else {
                _state.postValue(InitializeTerminal.Success)
            }
        } catch (e: TerminalException) {
            _state.postValue(InitializeTerminal.Error(e))
        }
    }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ],
    )
    fun discoverReaders(context: Context) = launch {
        if (discoveryTask == null && Terminal.getInstance().connectedReader == null) {
            _state.postValue(DiscoverReader.Loading)
            delay(200)

            val isApplicationDebuggable = 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
            val discoveryConfig = DiscoveryConfiguration.LocalMobileDiscoveryConfiguration(
                isSimulated = isApplicationDebuggable
            )
            discoveryTask = Terminal
                .getInstance()
                .discoverReaders(
                    config = discoveryConfig,
                    discoveryListener = object : DiscoveryListener {
                        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                            Timber.i("onUpdateDiscoveredReaders : $readers")
                            Timber.i("Online Reader : ${readers.filter { it.networkStatus != Reader.NetworkStatus.OFFLINE }}")
                            if (readers.isNotEmpty()) {
                                _state.postValue(DiscoverReader.Success(readers[0]))
                            }
                        }
                    },
                    callback = object : Callback {
                        override fun onSuccess() {
                            discoveryTask = null
                        }

                        override fun onFailure(e: TerminalException) {
                            discoveryTask = null
                            _state.postValue(DiscoverReader.Error(e))
                        }
                    }
                )
        }
    }

    fun connectToReader(reader: Reader, locationId: String) = launch {
        _state.postValue(ConnectToReader.Loading)
        delay(500)
        val config = ConnectionConfiguration.LocalMobileConnectionConfiguration(locationId)

        Terminal.getInstance().connectLocalMobileReader(
            reader,
            config,
            object : ReaderCallback {
                override fun onFailure(e: TerminalException) {
                    _state.postValue(ConnectToReader.Error(e))
                }

                override fun onSuccess(reader: Reader) {
                    _state.postValue(ConnectToReader.Success(reader))
                }
            },
        )
    }

    fun stopDiscovery(onSuccess: () -> Unit = { }) {
        discoveryTask?.cancel(object : Callback {
            override fun onSuccess() {
                discoveryTask = null
                launch { onSuccess() }
            }

            override fun onFailure(e: TerminalException) {
                discoveryTask = null
            }
        }) ?: run {
            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}