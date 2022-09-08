package com.example.textonvideo.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaCodec
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.absoluteValue


/**
 * Copyright (c) 2019 by Roman Sisik. All rights reserved.
 */

fun isServiceRunning(context: Context, clazz: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    for (i in am.getRunningServices(Integer.MAX_VALUE)) {
        if (i.service.className == clazz.name) return true
    }
    return false
}

fun getSupportedVideoSize(mediaCodec: MediaCodec, mime: String, preferredResolution: Size): Size {
    // First check if exact combination supported
    if (mediaCodec.codecInfo.getCapabilitiesForType(mime)
            .videoCapabilities.isSizeSupported(
                preferredResolution.width,
                preferredResolution.height
            )
    )
        return preferredResolution

    // I'm using the resolutions suggested by docs for H.264 and VP8
    // https://developer.android.com/guide/topics/media/media-formats#video-encoding
    // TODO: find more supported resolutions
    val resolutions = arrayListOf(
        Size(176, 144),
        Size(320, 240),
        Size(320, 180),
        Size(640, 360),
        Size(720, 480),
        Size(1280, 720),
        Size(1920, 1080)
    )

    // I prefer similar resolution with similar aspect
    val pix = preferredResolution.width * preferredResolution.height
    val preferredAspect = preferredResolution.width.toFloat() / preferredResolution.height.toFloat()

    val nearestToFurthest = resolutions.sortedWith(
        compareBy(
            // Find similar size
            {
                pix - it.width * it.height
            },
            // Consider aspect
            {
                val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                else it.height.toFloat() / it.width.toFloat()
                (preferredAspect - aspect).absoluteValue
            })
    )

    for (size in nearestToFurthest) {
        if (mediaCodec.codecInfo.getCapabilitiesForType(mime)
                .videoCapabilities.isSizeSupported(size.width, size.height)
        )
            return size
    }

    throw RuntimeException("Couldn't find supported resolution")
}

@SuppressLint("Range")
fun getName(context: Context, fromUri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(
        fromUri, null, null, null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst())
            name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
    }
    return name
}

fun textToBitmap(text: String, width: Int, height: Int): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Pick an initial size to calculate the requested size later
    paint.textSize = 62f

    // Configure your text properties
    paint.color = 0xFF009FE3.toInt()// Color.parseColor("#FF009FE3")
    paint.textAlign = Paint.Align.LEFT // This affects the origin of x in Canvas.drawText()
    // setTypeface(), setUnderlineText(), ....

    // After setting parameters that could affect the size and position,
    // now try to fit text within requested bitmap width & height
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    paint.textSize = paint.textSize * width.toFloat() / bounds.width()

    // Or fit to height
    // paint.textSize = ceil(paint.textSize * height.toDouble() / bounds.height()).toFloat()

    // You can also affect the aspect ratio of text and try to fit both, width and height,
    // with paint.setTextScaleX()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Measure once again to get current top, left position, so that
    // we can position the final text from fop left corner
    paint.getTextBounds(text, 0, text.length, bounds)

    canvas.drawText(text, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
    return bitmap
}

