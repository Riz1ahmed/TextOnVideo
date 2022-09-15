package com.example.textonvideo

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.textonvideo.databinding.ActivityMainBinding
import com.example.textonvideo.utils.getName
import com.example.textonvideo.utils.isServiceRunning
import com.learner.codereducer.utils.MediaUtils
import com.learner.codereducer.utils.MyActivityResult
import com.learner.codereducer.utils.hide
import com.video_lab.permission_controller.PermissionListener
import com.video_lab.permission_controller.PermissionsController
import java.io.File

class MainActivity : AppCompatActivity() {

    private var inputFile: Uri? = null
    lateinit var binding: ActivityMainBinding
    val activityResult = MyActivityResult(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkSaveInstanceState(savedInstanceState)
        initButtons()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /**When process finish, Hide the progress*/
        if (requestCode == CODE_PROCESSING_FINISHED) binding.progressEncoding.hide()
    }


    private fun initButtons() {
        binding.butSelectVid.setOnClickListener {
            PermissionsController.check(this, arrayListOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), object : PermissionListener {
                override fun allGranted() {
                    //Take as video picking configured intent
                    val videoPickerIntent = MediaUtils.getVideoPickerIntent()
                    //Call activityResult with the intent so that, pick video from storage.
                    activityResult.startIntent(videoPickerIntent) { result ->
                        if (result?.data?.data != null) setInputVideoUri(result.data?.data!!)
                    }
                }
            })
        }
        binding.ivPreview.setOnClickListener {
            MediaUtils.playVideo(this, getOutputPath())
        }
        binding.butProcessVideo.setOnClickListener { processVideo() }
    }

    private fun setInputVideoUri(uri: Uri) {
        inputFile = uri
        //Show thumbnail
        //MediaAPIUtils.getVideoThumbnail(this, uri) {}
    }

    private fun processVideo() {
        if (inputFile != null) {
            val intent = Intent(this, VideoProcessingService::class.java).apply {

                putExtra(VideoProcessingService.KEY_OUT_PATH, getOutputPath())
                putExtra(VideoProcessingService.KEY_INPUT_VID_URI, inputFile)
                putExtra(VideoProcessingService.KEY_DRAW_TEXT, binding.etText.text?.toString())

                // We want this Activity to get notified once the encoding has finished
                val pi = createPendingResult(CODE_PROCESSING_FINISHED, intent, 0)
                putExtra(VideoProcessingService.KEY_RESULT_INTENT, pi)
            }

            startService(intent)

            binding.progressEncoding.visibility = View.VISIBLE
        } else showToast("Select video file that you want to convert to grayscale first")
    }

    var tovService: TextOnVideoService? = null
    private fun createVideo() {
        Intent(this, TextOnVideoService::class.java).apply {
            putExtra(VideoProcessingService.KEY_OUT_PATH, getOutputPath())
            putExtra(VideoProcessingService.KEY_INPUT_VID_URI, inputFile)
            putExtra(VideoProcessingService.KEY_DRAW_TEXT, binding.etText.text?.toString())
            //serviceIntent.action = //Pass as string and get in service
            startService(this)
            bindMyService(this) { tovService = it }
        }
    }

    private fun bindMyService(serviceIntent: Intent, callback: (TextOnVideoService) -> Unit) {
        bindService(serviceIntent, object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {}
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val s = (service as TextOnVideoService.VPSBinder).getService()
                callback(s)
            }
        }, BIND_AUTO_CREATE)
    }


    private fun configureUi() {
        if (isServiceRunning(this, VideoProcessingService::class.java))
            binding.progressEncoding.visibility = View.VISIBLE
        else binding.progressEncoding.visibility = View.INVISIBLE

        binding.tvSelectedVideo.text = ""
        if (inputFile != null)
            binding.tvSelectedVideo.text = getName(this, inputFile!!) ?: ""

        val outFile = File(getOutputPath())
        if (outFile.exists()) {
            val thumb = ThumbnailUtils.createVideoThumbnail(
                outFile.absolutePath,
                MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
            )
            binding.ivPreview.setImageBitmap(thumb)
        }
    }

    private fun checkSaveInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null)
            inputFile = savedInstanceState.getParcelable("inputFile")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("inputFile", inputFile)
    }

    override fun onResume() {
        super.onResume()
        configureUi()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun getOutputPath() = cacheDir.absolutePath + "/" + OUT_FILE_NAME

    companion object {
        const val TAG = "MainActivity"

        const val CODE_SELECT_VID = 6660
        const val CODE_THUMB = 6661
        const val CODE_PROCESSING_FINISHED = 6662

        const val OUT_FILE_NAME = "out.mp4"
    }
}
