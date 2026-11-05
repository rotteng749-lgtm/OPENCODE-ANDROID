package dev.bidzz.opencode.skills

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Skill manager: install, update, and execute OpenCode skills.
 *
 * Skills are loaded from `SKILL.md` files and stored under
 * [context.filesDir]/skills. A skill can be added:
 * - from a GitHub repository URL
 * - from a local SKILL.md file
 * - from a downloaded ZIP
 */
class SkillManager(private val context: Context) {

    private val skillsDir get() = File(context.filesDir, "skills").apply { mkdirs() }

    data class Skill(
        val id: String,
        val name: String,
        val description: String,
        val triggers: List<String>,
        val instructions: String,
        val source: String,
        val installed: Boolean = false,
    )

    /** Built-in registry of useful skills. */
    val registry = listOf(
        Skill(
            id = "opencode",
            name = "OpenCode",
            description = "How OpenCode works, configuration, plugins, skills, troubleshooting",
            triggers = listOf("opencode", "open code", "opencode config"),
            instructions = "You are an expert on OpenCode. Answer questions about setup, agents, skills, MCP servers, and troubleshooting.",
            source = "builtin",
        ),
        Skill(
            id = "android",
            name = "Android Development",
            description = "Android Kotlin, Jetpack Compose, ADB, Gradle, and app architecture",
            triggers = listOf("android", "compose", "gradle", "adb"),
            instructions = "You are an Android development expert. Give practical Kotlin/Jetpack Compose/Gradle advice with code examples.",
            source = "builtin",
        ),
        Skill(
            id = "reverse-engineering",
            name = "Reverse Engineering",
            description = "Static analysis, disassembly, il2cpp, memory, offsets, patching",
            triggers = listOf("reverse", "il2cpp", "so", "offsets", "ghidra"),
            instructions = "You are a reverse engineering expert. Explain tools like Ghidra, IDA, rizin, il2cpp, and techniques for Android game analysis.",
            source = "builtin",
        ),
        Skill(
            id = "ui-ux-pro-max",
            name = "UI/UX Pro Max",
            description = "Premium UI/UX design direction for web and mobile",
            triggers = listOf("design", "ui", "ux", "redesign", "frontend"),
            instructions = "You are a premium UI/UX designer. Give anti-generic, high-end design direction with palette, typography, layout, and animation ideas.",
            source = "builtin",
        ),
    )

    /** Returns all installed skills. */
    fun installedSkills(): List<Skill> {
        return skillsDir.listFiles { _, name -> name.endsWith(".md") }?.mapNotNull { file ->
            parseSkillFile(file)
        } ?: emptyList()
    }

    /** Returns all available skills (built-in + installed). */
    fun allSkills(): List<Skill> {
        val installed = installedSkills().associateBy { it.id }
        return registry.map { installed[it.id] ?: it } +
            installed.values.filter { it.id !in registry.map { r -> r.id } }
    }

    /**
     * Installs a skill from a GitHub URL.
     *
     * Supports:
     * - https://github.com/user/repo (clones repo, finds SKILL.md)
     * - https://raw.githubusercontent.com/.../SKILL.md
     */
    suspend fun installFromGitHub(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rawUrl = when {
                url.contains("raw.githubusercontent.com") -> url
                url.contains("github.com") -> {
                    val normalized = url.trimEnd('/')
                        .replace("github.com", "raw.githubusercontent.com")
                        .replace("/tree/", "/")
                    if (normalized.contains("/SKILL.md")) normalized else "$normalized/main/SKILL.md"
                }
                else -> url
            }

            val client = OkHttpClient()
            val request = Request.Builder().url(rawUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false
            val body = response.body?.string() ?: return@withContext false
            saveSkill(extractSkillFromMarkdown(body, rawUrl))
        } catch (e: Exception) {
            false
        }
    }

    /** Installs a skill from a local markdown string. */
    fun installFromMarkdown(id: String, markdown: String): Boolean {
        return saveSkill(extractSkillFromMarkdown(markdown, "manual:$id", id))
    }

    /** Installs a skill from a ZIP archive containing SKILL.md. */
    suspend fun installFromZip(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("SKILL.md", ignoreCase = true)) {
                        val content = zis.bufferedReader().readText()
                        saveSkill(extractSkillFromMarkdown(content, "zip:${zipFile.name}"))
                        return@withContext true
                    }
                    entry = zis.nextEntry
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /** Removes an installed skill. */
    fun uninstall(skillId: String): Boolean {
        return File(skillsDir, "$skillId.md").delete()
    }

    /**
     * Activates skills for a given user query.
     *
     * Returns the combined system instructions for all matching skills.
     */
    fun activate(query: String): String? {
        val matches = allSkills().filter { skill ->
            skill.installed && skill.triggers.any { trigger ->
                query.contains(trigger, ignoreCase = true)
            }
        }
        if (matches.isEmpty()) return null
        return matches.joinToString("\n\n") { "--- ${it.name} ---\n${it.instructions}" }
    }

    /** Serializes skills to JSON. */
    fun toJson(skills: List<Skill>): JSONArray {
        val array = JSONArray()
        skills.forEach { skill ->
            array.put(JSONObject().apply {
                put("id", skill.id)
                put("name", skill.name)
                put("description", skill.description)
                put("triggers", JSONArray(skill.triggers))
                put("source", skill.source)
                put("installed", skill.installed)
            })
        }
        return array
    }

    private fun saveSkill(skill: Skill?): Boolean {
        if (skill == null) return false
        return try {
            File(skillsDir, "${skill.id}.md").writeText(skill.instructions)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseSkillFile(file: File): Skill? {
        return try {
            val content = file.readText()
            extractSkillFromMarkdown(content, "file:${file.name}", file.nameWithoutExtension)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractSkillFromMarkdown(
        markdown: String,
        source: String,
        fallbackId: String? = null,
    ): Skill? {
        val name = extractHeader(markdown, "# ") ?: fallbackId ?: "skill-${UUID.randomUUID()}"
        val id = fallbackId ?: name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
        val description = extractSection(markdown, "## Description", "## Triggers") ?: ""
        val triggersText = extractSection(markdown, "## Triggers", "## Instructions") ?: ""
        val triggers = triggersText.lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(name.lowercase()) }
        val instructions = extractSection(markdown, "## Instructions", null) ?: markdown
        return Skill(
            id = id,
            name = name.removePrefix("# ").trim(),
            description = description,
            triggers = triggers,
            instructions = instructions,
            source = source,
            installed = true,
        )
    }

    private fun extractHeader(text: String, prefix: String): String? {
        return text.lineSequence().firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()
    }

    private fun extractSection(text: String, start: String, end: String?): String? {
        val startIndex = text.indexOf(start)
        if (startIndex < 0) return null
        val contentStart = startIndex + start.length
        val endIndex = if (end != null) text.indexOf(end, contentStart) else -1
        val content = if (endIndex >= 0) text.substring(contentStart, endIndex) else text.substring(contentStart)
        return content.trim()
    }
}
