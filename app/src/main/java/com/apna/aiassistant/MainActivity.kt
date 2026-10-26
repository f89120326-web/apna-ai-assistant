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
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var busy = false
    private val chatHistory = JSONArray()
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var language: Spinner
    private lateinit var typed: EditText
    private lateinit var micButton: TextView
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
            setPadding(18, 14, 18, 12)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 9, 22), Color.rgb(10, 18, 38), Color.rgb(5, 8, 18))
            )
        }

        // Compact app bar
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 2, 2, 14)
        }
        val logo = TextView(this).apply {
            text = "✦"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(cyan, Color.rgb(100, 255, 210))).apply { cornerRadius = 19f }
        }
        top.addView(logo, LinearLayout.LayoutParams(48, 48))
        val nameBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 0, 0, 0)
            addView(label("APNA AI", 21f, white, true))
            addView(label("YOUR PERSONAL ASSISTANT", 9f, muted, true))
        }
        top.addView(nameBlock, LinearLayout.LayoutParams(0, -2, 1f))
        val live = TextView(this).apply {
            text = "● READY"
            textSize = 10f
            setTextColor(Color.rgb(104, 255, 190))
            setPadding(11, 8, 11, 8)
            background = rounded(Color.rgb(15, 47, 45), 30f)
        }
        top.addView(live)
        root.addView(top)

        // Main assistant card
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 16, 14, 16)
            background = rounded(Color.rgb(14, 27, 50), 24f, Color.rgb(35, 76, 111))
        }
        val orb = TextView(this).apply {
            text = "✦"
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 65, 91), Color.rgb(22, 33, 68))).apply {
                cornerRadius = 100f
                setStroke(2, Color.rgb(58, 181, 213))
            }
        }
        hero.addView(orb, LinearLayout.LayoutParams(76, 76))
        val heroText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 0, 0, 0)
            addView(label("I'm listening.", 19f, white, true))
            addView(label("Speak naturally in Punjabi, Hindi or English.", 12f, muted).apply {
                setPadding(0, 5, 0, 0)
            })
            status = label("SYSTEM READY • TAP THE MIC", 10f, cyan, true).apply {
                setPadding(0, 9, 0, 0)
            }
            addView(status)
        }
        hero.addView(heroText, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 })

        // Language selector is deliberately compact, not a full-width form field.
        val langRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 8)
        }
        langRow.addView(label("LANGUAGE", 10f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            background = rounded(Color.rgb(17, 29, 51), 14f, Color.rgb(42, 72, 103))
            setPadding(8, 0, 8, 0)
        }
        langRow.addView(language, LinearLayout.LayoutParams(190, 42))
        root.addView(langRow)

        // Chat conversation panel
        val chatHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(3, 4, 3, 9)
            addView(label("YOUR CONVERSATION", 11f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
            addView(label("AI CHAT  ✦", 9f, cyan, true))
        }
        root.addView(chatHeader)

        val chatPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 14, 12, 14)
            background = rounded(Color.rgb(9, 16, 32), 22f, Color.rgb(29, 47, 73))
        }
        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 14)
            background = rounded(Color.rgb(17, 32, 55), 18f)
        }
        welcome.addView(label("✦  APNA AI", 11f, cyan, true))
        welcome.addView(label("Sat Sri Akal! 👋\nI'm ready to help. Ask me anything or tell me what to open.", 14f, white).apply {
            setLineSpacing(4f, 1f)
            setPadding(0, 8, 0, 0)
        })
        chatPanel.addView(welcome)
        transcript = label("You: —\n\nAssistant: Hey! I'm ready. Try saying “Open Instagram” or “What time is it?”", 13f, white)
        transcript.setLineSpacing(5f, 1f)
        transcript.setPadding(14, 13, 14, 13)
        transcript.background = rounded(Color.rgb(20, 29, 48), 18f, Color.rgb(36, 58, 83))
        chatPanel.addView(transcript, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 })
        root.addView(chatPanel, LinearLayout.LayoutParams(-1, 0, 1f))

        // Useful one-tap commands
        val quickTitle = label("TRY A QUICK COMMAND", 9f, muted, true).apply {
            setPadding(3, 12, 0, 7)
        }
        root.addView(quickTitle)
        val quickScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun quickChip(caption: String, command: String) {
            val chip = TextView(this).apply {
                text = caption
                textSize = 12f
                setTextColor(white)
                gravity = Gravity.CENTER
                setPadding(15, 11, 15, 11)
                background = rounded(Color.rgb(18, 34, 57), 24f, Color.rgb(39, 75, 105))
                setOnClickListener { handleCommand(command) }
            }
            quickRow.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = 8 })
        }
        quickChip("◉  Time", "What time is it?")
        quickChip("▣  Instagram", "Open Instagram")
        quickChip("▶  YouTube", "Open YouTube")
        quickChip("⌕  Search", "Search Google")
        quickScroll.addView(quickRow)
        root.addView(quickScroll)

        // Chat-style composer anchored to the bottom
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 7, 7, 7)
            background = rounded(Color.rgb(13, 23, 42), 25f, Color.rgb(42, 67, 95))
        }
        typed = EditText(this).apply {
            hint = "Message APNA AI…"
            textSize = 14f
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
            setPadding(10, 10, 6, 10)
            background = null
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    val cmd = text.toString().trim()
                    if (cmd.isNotEmpty()) handleCommand(cmd)
                    true
                } else false
            }
        }
        composer.addView(typed, LinearLayout.LayoutParams(0, 48, 1f))
        val send = TextView(this).apply {
            text = "➤"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(cyan, Color.rgb(104, 255, 210))).apply { cornerRadius = 20f }
            setOnClickListener {
                val cmd = typed.text.toString().trim()
                if (cmd.isNotEmpty()) handleCommand(cmd) else status.text = "TYPE A MESSAGE FIRST"
            }
        }
        composer.addView(send, LinearLayout.LayoutParams(46, 46).apply { leftMargin = 5 })
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 })

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 9, 2, 0)
        }
        val privacy = label("🔒 PRIVATE BY DESIGN • MIC ONLY WHEN TAPPED", 9f, muted, true)
        bottomRow.addView(privacy, LinearLayout.LayoutParams(0, -2, 1f))
        micButton = TextView(this).apply {
            text = "🎙  SPEAK"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(13, 9, 13, 9)
            background = rounded(Color.rgb(14, 42, 59), 24f, Color.rgb(44, 136, 161))
            setOnClickListener { startListening() }
        }
        bottomRow.addView(micButton)
        root.addView(bottomRow)

        setContentView(root)
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
        if (command.contains("developer") || command.contains("who made you") || command.contains("who created you") ||
            command.contains("ਕਿਸ ਨੇ ਬਣਾਇਆ") || command.contains("ਕਿਸਨੇ ਬਣਾਇਆ") || command.contains("डेवलपर") || command.contains("किसने बनाया")) {
            transcript.text = "YOU  /  " + raw + "\n\nAPNA AI  /  Pindu-Tiwana"
            speak("Pindu-Tiwana")
            status.text = "SYSTEM READY • APNA AI"
            return
        }
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
            else -> { askAI(raw); return }
        }
        transcript.text = "YOU  /  $raw\n\nAPNA AI  /  $reply"
        status.text = "SYSTEM READY  •  TAP TO ACTIVATE"
        speak(reply)
    }

    private fun askAI(raw: String) {
        if (busy) return
        busy = true
        status.text = "✦ THINKING…"
        transcript.text = "YOU  /  " + raw + "\n\nAPNA AI  /  ਸੋਚ ਰਿਹਾ ਹਾਂ…"
        val history = JSONArray()
        val start = (chatHistory.length() - 8).coerceAtLeast(0)
        for (i in start until chatHistory.length()) history.put(chatHistory.getJSONObject(i))
        AiClient.ask(raw, locales[language.selectedItemPosition], history) { answer, error ->
            runOnUiThread {
                busy = false
                if (answer != null) {
                    chatHistory.put(JSONObject().put("role", "user").put("content", raw))
                    chatHistory.put(JSONObject().put("role", "assistant").put("content", answer))
                    while (chatHistory.length() > 12) chatHistory.remove(0)
                    transcript.text = "YOU  /  " + raw + "\n\nAPNA AI  /  " + answer
                    status.text = "SYSTEM READY • APNA AI"
                    typed.setText("")
                    speak(answer)
                } else {
                    status.text = "CONNECTION ISSUE • TRY AGAIN"
                    transcript.text = "YOU  /  " + raw + "\n\nAPNA AI  /  I couldn't connect. Check internet and try again. " + (error ?: "")
                }
            }
        }
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
            val punjabi = Locale.forLanguageTag("pa-IN")
            val available = tts?.isLanguageAvailable(punjabi) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (available >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = punjabi
                status.text = "PUNJABI VOICE READY"
            } else {
                tts?.language = Locale("en", "IN")
                status.text = "READY • VOICE DEPENDS ON PHONE"
            }
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
