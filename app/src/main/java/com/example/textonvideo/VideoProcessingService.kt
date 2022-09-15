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
        val inputVidUri: Uri? = intent?.getParcelableExtra(KEY_INPUT_VID_URI)
        val text = intent?.getStringExtra(KEY_DRAW_TEXT) ?: "Android Video render"

        val fd = contentResolver.openFileDescriptor(inputVidUri!!, "r")!!.fileDescriptor
        AddTextToVideoProcessor().startEngine(outPath!!, fd, text)

        intent.getParcelableExtra<PendingIntent>(KEY_RESULT_INTENT)?.send()
        logD("Service called")
    }

    companion object {
        const val KEY_OUT_PATH = "key.OUT_PATH"
        const val KEY_INPUT_VID_URI = "key.INPUT_VID_URI"
        const val KEY_DRAW_TEXT = "key.TEXT"
        const val KEY_RESULT_INTENT = "key.RESULT_INTENT"
    }
}