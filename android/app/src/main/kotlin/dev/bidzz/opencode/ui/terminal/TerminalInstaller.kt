package dev.bidzz.opencode.ui.terminal

import android.content.Context
import android.system.Os
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Installs the Termux bootstrap packages under the app's files directory.
 *
 * The bootstrap binaries hardcode `/data/data/com.termux`, so they cannot run
 * directly under `dev.bidzz.opencode`. This class extracts the official
 * bootstrap zip into a staging directory, then renames it into place. The
 * shell that uses it will run inside proot with a bind mount that makes the
 * prefix look like `/data/data/com.termux/files`.
 *
 * The zip is downloaded at runtime to keep the APK small; the SHA-256 of the
 * downloaded file is verified before anything is extracted.
 */
class TerminalInstaller(private val context: Context) {

    private val filesDir: File = context.filesDir
    private val prefixDir: File = File(filesDir, "usr")
    private val stagingDir: File = File(filesDir, "usr-staging")
    private val markerFile: File = File(prefixDir, ".installed")

    sealed class State {
        object NotInstalled : State()
        object Downloading : State()
        object Extracting : State()
        object Installed : State()
        data class Failed(val reason: String) : State()
    }

    /** True when the prefix is already present and complete. */
    fun isInstalled(): Boolean =
        prefixDir.isDirectory && File(prefixDir, "bin/sh").exists() && markerFile.exists()

    /**
     * Downloads and extracts the bootstrap if needed.
     *
     * @return the directory that contains `usr/`, or null on failure.
     */
    suspend fun installIfNeeded(progress: (State) -> Unit): File? = withContext(Dispatchers.IO) {
        if (isInstalled()) {
            progress(State.Installed)
            return@withContext filesDir
        }

        progress(State.Downloading)

        val abi = pickAbi()
        val url = "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.02.12-r1%2Bapt.android-7/bootstrap-$abi.zip"
        val sha256 = when (abi) {
            "aarch64" -> "ea2aeba8819e517db711f8c32369e89e7c52cee73e07930ff91185e1ab93f4f3"
            "x86_64" -> "b7fd0f2e3a4de534be3144f9f91acc768630fc463eaf134ab2e64c545e834f7a"
            else -> error("unsupported ABI: $abi")
        }

        val zipFile = File(context.cacheDir, "bootstrap-$abi.zip")
        try {
            download(url, zipFile)
            val actual = sha256(zipFile)
            if (actual != sha256) {
                progress(State.Failed("SHA-256 mismatch for $abi: expected $sha256, got $actual"))
                return@withContext null
            }
        } catch (e: Exception) {
            progress(State.Failed("download failed: ${e.message}"))
            return@withContext null
        }

        progress(State.Extracting)

        try {
            deleteRecursively(stagingDir)
            deleteRecursively(prefixDir)
            stagingDir.mkdirs()

            val symlinks = mutableListOf<Pair<String, String>>()
            extractZip(zipFile, stagingDir, symlinks)
            for ((target, link) in symlinks) {
                Os.symlink(target, link)
            }

            if (!stagingDir.renameTo(prefixDir)) {
                progress(State.Failed("could not move staging into place"))
                return@withContext null
            }

            // Marker lets isInstalled() distinguish a partial install from a real one.
            markerFile.writeText(abi)
            progress(State.Installed)
            filesDir
        } catch (e: Exception) {
            progress(State.Failed("extract failed: ${e.message}"))
            deleteRecursively(stagingDir)
            null
        } finally {
            zipFile.delete()
        }
    }

    private fun pickAbi(): String {
        val supported = android.os.Build.SUPPORTED_ABIS
        return when {
            supported.contains("arm64-v8a") -> "aarch64"
            supported.contains("x86_64") -> "x86_64"
            else -> throw IllegalStateException("device ABI not supported: ${supported.joinToString()}")
        }
    }

    private fun download(url: String, out: File) {
        URL(url).openStream().use { input ->
            FileOutputStream(out).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(file.inputStream()).use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Extracts the bootstrap zip into [dest] and returns the symlinks that
     * must be created afterwards.
     *
     * The zip contains a special entry `SYMLINKS.txt` whose lines are
     * `target←path`. The targets are absolute Termux paths; the paths are
     * relative to the prefix. We extract everything except that entry, then
     * create the symlinks so the archive can be unpacked atomically.
     */
    private fun extractZip(zipFile: File, dest: File, symlinks: MutableList<Pair<String, String>>) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "SYMLINKS.txt") {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    for (line in text.lineSequence()) {
                        if (line.isBlank()) continue
                        val parts = line.split("←")
                        if (parts.size != 2) throw IllegalStateException("bad symlink line: $line")
                        val target = parts[0]
                        val link = File(dest, parts[1]).absolutePath
                        File(link).parentFile?.mkdirs()
                        symlinks += target to link
                    }
                } else {
                    val target = File(dest, name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output ->
                            zip.copyTo(output)
                        }
                        // Make the bits in bin/, libexec and apt helpers executable.
                        if (name.startsWith("bin/") || name.startsWith("libexec") ||
                            name.startsWith("lib/apt/apt-helper") || name.startsWith("lib/apt/methods")) {
                            Os.chmod(target.absolutePath, 0b111000000)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}
