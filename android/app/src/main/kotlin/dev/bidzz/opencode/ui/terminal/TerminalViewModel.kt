package dev.bidzz.opencode.ui.terminal

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.system.Os
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the state for one or more terminal sessions.
 *
 * Each session is a real PTY running inside proot so the Termux userland sees
 * the paths it expects. The ViewModel owns installation of the bootstrap and
 * the lifecycle of the sessions; the screen owns the view and forwards input.
 */
class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    data class Session(
        val id: String,
        val name: String,
        val handle: String,
        val shell: String,
        val isFallback: Boolean,
    )

    data class UiState(
        val installState: TerminalInstaller.State = TerminalInstaller.State.NotInstalled,
        val sessions: List<Session> = emptyList(),
        val selectedId: String? = null,
        val canPaste: Boolean = false,
        val showExtraKeys: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val installer = TerminalInstaller(application)
    private val manager = TerminalSessionManager(application, installer)

    init {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        _state.value = _state.value.copy(canPaste = clipboard.hasPrimaryClip())

        viewModelScope.launch {
            manager.state.collect { sessions ->
                val selected = _state.value.selectedId ?: sessions.firstOrNull()?.id
                _state.value = _state.value.copy(
                    sessions = sessions,
                    selectedId = selected,
                )
            }
        }

        viewModelScope.launch {
            manager.installState.collect { installState ->
                _state.value = _state.value.copy(installState = installState)
            }
        }
    }

    /** Called when the screen first appears. Kicks off bootstrap install if needed. */
    fun onScreenShown() {
        viewModelScope.launch {
            manager.ensureReady()
        }
    }

    fun retryInstall() {
        viewModelScope.launch {
            manager.ensureReady(force = true)
        }
    }

    fun newSession() {
        manager.createSession()
    }

    fun selectSession(id: String) {
        _state.value = _state.value.copy(selectedId = id)
    }

    fun renameSession(id: String, name: String) {
        manager.renameSession(id, name)
    }

    fun closeSession(id: String) {
        manager.closeSession(id)
    }

    fun attach(view: TerminalView, id: String) {
        manager.attach(view, id)
    }

    fun paste(text: String) {
        manager.paste(text)
    }

    fun copyFromTerminal(): String = manager.copySelection()

    fun toggleExtraKeys() {
        _state.value = _state.value.copy(showExtraKeys = !_state.value.showExtraKeys)
    }

    fun sendControlKey(key: String) {
        manager.sendControlKey(key)
    }

    fun onClipboardChanged() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        _state.value = _state.value.copy(canPaste = clipboard.hasPrimaryClip())
    }

    fun onBell() {
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    override fun onCleared() {
        super.onCleared()
        manager.destroy()
    }
}

/**
 * Owns the actual [TerminalSession] objects, their proot command lines, and the
 * bindings to [TerminalView].
 */
