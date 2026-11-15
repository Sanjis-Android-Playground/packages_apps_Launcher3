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

import android.content.Context

import androidx.annotation.DrawableRes

import com.android.launcher3.R

/**
 * Shape used by the Rising tile widgets.
 *
 * The value is global rather than per app widget id: the configure activity result runs through
 * the framework {@code AppWidgetConfigActivityProxy}, and when that result is lost the launcher
 * drops the widget that was just added. Keeping the choice global means the selection is already
 * applied on the next update and the user never has to set it up twice.
 */
object WidgetShapes {

    const val ROUNDED = "rounded"
    const val SQUIRCLE = "squircle"
    const val CIRCLE = "circle"
    const val SQUARE = "square"

    val all = listOf(ROUNDED, SQUIRCLE, CIRCLE, SQUARE)

    private const val PREFS = "rising_widgets"
    private const val KEY = "tile_shape"

    fun getShape(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, ROUNDED) ?: ROUNDED
    }

    fun setShape(context: Context, shape: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, shape)
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
            else -> if (active) R.drawable.widget_tile_rounded_on
                else R.drawable.widget_tile_rounded_off
        }
    }
}
