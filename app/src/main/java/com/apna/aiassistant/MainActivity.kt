package com.apna.aiassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var language: Spinner
    private lateinit var typed: EditText
    private val languages = listOf("Punjabi (ਪੰਜਾਬੀ)", "Hindi (हिन्दी)", "English")
    private val locales = listOf("pa-IN", "hi-IN", "en-IN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFF0B1020.toInt())
        }
        root.addView(TextView(this).apply {
            text = "✦ APNA AI"
            textSize = 30f
            setTextColor(0xFF8BE9FD.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "YOUR VOICE • YOUR PHONE"
            textSize = 12f
            setTextColor(0xFF9AA4B2.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 18)
        })
        status = TextView(this).apply {
            text = "Ready • Tap the mic to speak"
            textSize = 16f
            setTextColor(0xFFE6EAF2.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 18)
        }
        root.addView(status)
        language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
        }
        root.addView(language, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(this).apply {
            text = "🎙  TAP TO SPEAK"
            textSize = 18f
            setOnClickListener { startListening() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16; bottomMargin = 16 })
        transcript = TextView(this).apply {
            text = "Try: Open WhatsApp • Open Instagram • What time is it?"
            textSize = 16f
            setTextColor(0xFFE6EAF2.toInt())
            setPadding(0, 14, 0, 18)
        }
        root.addView(transcript)
        typed = EditText(this).apply {
            hint = "Type a command…"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9AA4B2.toInt())
            setSingleLine(true)
        }
        root.addView(typed, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(this).apply {
            text = "Run command"
            setOnClickListener {
                val cmd = typed.text.toString().trim()
                if (cmd.isNotEmpty()) handleCommand(cmd)
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })
        root.addView(TextView(this).apply {
            text = "Privacy: microphone starts only when you tap. Messages open as drafts for you to review and send. No secret background listening."
            textSize = 12f
            setTextColor(0xFFB8C1D1.toInt())
            setPadding(0, 24, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition unavailable • Type a command"
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
                override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "Processing…" }
                override fun onError(error: Int) { status.text = "Didn't catch that • Try again" }
                override fun onResults(results: Bundle?) {
                    val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (said.isNotBlank()) handleCommand(said) else status.text = "No speech detected"
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            status.text = "Couldn't start microphone • Check permission"
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
        transcript.text = "You: " + raw
        val reply: String = when {
            command.contains("time") || command.contains("समय") || command.contains("ਟਾਈਮ") || command.contains("ਵਕਤ") ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            command.contains("date") || command.contains("today") || command.contains("तारीख") || command.contains("ਅੱਜ") || command.contains("ਤਾਰੀਖ") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            command.contains("whatsapp") || command.contains("व्हाट्सऐप") || command.contains("ਵਟਸਐਪ") -> {
                if (command.contains("message") || command.contains("msg") || command.contains("मैसेज") || command.contains("ਮੈਸੇਜ")) {
                    val msg = raw.replace(Regex("(?i).*?(message|msg|मैसेज|ਮੈਸੇਜ)\\s*"), "").trim()
                    if (msg.isNotBlank() && !msg.equals(raw, true)) {
                        openUrl("https://wa.me/?text=" + Uri.encode(msg))
                        "WhatsApp draft opened. Choose the recipient, review it, then tap Send yourself."
                    } else openApp("com.whatsapp", "WhatsApp")
                } else openApp("com.whatsapp", "WhatsApp")
            }
            command.contains("instagram") || command.contains("इंस्टाग्राम") -> openApp("com.instagram.android", "Instagram")
            command.contains("youtube") || command.contains("ਯੂਟਿਊਬ") || command.contains("यूट्यूब") -> openApp("com.google.android.youtube", "YouTube")
            command.contains("chrome") -> openApp("com.android.chrome", "Chrome")
            command.contains("camera") || command.contains("कैमरा") || command.contains("ਕੈਮਰਾ") -> {
                try { startActivity(Intent("android.media.action.IMAGE_CAPTURE")); "Opening Camera." }
                catch (_: Exception) { "Couldn't open Camera." }
            }
            command.contains("maps") -> openApp("com.google.android.apps.maps", "Maps")
            command.contains("settings") || command.contains("सेटिंग") || command.contains("ਸੈਟਿੰਗ") -> {
                try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)); "Opening Settings." }
                catch (_: Exception) { "Couldn't open Settings." }
            }
            command.contains("search") || command.contains("google") || command.contains("खोज") || command.contains("सर्च") || command.contains("ਖੋਜ") -> {
                val query = raw.replace(Regex("(?i)search for|search|google|खोज|सर्च|ਖੋਜ"), "").trim()
                openUrl("https://www.google.com/search?q=" + Uri.encode(query.ifBlank { raw }))
                "Opening search results."
            }
            command.contains("hello") || command.contains("ਸਤ ਸ੍ਰੀ ਅਕਾਲ") || command.contains("नमस्ते") ->
                when (language.selectedItemPosition) {
                    0 -> "ਸਤ ਸ੍ਰੀ ਅਕਾਲ! ਮੈਂ ਤੁਹਾਡੀ ਕਿਵੇਂ ਮਦਦ ਕਰ ਸਕਦਾ ਹਾਂ?"
                    1 -> "नमस्ते! मैं आपकी कैसे मदद कर सकता हूँ?"
                    else -> "Hello! How can I help you?"
                }
            else -> "I heard: " + raw + ". Try open WhatsApp, open Instagram, open YouTube, or what time is it."
        }
        transcript.text = "You: " + raw + "\n\nAssistant: " + reply
        status.text = "Ready"
        speak(reply)
    }

    private fun openApp(packageName: String, label: String): String {
        return try {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                startActivity(launch)
                "Opening " + label + "."
            } else if (label == "WhatsApp") {
                openUrl("https://www.whatsapp.com/")
                "WhatsApp app not found. Opening its website."
            } else "Couldn't find " + label + " installed on this phone."
        } catch (_: Exception) { "Couldn't open " + label + "." }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "Couldn't open link.", Toast.LENGTH_SHORT).show() }
    }

    private fun speak(message: String) {
        if (!ttsReady) {
            status.text = "Reply shown • Voice engine is still loading"
            return
        }
        val desired = Locale.forLanguageTag(locales[language.selectedItemPosition])
        val result = tts?.setLanguage(desired) ?: TextToSpeech.ERROR
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale("en", "IN")
            Toast.makeText(this, "Selected voice unavailable. Check Android Text-to-Speech voice data.", Toast.LENGTH_LONG).show()
        }
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "apna-reply")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.language = Locale("en", "IN")
            status.text = "Ready • Tap the mic to speak"
        } else {
            status.text = "Voice engine unavailable • Check Android Text-to-Speech settings"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else status.text = "Microphone permission is needed for voice input"
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
