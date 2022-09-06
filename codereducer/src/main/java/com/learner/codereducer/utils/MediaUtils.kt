package com.learner.codereducer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object MediaUtils {
    fun playVideo(context: Context, videoPath: String) {
        val outFile = File(videoPath)
        if (outFile.exists()) {
            val uri =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) Uri.parse(outFile.absolutePath)
                else FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
            playVideo(context, uri)
        }
    }

    fun playVideo(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setDataAndType(uri, "video/*") //or specify with "video/mp4"
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    fun getVideoPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).also {
            it.addCategory(Intent.CATEGORY_OPENABLE)
            it.type = "video/*"
            it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
    }
}