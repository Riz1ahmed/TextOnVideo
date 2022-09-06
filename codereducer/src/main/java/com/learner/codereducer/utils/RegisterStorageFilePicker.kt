package com.learner.codereducer.utils

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**This class must be initiate before onStart Activity (safe way is
 * initialise from onCreate activity, onCreate is before than onStart in lifeCycle)*/
class RegisterStorageFilePicker(ca: ComponentActivity, pickerFile: PickerFile) {
    private var mediaIntent = MediaUtils.getVideoPickerIntent().also {
        it.type = pickerFile.type
    }
    private val launcher =
        ca.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null && result.data!!.data != null) {
                listener?.invoke(result.data!!.data!!)
            }
        }

    private var listener: ((Uri) -> Unit)? = null

    fun start(block: (Uri) -> Unit) {
        this.listener = block
        launcher.launch(mediaIntent)
    }

    fun setMyIntent(intent: Intent): RegisterStorageFilePicker {
        mediaIntent = intent
        return this
    }

    enum class PickerFile(val type: String) {
        IMAGE("image/*"), VIDEO("video/*")
    }
}