package com.example.textonvideo

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.*
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import com.example.textonvideo.gl.TextAnimator
import com.example.textonvideo.gl.TextureRenderer
import com.example.textonvideo.utils.getSupportedVideoSize
import com.example.textonvideo.utils.textToBitmap
import com.learner.codereducer.utils.logD
import java.io.FileDescriptor
import java.security.InvalidParameterException

class AddTextToVideoProcessor {

    // Format for the greyscale video output file
    private val outMime = "video/avc"

    // Main classes from Android's API responsible
    // for processing of the video
    private var extractor: MediaExtractor? = null
    private var muxer: MediaMuxer? = null

    //For get details from Input video
    private var decoder: MediaCodec? = null

    //For create Output video from Details (Details may anywhere)
    private var encoder: MediaCodec? = null

    private val mediaCodedTimeoutUs = 10000L
    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1

    // These control the state of video processing and the draining/feeding
    // of buffers
    private var allInputExtracted = false
    private var allInputDecoded = false
    private var allOutputEncoded = false

    // Handles to raw video data used by MediaCodec encoder & decoder
    private var inputSurface: Surface? = null//Instance from encoder
    private var outputSurface: Surface? = null

    // Helper for the OpenGL rendering stuff
    private var videoRenderer: TextureRenderer? = null
    private var textRenderer: TextureRenderer? = null

    // Helps to calculate the transformations for moving text around
    private var textAnimator = TextAnimator()

    // Makes the decoded video frames available to OpenGL
    private var surfaceTexture: SurfaceTexture? = null

    // EGL stuff for initializing OpenGL context
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null

    // OpenGL transformation applied to UVs of the texture that holds
    // the decoded frame
    private val texMatrix = FloatArray(16)

    /*private var width = -1
    private var height = -1*/
    private var outputSize = Size(-1, -1)

    // Signalizes when a new decoded frame is available as texture
    // for OpenGL rendering
    @Volatile
    private var frameAvailable = false

    private var thread: HandlerThread? = null

    // OnFrameAvailable Callback is called from a different thread than
    // our OpenGL rendering thread, so we need some synchronization
    private val lock = Object()

    private var text: String? = null


    /**
     * Adds text to existing video
     *
     * @outPath path to output video file
     * @inputVidFd fd to input video file. I decided to use FileDescriptor
     *             simply because it is one of data sources accepted by MediaExtractor
     *             and it can be obtained from Uri (which I get from system file picker).
     *             Feel free to adjust to your preferences.
     */
    fun startEngine(outPath: String, inputVidFd: FileDescriptor, text: String) {
        this.text = text
        try {
            init(outPath, inputVidFd)
            startEngine()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            release()
        }
    }

    private fun init(outPath: String, inputVidFd: FileDescriptor) {
        // Init extractor
        val inFormat = InitExtractorAndGetVideoFile(inputVidFd)

        initEncoderAndInputSurface(inFormat)
        initEgl()
        initDecoderAndOutputSurface(inFormat)
        initMuxer(outPath)

        encoder!!.start()//Ready to process
        decoder!!.start()//Ready to process
    }

    private fun initEncoderAndInputSurface(inFormat: MediaFormat) {
        // Create H.264 encoder
        encoder = MediaCodec.createEncoderByType(outMime)

        // Prepare output format for the encoder
        val outFormat = getOutputFormat(inFormat)
        //width = outFormat.getInteger(MediaFormat.KEY_WIDTH)
        //height = outFormat.getInteger(MediaFormat.KEY_HEIGHT)

        // Configure the encoder & input surface
        encoder!!.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder!!.createInputSurface()
    }

