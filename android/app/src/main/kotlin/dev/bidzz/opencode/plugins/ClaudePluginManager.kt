package dev.bidzz.opencode.plugins

import android.content.Context
import dev.bidzz.opencode.engine.DirectApiChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Claude plugin registry and installer.
 *
 * Maintains a catalog of Claude Code / Claude for Desktop plugins.
 * Plugins are stored under [context.filesDir]/plugins and loaded as simple
 * JSON manifests describing slash commands and prompt templates.
 */
class ClaudePluginManager(private val context: Context) {

    private val pluginsDir get() = File(context.filesDir, "plugins").apply { mkdirs() }

    data class Plugin(
        val id: String,
        val name: String,
        val description: String,
        val author: String,
        val sourceUrl: String?,
        val commands: List<SlashCommand>,
        val installed: Boolean = false,
    )

    data class SlashCommand(
        val name: String,
        val description: String,
        val template: String,
    )

    /** Built-in registry of known official + community plugins. */
    val registry = listOf(
        Plugin(
            id = "ui-ux-pro-max",
            name = "UI/UX Pro Max",
            description = "Premium UI/UX design intelligence with palettes, fonts, GSAP presets",
            author = "Community",
            sourceUrl = "https://github.com/anthropics/claude-plugins-official/tree/main/ui-ux-pro-max",
            commands = listOf(
                SlashCommand(
                    name = "/design",
                    description = "Get design direction for a page or component",
                    template = "You are a premium UI/UX designer. Give design direction for: {{topic}}. Include palette, typography, layout, and animation ideas.",
                ),
            ),
        ),
        Plugin(
            id = "frontend-design",
            name = "Frontend Design",
            description = "Anthropic official frontend design plugin",
            author = "Anthropic",
            sourceUrl = "https://github.com/anthropics/claude-plugins-official/tree/main/frontend-design",
            commands = listOf(
                SlashCommand(
                    name = "/frontend",
                    description = "Generate frontend code with design intent",
                    template = "Design and implement frontend for: {{topic}}. Use modern HTML/CSS/JS or the project's framework.",
                ),
            ),
        ),
        Plugin(
            id = "codebase-search",
            name = "Codebase Search",
            description = "Semantic and grep-based codebase search",
            author = "Community",
            sourceUrl = "https://github.com/anthropics/claude-plugins-official/tree/main/codebase-search",
            commands = listOf(
                SlashCommand(
                    name = "/search",
                    description = "Search the codebase",
                    template = "Find code related to: {{topic}}. Search files, functions, and comments.",
                ),
            ),
        ),
        Plugin(
            id = "git-expert",
            name = "Git Expert",
            description = "Advanced Git workflows and commit message generation",
            author = "Community",
            sourceUrl = "https://github.com/anthropics/claude-plugins-official/tree/main/git-expert",
            commands = listOf(
                SlashCommand(
                    name = "/commit",
                    description = "Generate a commit message",
                    template = "Analyze the staged diff and write a concise Conventional Commits message.",
                ),
            ),
        ),
        Plugin(
            id = "docker-helper",
            name = "Docker Helper",
            description = "Dockerfile and docker-compose generation",
            author = "Community",
            sourceUrl = "https://github.com/anthropics/claude-plugins-official/tree/main/docker-helper",
            commands = listOf(
                SlashCommand(
                    name = "/docker",
                    description = "Create Docker files",
                    template = "Create a Dockerfile and docker-compose.yml for: {{topic}}.",
                ),
            ),
        ),
    )

    /** Lists installed plugins. */
    fun installedPlugins(): List<Plugin> {
        return pluginsDir.listFiles { _, name -> name.endsWith(".json") }?.mapNotNull { file ->
            parsePlugin(file.readText(), installed = true)
        } ?: emptyList()
    }

    /** Installs a plugin from the registry by id. */
    suspend fun installFromRegistry(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        val plugin = registry.find { it.id == pluginId } ?: return@withContext false
        install(plugin)
    }

    /** Installs a plugin from a GitHub repository URL. */
    suspend fun installFromGitHub(repoUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Normalize GitHub URL to raw plugin.json
            val normalized = repoUrl.trimEnd('/')
            val rawUrl = when {
                normalized.contains("raw.githubusercontent.com") -> normalized
                normalized.contains("github.com") -> normalized
                    .replace("github.com", "raw.githubusercontent.com")
                    .replace("/tree/", "/") + "/main/plugin.json"
                else -> repoUrl
            }
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(rawUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false
            val body = response.body?.string() ?: return@withContext false
            val plugin = parsePlugin(body, installed = false) ?: return@withContext false
            install(plugin.copy(sourceUrl = repoUrl))
        } catch (e: Exception) {
            false
        }
    }

    /** Installs a plugin from a local JSON string. */
    fun installFromJson(json: String): Boolean {
        val plugin = parsePlugin(json, installed = false) ?: return false
        return install(plugin)
    }

    /** Removes an installed plugin. */
    fun uninstall(pluginId: String): Boolean {
        return File(pluginsDir, "$pluginId.json").delete()
    }

    /** Applies a slash command and returns the enriched message. */
    fun applyCommand(pluginId: String? = null, commandName: String, args: String): String? {
        val plugin = if (pluginId != null) {
            installedPlugins().find { it.id == pluginId }
        } else {
            installedPlugins().find { p -> p.commands.any { it.name == commandName } }
        } ?: return null
        val command = plugin.commands.find { it.name == commandName } ?: return null
        return command.template.replace("{{topic}}", args).replace("{{args}}", args)
    }

    /** Serializes plugin list to JSON. */
    fun toJson(plugins: List<Plugin>): JSONArray {
        val array = JSONArray()
        plugins.forEach { plugin ->
            array.put(JSONObject().apply {
                put("id", plugin.id)
                put("name", plugin.name)
                put("description", plugin.description)
                put("author", plugin.author)
                put("sourceUrl", plugin.sourceUrl ?: "")
                put("installed", plugin.installed)
                val commandsArray = JSONArray()
                plugin.commands.forEach { cmd ->
                    commandsArray.put(JSONObject().apply {
                        put("name", cmd.name)
                        put("description", cmd.description)
                        put("template", cmd.template)
                    })
                }
                put("commands", commandsArray)
            })
        }
        return array
    }

    private fun install(plugin: Plugin): Boolean {
        return try {
            val file = File(pluginsDir, "${plugin.id}.json")
            file.writeText(toJson(listOf(plugin)).getJSONObject(0).toString(2))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parsePlugin(json: String, installed: Boolean): Plugin? {
        return try {
            val obj = JSONObject(json)
            val commandsArray = obj.optJSONArray("commands") ?: JSONArray()
            val commands = mutableListOf<SlashCommand>()
            for (i in 0 until commandsArray.length()) {
                val cmd = commandsArray.getJSONObject(i)
                commands.add(
                    SlashCommand(
                        name = cmd.optString("name"),
                        description = cmd.optString("description"),
                        template = cmd.optString("template"),
                    ),
                )
            }
            Plugin(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name"),
                description = obj.optString("description"),
                author = obj.optString("author"),
                sourceUrl = obj.optString("sourceUrl").takeIf { it.isNotBlank() },
                commands = commands,
                installed = installed,
            )
        } catch (e: Exception) {
            null
        }
    }
}
