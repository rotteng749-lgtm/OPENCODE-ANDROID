package dev.bidzz.opencode.root

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Browser and app automation for rooted devices.
 *
 * Uses `su` + `input` + `uiautomator dump` to inspect and control any app
 * without relying on the Accessibility Service (which requires user approval
 * and cannot see all windows on some ROMs).
 */
class BrowserAutomation(
    private val context: Context,
    private val root: RootShell = RootShell(),
) {

    data class UiNode(
        val bounds: String,
        val text: String?,
        val contentDesc: String?,
        val resourceId: String?,
        val className: String?,
        val clickable: Boolean,
    )

    /** Launches a URL in the default browser. */
    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Launches an app by package name. */
    fun openApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent != null) context.startActivity(intent)
    }

    /** Dumps the current UI hierarchy to a temporary file and parses it. */
    suspend fun dumpUi(): List<UiNode> = withContext(Dispatchers.IO) {
        val path = "/sdcard/opencode_ui_dump.xml"
        val result = root.dumpUi(path)
        if (result.exitCode != 0) return@withContext emptyList()

        val file = File(path)
        if (!file.exists()) return@withContext emptyList()

        parseNodes(file.readText())
    }

    /** Parses the XML dump into UI nodes. */
    private fun parseNodes(xml: String): List<UiNode> {
        val nodes = mutableListOf<UiNode>()
        val regex = Regex("<node ([^>]+)/?>")
        regex.findAll(xml).forEach { match ->
            val attrs = parseAttributes(match.groupValues[1])
            nodes.add(
                UiNode(
                    bounds = attrs["bounds"] ?: "",
                    text = attrs["text"],
                    contentDesc = attrs["content-desc"],
                    resourceId = attrs["resource-id"],
                    className = attrs["class"],
                    clickable = attrs["clickable"] == "true",
                ),
            )
        }
        return nodes
    }

    private fun parseAttributes(text: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""([\\w\\-]+)="([^"]*)": """)
        regex.findAll(text).forEach {
            map[it.groupValues[1]] = it.groupValues[2]
        }
        return map
    }

    /** Taps the center of a node matching the predicate. */
    suspend fun tap(predicate: (UiNode) -> Boolean): Boolean {
        val node = dumpUi().find(predicate) ?: return false
        val (x, y) = centerOf(node.bounds) ?: return false
        return root.tap(x, y).exitCode == 0
    }

    /** Taps a node by text. */
    suspend fun tapByText(text: String): Boolean = tap {
        it.text?.contains(text, ignoreCase = true) == true ||
            it.contentDesc?.contains(text, ignoreCase = true) == true
    }

    /** Swipes the screen. */
    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 300): Boolean =
        root.swipe(x1, y1, x2, y2, durationMs).exitCode == 0

    /** Types text into the focused field. */
    suspend fun type(text: String): Boolean = root.inputText(text).exitCode == 0

    /** Presses the back key. */
    suspend fun back(): Boolean = root.keyEvent("KEYCODE_BACK").exitCode == 0

    /** Presses the home key. */
    suspend fun home(): Boolean = root.keyEvent("KEYCODE_HOME").exitCode == 0

    /** Takes a screenshot and returns the file path. */
    suspend fun screenshot(): String {
        val path = "/sdcard/opencode_screenshot.png"
        root.screenshot(path)
        return path
    }

    /** Returns the center point of a bounds string `[x1,y1][x2,y2]`. */
    fun centerOf(bounds: String): Pair<Int, Int>? {
        val regex = Regex("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")
        val match = regex.find(bounds) ?: return null
        val (x1, y1, x2, y2) = match.destructured
        return Pair((x1.toInt() + x2.toInt()) / 2, (y1.toInt() + y2.toInt()) / 2)
    }

    /** Serializes UI nodes to JSON for the AI. */
    fun nodesToJson(nodes: List<UiNode>): JSONArray {
        val array = JSONArray()
        nodes.forEach { node ->
            array.put(JSONObject().apply {
                put("bounds", node.bounds)
                put("text", node.text ?: "")
                put("contentDesc", node.contentDesc ?: "")
                put("resourceId", node.resourceId ?: "")
                put("class", node.className ?: "")
                put("clickable", node.clickable)
            })
        }
        return array
    }
}
