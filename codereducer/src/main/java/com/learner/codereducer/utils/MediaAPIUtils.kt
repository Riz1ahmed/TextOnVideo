package com.learner.codereducer.utils

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Size
import java.nio.ByteBuffer

object MediaAPIUtils {
    private const val videoMime = "video/"
    private const val audioMime = "audio/"

    fun getVideoSize(context: Context, uri: Uri, callBack: (Size?) -> Unit) {
        var size: Size? = null
        getVideoFile(context, uri) {
            size = Size(it.getInteger(MediaFormat.KEY_WIDTH), it.getInteger(MediaFormat.KEY_HEIGHT))
        }
        callBack(size)
    }

    fun getVideoDuration(context: Context, uri: Uri, callBack: (Long?) -> Unit) {
        var vDuration: Long? = null
        getVideoFile(context, uri) { vDuration = it.getLong(MediaFormat.KEY_DURATION) }
        callBack(vDuration)
    }

    private fun getVideoFile(context: Context, videoUri: Uri, callBack: (MediaFormat) -> Unit) {
        getSpecificFile(context, videoUri, videoMime) { callBack(it) }
    }

    private fun getAudioFile(context: Context, videoUri: Uri, callBack: (MediaFormat) -> Unit) {
        getSpecificFile(context, videoUri, audioMime) { callBack(it) }
    }

    private fun getSpecificFile(
        context: Context, mediaUri: Uri, mimeType: String, callBack: (MediaFormat) -> Unit
    ) {
        MediaExtractor().use { extractor ->
            extractor.setDataSource(context, mediaUri, null)
            //extractor.setDataSource(this, Uri.parse(assetPath), null)
            //extractor.setDataSource(this, Uri.parse(rawPath), null)
            for (i in 0 until extractor.trackCount) {
                val mediaFormat = extractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith(mimeType) == true) callBack(mediaFormat)
                extractor.selectTrack(i)
            }

            val buffer = ByteBuffer.allocate(16)
            while (extractor.readSampleData(buffer, 0) >= 0) {
                //buffer.
                extractor.advance()
            }
        }
    }

    fun getVideoThumbnail(context: Context, uri: Uri, callback: () -> Unit) {
        getSpecificFile(context, uri, videoMime) {

        }
    }
}

private fun MediaExtractor.use(block: (MediaExtractor) -> Unit) {
    block(this)
    this.release()
}