class TerminalSessionManager(
    private val context: Context,
    private val installer: TerminalInstaller,
) {

    private data class ManagedSession(
        val id: String,
        var name: String,
        val session: TerminalSession,
        val client: CompositeTerminalClient,
    )

    private val _state = MutableStateFlow<List<TerminalViewModel.Session>>(emptyList())
    val state: StateFlow<List<TerminalViewModel.Session>> = _state

    private val _installState = MutableStateFlow<TerminalInstaller.State>(TerminalInstaller.State.NotInstalled)
    val installState: StateFlow<TerminalInstaller.State> = _installState

    private val handler = Handler(Looper.getMainLooper())
    private val sessions = mutableListOf<ManagedSession>()
    private var nextIndex = 1

    init {
        extractProotAssets()
    }

    /**
     * Ensures the Termux bootstrap is installed and creates an initial session.
     * Safe to call multiple times; if already installed it is a no-op.
     */
    suspend fun ensureReady(force: Boolean = false) {
        if (installer.isInstalled() && !force) {
            _installState.value = TerminalInstaller.State.Installed
            if (sessions.isEmpty()) createSession(File(context.filesDir, "usr"))
            return
        }
        val prefix = withContext(Dispatchers.IO) {
            installer.installIfNeeded { _installState.value = it }
        }
        if (sessions.isEmpty()) createSession(prefix)
    }

    fun createSession(prefix: File? = null) {
        val fallback = prefix == null
        val root = prefix ?: File(context.filesDir, "usr")
        val args = if (fallback) {
            arrayOf("-sh")
        } else {
            buildProotArgs(context.filesDir)
        }
        val shell = if (fallback) "/system/bin/sh" else prootBinary().absolutePath

        // proot's -w flag points at the *guest* path, so the home directory must
        // exist inside the bind-mounted filesDir before the session starts.
        File(context.filesDir, "home").mkdirs()

        val client = CompositeTerminalClient()
        val session = TerminalSession(
            shell,
            if (fallback) "/" else "/data/data/com.termux/files/home",
            args,
            buildEnv(root, fallback).toTypedArray(),
            TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            client,
        )

        val id = UUID.randomUUID().toString()
        val name = context.getString(dev.bidzz.opencode.R.string.terminal_session_unnamed, nextIndex++)
        client.session = session
        sessions += ManagedSession(id, name, session, client)
        emit()

        // Initialise with a default size so the first frame appears before a view attaches.
        session.updateSize(80, 24, 12, 24)
    }

    fun renameSession(id: String, name: String) {
        sessions.find { it.id == id }?.let {
            it.name = name
            emit()
        }
    }

    fun closeSession(id: String) {
        val index = sessions.indexOfFirst { it.id == id }
        if (index == -1) return
        val managed = sessions.removeAt(index)
        managed.session.finishIfRunning()
        emit()
    }

    fun attach(view: TerminalView, id: String) {
        val managed = sessions.find { it.id == id } ?: return
        managed.client.view = view
        view.attachSession(managed.session)
        view.setTerminalViewClient(managed.client)
    }

    fun paste(text: String) {
        val view = sessions.lastOrNull()?.client?.view
        val emulator = view?.mEmulator
        if (emulator != null) {
            emulator.paste(text)
        } else {
            val bytes = text.toByteArray(Charsets.UTF_8)
            sessions.lastOrNull()?.session?.write(bytes, 0, bytes.size)
        }
    }

    fun copySelection(): String {
        return sessions.lastOrNull()?.client?.view?.getSelectedText().orEmpty()
    }

    fun sendControlKey(key: String) {
        val view = sessions.lastOrNull()?.client?.view ?: return
        val session = sessions.lastOrNull()?.session ?: return

        when (key) {
            "ESC" -> session.write(byteArrayOf(27), 0, 1)
            "TAB" -> session.write(byteArrayOf(9), 0, 1)
            "UP" -> view.handleKeyCode(KeyEvent.KEYCODE_DPAD_UP, 0)
            "DOWN" -> view.handleKeyCode(KeyEvent.KEYCODE_DPAD_DOWN, 0)
            "LEFT" -> view.handleKeyCode(KeyEvent.KEYCODE_DPAD_LEFT, 0)
            "RIGHT" -> view.handleKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT, 0)
            "PGUP" -> view.handleKeyCode(KeyEvent.KEYCODE_PAGE_UP, 0)
            "PGDN" -> view.handleKeyCode(KeyEvent.KEYCODE_PAGE_DOWN, 0)
            "HOME" -> view.handleKeyCode(KeyEvent.KEYCODE_MOVE_HOME, 0)
            "END" -> view.handleKeyCode(KeyEvent.KEYCODE_MOVE_END, 0)
            "BKSP" -> view.handleKeyCode(KeyEvent.KEYCODE_DEL, 0)
            "DEL" -> view.handleKeyCode(KeyEvent.KEYCODE_FORWARD_DEL, 0)
            "CTRL", "ALT" -> {
                // Modifier keys are handled by the extra-keys bar state; nothing to send yet.
            }
            else -> {
                if (key.length == 1) {
                    session.writeCodePoint(false, key[0].code)
                }
            }
        }
    }

    fun destroy() {
        sessions.toList().forEach { it.session.finishIfRunning() }
        sessions.clear()
    }

    private fun prootBinary(): File = File(prootDir(), "proot")

    private fun prootDir(): File = File(context.filesDir, "proot-${abiName()}")

    private fun buildProotArgs(realFiles: File): Array<String> {
        val login = File(File(realFiles, "usr"), "bin/login").absolutePath
        return arrayOf(
            "--link2symlink",
            "-0",
            "-r", "/",
            "-b", "${realFiles.absolutePath}:/data/data/com.termux/files",
            "-w", "/data/data/com.termux/files/home",
            "--kill-on-exit",
            login,
        )
    }

    private fun buildEnv(root: File, fallback: Boolean): List<String> {
        if (fallback) {
            return listOf(
                "PATH=/system/bin:/vendor/bin",
                "HOME=/",
                "TERM=xterm-256color",
            )
        }

        val dir = prootDir()
        val home = "/data/data/com.termux/files/home"
        val prefix = "/data/data/com.termux/files/usr"
        val tmp = "/data/data/com.termux/files/usr/tmp"
        val prootTmp = File(context.cacheDir, "proot").apply { mkdirs() }

        return listOf(
            "HOME=$home",
            "PREFIX=$prefix",
            "TMPDIR=$tmp",
            "PATH=$prefix/bin:$prefix/bin/applets",
            "TERMUX_VERSION=0.119.0",
            "TERMUX_APP_PACKAGE_MANAGER=apt",
            "TERMUX_MAIN_PACKAGE_FORMAT=debian",
            "LD_LIBRARY_PATH=${dir.absolutePath}",
            "PROOT_LOADER=${dir.absolutePath}/loader",
            "PROOT_LOADER_32=${dir.absolutePath}/loader32",
            "PROOT_TMP_DIR=${prootTmp.absolutePath}",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
        )
    }

    private fun abiName(): String = when {
        android.os.Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "aarch64"
        android.os.Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
        else -> throw IllegalStateException("device ABI not supported: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun extractProotAssets() {
        val abi = abiName()
        val dest = File(context.filesDir, "proot-$abi")
        if (File(dest, "proot").exists()) return

        dest.mkdirs()
        val assetDir = "terminal/$abi"
        val files = listOf("proot", "loader", "loader32", "libtalloc.so.2", "libandroid-shmem.so")
        for (name in files) {
            context.assets.open("$assetDir/$name").use { input ->
                FileOutputStream(File(dest, name)).use { output ->
                    input.copyTo(output)
                }
            }
            File(dest, name).setExecutable(true, false)
        }
    }

    private fun emit() {
        _state.value = sessions.map {
            TerminalViewModel.Session(
                id = it.id,
                name = it.name,
                handle = it.session.mHandle,
                shell = if (installer.isInstalled()) "bash" else "sh",
                isFallback = !installer.isInstalled(),
            )
        }
    }

    /**
     * Bridges the session and view callbacks into the manager. The terminal emulator
     * and view call back on the main thread, so no extra synchronization is needed.
     */
    inner class CompositeTerminalClient : TerminalSessionClient, TerminalViewClient {

        var session: TerminalSession? = null
        var view: TerminalView? = null

        override fun onTextChanged(changedSession: TerminalSession) {
            handler.post { view?.onScreenUpdated() }
        }

        override fun onTitleChanged(changedSession: TerminalSession) {}

        override fun onSessionFinished(finishedSession: TerminalSession) {}

        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            val clipboard = view?.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            clipboard.setPrimaryClip(ClipData.newPlainText("terminal", text))
        }

        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            val clipboard = view?.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
            this.session?.write(text.toByteArray(Charsets.UTF_8), 0, text.length)
        }

        override fun onBell(session: TerminalSession) {}

        override fun onColorsChanged(session: TerminalSession) {
            handler.post { view?.invalidate() }
        }

        override fun onTerminalCursorStateChange(state: Boolean) {}

        override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

        override fun getTerminalCursorStyle(): Int? = null

        override fun logError(tag: String?, message: String?) {
            Log.e(tag, message.orEmpty())
        }

        override fun logWarn(tag: String?, message: String?) {
            Log.w(tag, message.orEmpty())
        }

        override fun logInfo(tag: String?, message: String?) {
            Log.i(tag, message.orEmpty())
        }

        override fun logDebug(tag: String?, message: String?) {
            Log.d(tag, message.orEmpty())
        }

        override fun logVerbose(tag: String?, message: String?) {
            Log.v(tag, message.orEmpty())
        }

        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
            Log.e(tag, message, e)
        }

        override fun logStackTrace(tag: String?, e: Exception?) {
            Log.e(tag, null, e)
        }

        // TerminalViewClient
        override fun onScale(scale: Float): Float = scale.coerceIn(0.5f, 3f)

        override fun onSingleTapUp(e: MotionEvent) {}

        override fun shouldBackButtonBeMappedToEscape(): Boolean = true

        override fun shouldEnforceCharBasedInput(): Boolean = false

        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

        override fun isTerminalViewSelected(): Boolean = true

        override fun copyModeChanged(copyMode: Boolean) {}

        override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false

        override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

        override fun onLongPress(event: MotionEvent): Boolean = false

        override fun readControlKey(): Boolean = false

        override fun readAltKey(): Boolean = false

        override fun readShiftKey(): Boolean = false

        override fun readFnKey(): Boolean = false

        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false

        override fun onEmulatorSet() {}

    }
}