package com.abc15018045126.notes.compose.ui

import android.graphics.Typeface
import android.view.View
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import com.abc15018045126.notes.compose.EditorUiState
import com.abc15018045126.notes.compose.EditorViewModel
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class EditorControl {
    private var editor: CodeEditor? = null

    fun attach(editor: CodeEditor) {
        this.editor = editor
    }

    fun jumpTo(pos: Int) {
        if (editor == null) return 
        editor?.let {
            if (androidx.core.view.ViewCompat.isLaidOut(it)) {
                performJump(it, pos)
            } else {
                it.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                        it.removeOnLayoutChangeListener(this)
                        performJump(it, pos)
                    }
                })
            }
        }
    }
    
    fun getCurrentCursorPosition(): Int {
        val editor = this.editor ?: return 0
        try {
            val cursor = editor.cursor
            val targetLine = cursor.leftLine
            val targetCol = cursor.leftColumn
            val text = editor.text.toString()
            
            var idx = 0
            var curLine = 0
            val len = text.length
            
            while (idx < len && curLine < targetLine) {
                if (text[idx] == '\n') {
                    curLine++
                }
                idx++
            }
            return (idx + targetCol).coerceAtMost(len)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    private fun performJump(it: CodeEditor, pos: Int) {
        try {
            it.requestFocus()
            val text = it.text.toString()
            val safePos = pos.coerceIn(0, text.length)
            
            var line = 0
            var col = 0
            for (i in 0 until safePos) {
                if (text[i] == '\n') {
                    line++
                    col = 0
                } else {
                    col++
                }
            }
            
            it.setSelection(line, col)
            it.ensureSelectionVisible()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun search(text: String) {
        try {
            editor?.searcher?.search(text, io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(false, false))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findNext() {
        try {
            editor?.searcher?.gotoNext()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findPrevious() {
        try {
            editor?.searcher?.gotoPrevious()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun replace(text: String) {
        try {
            editor?.searcher?.replaceThis(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun replaceAll(text: String) {
        try {
            editor?.searcher?.replaceAll(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSearch() {
        try {
            editor?.searcher?.stopSearch()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun undo() {
        editor?.undo()
    }

    fun redo() {
        editor?.redo()
    }

    fun canUndo(): Boolean = editor?.canUndo() ?: false
    fun canRedo(): Boolean = editor?.canRedo() ?: false

    fun insertText(text: String) {
        editor?.insertText(text, text.length)
    }
}

@Composable
fun SoraEditorView(
    content: String,
    onContentChange: (String) -> Unit,
    onSelectionChange: (Int, Int, Int) -> Unit = { _, _, _ -> },
    fontSize: Float = 18f,
    showLineNumbers: Boolean = true,
    wordWrap: Boolean = false,
    editable: Boolean = true,
    backgroundColor: String = "#FFFFFF",
    modifier: Modifier = Modifier,
    control: EditorControl? = null,
    onSearchMatchesChange: (Int, Int) -> Unit = { _, _ -> },
    onScroll: () -> Unit = {},
    onTap: () -> Unit = {},
    autoFocus: Boolean = false
) {
    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }
    
    // Ensure we always have the latest callbacks even if factory is not re-run
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnContentChange by rememberUpdatedState(onContentChange)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    
    LaunchedEffect(editorInstance, control) {
        editorInstance?.let { control?.attach(it) }
    }
    
    AndroidView(
        factory = { context ->
            CodeEditor(context).apply {
                setTextSize(fontSize)
                setTypefaceText(Typeface.MONOSPACE)
                isLineNumberEnabled = showLineNumbers
                isWordwrap = wordWrap
                setEditable(editable)
                setText(content)
                
                // Use GestureDetector for reliable tap detection
                val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        currentOnTap()
                        return true
                    }
                })
                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    false // Return false so the editor still receives touch events for selection/scrolling
                }
                
                subscribeEvent(io.github.rosemoe.sora.event.SelectionChangeEvent::class.java) { _, _ ->
                     val cursor = this.cursor
                     val line = cursor.leftLine
                     val col = cursor.leftColumn
                     
                     val textStr = text.toString()
                     var charPos = 0
                     try {
                         var curL = 0
                         var i = 0
                         while (i < textStr.length && curL < line) {
                             if (textStr[i] == '\n') curL++
                             i++
                         }
                         charPos = i + col
                     } catch (e: Exception) {}
                     
                     currentOnSelectionChange(charPos, line, col)
                     
                     if (searcher.hasQuery()) {
                         onSearchMatchesChange(searcher.currentMatchedPositionIndex + 1, searcher.matchedPositionCount)
                     }
                }

                subscribeEvent(io.github.rosemoe.sora.event.PublishSearchResultEvent::class.java) { _, _ ->
                    onSearchMatchesChange(searcher.currentMatchedPositionIndex + 1, searcher.matchedPositionCount)
                }

                subscribeEvent(io.github.rosemoe.sora.event.ScrollEvent::class.java) { _, _ ->
                    onScroll()
                }

                subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
                    val newText = text.toString()
                    // Don't update if nothing changed? 
                    // But we need to tell parent. Parent will update state. 
                    // Parent update will come back via 'content' prop.
                    // If we just sync blindly, we loop.
                    // But 'update' block handles the loop break.
                    currentOnContentChange(newText)
                }

                try {
                    val color = android.graphics.Color.parseColor(backgroundColor)
                    val r = android.graphics.Color.red(color)
                    val g = android.graphics.Color.green(color)
                    val b = android.graphics.Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    
                    colorScheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                    colorScheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                    
                    if (luminance < 0.5) {
                        colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.WHITE)
                        colorScheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.GRAY)
                    } else {
                        colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.BLACK)
                        colorScheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.DKGRAY)
                    }
                } catch (e: Exception) {}
                
                editorInstance = this
                control?.attach(this)
                
                // Auto-focus if requested (for new notes)
                if (autoFocus) {
                    postDelayed({
                        requestFocus()
                        // Show keyboard with SHOW_FORCED to ensure it appears
                        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                    }, 200) // Delay to ensure view is fully laid out
                }
            }
        },
        update = { view ->
            view.setTextSize(fontSize)
            view.isLineNumberEnabled = showLineNumbers
            view.isWordwrap = wordWrap
            view.setEditable(editable)
            
            // Only update text if it strictly differs.
            if (view.text.toString() != content) {
                // Save cursor? setText resets it usually.
                // If the difference is just line endings, we might be screwing up.
                // But view.text should match content if we are the ones who emitted it.
                view.setText(content)
            }
            
            try {
                val color = android.graphics.Color.parseColor(backgroundColor)
                val r = android.graphics.Color.red(color)
                val g = android.graphics.Color.green(color)
                val b = android.graphics.Color.blue(color)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                
                view.colorScheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                view.colorScheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                
                if (luminance < 0.5) {
                    view.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.WHITE)
                    view.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.GRAY)
                } else {
                    view.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.BLACK)
                    view.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.DKGRAY)
                }
            } catch (e: Exception) {}
        },
        modifier = modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val editorControl = remember { EditorControl() }
    val localContext = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings(localContext)
    }
    
    LaunchedEffect(uiState.isReadOnly, uiState.showToolbar) {
        if (uiState.isReadOnly && uiState.showToolbar) {
            kotlinx.coroutines.delay(2000)
            viewModel.setShowToolbar(false)
        }
    }
    
    val handleBack = {
        if (uiState.showSettings) {
            viewModel.setShowSettings(false)
        } else if (!uiState.autoSave && uiState.isModified) {
            viewModel.setShowExitConfirmation(true)
        } else {
            onBack()
        }
    }

    androidx.activity.compose.BackHandler(onBack = handleBack)
    
    if (uiState.showSettings) {
        EditorSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { viewModel.setShowSettings(false) },
            onFontSizeChange = { viewModel.setFontSize(localContext, it) },
            onToggleLineNumbers = { viewModel.toggleLineNumbers(localContext) },
            onToggleWordWrap = { viewModel.toggleWordWrap(localContext) },
            onBackgroundColorChange = { viewModel.setBackgroundColor(localContext, it) }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = uiState.showToolbar || !uiState.isReadOnly,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    TopAppBar(
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = try { Color(android.graphics.Color.parseColor(uiState.uiColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }
                    ),
                    navigationIcon = {
                            IconButton(onClick = { 
                                val pos = editorControl.getCurrentCursorPosition()
                                viewModel.setCursorPosition(pos, uiState.cursorLine - 1, uiState.cursorColumn)
                                viewModel.setShowToc(true) 
                            }) {
                                Icon(Icons.Default.Menu, "目录")
                            }
                        },
                        actions = {
                        IconButton(onClick = { viewModel.saveFile(localContext) }) {
                            Icon(Icons.Default.Save, "保存")
                        }
                        IconButton(onClick = { editorControl.undo() }, enabled = editorControl.canUndo()) {
                            Icon(Icons.Default.Undo, "撤销")
                        }
                        IconButton(onClick = { editorControl.redo() }, enabled = editorControl.canRedo()) {
                            Icon(Icons.Default.Redo, "反撤销")
                        }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(try { Color(android.graphics.Color.parseColor(uiState.menuColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface })
                        ) {
                            DropdownMenuItem(
                                text = { Text("返回") },
                                onClick = { showMoreMenu = false; handleBack() },
                                leadingIcon = { Icon(Icons.Default.ArrowBack, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("搜索") },
                                onClick = { 
                                    viewModel.setShowSearch(!uiState.showSearch)
                                    showMoreMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Search, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = { viewModel.setShowRenameDialog(true); showMoreMenu = false },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("属性") },
                                onClick = { viewModel.setShowFileProperties(true); showMoreMenu = false },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("重做 (还原为初始)") },
                                onClick = { 
                                    viewModel.updateContent(localContext, uiState.originalContent)
                                    showMoreMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, null) }
                            )
                            DropdownMenuItem(
                                    text = { Text("只读模式: ${if (uiState.isReadOnly) "ON" else "OFF"}") },
                                    onClick = { viewModel.toggleReadOnly(); showMoreMenu = false },
                                    leadingIcon = { Icon(if(uiState.isReadOnly) Icons.Default.Lock else Icons.Default.LockOpen, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("编辑器设置") },
                                    onClick = { viewModel.setShowSettings(true); showMoreMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                            }
                        }
                    )
                }
                
                if (uiState.showSearch) {
                    SearchPanel(
                        searchQuery = uiState.searchQuery,
                        replaceText = uiState.replaceText,
                        currentMatch = uiState.currentMatch,
                        totalMatches = uiState.totalMatches,
                        backgroundColor = uiState.searchColor,
                        onSearchQueryChange = { 
                            viewModel.setSearchQuery(it)
                            if (it.isNotEmpty()) editorControl.search(it) else editorControl.stopSearch()
                        },
                        onReplaceTextChange = { viewModel.setReplaceText(it) },
                        onFindNext = { editorControl.findNext() },
                        onFindPrevious = { editorControl.findPrevious() },
                        onReplace = { editorControl.replace(uiState.replaceText) },
                        onReplaceAll = { editorControl.replaceAll(uiState.replaceText) },
                        onClose = { viewModel.setShowSearch(false) }
                    )
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    SoraEditorView(
                        content = uiState.content,
                        onContentChange = { viewModel.updateContent(localContext, it) },
                        onSelectionChange = { pos, line, col -> viewModel.setCursorPosition(pos, line, col) },
                        fontSize = uiState.fontSize,
                        showLineNumbers = uiState.showLineNumbers,
                        wordWrap = uiState.wordWrap,
                        editable = !uiState.isReadOnly,
                        backgroundColor = uiState.backgroundColor,
                        control = editorControl,
                        onSearchMatchesChange = { current, total -> viewModel.setMatchResults(current, total) },
                        onScroll = { if (uiState.isReadOnly && uiState.showToolbar) viewModel.setShowToolbar(false) },
                        onTap = { if (uiState.isReadOnly) viewModel.toggleToolbar() },
                        autoFocus = uiState.shouldAutoFocus
                    )
                }
                
                Column {
                    if (uiState.showSymbolBar && !uiState.isReadOnly) {
                        SymbolBar(
                            uiColor = uiState.uiColor,
                            onSymbolClick = { editorControl.insertText(it) }
                        )
                    }
                    if (uiState.showStatusBar) {
                        StatusBar(
                            uiColor = uiState.uiColor,
                            fileName = uiState.fileName,
                            cursorLine = uiState.cursorLine,
                            cursorColumn = uiState.cursorColumn,
                            currentCursorPos = uiState.currentCursorPos
                        )
                    }
                }
            }
            
            if (uiState.showToc) {
                TocPanel(
                    content = uiState.content,
                    currentCursorPos = uiState.currentCursorPos,
                    tocMode = uiState.tocMode,
                    surfaceColor = uiState.tocColor,
                    onModeChange = { viewModel.setTocMode(it) },
                    onChapterClick = { editorControl.jumpTo(it); viewModel.setShowToc(false) },
                    onDismiss = { viewModel.setShowToc(false) }
                )
            }
            
            if (uiState.showRenameDialog) {
                RenameDialog(
                    currentName = uiState.fileName,
                    backgroundColor = uiState.menuColor,
                    onRename = { viewModel.renameFile(it); viewModel.setShowRenameDialog(false) },
                    onDismiss = { viewModel.setShowRenameDialog(false) }
                )
            }

            if (uiState.showFileProperties) {
                FilePropertiesDialog(
                    properties = viewModel.getFileDetails(),
                    backgroundColor = uiState.menuColor,
                    onDismiss = { viewModel.setShowFileProperties(false) }
                )
            }

            if (uiState.showExitConfirmation) {
                ExitConfirmationDialog(
                    onSave = { viewModel.saveFile(localContext); onBack() },
                    onDiscard = { onBack() },
                    onDismiss = { viewModel.setShowExitConfirmation(false) }
                )
            }
        }
    }
}

@Composable
fun SymbolBar(uiColor: String, onSymbolClick: (String) -> Unit) {
    val symbols = listOf(",", ".", ";", "(", ")", "{", "}", "[", "]", "\"", "'", ":", "/", "<", ">", "=", "+", "-", "*", "&", "|", "!", "?", "#", "@", "$", "%", "^", "~", "`")
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(try { Color(android.graphics.Color.parseColor(uiColor)) } catch(e:Exception) { Color(0xFFF0F0F0) })
            .border(androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(symbols.size) { index ->
            Surface(
                onClick = { onSymbolClick(symbols[index]) },
                modifier = Modifier
                    .size(width = 36.dp, height = 36.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbols[index], fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun StatusBar(uiColor: String, fileName: String, cursorLine: Int, cursorColumn: Int, currentCursorPos: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        color = try { Color(android.graphics.Color.parseColor(uiColor)) } catch(e:Exception) { Color(0xFFEEEEEE) },
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
             Text(
                text = fileName,
                fontSize = 11.sp,
                color = Color.DarkGray,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "第 ${cursorLine} 行, 第 ${cursorColumn} 列, 第 ${currentCursorPos} 字",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun SearchPanel(
    searchQuery: String,
    replaceText: String,
    currentMatch: Int,
    totalMatches: Int,
    backgroundColor: String,
    onSearchQueryChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }, tonalElevation = 1.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("查找文本", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        if (totalMatches > 0) Text("${currentMatch.coerceAtLeast(0)}/$totalMatches", fontSize = 12.sp)
                    }
                )
                Button(onClick = onFindPrevious, contentPadding = PaddingValues(0.dp), modifier = Modifier.defaultMinSize(minWidth = 48.dp)) { Text("上个", fontSize = 12.sp) }
                Button(onClick = onFindNext, contentPadding = PaddingValues(0.dp), modifier = Modifier.defaultMinSize(minWidth = 48.dp)) { Text("下个", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceTextChange,
                    placeholder = { Text("替换到的文本", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                TextButton(onClick = onReplace) { Text("替换", fontSize = 13.sp) }
                TextButton(onClick = onReplaceAll) { Text("全部", fontSize = 13.sp) }
            }
        }
    }
}

data class Chapter(val index: Int, val pos: Int, val title: String)

@Composable
fun TocPanel(
    content: String,
    currentCursorPos: Int,
    tocMode: String,
    surfaceColor: String,
    onModeChange: (String) -> Unit,
    onChapterClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val chapters = remember(content, tocMode) {
        if (tocMode == "chars") {
            val count = kotlin.math.ceil(content.length / 2000.0).toInt()
            List(count) { i -> Chapter(i, i * 2000, "第 ${i + 1} 章") }
        } else {
            val result = mutableListOf<Chapter>()
            var currentPos = 0
            var lineCount = 0
            var chunkStartLine = 1
            for (i in content.indices) {
                if (content[i] == '\n') {
                    lineCount++
                    if (lineCount % 100 == 0) {
                        result.add(Chapter(result.size, currentPos, "第 $chunkStartLine - $lineCount 行"))
                        chunkStartLine = lineCount + 1
                    }
                }
                currentPos++
            }
            if (result.isEmpty() || lineCount >= chunkStartLine) {
                 result.add(Chapter(result.size, if(result.isEmpty()) 0 else currentPos, "第 $chunkStartLine - ${lineCount + 1} 行"))
            }
            result
        }
    }
    
    val activeIndex = chapters.indexOfLast { it.pos <= currentCursorPos }.coerceAtLeast(0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Auto-hide logic for scrollbar
    var showScrollbar by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            showScrollbar = true
        } else {
            kotlinx.coroutines.delay(3000)
            showScrollbar = false
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex > 0) listState.scrollToItem((activeIndex - 5).coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onDismiss)) {
        Surface(Modifier.fillMaxHeight().fillMaxWidth(0.75f).clickable(enabled = false) { }, color = try { Color(android.graphics.Color.parseColor(surfaceColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface }, tonalElevation = 8.dp) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("目录", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("chars" to "按字", "lines" to "按行").forEach { (mode, label) ->
                            Button(
                                onClick = { onModeChange(mode) }, 
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tocMode == mode) Color(0xFFE0E0E0) else Color.Transparent,
                                    contentColor = if (tocMode == mode) Color.Black else Color.DarkGray
                                ), 
                                modifier = Modifier.height(32.dp), 
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                HorizontalDivider()
                Box(modifier = Modifier.weight(1f)) {
                    androidx.compose.foundation.lazy.LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(chapters.size) { index ->
                            val isActive = index == activeIndex
                            Surface(
                                onClick = { onChapterClick(chapters[index].pos); onDismiss() },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            ) {
                                Text(chapters[index].title, modifier = Modifier.padding(20.dp, 12.dp), fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else null)
                            }
                        }
                    }
                    
                    // Improved Draggable Scrollbar with Auto-hide and Enlarge size
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollbar,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        if (chapters.size > 10) {
                            val scope = rememberCoroutineScope()
                            val totalItems = chapters.size
                            
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(50.dp) // Large touch area
                                    .pointerInput(totalItems) {
                                        detectDragGestures(
                                            onDragStart = { showScrollbar = true },
                                            onDrag = { change, dragAmount ->
                                                showScrollbar = true
                                                change.consume()
                                                val trackHeight = size.height.toFloat()
                                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                                if (visibleItems.isNotEmpty()) {
                                                    val visibleCount = visibleItems.size
                                                    val thumbHeight = trackHeight * (visibleCount.toFloat() / totalItems).coerceIn(0.1f, 1f)
                                                    val travelDistance = (trackHeight - thumbHeight).coerceAtLeast(1f)
                                                    
                                                    val deltaPercent = dragAmount.y / travelDistance
                                                    val currentFirstVisible = listState.firstVisibleItemIndex
                                                    val currentPercent = currentFirstVisible.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)
                                                    
                                                    val newPercent = (currentPercent + deltaPercent).coerceIn(0f, 1f)
                                                    val targetIndex = (newPercent * (totalItems - visibleCount)).toInt()
                                                    scope.launch {
                                                        listState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                scope.launch {
                                                    kotlinx.coroutines.delay(3000)
                                                    showScrollbar = false
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val trackHeightPx = constraints.maxHeight.toFloat()
                                val layoutInfo = listState.layoutInfo
                                val visibleItems = layoutInfo.visibleItemsInfo
                                
                                if (visibleItems.isNotEmpty()) {
                                    val firstVisible = listState.firstVisibleItemIndex
                                    val visibleCount = visibleItems.size
                                    val thumbHeightPercent = (visibleCount.toFloat() / totalItems).coerceIn(0.1f, 1f)
                                    val scrollPercent = firstVisible.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)
                                    
                                    // Background track
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(12.dp) // Double size (6dp -> 12dp)
                                            .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                                            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    )
                                    
                                    // Thumb
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 4.dp)
                                            .width(12.dp) // Double size (6dp -> 12dp)
                                            .fillMaxHeight(thumbHeightPercent)
                                            .graphicsLayer {
                                                translationY = trackHeightPx * (1f - thumbHeightPercent) * scrollPercent
                                            }
                                            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorSettingsScreen(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleLineNumbers: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onBackgroundColorChange: (String) -> Unit
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑器设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                color = try { Color(android.graphics.Color.parseColor(uiState.backgroundColor)) } catch(e:Exception) { Color.Gray },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("预览文本效果 Preview Text", fontSize = uiState.fontSize.sp, color = if (uiState.backgroundColor == "#000000") Color.White else Color.Black)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("字体大小: ${uiState.fontSize.toInt()}px")
                Slider(value = uiState.fontSize, onValueChange = onFontSizeChange, valueRange = 12f..36f)
            }

            SettingsSwitchItem("显示行号", "在左侧显示行号", uiState.showLineNumbers) { viewModel.toggleLineNumbers(localContext) }
            SettingsSwitchItem("自动换行", "自动折行显示", uiState.wordWrap) { viewModel.toggleWordWrap(localContext) }
            SettingsSwitchItem("自动保存", "编辑时自动保存", uiState.autoSave) { viewModel.setAutoSave(localContext, it) }
            SettingsSwitchItem("显示状态栏", "显示底部的行、列、字符数信息", uiState.showStatusBar) { viewModel.toggleStatusBar(localContext) }
            SettingsSwitchItem("符号快捷键", "在底部显示常用符号栏", uiState.showSymbolBar) { viewModel.toggleSymbolBar(localContext) }


            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("主题颜色自定义 (也可在下面 JSON 自由配置)", style = MaterialTheme.typography.titleMedium)
                val colors = listOf("#FFFFFF" to "白", "#F5F5F5" to "灰", "#E0E0E0" to "深灰", "#FFF8DC" to "米", "#E8F5E9" to "绿", "#E3F2FD" to "蓝", "#000000" to "黑")
                
                Column {
                    Text("编辑器背景 (Editor)", fontSize = 12.sp, color = Color.Gray)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { (c, l) -> 
                            ColorOption(c, l, uiState.backgroundColor == c) { viewModel.setBackgroundColor(localContext, c) }
                        }
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("应用 UI 颜色 (Toolbar/Bottom)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                        TextButton(onClick = { 
                            viewModel.setTocColor(localContext, uiState.uiColor)
                            viewModel.setSearchColor(localContext, uiState.uiColor)
                            viewModel.setMenuColor(localContext, uiState.uiColor)
                        }) {
                            Text("同步到所有面板", fontSize = 10.sp)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { (c, l) -> 
                            ColorOption(c, l, uiState.uiColor == c) { viewModel.setUiColor(localContext, c) }
                        }
                    }
                }

                Column {
                    Text("更多菜单颜色 (More Menu)", fontSize = 12.sp, color = Color.Gray)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { (c, l) -> 
                            ColorOption(c, l, uiState.menuColor == c) { viewModel.setMenuColor(localContext, c) }
                        }
                    }
                }

                Column {
                    Text("目录面板颜色 (TOC)", fontSize = 12.sp, color = Color.Gray)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { (c, l) -> 
                            ColorOption(c, l, uiState.tocColor == c) { viewModel.setTocColor(localContext, c) }
                        }
                    }
                }

                Column {
                    Text("搜索面板颜色 (Search)", fontSize = 12.sp, color = Color.Gray)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { (c, l) -> 
                            ColorOption(c, l, uiState.searchColor == c) { viewModel.setSearchColor(localContext, c) }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("配置 JSON", style = MaterialTheme.typography.titleMedium)
                var jsonText by remember { mutableStateOf(viewModel.getSettingsJson()) }
                
                LaunchedEffect(uiState) {
                    jsonText = viewModel.getSettingsJson()
                }

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                     TextButton(onClick = { viewModel.resetSettings(localContext) }) {
                         Text("重置所有设置", color = Color.Red)
                     }
                     Spacer(Modifier.width(8.dp))
                     Button(
                        onClick = { 
                            viewModel.applySettingsFromJson(localContext, jsonText)
                        }
                    ) {
                        Text("保存 JSON 设置")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ColorOption(color: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable { onClick() }) {
        Box(Modifier.size(40.dp).background(try { Color(android.graphics.Color.parseColor(color)) } catch(e:Exception) { Color.Gray }, RoundedCornerShape(20.dp)).border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(20.dp)))
        Text(label, fontSize = 10.sp)
    }
}

@Composable
fun RenameDialog(currentName: String, backgroundColor: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface },
        title = { Text("重命名") }, 
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("新名字") }) }, 
        confirmButton = { TextButton(onClick = { onRename(name) }) { Text("OK") } }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FilePropertiesDialog(properties: Map<String, String>, backgroundColor: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = try { Color(android.graphics.Color.parseColor(backgroundColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surface },
        title = { Text("属性") }, 
        text = { Column { properties.forEach { (k, v) -> Text("$k: $v") } } }, 
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ExitConfirmationDialog(onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("保存？") }, text = { Text("内容已修改") }, confirmButton = { TextButton(onClick = onSave) { Text("保存") } }, dismissButton = { Row { TextButton(onClick = onDiscard) { Text("不保存") }; TextButton(onClick = onDismiss) { Text("取消") } } })
}
