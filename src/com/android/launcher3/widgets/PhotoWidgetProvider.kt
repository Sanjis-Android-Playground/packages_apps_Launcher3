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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews

import com.android.launcher3.R

import java.io.ByteArrayOutputStream

class PhotoWidgetProvider : BaseWidgetProvider() {

    companion object {
        const val ACTION_SELECT_PHOTO = "com.android.launcher3.action.SELECT_PHOTO"
        const val MAX_IMAGE_SIZE = 500

        fun refresh(context: Context) {
            PhotoWidgetProvider().onWidgetUpdate(context)
        }
    }

    override fun getLayoutId(): Int = R.layout.widget_photo

    override fun onWidgetUpdate(context: Context) {
        update(context)
    }

    private fun update(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, PhotoWidgetProvider::class.java)
        )
        if (appWidgetIds.isEmpty()) return
        val photoFilePath = Settings.System.getString(
            context.contentResolver, "home_photo_widget_uri"
        )
        val isGrayscale = Settings.System.getIntForUser(
            context.contentResolver, "home_photo_widget_grayscale", 0, UserHandle.USER_CURRENT
        ) == 1
        val bitmap = photoFilePath
            ?.takeIf { it.isNotEmpty() }
            ?.let { BitmapFactory.decodeFile(it) }
            ?.let { if (isGrayscale) applyGrayscale(it) else it }
            ?.let { compress(it) }
        appWidgetIds.forEach { appWidgetId ->
            val remoteViews = RemoteViews(context.packageName, getLayoutId()).apply {
                if (bitmap != null) {
                    setImageViewBitmap(R.id.widget_image, bitmap)
                    setViewVisibility(R.id.widget_image, View.VISIBLE)
                    setViewVisibility(R.id.widget_add_photo_text, View.GONE)
                } else {
                    setViewVisibility(R.id.widget_image, View.GONE)
                    setViewVisibility(R.id.widget_add_photo_text, View.VISIBLE)
                }
                val intent = Intent(context, PhotoPickerActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    private fun compress(bitmap: Bitmap): Bitmap {
        val scaledBitmap = scaleBitmap(bitmap)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (width > MAX_IMAGE_SIZE) MAX_IMAGE_SIZE else width
        val newHeight = if (height > MAX_IMAGE_SIZE) MAX_IMAGE_SIZE else height
        return if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
            val aspectRatio = width.toFloat() / height.toFloat()
            val newAspectRatio = newWidth.toFloat() / newHeight.toFloat()
            val scaledWidth = if (aspectRatio > newAspectRatio) {
                MAX_IMAGE_SIZE
            } else {
                (MAX_IMAGE_SIZE * aspectRatio).toInt()
            }
            val scaledHeight = if (aspectRatio > newAspectRatio) {
                (MAX_IMAGE_SIZE / aspectRatio).toInt()
            } else {
                MAX_IMAGE_SIZE
            }
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap
        }
    }
}
