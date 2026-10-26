package com.apna.aiassistant

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    private const val ENDPOINT = "https://apna-ai-chat-api.apna-ai-f89120326.workers.dev/api/chat"

    fun ask(message: String, language: String, history: JSONArray, callback: (String?, String?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val body = JSONObject()
                    .put("message", message)
                    .put("language", language)
                    .put("history", history)
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 18000
                    readTimeout = 45000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val result = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(result)
                if (code !in 200..299) throw Exception(json.optString("error", "AI service error " + code))
                val answer = json.optString("response", json.optString("reply", "")).trim()
                if (answer.isEmpty()) throw Exception("AI returned an empty answer")
                callback(answer, null)
            } catch (error: Exception) {
                callback(null, error.message ?: "Connection failed")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}