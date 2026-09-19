// wear/WearConnector.kt

package com.neurotact.coach.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearConnector {

    private const val TAG = "WearConnector"
    private const val PATH = "/tactical_command"

    fun sendMorseCode(context: Context, code: String, morse: String, label: String) {
        val putDataMapRequest = PutDataMapRequest.create(PATH)
        putDataMapRequest.dataMap.apply {
            putString("code", code)
            putString("morse", morse)
            putString("label", label)
            putLong("timestamp", System.currentTimeMillis())
        }

        val putDataRequest = putDataMapRequest.asPutDataRequest()
        putDataRequest.setUrgent()

        Wearable.getDataClient(context)
            .putDataItem(putDataRequest)
            .addOnSuccessListener {
                Log.d(TAG, "Berhasil kirim ke WearOS: code=$code morse=$morse")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal kirim ke WearOS: ${e.message}")
            }
    }
}