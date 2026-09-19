package com.neurotact.coach.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearConnector {

    private const val TAG  = "WearConnector"
    private const val PATH = "/tactical_command"

    fun sendCommand(
        context   : Context,
        code      : String,
        label     : String,
        timestamp : Long,
        index     : Int
    ) {
        // Pakai path unik per item agar tidak overwrite satu sama lain
        val uniquePath = "$PATH/$index/$timestamp"

        val putDataMapRequest = PutDataMapRequest.create(uniquePath)
        putDataMapRequest.dataMap.apply {
            putString("code",      code)
            putString("label",     label)
            putLong("timestamp",   timestamp)
            putInt("index",        index)
        }

        val putDataRequest = putDataMapRequest.asPutDataRequest()
        putDataRequest.setUrgent()

        Wearable.getDataClient(context)
            .putDataItem(putDataRequest)
            .addOnSuccessListener {
                Log.d(TAG, "Terkirim ke WearOS: index=$index code=$code")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal kirim ke WearOS: ${e.message}")
            }
    }
}