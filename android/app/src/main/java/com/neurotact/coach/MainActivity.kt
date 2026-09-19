package com.neurotact.coach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.neurotact.coach.network.ApiClient
import com.neurotact.coach.network.SpeechRequest
import com.neurotact.coach.network.TacticalItem
import com.neurotact.coach.ui.theme.NeuroTactCoachTheme
import com.neurotact.coach.wear.WearConnector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer

    private var statusText   = mutableStateOf("Siap menerima instruksi")
    private var speechText   = mutableStateOf("-")
    private var isListening  = mutableStateOf(false)
    private var isLoading    = mutableStateOf(false)
    private var queueItems   = mutableStateOf<List<TacticalItem>>(emptyList())
    private var deviceIp = mutableStateOf("Memuat...")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startListening()
        else statusText.value = "Izin mikrofon diperlukan"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechListener()

        deviceIp.value = getLocalIpAddress()  // tambahkan ini

        setContent {
            NeuroTactCoachTheme {
                NeuroTactScreen(
                    status       = statusText.value,
                    speechText   = speechText.value,
                    isListening  = isListening.value,
                    isLoading    = isLoading.value,
                    queueItems   = queueItems.value,
                    deviceIp     = deviceIp.value,      // tambahkan ini
                    onSpeakClick = { checkPermissionAndListen() }
                )
            }
        }
    }

    private fun checkPermissionAndListen() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> startListening()
            else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        statusText.value  = "Mendengarkan..."
        isListening.value = true
        queueItems.value  = emptyList()
        speechRecognizer.startListening(intent)
    }

    private fun setupSpeechListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                isListening.value = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""

                speechText.value = text.ifEmpty { "Tidak terdeteksi" }
                statusText.value = "Memproses..."
                isLoading.value  = true

                if (text.isNotEmpty()) processToServer(text)
                else resetUI("Tidak ada suara terdeteksi")
            }

            override fun onError(error: Int) {
                isListening.value = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH       -> "Tidak ada suara yang cocok"
                    SpeechRecognizer.ERROR_NETWORK        -> "Error jaringan"
                    SpeechRecognizer.ERROR_AUDIO          -> "Error audio"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout, coba lagi"
                    else -> "Error code: $error"
                }
                resetUI(msg)
            }

            override fun onReadyForSpeech(params: Bundle?)         {}
            override fun onBeginningOfSpeech()                     {}
            override fun onRmsChanged(rmsdB: Float)                {}
            override fun onBufferReceived(buffer: ByteArray?)      {}
            override fun onEndOfSpeech()                           {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?)  {}
        })
    }

    private fun processToServer(text: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.service.processText(SpeechRequest(text))

                queueItems.value = response.results
                isLoading.value  = false

                // Kirim ke WearOS satu per satu sesuai delay
                response.results.forEachIndexed { index, item ->
                    if (index > 0) {
                        statusText.value = "Menunggu ${index}/${response.results.size}..."
                        delay(item.delay_ms)
                    }

                    statusText.value = "Mengirim ${index + 1}/${response.results.size}: ${item.label}"

                    WearConnector.sendCommand(
                        context   = this@MainActivity,
                        code      = item.code,
                        label     = item.label,
                        timestamp = item.timestamp,
                        index     = index
                    )
                }

                statusText.value = "Semua instruksi terkirim"

            } catch (e: Exception) {
                Log.e("MainActivity", "Error: ${e.message}")
                statusText.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun resetUI(status: String) {
        statusText.value  = status
        isListening.value = false
        isLoading.value   = false
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress &&
                        address is java.net.Inet4Address) {
                        return address.hostAddress ?: "Tidak ditemukan"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error ambil IP: ${e.message}")
        }
        return "Tidak ditemukan"
    }
}

// ── Composable UI ─────────────────────────────────────────────

@Composable
fun NeuroTactScreen(
    status      : String,
    speechText  : String,
    isListening : Boolean,
    isLoading   : Boolean,
    queueItems  : List<TacticalItem>,
    deviceIp    : String,              // tambahkan ini
    onSpeakClick: () -> Unit
) {
    val navyBlue = Color(0xFF1F4E79)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text       = "NeuroTact Coach",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = navyBlue
        )
        Text(
            text     = "Sistem Komunikasi Taktis",
            fontSize = 14.sp,
            color    = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tombol bicara
        Button(
            onClick  = onSpeakClick,
            enabled  = !isListening && !isLoading,
            shape    = CircleShape,
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFE94560) else navyBlue
            ),
            modifier = Modifier.size(140.dp)
        ) {
            Text(
                text      = if (isListening) "Mendengar..." else "Tekan\n& Bicara",
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
                color     = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // IP Address untuk koneksi WearOS
        Surface(
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(8.dp),
            color          = Color(0xFF1F4E79),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier            = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text     = "IP Address Device",
                    fontSize = 11.sp,
                    color    = Color(0xFFADD8E6)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = deviceIp,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text     = "Port Server: 8000",
                    fontSize = 11.sp,
                    color    = Color(0xFFADD8E6)
                )
            }
        }

        Text(text = status, fontSize = 13.sp, color = Color.Gray)

        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(
                color    = navyBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Teks hasil STT
        InfoCard(label = "Teks Instruksi", value = speechText)

        Spacer(modifier = Modifier.height(12.dp))

        // Queue instruksi
        if (queueItems.isNotEmpty()) {
            Text(
                text     = "Antrian Instruksi",
                fontSize = 12.sp,
                color    = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(queueItems) { index, item ->
                    QueueItemCard(
                        index = index + 1,
                        item  = item,
                        color = navyBlue
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemCard(
    index : Int,
    item  : TacticalItem,
    color : Color
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(8.dp),
        color          = Color.White,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nomor urut
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text      = "$index",
                        color     = Color.White,
                        fontSize  = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text       = item.label,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
                Text(
                    text     = "Kode: ${item.code}" +
                            if (item.delay_ms > 0) "  |  Delay: ${item.delay_ms / 1000}s" else "",
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    label : String,
    value : String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(8.dp),
            color          = Color.White,
            tonalElevation = 2.dp
        ) {
            Text(
                text     = value,
                fontSize = 15.sp,
                color    = Color(0xFF333333),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}