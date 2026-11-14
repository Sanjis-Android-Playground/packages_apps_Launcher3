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

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.Toast

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

import com.android.launcher3.R

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoPickerActivity : AppCompatActivity() {

    private lateinit var switchGrayscale: Switch

    /**
     * Uses the system photo picker instead of an implicit [android.content.Intent.ACTION_PICK].
     * The launcher itself declares an ACTION_PICK handler, so the implicit intent showed the
     * launcher twice in the chooser.
     */
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                savePhoto(uri)
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_picker)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        switchGrayscale = findViewById(R.id.switchGrayscale) ?: return
        val pickPhotoButton: Button? = findViewById(R.id.btnPickPhoto)

        val isGrayscale = Settings.System.getIntForUser(
            contentResolver,
            "home_photo_widget_grayscale",
            0,
            UserHandle.USER_CURRENT
        ) == 1
        switchGrayscale.isChecked = isGrayscale

        switchGrayscale.setOnCheckedChangeListener { _, isChecked ->
            val resolver: ContentResolver = applicationContext.contentResolver
            Settings.System.putIntForUser(
                resolver,
                "home_photo_widget_grayscale",
                if (isChecked) 1 else 0,
                UserHandle.USER_CURRENT
            )
            WidgetRefreshReceiver.refreshAll(applicationContext)
        }

        pickPhotoButton?.setOnClickListener {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun savePhoto(photoUri: Uri) {
        val savedImagePath =
            saveImageToInternalStorage(applicationContext, photoUri, "home_widget", "HOME_WIDGET_PHOTO")
        if (savedImagePath == null) {
            Toast.makeText(this, R.string.photo_widget_save_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val resolver: ContentResolver = applicationContext.contentResolver
        Settings.System.putStringForUser(
            resolver, "home_photo_widget_uri", savedImagePath, UserHandle.USER_CURRENT
        )
        Settings.System.putIntForUser(
            resolver,
            "home_photo_widget_grayscale",
            if (switchGrayscale.isChecked) 1 else 0,
            UserHandle.USER_CURRENT
        )
        WidgetRefreshReceiver.refreshAll(applicationContext)
        Toast.makeText(this, R.string.photo_widget_selected, Toast.LENGTH_SHORT).show()
    }

    private fun saveImageToInternalStorage(context: Context, imgUri: Uri, featurePath: String, filePrefix: String): String? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(imgUri)
            if (imgUri.toString().startsWith("content://com.google.android.apps.photos.contentprovider")) {
                val segments = imgUri.pathSegments
                if (segments.size > 2) {
                    val mediaUriString = URLDecoder.decode(segments[2], StandardCharsets.UTF_8.name())
                    val mediaUri = Uri.parse(mediaUriString)
                    inputStream = context.contentResolver.openInputStream(mediaUri)
                } else {
                    throw Exception("Failed to parse Google Photos content URI")
                }
            }
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "$filePrefix" + "_$timeStamp.png"

            val directory = File(context.getExternalFilesDir(null), "risingOS/$featurePath")
            if (!directory.exists() && !directory.mkdirs()) {
                return null
            }
            val files = directory.listFiles { _, name -> name.startsWith(filePrefix) && name.endsWith(".png") }
            files?.forEach { it.delete() }
            val file = File(directory, imageFileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            return file.absolutePath
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }
        return null
    }
}
