package com.example.taptopayandroid.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

fun ViewModel.launch(block: suspend () -> Unit) {
    try {
        viewModelScope.launch { block() }
    } catch (_: CancellationException) {
    } catch (_: Exception) {
    }
}

/**
 * @see https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
 *
 * This function must be called in `onViewCreated()` in case of Fragment
 */
fun <T> Flow<T>.collectOnStart(viewLifecycleOwner: LifecycleOwner, callback: (T) -> Unit) {
    collectOnStart(viewLifecycleOwner.lifecycle, viewLifecycleOwner.lifecycleScope, callback)
}

/**
 * Used for Activity
 */
fun <T> Flow<T>.collectOnStart(lifecycle: Lifecycle, lifecycleCoroutineScope: LifecycleCoroutineScope, callback: (T) -> Unit) {
    lifecycleCoroutineScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(callback)
        }
    }
}