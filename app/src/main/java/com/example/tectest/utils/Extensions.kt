package com.example.tectest.utils

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

// Helper extension for Bundle to get Parcelable for modern Android versions
fun <T : Parcelable> Bundle.getParcelableCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key) as? T
    }
}