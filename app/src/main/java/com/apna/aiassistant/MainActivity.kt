package com.apna.aiassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var language: Spinner
    private val languages = listOf("Punjabi (ਪੰਜਾਬੀ)", "Hindi (हिन्दी)", "English")
    private val locales = listOf("pa-IN", "hi-IN", "en-IN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition isn't available on this phone. Use typed commands."
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(0xFF0B1020.toInt())
        }
        val title = TextView(this).apply {
            text = "✦ APNA AI ASSISTANT"
            textSize = 26f
            setTextColor(0xFF8BE9FD.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Ready • Tap the mic to speak"
            textSize = 16f
            setTextColor(0xFFE6EAF2.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 20)
        }
        language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
        }
        val mic = Button(this).apply {
            text = "🎙  TAP TO SPEAK"
            setOnClickListener { startListening() }
        }
        transcript = TextView(this).apply {
            text = "Your conversation will appear here."
            textSize = 17f
            setTextColor(0xFFE6EAF2.toInt())
            setPadding(0, 24, 0, 16)
        }
        val typed = android.widget.EditText(this).apply {
            hint = "Or type a command…"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9AA4B2.toInt())
            setSingleLine(true)
        }
        val send = Button(this).apply {
            text = "Run command"
            setOnClickListener {
                val command = typed.text.toString().trim()
                if (command.isNotEmpty()) handleCommand(command)
            }
        }
        val help = TextView(this).apply {
            text = "Try: “What time is it?”, “Open YouTube”, “Search for wheat prices”.\n\nPrivacy: microphone starts only after you tap. Android/browser speech services may process recognition. This starter does not secretly listen in the background, read SMS/call logs, or send messages."
            textSize = 13f
            setTextColor(0xFFB8C1D1.toInt())
            setPadding(0, 22, 0, 0)
        }
        root.addView(title)
        root.addView(status)
        root.addView(language)
        root.addView(mic)
        root.addView(transcript)
        root.addView(typed, LinearLayout.LayoutParams(-1, -2))
        root.addView(send)
        root.addView(help)
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition isn't available. Please type a command."
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locales[language.selectedItemPosition])
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locales[language.selectedItemPosition])
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…"; }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "Processing…"; }
                override fun onError(error: Int) {
                    status.text = "Couldn't recognize that. Try again or type a command."
                }
                override fun onResults(results: Bundle?) {
                    val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (said.isNotBlank()) handleCommand(said) else status.text = "No speech detected."
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            status.text = "Could not start microphone. Check Android microphone permission."
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
        transcript.text = "You: $raw"
        val reply = when {
            listOf("time", "समय", "ਟਾਈਮ", "ਵਕਤ").any { command.contains(it) } ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            listOf("date", "today", "तारीख", "आज", "ਤਾਰੀਖ", "ਅੱਜ").any { command.contains(it) } ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            listOf("youtube", "ਯੂਟਿਊਬ", "यूट्यूब").any { command.contains(it) } -> {
                openUrl("https://www.youtube.com")
                "Opening YouTube."
            }
            listOf("search", "google", "ਖੋਜ", "सर्च", "खोज").any { command.contains(it) } -> {
                val query = raw.replace(Regex("(?i)search for|search|google|ਖੋਜ|सर्च|खोज"), "").trim()
                val url = "https://www.google.com/search?q=" + android.net.Uri.encode(query.ifBlank { raw })
                openUrl(url)
                "Opening search results. Check the results before acting."
            }
            listOf("hello", "hi", "ਸਤ ਸ੍ਰੀ ਅਕਾਲ", "नमस्ते").any { command.contains(it) } ->
                when (language.selectedItemPosition) {
                    0 -> "ਸਤ ਸ੍ਰੀ ਅਕਾਲ! ਮੈਂ ਤੁਹਾਡੀ ਕਿਵੇਂ ਮਦਦ ਕਰ ਸਕਦਾ ਹਾਂ?"
                    1 -> "नमस्ते! मैं आपकी कैसे मदद कर सकता हूँ?"
                    else -> "Hello! How can I help you?"
                }
            listOf("call", "message", "sms", "ਕਾਲ", "ਮੈਸੇਜ", "कॉल", "मैसेज").any { command.contains(it) } ->
                "For safety, this starter won't place calls or send messages automatically. Use your phone's normal app and confirm the recipient yourself."
            else -> "I heard: $raw. I can tell the time or date, open YouTube, and search the web. More device actions need to be added carefully."
        }
        transcript.text = "You: $raw\n\nAssistant: $reply"
        status.text = "Ready"
        tts?.language = Locale.forLanguageTag(locales[language.selectedItemPosition])
        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "apna-reply")
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "No browser app found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag("en-IN")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else status.text = "Microphone permission is needed for voice input."
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
