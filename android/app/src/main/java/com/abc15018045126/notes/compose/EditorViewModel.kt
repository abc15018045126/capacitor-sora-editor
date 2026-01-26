package com.abc15018045126.notes.compose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class EditorUiState(
    val content: String = "",
    val filePath: String = "",
    val fileName: String = "",
    val isModified: Boolean = false,
    val fontSize: Float = 18f,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val isReadOnly: Boolean = false,
    val showToolbar: Boolean = true,
    val showSearch: Boolean = false,
    val searchQuery: String = "",
    val replaceText: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
    val showToc: Boolean = false,
    val tocMode: String = "chars", // "chars" or "lines"
    val showSettings: Boolean = false,
    val backgroundColor: String = "#FFFFFF",
    val isDarkTheme: Boolean = false,
    val currentCursorPos: Int = 0,
    val autoSave: Boolean = true,
    val showFileProperties: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val cursorLine: Int = 1,
    val cursorColumn: Int = 0,
    val originalContent: String = ""
)

class EditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // ... existing loadFile method ...

    fun setCursorPosition(pos: Int, line: Int, col: Int) {
        _uiState.value = _uiState.value.copy(
            currentCursorPos = pos,
            cursorLine = line + 1, // 1-indexed for display
            cursorColumn = col
        )
    }

    fun loadFile(context: Context, filePath: String) {
        viewModelScope.launch {
            try {
                // Convert URI to actual path if needed
                var actualPath = if (filePath.startsWith("file://")) {
                    filePath.substring(7) // Remove "file://" prefix
                } else {
                    filePath
                }
                
                // Decode URI-encoded characters (like %E4%BD%9C%E8%80%85 for Chinese)
                actualPath = java.net.URLDecoder.decode(actualPath, "UTF-8")
                
                android.util.Log.d("EditorViewModel", "Loading file from: $actualPath")
                val file = File(actualPath)
                
                if (file.exists()) {
                    val content = file.readText()
                    android.util.Log.d("EditorViewModel", "File loaded successfully, size: ${content.length}")
                    _uiState.value = _uiState.value.copy(
                        content = content,
                        filePath = actualPath,
                        fileName = file.name,
                        originalContent = content,
                        isModified = false
                    )
                } else {
                    android.util.Log.e("EditorViewModel", "File does not exist: $actualPath")
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Error loading file", e)
                e.printStackTrace()
            }
        }
    }

    fun updateContent(context: Context, newContent: String) {
        _uiState.value = _uiState.value.copy(
            content = newContent,
            isModified = true
        )
        if (_uiState.value.autoSave) {
            saveFile(context)
        }
    }

    fun saveFile(context: Context): Boolean {
        return try {
            val file = File(_uiState.value.filePath)
            file.writeText(_uiState.value.content)
            _uiState.value = _uiState.value.copy(isModified = false)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setFontSize(size: Float) {
        _uiState.value = _uiState.value.copy(fontSize = size)
    }

    fun toggleLineNumbers() {
        _uiState.value = _uiState.value.copy(showLineNumbers = !_uiState.value.showLineNumbers)
    }

    fun toggleWordWrap() {
        _uiState.value = _uiState.value.copy(wordWrap = !_uiState.value.wordWrap)
    }

    fun toggleReadOnly() {
        val nextReadOnly = !_uiState.value.isReadOnly
        _uiState.value = _uiState.value.copy(
            isReadOnly = nextReadOnly,
            // Hide toolbar when entering read-only mode, but force show when entering edit mode
            showToolbar = if (nextReadOnly) false else true
        )
    }

    fun toggleToolbar() {
        _uiState.value = _uiState.value.copy(showToolbar = !_uiState.value.showToolbar)
    }

    fun setShowToolbar(show: Boolean) {
        _uiState.value = _uiState.value.copy(showToolbar = show)
    }

    fun setShowSearch(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSearch = show)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setReplaceText(text: String) {
        _uiState.value = _uiState.value.copy(replaceText = text)
    }

    fun setShowToc(show: Boolean) {
        _uiState.value = _uiState.value.copy(showToc = show)
    }

    fun setTocMode(mode: String) {
        _uiState.value = _uiState.value.copy(tocMode = mode)
    }

    fun setShowSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = show)
    }

    fun setBackgroundColor(color: String) {
        _uiState.value = _uiState.value.copy(backgroundColor = color)
    }

    fun setDarkTheme(isDark: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkTheme = isDark)
    }
    fun setMatchResults(current: Int, total: Int) {
        _uiState.value = _uiState.value.copy(currentMatch = current, totalMatches = total)
    }

    fun setAutoSave(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSave = enabled)
    }

    fun setShowFileProperties(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFileProperties = show)
    }

    fun setShowRenameDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showRenameDialog = show)
    }

    fun setShowExitConfirmation(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExitConfirmation = show)
    }

    fun renameFile(newName: String): Boolean {
        val currentFile = File(_uiState.value.filePath)
        val parent = currentFile.parentFile
        val newFile = File(parent, newName)
        return if (currentFile.renameTo(newFile)) {
            _uiState.value = _uiState.value.copy(
                filePath = newFile.absolutePath,
                fileName = newFile.name
            )
            true
        } else {
            false
        }
    }

    fun moveToRecycleBin(): Boolean {
        return try {
            val file = File(_uiState.value.filePath)
            if (!file.exists()) return false
            
            // Assume the notes root is the parent of the first folder that doesn't start with '.'
            // Or more simply, let's just create a .recycle folder in the same directory as the file for now,
            // or go up until we find a reasonable root.
            // Let's go with a sibling ".recycle" folder in the same directory.
            val parent = file.parentFile ?: return false
            val recycleDir = File(parent, ".recycle")
            if (!recycleDir.exists()) {
                recycleDir.mkdirs()
            }
            
            val targetFile = File(recycleDir, file.name)
            // If target exists, append timestamp
            val finalTarget = if (targetFile.exists()) {
                File(recycleDir, "${System.currentTimeMillis()}_${file.name}")
            } else {
                targetFile
            }
            
            file.renameTo(finalTarget)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getFileDetails(): Map<String, String> {
        val file = File(_uiState.value.filePath)
        val details = mutableMapOf<String, String>()
        details["文件名"] = file.name
        details["路径"] = file.absolutePath
        details["大小"] = "${file.length()} 字节"
        details["最后修改"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
        return details
    }
}
