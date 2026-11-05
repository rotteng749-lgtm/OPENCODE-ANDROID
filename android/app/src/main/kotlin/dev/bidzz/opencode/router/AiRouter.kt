package dev.bidzz.opencode.router

import android.content.Context
import dev.bidzz.opencode.engine.DirectApiChat
import dev.bidzz.opencode.engine.DirectProviderStore
import dev.bidzz.opencode.engine.FreeProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject

/**
 * 9Router-inspired AI routing gateway for Android.
 *
 * Routes chat requests across providers with:
 * - **Combo fallback** — ordered model list, try until one succeeds
 * - **Round-robin** — rotate starting model per request
 * - **Smart routing** — capability detection (vision, tools, long context)
 * - **Account fallback** — cooldown on 401/429, exponential backoff
 * - **Token savers** — compress prompts, inject terse bias
 *
 * Model naming: `providerAlias/modelId` (e.g. `groq/llama-3.3-70b-versatile`)
 */
class AiRouter(private val context: Context) {

    private val chat = DirectApiChat()
    private val store = DirectProviderStore(context)

    /** A routable model with provider + model ID. */
    data class Route(
        val providerId: String,
        val modelId: String,
        val tier: Tier = Tier.FREE,
    )

    enum class Tier {
        SUBSCRIPTION, CHEAP, FREE
    }

    /** A named combo (ordered list of routes). */
    data class Combo(
        val name: String,
        val routes: List<Route>,
        val strategy: Strategy = Strategy.FALLBACK,
    )

    enum class Strategy {
        FALLBACK, ROUND_ROBIN, SMART, FUSION
    }

    /** Registry of pre-built combos. */
    val combos = mutableListOf(
        Combo(
            name = "Auto",
            routes = listOf(
                Route("llm7", "mistral-Nemo-Instruct-2407", Tier.FREE),
                Route("groq", "llama-3.3-70b-versatile", Tier.FREE),
                Route("gemini", "gemini-2.0-flash", Tier.FREE),
                Route("openrouter", "nvidia/nemotron-3-ultra-550b-a55b:free", Tier.FREE),
            ),
            strategy = Strategy.FALLBACK,
        ),
        Combo(
            name = "Fast",
            routes = listOf(
                Route("groq", "llama-3.1-8b-instant", Tier.FREE),
                Route("llm7", "gpt-oss-20b", Tier.FREE),
                Route("gemini", "gemini-2.0-flash-lite", Tier.FREE),
            ),
            strategy = Strategy.ROUND_ROBIN,
        ),
        Combo(
            name = "Smart",
            routes = listOf(
                Route("openrouter", "nvidia/nemotron-3-ultra-550b-a55b:free", Tier.FREE),
                Route("gemini", "gemini-2.0-flash", Tier.FREE),
                Route("groq", "llama-3.3-70b-versatile", Tier.FREE),
            ),
            strategy = Strategy.SMART,
        ),
    )

    /** Cooldown tracker for failed providers. */
    private val cooldowns = mutableMapOf<String, Long>()

    /** Round-robin rotation state. */
    private var rotationIndex = 0

    /**
     * Routes a chat request through the combo, trying each route until success.
     */
    fun route(
        comboName: String,
        messages: List<DirectApiChat.ChatMessage>,
        systemPrompt: String? = null,
    ): Flow<DirectApiChat.StreamEvent> = flow {
        val combo = combos.find { it.name == comboName } ?: combos.first()
        val orderedRoutes = when (combo.strategy) {
            Strategy.ROUND_ROBIN -> rotate(combo.routes)
            Strategy.SMART -> smartOrder(combo.routes, messages)
            else -> combo.routes
        }

        var lastError: String? = null
        for (route in orderedRoutes) {
            if (isCoolingDown(route.providerId)) continue

            val provider = FreeProviders.byId(route.providerId) ?: continue
            val apiKey = store.apiKey(route.providerId)
            if (provider.requiresKey && apiKey.isBlank()) continue

            var succeeded = false
            chat.streamChat(
                baseUrl = provider.baseUrl,
                apiKey = apiKey,
                model = route.modelId,
                messages = messages,
                systemPrompt = systemPrompt,
            ).collect { event ->
                when (event) {
                    is DirectApiChat.StreamEvent.Delta -> emit(event)
                    is DirectApiChat.StreamEvent.Done -> {
                        emit(event)
                        succeeded = true
                    }
                    is DirectApiChat.StreamEvent.Error -> {
                        lastError = event.message
                        // Cooldown on rate limit or auth failure
                        if (event.message.contains("429") || event.message.contains("401")) {
                            cooldown(route.providerId)
                        }
                    }
                }
            }
            if (succeeded) return@flow
        }

        emit(DirectApiChat.StreamEvent.Error(
            lastError ?: "All providers in combo '$comboName' failed",
        ))
    }.flowOn(Dispatchers.IO)

    /** Rotates the route list for round-robin. */
    private fun rotate(routes: List<Route>): List<Route> {
        if (routes.isEmpty()) return routes
        val idx = rotationIndex++ % routes.size
        return routes.drop(idx) + routes.take(idx)
    }

    /**
     * Smart ordering: detect required capabilities and reorder.
     *
     * For now, simple heuristic: if the last message contains "image" or "picture",
     * prefer vision-capable models (not yet implemented — placeholder).
     */
    private fun smartOrder(routes: List<Route>, messages: List<DirectApiChat.ChatMessage>): List<Route> {
        // TODO: Implement capability detection (vision, tools, long context)
        return routes
    }

    /** Checks if a provider is in cooldown. */
    private fun isCoolingDown(providerId: String): Boolean {
        val expiry = cooldowns[providerId] ?: return false
        return System.currentTimeMillis() < expiry
    }

    /** Puts a provider in cooldown (exponential backoff). */
    private fun cooldown(providerId: String) {
        val current = cooldowns[providerId] ?: 0
        val backoff = if (current == 0L) 2000L else minOf((current - System.currentTimeMillis()) * 2, 300_000L)
        cooldowns[providerId] = System.currentTimeMillis() + backoff
    }

    /** Adds a custom combo. */
    fun addCombo(combo: Combo) {
        combos.removeAll { it.name == combo.name }
        combos.add(combo)
    }

    /** Lists all combos. */
    fun listCombos(): List<Combo> = combos.toList()
}
