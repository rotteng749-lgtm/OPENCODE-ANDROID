package dev.bidzz.opencode.engine

import android.content.Context

/**
 * Persists direct-API provider configuration in SharedPreferences.
 *
 * Direct mode bypasses the OpenCode engine, so the engine's own credential store
 * is not available. Keys are stored per-provider, plus the active provider/model
 * selection and the per-session system prompt.
 */
class DirectProviderStore(context: Context) {

    companion object {
        private const val PREFS = "direct_providers"
        private const val KEY_ACTIVE_PROVIDER = "active_provider"
        private const val KEY_ACTIVE_MODEL = "active_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_PREFIX_API_KEY = "apikey_"
        private const val KEY_ROUTER_ENABLED = "router_enabled"
        private const val KEY_ACTIVE_COMBO = "active_combo"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Currently selected provider id (e.g. "openrouter"), or null. */
    var activeProviderId: String?
        get() = prefs.getString(KEY_ACTIVE_PROVIDER, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_PROVIDER, value).apply()

    /** Currently selected model id, or null. */
    var activeModelId: String?
        get() = prefs.getString(KEY_ACTIVE_MODEL, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_MODEL, value).apply()

    /** API key for a provider. Empty string = not set. */
    fun apiKey(providerId: String): String =
        prefs.getString(KEY_PREFIX_API_KEY + providerId, "") ?: ""

    fun setApiKey(providerId: String, key: String) {
        prefs.edit().putString(KEY_PREFIX_API_KEY + providerId, key).apply()
    }

    /** Whether the given provider has a usable key (or needs none). */
    fun isReady(providerId: String): Boolean {
        val provider = FreeProviders.byId(providerId) ?: return false
        return !provider.requiresKey || apiKey(providerId).isNotBlank()
    }

    /** Global system prompt used by direct-API sessions. */
    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    /** Whether direct-API chat goes through the AI router (combo) or a fixed model. */
    var routerEnabled: Boolean
        get() = prefs.getBoolean(KEY_ROUTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ROUTER_ENABLED, value).apply()

    /** Selected routing combo name (Auto/Fast/Smart). */
    var activeCombo: String
        get() = prefs.getString(KEY_ACTIVE_COMBO, "Auto") ?: "Auto"
        set(value) = prefs.edit().putString(KEY_ACTIVE_COMBO, value).apply()

    /** Provider ids the user has configured a key for (or that need none). */
    fun configuredProviders(): List<FreeProvider> =
        FreeProviders.all.filter { isReady(it.id) }

    /** The active provider if fully configured, else the first configured one. */
    fun activeProvider(): FreeProvider? {
        val id = activeProviderId
        if (id != null && isReady(id)) return FreeProviders.byId(id)
        return configuredProviders().firstOrNull()
    }

    /** The active model, falling back to the provider's first model. */
    fun activeModel(provider: FreeProvider): FreeModel {
        val id = activeModelId
        return provider.models.find { it.id == id } ?: provider.models.first()
    }

    /** True when at least one provider is ready to chat. */
    fun hasUsableProvider(): Boolean = configuredProviders().isNotEmpty()

    private val DEFAULT_SYSTEM_PROMPT =
        "You are OpenCode, an AI coding assistant running on Android. " +
        "Be concise, helpful, and direct. Answer in the user's language."
}
