package com.example.textonvideo

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.learner.codereducer.utils.logD

/**
 * Receives the input video and text from MainActivity and generates the
 * final video in background
 */
class VideoProcessingService : IntentService("VideoProcessingService") {
    override fun onHandleIntent(intent: Intent?) {
        val outPath = intent?.getStringExtra(KEY_OUT_PATH)
        val inputVidUri = intent?.getParcelableExtra<Uri>(KEY_INPUT_VID_URI)
        val text = intent?.getStringExtra(KEY_DRAW_TEXT) ?: "www.sisik.eu"

        AddTextToVideoProcessor().process(
            outPath!!,
            contentResolver.openFileDescriptor(inputVidUri!!, "r")!!.fileDescriptor,
            text
        )
        intent.getParcelableExtra<PendingIntent>(KEY_RESULT_INTENT)?.send()
        logD("Service called")
    }

    companion object {
        const val KEY_OUT_PATH = "eu.sisik.videotogreyscale.key.OUT_PATH"
        const val KEY_INPUT_VID_URI = "eu.sisik.videotogreyscale.key.INPUT_VID_URI"
        const val KEY_DRAW_TEXT = "eu.sisik.videotogreyscale.key.TEXT"
        const val KEY_RESULT_INTENT = "eu.sisik.videotogreyscale.key.RESULT_INTENT"
    }
}