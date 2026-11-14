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

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.TypedValue
import android.widget.RemoteViews

import androidx.core.content.ContextCompat

import com.android.launcher3.R

class BatteryWidgetProvider : BaseWidgetProvider() {

    override fun getLayoutId(): Int = R.layout.widget_battery

    override fun onWidgetUpdate(context: Context) {
        update(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_CLICK) {
            update(context)
            // Without usage access there is no screen time to show, so send the user to the
            // screen that lets them grant it instead of the battery saver screen.
            val target = if (hasUsageStatsAccess(context)) {
                Settings.ACTION_BATTERY_SAVER_SETTINGS
            } else {
                Settings.ACTION_USAGE_ACCESS_SETTINGS
            }
            context.startActivity(Intent(target).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    private fun update(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, BatteryWidgetProvider::class.java)
        )
        if (appWidgetIds.isEmpty()) return
        val batteryInfo = getBatteryInfo(context)
        val deviceName = Build.MODEL
        val remoteViews = RemoteViews(context.packageName, getLayoutId()).apply {
            setTextViewText(R.id.batteryLevel, batteryInfo)
            setImageViewBitmap(R.id.phone_icon, getWidgetIcon(context, batteryInfo))
            setTextViewText(R.id.deviceName, deviceName)
            val intent = Intent(context, BatteryWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CLICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            setOnClickPendingIntent(R.id.batteryRootLayout, pendingIntent)
        }
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, remoteViews) }
    }

    private fun getBatteryInfo(context: Context): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val batteryIntent = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val currentCharging = batteryIntent?.getIntExtra("max_charging_current", -1) ?: -1
        val isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
            BatteryManager.BATTERY_STATUS_CHARGING
        if (isCharging) {
            val currentText = if (currentCharging.toString().length >= 5) {
                (currentCharging / 1000).toString()
            } else {
                currentCharging.toString()
            }
            return "$batteryLevel% - ${currentText}mA"
        }
        // Screen time is only meaningful when the usage access app op was granted, otherwise
        // the widget would show a misleading "0 H 0".
        val screenTime = getTotalScreenTime(context) ?: return "$batteryLevel%"
        return "$batteryLevel% - ${formatScreenTime(screenTime)}"
    }

    private fun hasUsageStatsAccess(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun getTotalScreenTime(context: Context): Long? {
        if (!hasUsageStatsAccess(context)) return null
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        if (stats.isNullOrEmpty()) return 0
        return stats.sumOf { it.totalTimeInForeground } / (1000 * 60)
    }

    private fun formatScreenTime(minutes: Long): String {
        return "${minutes / 60} H ${minutes % 60}"
    }

    private fun getWidgetIcon(context: Context, batteryInfo: String): Bitmap {
        val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.device_phone_default)
        return generateBitmap(context, batteryInfo.substringBefore('%').toInt(), icon, 24f)
    }

    companion object {
        private const val ACTION_WIDGET_CLICK =
            "com.android.launcher3.widgets.action.BATTERY_WIDGET_CLICK"

        fun refresh(context: Context) {
            BatteryWidgetProvider().onWidgetUpdate(context)
        }

        fun generateBitmap(
            context: Context,
            percentage: Int,
            iconDrawable: Drawable?,
            iconSizePx: Float
        ): Bitmap {
            val width = 200
            val height = 200
            val stroke = 22
            val padding = 20
            val minAngle = 135f
            val maxAngle = 275f
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
            paint.strokeWidth = stroke.toFloat()
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            val arc = RectF().apply {
                set((stroke / 2f) + padding, (stroke / 2f) + padding, width - padding - (stroke / 2f), height - padding - (stroke / 2f))
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val color = ContextCompat.getColor(context, R.color.battery_device_primary_color)
            paint.color = Color.argb(75, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawArc(arc, minAngle, maxAngle, false, paint)
            paint.color = color
            canvas.drawArc(arc, minAngle, (maxAngle / 100) * percentage, false, paint)
            iconDrawable?.let {
                val arcCenterX = (arc.left + arc.right) / 2
                val arcCenterY = (arc.top + arc.bottom) / 2
                val size = dp2px(context, iconSizePx).toFloat()
                val left = (arcCenterX - size / 2).toInt()
                val top = (arcCenterY - size / 2).toInt()
                it.setBounds(left, top, left + size.toInt(), top + size.toInt())
                it.draw(canvas)
            }
            return bitmap
        }

        private fun dp2px(context: Context, dp: Float): Int {
            val displayMetrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics).toInt()
        }
    }
}
