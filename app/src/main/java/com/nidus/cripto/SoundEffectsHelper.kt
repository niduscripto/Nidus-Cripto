package com.nidus.cripto

import android.content.Context

object SoundEffectsHelper {

    fun playReceiveTone(context: Context) {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, R.raw.tx_receive)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSendTone(context: Context) {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, R.raw.tx_sent)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}