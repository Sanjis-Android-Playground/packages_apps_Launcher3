/*
 * Copyright (C) 2023-2025 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.widgets

import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager

import androidx.core.content.ContextCompat

/**
 * Keeps the Rising widgets in sync with the system state they display.
 *
 * The widgets used to poll from a coroutine every 1-2 seconds, which kept the launcher process
 * busy and drained the battery. They are event driven instead: this receiver is only registered
 * while at least one of the widgets exists, and each widget also refreshes from its normal app
 * widget update period and when it is tapped.
 */
object WidgetRefreshReceiver : BroadcastReceiver() {

    private val components = listOf(
        BatteryWidgetProvider::class.java,
        PhotoWidgetProvider::class.java,
        WifiWidgetProvider::class.java,
        BluetoothToggleWidgetProvider::class.java,
        DataToggleWidgetProvider::class.java,
    )

    private var registered = false

    fun register(context: Context) {
        if (registered) return
        val appContext = context.applicationContext
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        ContextCompat.registerReceiver(
            appContext, this, filter, ContextCompat.RECEIVER_EXPORTED
        )
        registered = true
    }

    fun unregisterIfUnused(context: Context) {
        if (!registered) return
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val inUse = components.any {
            appWidgetManager.getAppWidgetIds(ComponentName(appContext, it)).isNotEmpty()
        }
        if (inUse) return
        appContext.unregisterReceiver(this)
        registered = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        refreshAll(context)
    }

    /** Refreshes every Rising widget. */
    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        BatteryWidgetProvider.refresh(appContext)
        PhotoWidgetProvider.refresh(appContext)
        WifiWidgetProvider.refresh(appContext)
        BluetoothToggleWidgetProvider.refresh(appContext)
        DataToggleWidgetProvider.refresh(appContext)
    }
}
