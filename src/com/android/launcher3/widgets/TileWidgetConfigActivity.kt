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

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.android.launcher3.R
import com.google.android.material.card.MaterialCardView

/** Lets the user pick the shape of the Rising tile widgets. */
class TileWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var shapeCards: Map<String, MaterialCardView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        // The id can be missing when the framework proxy loses it. The shape is global, so the
        // screen still works and the selection is applied immediately.
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContentView(R.layout.activity_tile_widget_config)
        shapeCards = mapOf(
            WidgetShapes.ROUNDED to findViewById(R.id.shape_rounded),
            WidgetShapes.SQUIRCLE to findViewById(R.id.shape_squircle),
            WidgetShapes.CIRCLE to findViewById(R.id.shape_circle),
            WidgetShapes.SQUARE to findViewById(R.id.shape_square),
        )
        shapeCards.forEach { (shape, card) ->
            card.contentDescription = getShapeLabel(shape)
            card.setOnClickListener { select(shape, apply = true) }
        }
        select(WidgetShapes.getShape(this), apply = false)

        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            WidgetRefreshReceiver.refreshAll(this)
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            finish()
        }
    }

    /**
     * Resolves the stroke colour from launcher resources. Material's {@code R.attr} fields are
     * non-final and get stripped by resource shrinking, so referencing them from code crashes
     * with {@link NoSuchFieldError}.
     */
    private fun select(shape: String, apply: Boolean) {
        val selectedColor = ContextCompat.getColor(this, R.color.tile_shape_selected)
        val defaultColor = ContextCompat.getColor(this, R.color.tile_shape_outline)
        val selectedWidth = (2 * resources.displayMetrics.density).toInt()
        val defaultWidth = (1 * resources.displayMetrics.density).toInt()
        shapeCards.forEach { (cardShape, card) ->
            val isSelected = cardShape == shape
            card.strokeColor = if (isSelected) selectedColor else defaultColor
            card.strokeWidth = if (isSelected) selectedWidth else defaultWidth
        }
        if (apply) {
            WidgetShapes.setShape(this, shape)
            WidgetRefreshReceiver.refreshAll(this)
        }
    }

    private fun getShapeLabel(shape: String): CharSequence {
        return getString(
            when (shape) {
                WidgetShapes.SQUIRCLE -> R.string.widget_tile_shape_squircle
                WidgetShapes.CIRCLE -> R.string.widget_tile_shape_circle
                WidgetShapes.SQUARE -> R.string.widget_tile_shape_square
                else -> R.string.widget_tile_shape_rounded
            }
        )
    }
}
