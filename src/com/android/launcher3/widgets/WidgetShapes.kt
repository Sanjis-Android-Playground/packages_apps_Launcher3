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

/** Per widget tile shape, selected from [TileWidgetConfigActivity]. */
object WidgetShapes {

    const val ROUNDED = "rounded"
    const val SQUIRCLE = "squircle"
    const val CIRCLE = "circle"
    const val SQUARE = "square"

    val all = listOf(ROUNDED, SQUIRCLE, CIRCLE, SQUARE)

    private const val PREFS = "rising_widgets"
    private const val KEY_PREFIX = "tile_shape_"

    fun getShape(context: Context, appWidgetId: Int): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + appWidgetId, ROUNDED) ?: ROUNDED
    }

    fun setShape(context: Context, appWidgetId: Int, shape: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + appWidgetId, shape)
            .apply()
    }

    fun clearShape(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREFIX + appWidgetId)
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