    private fun initDecoderAndOutputSurface(inFormat: MediaFormat) {
        // Init output surface
        videoRenderer = TextureRenderer()
        textRenderer = TextureRenderer(false)
        surfaceTexture = SurfaceTexture(videoRenderer!!.texId)

        // Control the thread from which OnFrameAvailableListener will be called
        thread = HandlerThread("FrameHandlerThread")
        thread!!.start()

        surfaceTexture!!.setOnFrameAvailableListener({
            synchronized(lock) {
                val msg =
                    "Frame available before the last frame was process...we dropped some frames"
                if (frameAvailable) logD(msg)
                frameAvailable = true
                lock.notifyAll()
            }
        }, Handler(thread!!.looper))
        outputSurface = Surface(surfaceTexture)

        // Init decoder
        decoder = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder!!.configure(inFormat, outputSurface, null, 0)
    }

    private fun initMuxer(outPath: String) {
        // Init muxer
        muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    /**Initialize the [extractor] with the [inputVidFd]. Then search on it
     * Video file ("Video/"). if found, select and return it. Other
     * throw Not found exception.*/
    private fun InitExtractorAndGetVideoFile(inputVidFd: FileDescriptor): MediaFormat {
        //Init extractor
        extractor = MediaExtractor()
        extractor!!.setDataSource(inputVidFd)
        //Search video file. if found, select and return
        for (i in 0 until extractor!!.trackCount) {
            val format = extractor!!.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)!!.startsWith("video/")) {
                extractor!!.selectTrack(i)
                return format
            }
        }
        throw InvalidParameterException("File contains no video track")
    }

    /**
     * @param inputFormat it's for get the axact video size and FPS
     * @return a new MediaFormat
     *
     * * Also it's Init the the outSize. Also you can get this by
     * returnedFormat.getInteger(MediaFormat.KEY_WIDTH)
     * returnedFormat.getInteger(MediaFormat.KEY_HEIGHT)*/
    private fun getOutputFormat(inputFormat: MediaFormat): MediaFormat {
        // Preferably the output vid should have same resolution as input vid
        val inputSize = Size(
            inputFormat.getInteger(MediaFormat.KEY_WIDTH),
            inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        )
        outputSize = getSupportedVideoSize(encoder!!, outMime, inputSize)
        val mf = MediaFormat.createVideoFormat(outMime, outputSize.width, outputSize.height)
        mf.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mf.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
        mf.setInteger(
            MediaFormat.KEY_FRAME_RATE,
            inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
        )
        mf.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 15)
        mf.setString(MediaFormat.KEY_MIME, outMime)
        return mf
    }

    /**Init [eglDisplay], [eglContext] & [eglSurface]*/
    private fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY)
            throw RuntimeException(
                "eglDisplay == EGL14.EGL_NO_DISPLAY: " + GLUtils.getEGLErrorString(EGL14.eglGetError())
            )

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1))
            throw RuntimeException("eglInitialize(): " + GLUtils.getEGLErrorString(EGL14.eglGetError()))

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val nConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs,
                0, configs.size, nConfigs, 0
            )
        )
            throw RuntimeException(GLUtils.getEGLErrorString(EGL14.eglGetError()))

        var err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        val ctxAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext =
            EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)

        err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface =
            EGL14.eglCreateWindowSurface(eglDisplay, configs[0], inputSurface, surfaceAttribs, 0)
        err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext))
            throw RuntimeException("eglMakeCurrent(): " + GLUtils.getEGLErrorString(EGL14.eglGetError()))
    }

    private var textBitmap: Bitmap? = null
    private fun startEngine() {
        logD("Process called")
        allInputExtracted = false
        allInputDecoded = false
        allOutputEncoded = false

        textAnimator.setCamera(outputSize.width, outputSize.height)

        // Extract, decode, edit, encode, and mux
        while (!allOutputEncoded) {
            // Feed input to decoder
            if (!allInputExtracted)
                feedInputToDecoder()

            var encoderOutputAvailable = true
            var decoderOutputAvailable = !allInputDecoded

            while (encoderOutputAvailable || decoderOutputAvailable) {
                // Drain Encoder & mux to output file first
                val outBufferId = encoder!!.dequeueOutputBuffer(bufferInfo, mediaCodedTimeoutUs)
                if (outBufferId >= 0) {
                    val encodedBuffer = encoder!!.getOutputBuffer(outBufferId)
                    muxer!!.writeSampleData(trackIndex, encodedBuffer!!, bufferInfo)
                    encoder!!.releaseOutputBuffer(outBufferId, false)

                    // Are we finished here?
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        allOutputEncoded = true
                        break
                    }
                } else if (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    encoderOutputAvailable = false
                } else if (outBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer!!.addTrack(encoder!!.outputFormat)
                    muxer!!.start()
                }

                if (outBufferId != MediaCodec.INFO_TRY_AGAIN_LATER) continue

                // Get output from decoder and feed it to encoder
                if (!allInputDecoded) {
                    val outBufferID = decoder!!.dequeueOutputBuffer(bufferInfo, mediaCodedTimeoutUs)
                    if (outBufferID >= 0) {
                        val render = bufferInfo.size > 0
                        // Give the decoded frame to SurfaceTexture (onFrameAvailable() callback should
                        // be called soon after this)
                        decoder!!.releaseOutputBuffer(outBufferID, render)
                        if (render) {
                            // Wait till new frame available after onFrameAvailable has been called
                            waitTillFrameAvailable()

                            surfaceTexture!!.updateTexImage()
                            surfaceTexture!!.getTransformMatrix(texMatrix)

                            // Draw texture with opengl
                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
                            GLES20.glClearColor(0f, 0f, 0f, 0f)
                            GLES20.glViewport(0, 0, outputSize.width, outputSize.height)

                            videoRenderer?.draw(getMVP(), texMatrix, null)

                            textAnimator.update()
                            textRenderer!!.draw(
                                textAnimator.getMVP(), null,
                                textBitmap ?: textToBitmap(
                                    text!!,
                                    outputSize.width,
                                    outputSize.height
                                )
                                    .also { textBitmap = it }
                            )
                            logD("Drawing at time ${bufferInfo.presentationTimeUs / 1000000f}")

                            EGLExt.eglPresentationTimeANDROID(
                                eglDisplay, eglSurface, bufferInfo.presentationTimeUs * 1000
                            )

                            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                        }

                        // Did we get all output from decoder?
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            allInputDecoded = true
                            encoder!!.signalEndOfInputStream()
                            logD("Render Completed.")
                        }
                    } else if (outBufferID == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        decoderOutputAvailable = false
                    }
                }
            }
        }
    }

    private fun feedInputToDecoder() {
        val inBufferId = decoder!!.dequeueInputBuffer(mediaCodedTimeoutUs)
        if (inBufferId >= 0) {
            val buffer = decoder!!.getInputBuffer(inBufferId)
            val sampleSize = extractor!!.readSampleData(buffer!!, 0)

            if (sampleSize >= 0) {
                decoder!!.queueInputBuffer(
                    inBufferId, 0, sampleSize, extractor!!.sampleTime, extractor!!.sampleFlags
                )
                extractor!!.advance()
            } else {
                decoder!!.queueInputBuffer(
                    inBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                allInputExtracted = true
            }
        }
    }

    private fun waitTillFrameAvailable() {
        synchronized(lock) {
            while (!frameAvailable) {
                lock.wait(200)
                if (!frameAvailable) logD("Surface frame wait timed out")
            }
            frameAvailable = false
        }
    }

    private fun release() {
        extractor!!.release()

        decoder?.stop()
        decoder?.release()
        decoder = null

        encoder?.stop()
        encoder?.release()
        encoder = null

        releaseEgl()

        outputSurface?.release()
        outputSurface = null

        muxer?.stop()
        muxer?.release()
        muxer = null

        thread?.quitSafely()
        thread = null

        outputSize = Size(-1, -1)
        trackIndex = -1
    }

    private fun releaseEgl() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }

        inputSurface?.release()
        inputSurface = null

        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    private fun getMVP(): FloatArray {
        val mvp = FloatArray(16)
        Matrix.setIdentityM(mvp, 0)

        // Prepare your other transformations for video frames here
        // Matrix.scaleM(mvp, 0, 1f, -1f, 1f)
        return mvp
    }
}