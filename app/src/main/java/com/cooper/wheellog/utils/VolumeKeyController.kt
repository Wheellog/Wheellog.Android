package com.cooper.wheellog.utils

import android.content.Context
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.VolumeProviderCompat

class VolumeKeyController(private val mContext: Context) {
    private var mMediaSession: MediaSessionCompat? = null

    private fun createMediaSession(): MediaSessionCompat? {
        val log = "log"
        mMediaSession = MediaSessionCompat(mContext, log).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0f)
                    .build())
            setPlaybackToRemote(volumeProvider)
        }
        return mMediaSession
    }

    private val volumeProvider: VolumeProviderCompat
        get() {
            val audio = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = AudioManager.STREAM_MUSIC
            val currentVolume = audio.getStreamVolume(streamType)
            val maxVolume = audio.getStreamMaxVolume(streamType)
            return object : VolumeProviderCompat(VOLUME_CONTROL_RELATIVE, maxVolume, currentVolume) {
                override fun onAdjustVolume(direction: Int) {
                    // Up = 1, Down = -1, Release = 0
                    // Replace with your action, if you don't want to adjust system volume
                    if (direction == 0) {
                        SomeUtil.playBeep()
                    }
                    //audio.adjustStreamVolume(streamType, direction, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                    //setCurrentVolume(audio.getStreamVolume(streamType))
                }
            }
        }

    fun setActive(active: Boolean) {
        (mMediaSession ?: createMediaSession())?.isActive = active
    }

    fun destroy() {
        mMediaSession?.release()
    }
}