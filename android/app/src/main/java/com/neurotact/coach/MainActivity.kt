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
import com.neurotact.coach.ui.theme.NeuroTactCoachTheme
import com.neurotact.coach.wear.WearConnector
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer

    // State yang akan ditampilkan di UI
    private var statusText   = mutableStateOf("Siap menerima instruksi")
    private var speechText   = mutableStateOf("-")
    private var tacticalText = mutableStateOf("-")
    private var morseText    = mutableStateOf("-")
    private var isListening  = mutableStateOf(false)
    private var isLoading    = mutableStateOf(false)

    // Permission launcher
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

        setContent {
            NeuroTactCoachTheme {
                NeuroTactScreen(
                    status      = statusText.value,
                    speechText  = speechText.value,
                    tactical    = tacticalText.value,
                    morse       = morseText.value,
                    isListening = isListening.value,
                    isLoading   = isLoading.value,
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
        speechRecognizer.startListening(intent)
    }

    private fun setupSpeechListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                isListening.value = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""

                speechText.value  = text.ifEmpty { "Tidak terdeteksi" }
                statusText.value  = "Memproses ke server..."
                isLoading.value   = true

                if (text.isNotEmpty()) processToServer(text)
                else resetUI("Tidak ada suara terdeteksi")
            }

            override fun onError(error: Int) {
                isListening.value = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH     -> "Tidak ada suara yang cocok"
                    SpeechRecognizer.ERROR_NETWORK      -> "Error jaringan"
                    SpeechRecognizer.ERROR_AUDIO        -> "Error audio"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout, coba lagi"
                    else -> "Error code: $error"
                }
                resetUI(msg)
            }

            override fun onReadyForSpeech(params: Bundle?)    {}
            override fun onBeginningOfSpeech()                {}
            override fun onRmsChanged(rmsdB: Float)           {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech()                      {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?)  {}
        })
    }

    private fun processToServer(text: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.service.processText(SpeechRequest(text))

                tacticalText.value = response.label
                morseText.value    = "Morse: ${response.morse}  |  Kode: ${response.code}"
                statusText.value   = "Terkirim ke WearOS"

                WearConnector.sendMorseCode(
                    context = this@MainActivity,
                    code    = response.code,
                    morse   = response.morse,
                    label   = response.label
                )

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
}

// ── Composable UI ────────────────────────────────────────────

@Composable
fun NeuroTactScreen(
    status      : String,
    speechText  : String,
    tactical    : String,
    morse       : String,
    isListening : Boolean,
    isLoading   : Boolean,
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

        // Judul
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

        Spacer(modifier = Modifier.height(40.dp))

        // Tombol bicara
        Button(
            onClick  = onSpeakClick,
            enabled  = !isListening && !isLoading,
            shape    = CircleShape,
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFE94560) else navyBlue
            ),
            modifier = Modifier.size(160.dp)
        ) {
            Text(
                text      = if (isListening) "Mendengar..." else "Tekan\n& Bicara",
                fontSize  = 16.sp,
                textAlign = TextAlign.Center,
                color     = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status
        Text(
            text     = status,
            fontSize = 14.sp,
            color    = Color.Gray
        )

        // Loading indicator
        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(
                color    = navyBlue,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card hasil speech
        InfoCard(label = "Teks Instruksi", value = speechText)

        Spacer(modifier = Modifier.height(12.dp))

        // Card instruksi taktis
        InfoCard(
            label     = "Instruksi Taktis",
            value     = tactical,
            valueSize = 22.sp,
            valueBold = true,
            valueColor = navyBlue
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card kode morse
        InfoCard(label = "Kode Morse", value = morse)
    }
}

@Composable
fun InfoCard(
    label      : String,
    value      : String,
    valueSize  : androidx.compose.ui.unit.TextUnit = 16.sp,
    valueBold  : Boolean = false,
    valueColor : Color = Color(0xFF333333)
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            color    = Color.White,
            tonalElevation = 2.dp
        ) {
            Text(
                text       = value,
                fontSize   = valueSize,
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
                color      = valueColor,
                modifier   = Modifier.padding(12.dp)
            )
        }
    }
}