package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.R
import java.io.File
import java.io.FileOutputStream

data class ToneItem(
    val id: String,
    val titleAr: String,
    val isBuiltIn: Boolean = true,
    val localUri: Uri? = null
)

class ToneManager(private val context: Context) {

    private var previewPlayer: MediaPlayer? = null

    val builtInTones = listOf(
        ToneItem("tone_default", "نغمة الإنجاز (الافتراضية)", true),
        ToneItem("tone_gentle", "رنين هادئ", true),
        ToneItem("tone_focus", "تنبيه التركيز", true),
        ToneItem("tone_alert", "منبه قوي ومستمر", true)
    )

    fun getAllTones(): List<ToneItem> {
        val customTones = getSavedCustomTones()
        return builtInTones + customTones
    }

    fun getToneTitle(toneId: String): String {
        return getAllTones().firstOrNull { it.id == toneId }?.titleAr ?: "النغمة الافتراضية"
    }

    /**
     * Copies a user-picked audio file from SAF to app private storage (up to 10MB)
     */
    fun saveCustomToneFromUri(sourceUri: Uri, originalFileName: String): ToneItem? {
        return try {
            val tonesDir = File(context.filesDir, "tones")
            if (!tonesDir.exists()) tonesDir.mkdirs()

            val safeName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(tonesDir, "custom_${System.currentTimeMillis()}_$safeName")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytes = 0L
                    val maxBytes = 10 * 1024 * 1024 // 10MB limit per spec

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        if (totalBytes > maxBytes) {
                            targetFile.delete()
                            return null
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            ToneItem(
                id = targetFile.name,
                titleAr = originalFileName,
                isBuiltIn = false,
                localUri = Uri.fromFile(targetFile)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getSavedCustomTones(): List<ToneItem> {
        val tonesDir = File(context.filesDir, "tones")
        if (!tonesDir.exists()) return emptyList()

        return tonesDir.listFiles()?.filter { it.isFile }?.map { file ->
            ToneItem(
                id = file.name,
                titleAr = file.name.substringAfter("custom_").substringAfter("_"),
                isBuiltIn = false,
                localUri = Uri.fromFile(file)
            )
        } ?: emptyList()
    }

    fun playPreview(toneId: String) {
        stopPreview()
        try {
            val player = createMediaPlayerForTone(context, toneId)
            previewPlayer = player
            player.setOnCompletionListener {
                stopPreview()
            }
            player.start()
        } catch (_: Exception) {
            stopPreview()
        }
    }

    fun stopPreview() {
        previewPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        previewPlayer = null
    }

    companion object {
        fun createMediaPlayerForTone(context: Context, toneId: String): MediaPlayer {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            val rawRes = when (toneId) {
                "tone_gentle" -> R.raw.tone_gentle
                "tone_focus" -> R.raw.tone_focus
                "tone_alert" -> R.raw.tone_alert
                "tone_default" -> R.raw.tone_default
                else -> null
            }

            if (rawRes != null) {
                val afd = context.resources.openRawResourceFd(rawRes)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.prepare()
                return player
            }

            // Check custom tone file
            val customFile = File(File(context.filesDir, "tones"), toneId)
            if (customFile.exists() && customFile.canRead()) {
                player.setDataSource(customFile.absolutePath)
                player.prepare()
                return player
            }

            // Fallback to default tone
            val afd = context.resources.openRawResourceFd(R.raw.tone_default)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.prepare()
            return player
        }
    }
}
