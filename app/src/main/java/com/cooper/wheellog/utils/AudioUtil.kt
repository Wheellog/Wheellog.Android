package com.cooper.wheellog.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.Constants.ALARM_TYPE
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.sin

object AudioUtil {
    private const val duration = 1 // duration of sound
    private const val sampleRate = 44100 //22050; // Hz (maximum frequency is 7902.13Hz (B8))
    private const val numSamples = duration * sampleRate

    private const val freq = 440
    private val buffer = ShortArray(numSamples)

    private val audioTrack by lazy {
        prepareTone()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buffer.size)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size,
                AudioTrack.MODE_STATIC
            )
        }
    }

    private fun prepareTone() {
        for (i in 0 until numSamples) {
            val originalWave = sin(2 * Math.PI * freq * i / sampleRate)
            val harmonic1 = 0.5 * sin(2 * Math.PI * 2 * freq * i / sampleRate)
            val harmonic2 = 0.25 * sin(2 * Math.PI * 4 * freq * i / sampleRate)
            val secondWave = sin(2 * Math.PI * freq * 1.34f * i / sampleRate)
            val thirdWave = sin(2 * Math.PI * freq * 2.0f * i / sampleRate)
            val fourthWave = sin(2 * Math.PI * freq * 2.68f * i / sampleRate)
            if (i <= numSamples * 3 / 10) {
                buffer[i] =
                    ((originalWave + harmonic1 + harmonic2) * Short.MAX_VALUE).toInt()
                        .toShort() //+ harmonic1 + harmonic2
            } else if (i < numSamples * 3 / 5) {
                buffer[i] = ((originalWave + secondWave) * Short.MAX_VALUE).toInt().toShort()
            } else {
                buffer[i] = ((thirdWave + fourthWave) * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }

    var toneDuration = 0

    suspend fun playAlarm(alarmType: ALARM_TYPE) {
        if (WheelLog.AppConfig.useWheelBeepForAlarm && WheelData.getInstance() != null) {
            SomeUtil.playBeep(onlyByWheel = true, onlyDefault = false)
            return
        }
        try {
            withContext(Dispatchers.IO) {
                audioTrack.apply {
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                        stop()
                        val emptyBuffer = ShortArray(numSamples)
                        write(emptyBuffer, 0, buffer.size)
                    }
                    when (alarmType) {
                        ALARM_TYPE.CURRENT -> {
                            write(buffer, sampleRate * 3 / 10, 2 * sampleRate / 20)
                        }
                        // 100 ms for current
                        ALARM_TYPE.SPEED1,
                        ALARM_TYPE.SPEED2,
                        ALARM_TYPE.SPEED3,
                        ALARM_TYPE.PWM -> {
                            write(buffer, sampleRate / 20, toneDuration * sampleRate / 1000)
                        }
                        // 50, 100, 150 ms depends on number of speed alarm
                        else -> {
                            write(buffer, sampleRate * 3 / 10, 6 * sampleRate / 10)
                        }
                        // 600 ms temperature
                    }
                    play()
                }
            }
        } catch (ex: Exception) {
            Timber.i(ex)
        }
    }
}