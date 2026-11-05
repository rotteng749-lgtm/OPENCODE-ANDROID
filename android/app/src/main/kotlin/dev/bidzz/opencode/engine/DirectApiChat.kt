package dev.bidzz.opencode.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Direct API chat client — bypasses the OpenCode engine entirely.
 * Used when libopencode.so is missing or the engine fails to start.
 * Streams responses via SSE from OpenAI-compatible endpoints.
 *
 * Uses org.json (built into Android) because the app module does not depend on
 * kotlinx.serialization.
 */
class DirectApiChat(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build(),
) {

    data class ChatMessage(
        val role: String, // "system", "user", "assistant"
        val content: String,
    )

    sealed class StreamEvent {
        data class Delta(val text: String) : StreamEvent()
        data class Done(val fullText: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    /**
     * Sends a chat completion request and streams the response.
     *
     * @param baseUrl OpenAI-compatible base URL (e.g. https://openrouter.ai/api/v1)
     * @param apiKey API key, or empty string for no-auth providers
     * @param model Model ID
     * @param messages Full conversation history
     * @param systemPrompt System prompt injected at the head of every request
     */
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String? = null,
    ): Flow<StreamEvent> = flow {
        val messagesJson = JSONArray()
        if (!systemPrompt.isNullOrBlank()) {
            messagesJson.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        messages.forEach { msg ->
            messagesJson.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }

        val requestJson = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("messages", messagesJson)

        val body = requestJson.toString()
            .toRequestBody("application/json".toMediaType())

        val requestBuilder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(body)

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        if (baseUrl.contains("openrouter.ai")) {
            requestBuilder.addHeader("HTTP-Referer", "https://github.com/BIDZZ-RAWR/OPENCODE-ANDROID")
            requestBuilder.addHeader("X-Title", "OpenCode Android")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(500) ?: "HTTP ${response.code}"
                emit(StreamEvent.Error("API error (${response.code}): $errorBody"))
                return@flow
            }

            val responseBody = response.body
            if (responseBody == null) {
                emit(StreamEvent.Error("Empty response body"))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            val fullText = StringBuilder()

            reader.use { r ->
                var line: String? = r.readLine()
                while (line != null) {
                    val l = line!!
                    line = r.readLine()
                    if (!l.startsWith("data: ")) continue
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val parsed = JSONObject(data)
                        val choices = parsed.optJSONArray("choices") ?: continue
                        val first = choices.optJSONObject(0) ?: continue
                        val delta = first.optJSONObject("delta") ?: continue
                        val content = delta.optString("content", "")
                        if (content.isEmpty()) continue

                        fullText.append(content)
                        emit(StreamEvent.Delta(content))
                    } catch (_: Exception) {
                        // Skip malformed SSE lines
                    }
                }
            }

            emit(StreamEvent.Done(fullText.toString()))
        } catch (e: Exception) {
            emit(StreamEvent.Error("Connection failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
