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
import android.widget.RadioGroup

import androidx.appcompat.app.AppCompatActivity

import com.android.launcher3.R

/** Lets the user pick the shape of a Rising tile widget. */
class TileWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The widget is only added when the user confirms the configuration.
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_tile_widget_config)
        val shapeGroup = findViewById<RadioGroup>(R.id.tile_shape_group)
        shapeGroup.check(shapeToRadioId(WidgetShapes.getShape(this, appWidgetId)))
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            WidgetShapes.setShape(this, appWidgetId, radioIdToShape(shapeGroup.checkedRadioButtonId))
            WidgetRefreshReceiver.refreshAll(this)
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            finish()
        }
    }

    private fun shapeToRadioId(shape: String): Int {
        return when (shape) {
            WidgetShapes.SQUIRCLE -> R.id.shape_squircle
            WidgetShapes.CIRCLE -> R.id.shape_circle
            WidgetShapes.SQUARE -> R.id.shape_square
            else -> R.id.shape_rounded
        }
    }

    private fun radioIdToShape(radioId: Int): String {
        return when (radioId) {
            R.id.shape_squircle -> WidgetShapes.SQUIRCLE
            R.id.shape_circle -> WidgetShapes.CIRCLE
            R.id.shape_square -> WidgetShapes.SQUARE
            else -> WidgetShapes.ROUNDED
        }
    }
}
