package com.apna.aiassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
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
    private lateinit var micButton: Button
    private val languages = listOf("Punjabi • ਪੰਜਾਬੀ", "Hindi • हिन्दी", "English")
    private val locales = listOf("pa-IN", "hi-IN", "en-IN")
    private val bg = Color.rgb(6, 10, 24)
    private val panel = Color.rgb(15, 23, 43)
    private val cyan = Color.rgb(70, 229, 255)
    private val white = Color.rgb(235, 244, 255)
    private val muted = Color.rgb(147, 164, 190)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun rounded(color: Int, radius: Float, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke != Color.TRANSPARENT) setStroke(1, stroke)
        }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 28)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 13, 31), Color.rgb(12, 20, 43), Color.rgb(5, 9, 20))
            )
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = TextView(this).apply {
            text = "✦"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = rounded(cyan, 22f)
        }
        brandRow.addView(logo, LinearLayout.LayoutParams(54, 54))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 0, 0, 0)
            addView(label("APNA AI", 25f, white, true))
            addView(label("YOUR VOICE. YOUR PHONE.", 10f, cyan, true))
        }
        brandRow.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(label("● ONLINE", 10f, cyan, true))
        root.addView(brandRow)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(panel, 28f, Color.rgb(34, 66, 100))
            setPadding(18, 24, 18, 22)
        }
        val orb = TextView(this).apply {
            text = "◉"
            textSize = 50f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = rounded(Color.rgb(20, 42, 69), 100f, Color.rgb(48, 125, 167))
        }
        hero.addView(orb, LinearLayout.LayoutParams(104, 104).apply { bottomMargin = 14 })
        hero.addView(label("VOICE CONTROL", 18f, white, true))
        status = label("SYSTEM READY  •  TAP TO ACTIVATE", 11f, cyan, true)
        status.gravity = Gravity.CENTER
        status.setPadding(0, 8, 0, 0)
        hero.addView(status)
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 22 })

        val langLabel = label("ASSISTANT LANGUAGE", 10f, muted, true)
        langLabel.setPadding(3, 18, 0, 6)
        root.addView(langLabel)
        language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            background = rounded(panel, 16f, Color.rgb(40, 62, 91))
        }
        root.addView(language, LinearLayout.LayoutParams(-1, 52))

        micButton = Button(this).apply {
            text = "🎙   TAP TO SPEAK"
            textSize = 16f
            isAllCaps = false
            setTextColor(bg)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cyan, Color.rgb(104, 255, 210))).apply {
                cornerRadius = 22f
            }
            setOnClickListener { startListening() }
        }
        root.addView(micButton, LinearLayout.LayoutParams(-1, 62).apply { topMargin = 16 })

        val resultTitle = label("CONVERSATION", 10f, muted, true)
        resultTitle.setPadding(3, 22, 0, 8)
        root.addView(resultTitle)
        transcript = label("You: —\n\nAssistant: Hey! I'm ready. Try saying “Open Instagram” or “What time is it?”", 14f, white)
        transcript.setLineSpacing(5f, 1f)
        transcript.setPadding(16, 16, 16, 16)
        transcript.background = rounded(panel, 18f, Color.rgb(34, 54, 82))
        root.addView(transcript, LinearLayout.LayoutParams(-1, -2))

        typed = EditText(this).apply {
            hint = "Type a command…"
            textSize = 14f
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
            setPadding(16, 12, 16, 12)
            background = rounded(Color.rgb(10, 17, 34), 16f, Color.rgb(42, 69, 98))
        }
        root.addView(typed, LinearLayout.LayoutParams(-1, 54).apply { topMargin = 16 })

        val runButton = Button(this).apply {
            text = "RUN COMMAND  →"
            textSize = 14f
            isAllCaps = false
            setTextColor(white)
            background = rounded(Color.rgb(26, 48, 78), 16f, Color.rgb(56, 117, 160))
            setOnClickListener {
                val cmd = typed.text.toString().trim()
                if (cmd.isNotEmpty()) handleCommand(cmd)
                else status.text = "TYPE A COMMAND FIRST"
            }
        }
        root.addView(runButton, LinearLayout.LayoutParams(-1, 54).apply { topMargin = 8 })

        val privacy = label("PRIVACY FIRST\nMic activates only when you tap. Messages open as drafts for you to review and send. No secret background listening.", 11f, muted)
        privacy.setLineSpacing(3f, 1f)
        privacy.setPadding(4, 22, 4, 0)
        root.addView(privacy)

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, 0)
            addView(root)
        })
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "SPEECH SERVICE UNAVAILABLE • TYPE BELOW"
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val locale = locales[language.selectedItemPosition]
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status.text = "● LISTENING…"; micButton.text = "◉   LISTENING…" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "PROCESSING VOICE…"; micButton.text = "🎙   TAP TO SPEAK" }
                override fun onError(error: Int) { status.text = "DIDN'T CATCH THAT • TRY AGAIN"; micButton.text = "🎙   TAP TO SPEAK" }
                override fun onResults(results: Bundle?) {
                    val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (said.isNotBlank()) handleCommand(said) else status.text = "NO SPEECH DETECTED"
                    micButton.text = "🎙   TAP TO SPEAK"
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            status.text = "MIC ERROR • CHECK PERMISSION"
            micButton.text = "🎙   TAP TO SPEAK"
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
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
            else -> "I heard: $raw. Try open Instagram, open WhatsApp, open YouTube, or ask the time."
        }
        transcript.text = "YOU  /  $raw\n\nAPNA AI  /  $reply"
        status.text = "SYSTEM READY  •  TAP TO ACTIVATE"
        speak(reply)
    }

    private fun openApp(packageName: String, label: String): String {
        return try {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                "Opening $label."
            } else if (label == "WhatsApp") {
                openUrl("https://www.whatsapp.com/")
                "WhatsApp app wasn't detected. Opening WhatsApp website."
            } else if (label == "Instagram") {
                openUrl("https://www.instagram.com/")
                "Instagram app wasn't detected. Opening Instagram website instead."
            } else "Couldn't find $label installed. Check that it is installed and enabled."
        } catch (_: Exception) {
            "Couldn't open $label. Check that the app is installed and enabled."
        }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "Couldn't open link.", Toast.LENGTH_SHORT).show() }
    }

    private fun speak(message: String) {
        if (!ttsReady) {
            status.text = "REPLY SHOWN • VOICE ENGINE LOADING"
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
            status.text = "SYSTEM READY  •  TAP TO ACTIVATE"
        } else {
            status.text = "VOICE ENGINE UNAVAILABLE • CHECK TTS SETTINGS"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else status.text = "MIC PERMISSION REQUIRED FOR VOICE INPUT"
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
