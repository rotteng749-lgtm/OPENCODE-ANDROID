package dev.bidzz.opencode.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Executes commands in a root shell (`su`).
 *
 * Provides Android-specific tooling for rooted devices:
 * - raw shell commands
 * - package manager (list, install, uninstall)
 * - activity manager (start app, broadcast intent)
 * - file read/write on protected paths
 * - process list / kill
 *
 * All commands block until completion. Root access is checked lazily on first use.
 */
class RootShell {

    /** Result of a root command. */
    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    /** Whether `su` is available on this device. */
    val isRootAvailable: Boolean
        get() = Runtime.getRuntime().exec("su -c id").waitFor() == 0

    /**
     * Executes one or more shell commands under `su`.
     *
     * @param commands Lines to execute
     * @param timeoutMs Maximum wait time (0 = no timeout)
     */
    suspend fun execute(vararg commands: String, timeoutMs: Long = 30_000): Result =
        withContext(Dispatchers.IO) {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

                commands.forEach { cmd ->
                    os.writeBytes("$cmd\n")
                    os.flush()
                }
                os.writeBytes("exit\n")
                os.flush()

                if (timeoutMs > 0) {
                    process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                } else {
                    process.waitFor()
                }

                val stdout = stdoutReader.readText()
                val stderr = stderrReader.readText()
                Result(process.exitValue(), stdout, stderr)
            } catch (e: Exception) {
                Result(-1, "", e.message ?: "Root shell failed")
            } finally {
                process?.destroy()
            }
        }

    /** Reads a file from any path (including protected ones). */
    suspend fun readFile(path: String): Result = execute("cat '$path'")

    /** Writes text to a file as root. */
    suspend fun writeFile(path: String, content: String): Result =
        execute("mkdir -p '$(dirname $path)'", "cat > '$path' << 'EOF'\n$content\nEOF")

    /** Lists installed packages. */
    suspend fun listPackages(): Result = execute("pm list packages -f")

    /** Installs an APK. */
    suspend fun installApk(path: String): Result = execute("pm install -r '$path'")

    /** Uninstalls a package. */
    suspend fun uninstallPackage(packageName: String): Result = execute("pm uninstall $packageName")

    /** Starts an activity by package/class. */
    suspend fun startActivity(packageName: String, activityClass: String? = null): Result {
        val component = if (activityClass != null) "$packageName/$activityClass" else "$packageName/.MainActivity"
        return execute("am start -n $component")
    }

    /** Sends a broadcast intent. */
    suspend fun sendBroadcast(action: String): Result = execute("am broadcast -a $action")

    /** Lists running processes. */
    suspend fun listProcesses(): Result = execute("ps -A")

    /** Kills a process by PID. */
    suspend fun killProcess(pid: Int): Result = execute("kill $pid")

    /** Kills a process by name. */
    suspend fun killByName(name: String): Result = execute("pkill -f '$name'")

    /** Taps the screen at (x, y). */
    suspend fun tap(x: Int, y: Int): Result = execute("input tap $x $y")

    /** Swipes from (x1, y1) to (x2, y2). */
    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 300): Result =
        execute("input swipe $x1 $y1 $x2 $y2 $durationMs")

    /** Inputs text via shell. */
    suspend fun inputText(text: String): Result = execute("input text '${text.replace("'", "'\\''")}'")

    /** Presses a key. */
    suspend fun keyEvent(keyCode: String): Result = execute("input keyevent $keyCode")

    /** Captures a screenshot to the given path. */
    suspend fun screenshot(path: String): Result = execute("screencap -p '$path'")

    /** Dumps the accessibility/UI hierarchy. */
    suspend fun dumpUi(path: String): Result = execute("uiautomator dump '$path'")
}
