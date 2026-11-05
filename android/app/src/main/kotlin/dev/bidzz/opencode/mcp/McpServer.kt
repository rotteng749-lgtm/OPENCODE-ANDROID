package dev.bidzz.opencode.mcp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * MCP (Model Context Protocol) server registry and client.
 *
 * Manages MCP servers that expose tools, resources, and prompts to the AI.
 * Servers run as child processes communicating over stdio with JSON-RPC 2.0.
 *
 * NOTE: Requires `npx`/`uvx` in PATH — provided by the Termux bootstrap
 * (pkg install nodejs / python) inside the app's proot environment, or a
 * system-wide install.
 */
data class McpServerConfig(
    val id: String,
    val name: String,
    val description: String,
    val command: String,
    val args: List<String>,
    val env: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val requiresSetup: Boolean = false,
    val setupHint: String? = null,
)

class McpServerManager(private val context: Context) {

    private val servers = mutableMapOf<String, Process>()

    /** Registry of available MCP servers. */
    val registry = listOf(
        McpServerConfig(
            id = "filesystem",
            name = "Filesystem",
            description = "Secure file operations with configurable allowed directories",
            command = "npx",
            args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/sdcard"),
            requiresSetup = true,
            setupHint = "Requires Node.js (pkg install nodejs in terminal)",
        ),
        McpServerConfig(
            id = "memory",
            name = "Memory",
            description = "Persistent knowledge-graph memory across chats",
            command = "npx",
            args = listOf("-y", "@modelcontextprotocol/server-memory"),
            requiresSetup = true,
            setupHint = "Requires Node.js",
        ),
        McpServerConfig(
            id = "sequential-thinking",
            name = "Sequential Thinking",
            description = "Dynamic and reflective problem-solving through thought sequences",
            command = "npx",
            args = listOf("-y", "@modelcontextprotocol/server-sequential-thinking"),
            requiresSetup = true,
            setupHint = "Requires Node.js",
        ),
        McpServerConfig(
            id = "fetch",
            name = "Fetch",
            description = "Fetch web content and convert HTML to Markdown",
            command = "uvx",
            args = listOf("mcp-server-fetch"),
            requiresSetup = true,
            setupHint = "Requires Python + uv (pkg install python-uv)",
        ),
        McpServerConfig(
            id = "time",
            name = "Time",
            description = "Time and timezone conversion with IANA names",
            command = "uvx",
            args = listOf("mcp-server-time"),
            requiresSetup = true,
            setupHint = "Requires Python + uv",
        ),
        McpServerConfig(
            id = "git",
            name = "Git",
            description = "Read, search, and manipulate Git repositories",
            command = "uvx",
            args = listOf("mcp-server-git", "--repository", "/sdcard"),
            requiresSetup = true,
            setupHint = "Requires Python + uv",
        ),
        McpServerConfig(
            id = "puppeteer",
            name = "Puppeteer",
            description = "Browser automation: navigate, click, fill forms, screenshots",
            command = "npx",
            args = listOf("-y", "@modelcontextprotocol/server-puppeteer"),
            requiresSetup = true,
            setupHint = "Requires Node.js + Chromium download (~100 MB)",
        ),
        McpServerConfig(
            id = "sqlite",
            name = "SQLite",
            description = "SQLite database interaction and queries",
            command = "uvx",
            args = listOf("mcp-server-sqlite", "--db-path", "/sdcard/.opencode/data.sqlite"),
            requiresSetup = true,
            setupHint = "Requires Python + uv",
        ),
        McpServerConfig(
            id = "everything",
            name = "Everything",
            description = "Reference/test server exercising all MCP features",
            command = "npx",
            args = listOf("-y", "@modelcontextprotocol/server-everything"),
            requiresSetup = true,
            setupHint = "Requires Node.js",
        ),
    )

    /** Starts an MCP server process. Returns the process handle, or null on failure. */
    fun start(config: McpServerConfig): Process? {
        servers[config.id]?.let { if (it.isAlive) return it }

        return try {
            val pb = ProcessBuilder(listOf(config.command) + config.args)
            pb.environment().putAll(config.env)
            pb.directory(context.filesDir)
            pb.redirectErrorStream(true)
            val process = pb.start()
            servers[config.id] = process
            process
        } catch (e: Exception) {
            null
        }
    }

    /** Stops a running server. */
    fun stop(id: String) {
        servers[id]?.destroy()
        servers.remove(id)
    }

    /** Stops all servers. */
    fun stopAll() {
        servers.values.forEach { it.destroy() }
        servers.clear()
    }

    /** Checks if a server is running. */
    fun isRunning(id: String): Boolean = servers[id]?.isAlive == true

    /**
     * Sends a JSON-RPC request to a server and returns the response.
     *
     * MCP servers speak newline-delimited JSON-RPC 2.0 over stdio.
     */
    suspend fun call(
        serverId: String,
        method: String,
        params: JSONObject = JSONObject(),
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val process = servers[serverId]
            ?: return@withContext Result.failure(IllegalStateException("Server not running: $serverId"))

        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", System.currentTimeMillis())
            .put("method", method)
            .put("params", params)

        try {
            process.outputStream.write((request.toString() + "\n").toByteArray())
            process.outputStream.flush()

            val reader = process.inputStream.bufferedReader()
            val line = reader.readLine() ?: return@withContext Result.failure(
                IllegalStateException("No response from server"),
            )
            Result.success(JSONObject(line))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
