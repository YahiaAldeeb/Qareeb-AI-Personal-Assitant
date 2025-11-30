package com.example.qareeb

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs

class AudioRecorderHelper(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var outputFile: File? = null

    // Standard WAV settings for Whisper (16kHz, 16-bit Mono)
    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    // Variable to track volume for the animation
    @Volatile private var lastAmplitude = 0

    @SuppressLint("MissingPermission")
    fun startRecording(): File? {
        try {
            outputFile = File(context.cacheDir, "command.wav")

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return null
            }

            audioRecord?.startRecording()
            isRecording = true

            // Write data in a background thread
            recordingThread = Thread { writeAudioData() }
            recordingThread?.start()

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getAmplitude(): Int {
        return lastAmplitude
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
            recordingThread?.join() // Wait for file to finish writing

            // Add the WAV header so the file is valid
            if (outputFile != null) {
                addWavHeader(outputFile!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            recordingThread = null
        }
    }

    // --- INTERNAL HELPERS TO WRITE WAV ---

    private fun writeAudioData() {
        val data = ByteArray(BUFFER_SIZE)
        val rawFile = File(outputFile?.absolutePath + ".raw")
        val os = FileOutputStream(rawFile)

        try {
            while (isRecording) {
                val read = audioRecord?.read(data, 0, BUFFER_SIZE) ?: 0
                if (read > 0) {
                    os.write(data, 0, read)

                    // Calculate Amplitude for the Animation
                    var maxVal = 0
                    for (i in 0 until read step 2) {
                        val sample = (data[i].toInt() and 0xFF) or (data[i + 1].toInt() shl 8)
                        val amplitude = abs(sample.toShort().toInt())
                        if (amplitude > maxVal) maxVal = amplitude
                    }
                    lastAmplitude = maxVal
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            os.close()
        }
    }

    private fun addWavHeader(file: File) {
        val rawFile = File(file.absolutePath + ".raw")
        if (!rawFile.exists()) return

        val rawIn = FileInputStream(rawFile)
        val wavOut = FileOutputStream(file)

        val totalAudioLen = rawFile.length()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (16 * SAMPLE_RATE * 1 / 8).toLong()

        try {
            writeHeader(wavOut, totalAudioLen, totalDataLen, byteRate)
            rawIn.copyTo(wavOut)
        } finally {
            rawIn.close()
            wavOut.close()
            rawFile.delete() // Remove the temp raw file
        }
    }

    private fun writeHeader(out: FileOutputStream, totalAudioLen: Long, totalDataLen: Long, byteRate: Long) {
        val header = ByteArray(44)
        val channels = 1
        val sampleRate = SAMPLE_RATE.toLong()

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte(); header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte(); header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte(); header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte(); header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte(); header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte(); header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}