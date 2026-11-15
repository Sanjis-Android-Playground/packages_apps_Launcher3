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
import android.content.Context

import androidx.annotation.DrawableRes

import com.android.launcher3.R

/**
 * Shape used by the Rising widgets.
 *
 * Each widget keeps its own shape so changing one tile does not touch the others. A global
 * default is used when a widget has not been configured yet (or when the configure activity
 * was started without a valid app widget id).
 */
object WidgetShapes {

    const val ROUNDED = "rounded"
    const val SQUIRCLE = "squircle"
    const val CIRCLE = "circle"
    const val SQUARE = "square"
    const val HEART = "heart"

    val all = listOf(ROUNDED, SQUIRCLE, CIRCLE, SQUARE, HEART)

    private const val PREFS = "rising_widgets"
    private const val DEFAULT_KEY = "tile_shape"
    private const val KEY_PREFIX = "tile_shape_"

    private fun key(appWidgetId: Int) = KEY_PREFIX + appWidgetId

    fun getShape(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            prefs.getString(key(appWidgetId), null)?.let { return it }
        }
        return prefs.getString(DEFAULT_KEY, ROUNDED) ?: ROUNDED
    }

    fun setShape(context: Context, appWidgetId: Int, shape: String) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            editor.putString(DEFAULT_KEY, shape)
        } else {
            editor.putString(key(appWidgetId), shape)
        }
        editor.apply()
    }

    fun clearShape(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(appWidgetId))
            .apply()
    }

    @DrawableRes
    fun getBackground(shape: String, active: Boolean): Int {
        return when (shape) {
            SQUIRCLE -> if (active) R.drawable.widget_tile_squircle_on
                else R.drawable.widget_tile_squircle_off
            CIRCLE -> if (active) R.drawable.widget_tile_circle_on
                else R.drawable.widget_tile_circle_off
            SQUARE -> if (active) R.drawable.widget_tile_square_on
                else R.drawable.widget_tile_square_off
            HEART -> if (active) R.drawable.widget_tile_heart_on
                else R.drawable.widget_tile_heart_off
            else -> if (active) R.drawable.widget_tile_rounded_on
                else R.drawable.widget_tile_rounded_off
        }
    }
}
