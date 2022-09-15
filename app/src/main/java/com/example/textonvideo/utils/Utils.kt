package com.example.textonvideo.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.*
import android.media.MediaCodec
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Size
import kotlin.math.absoluteValue


/**
 * Copyright (c) 2019 by Roman Sisik. All rights reserved.
 */
fun isServiceRunning(context: Context, clazz: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    for (runningServiceInfo in am.getRunningServices(Integer.MAX_VALUE))
        if (runningServiceInfo.service.className == clazz.name) return true
    return false
}

fun getSupportedVideoSize(mediaCodec: MediaCodec, mime: String, preferredResolution: Size): Size {

    fun isSupportedSize(mediaCodec: MediaCodec, regulation: Size, mime: String) =
        mediaCodec.codecInfo.getCapabilitiesForType(mime).videoCapabilities
            .isSizeSupported(regulation.width, regulation.height)

    // First check if exact combination supported
    if (isSupportedSize(mediaCodec, preferredResolution, mime)) return preferredResolution

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
            { pix - it.width * it.height }, // Find similar size
            { // Consider aspect
                val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                else it.height.toFloat() / it.width.toFloat()
                (preferredAspect - aspect).absoluteValue
            })
    )

    for (size in nearestToFurthest) if (isSupportedSize(mediaCodec, size, mime)) return size

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
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)//All of text design inside pan
    paint.textSize = 62f//Text size. Also will change later
    paint.color = Color.CYAN//Text color
    paint.textAlign = Paint.Align.LEFT//Text start from left

    // Resize text size so that, draw text exact as bitmap size
    val bounds = Rect()//Text LRTD value will save hare after getTextBound() call
    paint.getTextBounds(text, 0, text.length, bounds)
    paint.textSize = paint.textSize * width.toFloat() / bounds.width()//Re-textSize

    // Or fit to height
    // paint.textSize = ceil(paint.textSize * height.toDouble() / bounds.height()).toFloat()
    // with paint.setTextScaleX()//Fit XY if want. By this can set aspect ration

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    paint.getTextBounds(text, 0, text.length, bounds)//Re-confirm the bounds
    canvas.drawText(text, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
    return bitmap
}

