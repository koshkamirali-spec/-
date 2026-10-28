package com.risefit.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the user's own RiseAI intermediary server. The AI key lives only on that server;
 * this app never contains or sees it.
 */
object AiClient {

    class AiException(message: String) : Exception(message)

    /** [history] is a list of (role, text) with role "user" or "assistant", the last one being the new question. */
    suspend fun ask(url: String, token: String, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            if (!url.startsWith("https://")) throw AiException("آدرس سرور باید با https:// شروع شود")
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 15_000
                conn.readTimeout = 45_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (token.isNotBlank()) conn.setRequestProperty("X-RiseFit-Token", token)

                val msgs = JSONArray()
                for ((role, content) in history) {
                    msgs.put(JSONObject().put("role", role).put("content", content))
                }
                val body = JSONObject().put("app", "risefit").put("messages", msgs).toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.use { String(it.readBytes(), Charsets.UTF_8) } ?: ""
                if (code == 401 || code == 403) throw AiException("رمز دسترسی سرور درست نیست")
                if (code !in 200..299) throw AiException("سرور خطا داد ($code)")
                val reply = JSONObject(text).optString("reply", "")
                if (reply.isBlank()) throw AiException("جواب خالی بود")
                reply
            } finally {
                conn.disconnect()
            }
        }
}
