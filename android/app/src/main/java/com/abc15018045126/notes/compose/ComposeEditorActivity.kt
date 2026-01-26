package com.abc15018045126.notes.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.abc15018045126.notes.compose.ui.EditorScreen
import com.abc15018045126.notes.compose.ui.theme.NotesTheme

class ComposeEditorActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get file path from intent
        val filePath = intent.getStringExtra("FILE_PATH") ?: ""
        android.util.Log.d("ComposeEditorActivity", "onCreate with filePath: $filePath")
        
        if (filePath.isNotEmpty()) {
            viewModel.loadFile(this, filePath)
        } else {
            android.util.Log.e("ComposeEditorActivity", "No file path provided")
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            NotesTheme(darkTheme = uiState.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Auto-save if enabled and modified
        val state = viewModel.uiState.value
        if (state.autoSave && state.isModified) {
            viewModel.saveFile(this)
        }
    }
}
