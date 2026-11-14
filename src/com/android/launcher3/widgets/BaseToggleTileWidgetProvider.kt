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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

import com.android.launcher3.R

abstract class BaseToggleTileWidgetProvider : BaseToggleWidgetProvider() {

    abstract fun toggleService(context: Context)

    protected abstract fun getIconResource(isActive: Boolean): Int
    protected abstract fun getActionString(): String

    override fun onUpdate(context: Context) {
        updateWidget(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == getActionString()) {
            toggleService(context)
            updateWidget(context)
        }
    }

    override fun updateWidget(context: Context) {
        val appWidgetIds = getAppWidgetIds(context)
        if (appWidgetIds.isEmpty()) return
        val isActive = isServiceActive(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetIds.forEach { appWidgetId ->
            val remoteViews = RemoteViews(context.packageName, getLayoutId()).apply {
                setInt(R.id.widget_root, "setBackgroundResource", WidgetShapes.getBackground(
                    WidgetShapes.getShape(context, appWidgetId), isActive
                ))
                setInt(R.id.widget_icon, "setColorFilter", context.getColor(
                    if (isActive) R.color.battery_device_secondary_color
                    else R.color.battery_device_primary_color
                ))
                setImageViewResource(R.id.widget_icon, getIconResource(isActive))
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    getToggleActionIntent(context, appWidgetId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    protected abstract fun getAppWidgetIds(context: Context): IntArray

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetShapes.clearShape(context, it) }
    }
}
