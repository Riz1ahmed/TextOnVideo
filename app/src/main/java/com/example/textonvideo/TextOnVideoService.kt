package com.example.textonvideo

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.learner.codereducer.utils.logD

class TextOnVideoService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //region Job here

        val outPath = intent?.getStringExtra(VideoProcessingService.KEY_OUT_PATH)
        val inputVidUri = intent?.getParcelableExtra<Uri>(VideoProcessingService.KEY_INPUT_VID_URI)
        val text = intent?.getStringExtra(VideoProcessingService.KEY_DRAW_TEXT)
            ?: "Android Video render"


        val fd = contentResolver.openFileDescriptor(inputVidUri!!, "r")!!.fileDescriptor
        AddTextToVideoProcessor().startEngine(outPath!!, fd, text)
        intent.getParcelableExtra<PendingIntent>(VideoProcessingService.KEY_RESULT_INTENT)?.send()
        logD("Service called")

        //endregion
        return super.onStartCommand(intent, flags, startId)
    }

    //region Listener
    /**
     * How to connect with this service
     * 1. after start service call bindService.
     *  bindService Code
     *   ```
     *   val myServiceIntent=Intent(context,MyService::java.class)
     *   val myService:MyService?=null
     *
     *   startService(serviceIntent)
     *   bindService(serviceIntent, object : ServiceConnection {
     *      override fun onServiceDisconnected(name: ComponentName?) {}
     *      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
     *          myService = (service as MyService.VPSBinder).getService()
     *      }
     *   }, BIND_AUTO_CREATE)
     *   ```
     * here initialized myService from onServiceConnected. So from now, everything
     * possible to access of MyService using this instance
     * */
    override fun onBind(intent: Intent): IBinder {
        return VPSBinder()
    }

    inner class VPSBinder : Binder() {
        fun getService() = this@TextOnVideoService
    }

    private var listener: MyServiceListener? = null
    fun setListener(listener: MyServiceListener) {
        this.listener = listener
    }

    interface MyServiceListener {
        fun videoCreateStarted()
        fun videoCreateFinished()
        fun videoCreateComplete(percent: Int)
    }

    //endregion
}