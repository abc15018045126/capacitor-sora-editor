
package io.github.abc15018045126.sora.widget

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.TransactionTooLargeException
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.*
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.collection.MutableIntSet
import androidx.collection.MutableLongLongMap
import io.github.abc15018045126.sora.I18nConfig
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.event.*
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRenderer
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRendererProvider
import io.github.abc15018045126.sora.lang.EmptyLanguage
import io.github.abc15018045126.sora.lang.Language
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer
import io.github.abc15018045126.sora.lang.format.Formatter
import io.github.abc15018045126.sora.lang.styling.*
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.abc15018045126.sora.lang.styling.inlayHint.IntSetUpdateRange
import io.github.abc15018045126.sora.text.*
import io.github.abc15018045126.sora.text.method.KeyMetaStates
import io.github.abc15018045126.sora.util.*
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.component.*
import io.github.abc15018045126.sora.widget.layout.Layout
import io.github.abc15018045126.sora.widget.layout.LineBreakLayout
import io.github.abc15018045126.sora.widget.layout.ViewMeasureHelper
import io.github.abc15018045126.sora.widget.layout.WordwrapLayout
import io.github.abc15018045126.sora.widget.rendering.RenderContext
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.snippet.SnippetController
import io.github.abc15018045126.sora.widget.style.*
import io.github.abc15018045126.sora.widget.style.builtin.DefaultLineNumberTip
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleDrop
import io.github.abc15018045126.sora.widget.style.builtin.HandleStyleSideDrop
import io.github.abc15018045126.sora.widget.style.builtin.MoveCursorAnimator
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.text.repeat

/**
 * CodeEditor is an editor that can highlight text regions by doing basic syntax analyzing
 * This project in <a href="https://github.com/abc15018045126/sora-editor">GitHub</a>
 *
 * Note:
 * Row and line are different in this editor
 * When we say 'row', it means a line displayed on screen. It can be a part of a line in the text object.
 * When we say 'line', it means a real line in the original text.
 *
 * @author abc15018045126
 */
@Suppress("unused")
open class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.codeEditorStyle,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), ContentListener, Formatter.FormatResultReceiver, InlayHintRendererProvider {

    override fun getInlayHintRendererForType(type: String): InlayHintRenderer? {
        return inlayHintRendererMap[type] ?: (if (::editorLanguage.isInitialized) editorLanguage.getInlayHintRendererForType(type) else null)
    }

    companion object {
        @JvmStatic
        fun hasVisibleRegion(begin: Int, end: Int, first: Int, last: Int): Boolean {
            return end > first && begin < last
        }

        const val DEFAULT_TEXT_SIZE = 18
        const val DEFAULT_LINE_INFO_TEXT_SIZE = 21
        const val DEFAULT_CURSOR_BLINK_PERIOD = 500
        const val FLAG_DRAW_WHITESPACE_LEADING = 1
        const val FLAG_DRAW_WHITESPACE_INNER = 1 shl 1
        const val FLAG_DRAW_WHITESPACE_TRAILING = 1 shl 2
        const val FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE = 1 shl 3
        const val FLAG_DRAW_LINE_SEPARATOR = 1 shl 4
        const val FLAG_DRAW_TAB_SAME_AS_SPACE = 1 shl 5
        const val FLAG_DRAW_WHITESPACE_IN_SELECTION = 1 shl 6
        const val FLAG_DRAW_WHITESPACE_FOR_EMPTY_ROW = 1 shl 7
        const val FLAG_DRAW_NON_PRINTABLE_ALWAYS = 1 shl 8
        const val FLAG_DRAW_BLOCK_LINE_FULL = 1
        const val STICKY_LINE_TAB_NOTHING = 0
        const val STICKY_LINE_TAB_DIVIDER = 1
        const val STICKY_LINE_TAB_SHADOW = 2
        const val ACTION_MODE_NONE = 0
        const val ACTION_MODE_SEARCH = 1
        const val ACTION_MODE_SELECT = 2
        const val LN_PANEL_FLOAT = 0
        const val LN_PANEL_SIDE = 1
        const val LN_PANEL_POSITION_LEFT = 0
        const val LN_PANEL_POSITION_RIGHT = 1

        private val logger = Logger.instance("CodeEditor")
        private const val NUMBER_DIGITS = "0 1 2 3 4 5 6 7 8 9"
        private const val LOG_TAG = "CodeEditor"
        private const val COPYRIGHT = "sora-editor\nCopyright (C) abc15018045126 roses2020@qq.com\nThis project is distributed under the LGPL v2.1 license"

        @JvmStatic
        fun getLineNumberPanelWidth(
            lineNumber: Int,
            lineNumberAlign: android.graphics.Paint.Align,
            lineNumberTextSize: Float,
            lineNumberMarginLeft: Float,
            dividerWidth: Float,
            dividerMarginLeft: Float,
            dividerMarginRight: Float,
            dpUnit: Float
        ): Float {
            val width = Paint().apply {
                textSize = lineNumberTextSize
            }.measureText(lineNumber.toString())
            return when (lineNumberAlign) {
                android.graphics.Paint.Align.LEFT -> lineNumberMarginLeft + width + dividerMarginLeft + dividerWidth + dividerMarginRight
                android.graphics.Paint.Align.CENTER -> lineNumberMarginLeft + width + dividerMarginLeft + dividerWidth + dividerMarginRight
                android.graphics.Paint.Align.RIGHT -> lineNumberMarginLeft + width + dividerMarginLeft + dividerWidth + dividerMarginRight
                else -> 0f
            }
        }

        @JvmStatic
        fun getLineNumberPanelWidth(
            lineNumber: Int,
            lineNumberAlign: android.graphics.Paint.Align,
            lineNumberTextSize: Float,
            lineNumberMarginLeft: Float,
            dividerWidth: Float,
            dividerMarginLeft: Float,
            dividerMarginRight: Float,
            dpUnit: Float,
            extraMarginRight: Float
        ): Float {
            return getLineNumberPanelWidth(
                lineNumber,
                lineNumberAlign,
                lineNumberTextSize,
                lineNumberMarginLeft,
                dividerWidth,
                dividerMarginLeft,
                dividerMarginRight,
                dpUnit
            ) + extraMarginRight
        }
    }

    protected val keyEventHandler = EditorKeyEventHandler(this)
    protected lateinit var languageSymbolPairs: SymbolPairMatch
    internal lateinit var textActionWindow: EditorTextActionWindow
    internal lateinit var diagnosticTooltip: EditorDiagnosticTooltipWindow
    internal var selectionAnchor: CharPosition? = null
    internal lateinit var inputConnection: EditorInputConnection
    internal lateinit var eventManager: EventManager
    internal var layout: Layout? = null

    internal var tabWidth = 0
    private var cursorPosition = 0
    private var downX = 0
    private var inputType = 0
    private var nonPrintableOptions = 0
    private var completionWndPosMode = 0
    private var availableFloatArrayRegion: Long = 0
    internal var dpUnit = 0f
    internal var dividerWidth = 0f
    internal var dividerMarginLeft = 0f
    internal var dividerMarginRight = 0f
    internal var extraMarginRight = 0f
    private var insertSelectionWidth = 0f
    private var blockLineWidth = 0f
    private var textBorderWidth = 0f
    private var verticalScrollFactor = 0f
    private var lineInfoTextSize = 0f
    private var lineSpacingMultiplier = 1f
    private var lineSpacingAdd = 0f
    private var wrapLineSpacingMultiplier = 1f
    private var wrapLineSpacingAdd = 0f
    private var lineNumberMarginLeft = 0f
    private var verticalExtraSpaceFactor = 0.5f
    private var waitForNextChange = false
    internal var scalable = false
    internal var editable = false
    internal var wordwrap = false
    private var undoEnabled = false
    private var mouseHover = false
    private var mouseButtonPressed = false
    private var lastAnchorIsSelLeft = false
    @get:UnsupportedUserUsage
    @set:UnsupportedUserUsage
    @Volatile
    var isLayoutBusy = false
        protected set
    internal var displayLnPanel = false
    private var lnPanelPosition = 0
    private var lnPanelPositionMode = 0
    private var rejectComposingCount = 0
    private var released = false
    private var lineNumberEnabled = false
    private var blockLineEnabled = false
    private var forceHorizontalScrollable = false
    private var highlightCurrentBlock = false
    internal var highlightCurrentLine = false
    internal var verticalScrollBarEnabled = false
    internal var horizontalScrollBarEnabled = false
    private var cursorAnimation = false
    private var pinLineNumber = false
    private var antiWordBreaking = false
    private var wordwrapRtlDisplaySupport = false
    private var firstLineNumberAlwaysVisible = false
    private var ligatureEnabled = false
    private var lastCursorState = false
    private var stickyTextSelection = false
    private var highlightBracketPair = false
    internal var isInLongSelect = false
    private var anyWrapContentSet = false
    private var renderFunctionCharacters = false
    private var isSoftKbdEnabled = false
    private var isDisableSoftKbdOnHardKbd = false
    private val handleDescLeft = SelectionHandleStyle.HandleDescriptor()
    private val handleDescRight = SelectionHandleStyle.HandleDescriptor()
    private val handleDescInsert = SelectionHandleStyle.HandleDescriptor()
    internal lateinit var clipboardManager: ClipboardManager
    private lateinit var inputMethodManager: InputMethodManager
    internal lateinit var cursor: Cursor
    internal lateinit var text: Content
    private lateinit var matrix: Matrix
    internal lateinit var colorScheme: EditorColorScheme
    internal lateinit var lineNumberTipTextProvider: LineNumberTipTextProvider
    internal lateinit var formatTip: String
    internal lateinit var editorLanguage: Language
    private var diagnosticStyle = DiagnosticIndicatorStyle.WAVY_LINE
    private var lastMakeVisible: Long = 0
    private lateinit var completionWindow: EditorAutoCompletion
    private lateinit var touchHandler: EditorTouchEventHandler
    private lateinit var lineNumberAlign: android.graphics.Paint.Align
    private lateinit var basicDetector: GestureDetector
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var anchorInfoBuilder: CursorAnchorInfo.Builder
    private lateinit var edgeEffectVertical: EdgeEffect
    private lateinit var edgeEffectHorizontal: EdgeEffect
    private var extractingTextRequest: ExtractedTextRequest? = null
    internal lateinit var editorSearcher: EditorSearcher
    private lateinit var cursorAnimator: CursorAnimator
    private lateinit var handleStyle: SelectionHandleStyle
    private lateinit var cursorBlink: CursorBlink
    internal lateinit var props: DirectAccessProps
    private var extraArguments: Bundle? = null
    internal var textStyles: Styles? = null
    internal var diagnostics: DiagnosticsContainer? = null
    internal var inlayHints: InlayHintsContainer? = null
    internal var highlightTextContainer: HighlightTextContainer? = null
    lateinit var renderContext: RenderContext
        private set
    lateinit var renderer: EditorRenderer
        private set
    private var hardwareAccAllowed = false
    private var scrollerFinalX = 0f
    private var scrollerFinalY = 0f
    private var verticalAbsorb = false
    private var horizontalAbsorb = false
    internal lateinit var lineSeparator: LineSeparator
    private var lastInsertion: TextRange? = null
    private var lastSelectedTextRange: TextRange? = null
    internal lateinit var snippetController: SnippetController
    private val inlayHintRendererMap: MutableMap<String, InlayHintRenderer> = HashMap()

    init {
        initialize(attrs, defStyleAttr, defStyleRes)
        applyAttributeSets(attrs, defStyleAttr, defStyleRes)
    }

    protected open fun initialize(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        Log.v(LOG_TAG, COPYRIGHT)

        eventManager = EventManager()
        renderFunctionCharacters = true
        renderContext = RenderContext(this)
        renderer = onCreateRenderer()

        styleDelegate = EditorStyleDelegate(this)

        verticalScrollFactor = ViewUtils.getVerticalScrollFactor(context)
        lineSeparator = LineSeparator.LF
        lineNumberTipTextProvider = DefaultLineNumberTip
        formatTip = I18nConfig.getString(context, R.string.sora_editor_editor_formatting)
        props = DirectAccessProps()
        dpUnit = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, Resources.getSystem().displayMetrics) / 10f
        dividerWidth = dpUnit
        insertSelectionWidth = dpUnit * 1.5f
        textBorderWidth = dpUnit
        dividerMarginRight = dpUnit * 2
        dividerMarginLeft = dividerMarginRight

        matrix = Matrix()
        handleStyle = HandleStyleSideDrop(context)
        editorSearcher = EditorSearcher(this)
        cursorAnimator = MoveCursorAnimator(this)
        setCursorBlinkPeriod(DEFAULT_CURSOR_BLINK_PERIOD)
        anchorInfoBuilder = CursorAnchorInfo.Builder()

        startedActionMode = ACTION_MODE_NONE
        setTextSize(DEFAULT_TEXT_SIZE.toFloat())
        setLineInfoTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_LINE_INFO_TEXT_SIZE.toFloat(), Resources.getSystem().displayMetrics))
        colorScheme = EditorColorScheme.getDefault()
        colorScheme.attachEditor(this)
        touchHandler = EditorTouchEventHandler(this)
        basicDetector = GestureDetector(context, touchHandler)
        basicDetector.setOnDoubleTapListener(touchHandler)
        scaleDetector = ScaleGestureDetector(context, touchHandler)
        lineNumberAlign = android.graphics.Paint.Align.RIGHT
        waitForNextChange = false
        blockLineEnabled = true
        blockLineWidth = 1f
        inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        isUndoEnabled = true
        cursorPosition = -1
        isScalable = true
        isFocusable = true
        isFocusableInTouchMode = true
        isHighlightBracketPair = true
        inputConnection = EditorInputConnection(this)
        completionWindow = EditorAutoCompletion(this)
        edgeEffectVertical = EdgeEffect(context)
        edgeEffectHorizontal = EdgeEffect(context)
        textActionWindow = EditorTextActionWindow(this)
        diagnosticTooltip = EditorDiagnosticTooltipWindow(this)
        contextMenuCreator = EditorContextMenuCreator(this)
        setEditorLanguage(null)
        setText(null)
        setTabWidth(4)
        isHighlightCurrentLine = true
        isVerticalScrollBarEnabled = true
        isHighlightCurrentBlock = true
        isDisplayLnPanel = true
        isHorizontalScrollBarEnabled = true
        isFirstLineNumberAlwaysVisible = true
        isCursorAnimationEnabled = true
        isEditable = true
        isLineNumberEnabled = true
        isHardwareAcceleratedDrawAllowed = true
        isInterceptParentHorizontalScrollEnabled = false
        setTypefaceText(Typeface.DEFAULT)
        isSoftKeyboardEnabled = true
        isDisableSoftKbdIfHardKbdAvailable = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
        if (context is ContextThemeWrapper) {
            setEdgeEffectColor(ThemeUtils.getColorPrimary(context as ContextThemeWrapper))
        }

        scaleDetector.isQuickScaleEnabled = false
        snippetController = SnippetController(this)
    }

    protected open fun applyAttributeSets(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.CodeEditor, defStyleAttr, defStyleRes)

        setHorizontalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbHorizontal))
        setHorizontalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackHorizontal))
        setVerticalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbVertical))
        setVerticalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackVertical))

        lnPanelPositionMode = array.getInt(R.styleable.CodeEditor_lnPanelPositionMode, LineInfoPanelPositionMode.FOLLOW)
        lnPanelPosition = array.getInt(R.styleable.CodeEditor_lnPanelPosition, LineInfoPanelPosition.CENTER)

        dividerWidth = array.getDimension(R.styleable.CodeEditor_dividerWidth, dividerWidth)
        val margin = array.getDimension(R.styleable.CodeEditor_dividerMargin, dividerMarginLeft)
        setDividerMargin(array.getDimension(R.styleable.CodeEditor_dividerMargin, margin), array.getDimension(R.styleable.CodeEditor_dividerMargin, margin))
        isLineNumberPinned = array.getBoolean(R.styleable.CodeEditor_pinLineNumber, false)

        isHighlightCurrentBlock = array.getBoolean(R.styleable.CodeEditor_highlightCurrentBlock, true)
        isHighlightCurrentLine = array.getBoolean(R.styleable.CodeEditor_highlightCurrentLine, true)
        isHighlightBracketPair = array.getBoolean(R.styleable.CodeEditor_highlightBracketPair, true)

        isLigatureEnabled = array.getBoolean(R.styleable.CodeEditor_ligatures, true)
        isLineNumberEnabled = array.getBoolean(R.styleable.CodeEditor_lineNumberVisible, isLineNumberEnabled)
        getComponent(EditorAutoCompletion::class.java).isEnabled = array.getBoolean(R.styleable.CodeEditor_autoCompleteEnabled, true)
        props.symbolPairAutoCompletion = array.getBoolean(R.styleable.CodeEditor_symbolCompletionEnabled, true)
        isRenderFunctionCharacters = array.getBoolean(R.styleable.CodeEditor_renderFunctionChars, isRenderFunctionCharacters)
        isScalable = array.getBoolean(R.styleable.CodeEditor_scalable, isScalable)

        setTextSizePx(array.getDimension(R.styleable.CodeEditor_textSize, textSizePx))
        setCursorBlinkPeriod(array.getInt(R.styleable.CodeEditor_cursorBlinkPeriod, cursorBlink.period))
        setTabWidth(array.getInt(R.styleable.CodeEditor_tabWidth, getTabWidth()))

        val wordwrapMode = array.getInt(R.styleable.CodeEditor_wordwrapMode, 0)
        if (wordwrapMode != 0) {
            setWordwrap(true, wordwrapMode > 1, false)
        }

        setText(array.getString(R.styleable.CodeEditor_text))

        array.recycle()
    }

    fun getSnippetController(): SnippetController = snippetController

    fun getProps(): DirectAccessProps = props

    var formatTipText: String
        get() = formatTip
        set(value) {
            formatTip = value
        }

    fun setFormatTip(@NonNull formatTip: String) {
        this.formatTip = formatTip
    }

    fun getFormatTip(): String = formatTip

    var isLineNumberPinned: Boolean
        get() = pinLineNumber
        set(value) {
            pinLineNumber = value
            if (isLineNumberEnabled) {
                invalidate()
            }
        }

    var isFirstLineNumberAlwaysVisible: Boolean
        get() = firstLineNumberAlwaysVisible
        set(value) {
            firstLineNumberAlwaysVisible = value
            if (isWordwrap) {
                invalidate()
            }
        }

    val offsetX: Int
        get() = touchHandler.scroller.currX

    val offsetY: Int
        get() = touchHandler.scroller.currY

    val rowHeight: Int
        get() = logicalRowHeight

    val logicalRowHeight: Int
        get() {
            val metrics = renderer.metricsText
            return max(1, metrics.descent - metrics.ascent + getLineSpacingPixels(lineSpacingMultiplier, lineSpacingAdd))
        }

    val wrapRowHeight: Int
        get() {
            val metrics = renderer.metricsText
            return max(1, metrics.descent - metrics.ascent + getLineSpacingPixels(wrapLineSpacingMultiplier, wrapLineSpacingAdd))
        }

    fun getRowHeight(row: Int): Int {
        if (layout == null) return rowHeight
        return if (layout!!.getRowAt(row).isTrailingRow) logicalRowHeight else wrapRowHeight
    }

    fun getRowTop(row: Int): Int {
        if (layout == null) return logicalRowHeight * row
        return layout!!.getRowTop(row)
    }

    fun getRowBottom(row: Int): Int {
        if (layout == null) return logicalRowHeight * (row + 1)
        return layout!!.getRowBottom(row)
    }

    /**
     * Get builtin component so that you can enable/disable them or do some other actions.
     *
     * @see io.github.abc15018045126.sora.widget.component
     */
    @Suppress("UNCHECKED_CAST")
    @NonNull
    fun <T : EditorBuiltinComponent> getComponent(@NonNull clazz: Class<T>): T {
        return when (clazz) {
            EditorAutoCompletion::class.java -> completionWindow as T
            Magnifier::class.java -> touchHandler.magnifier as T
            EditorTextActionWindow::class.java -> textActionWindow as T
            EditorDiagnosticTooltipWindow::class.java -> diagnosticTooltip as T
            EditorContextMenuCreator::class.java -> contextMenuCreator as T
            else -> throw IllegalArgumentException("Unknown component type")
        }
    }

    fun getLineSpacingPixels(): Int {
        return getLineSpacingPixels(lineSpacingMultiplier, lineSpacingAdd)
    }

    fun getLineSpacingPixels(multiplier: Float, add: Float): Int {
        val metrics = renderer.metricsText
        return (((metrics.descent - metrics.ascent) * (multiplier - 1f) + add).toInt()) / 2 * 2
    }

    fun insertText(text: String, selectionOffset: Int) {
        if (selectionOffset < 0 || selectionOffset > text.length) {
            throw IllegalArgumentException("selectionOffset is invalid")
        }
        val cur = getText().cursor
        if (cur.isSelected) {
            deleteText()
            notifyIMEExternalCursorChange()
        }
        this.text.insert(cur.rightLine, cur.rightColumn, text)
        notifyIMEExternalCursorChange()
        if (selectionOffset != text.length) {
            val pos = this.text.indexer.getCharPosition(cur.right - (text.length - selectionOffset))
            setSelection(pos.line, pos.column)
        }
    }

    fun setCursorBlinkPeriod(period: Int) {
        if (!::cursorBlink.isInitialized) {
            cursorBlink = CursorBlink(this, period)
        } else {
            val before = cursorBlink.period
            cursorBlink.setPeriod(period)
            if (before <= 0 && cursorBlink.valid && isAttachedToWindow) {
                postInLifecycle(cursorBlink)
            }
        }
    }

    fun getCursorBlink(): CursorBlink = cursorBlink

    var isLigatureEnabled: Boolean
        get() = ligatureEnabled
        set(value) {
            ligatureEnabled = value
            setFontFeatureSettings(if (value) null else "'liga' 0,'calt' 0,'hlig' 0,'dlig' 0,'clig' 0")
        }

    fun setFontFeatureSettings(features: String?) {
        renderer.paint.setFontFeatureSettingsWrapped(features)
        renderer.paintOther.fontFeatureSettings = features
        renderer.paintGraph.fontFeatureSettings = features
        renderer.updateTimestamp()
        invalidate()
    }

    fun setSelectionHandleStyle(@NonNull style: SelectionHandleStyle) {
        handleStyle = style
        invalidate()
    }

    fun getHandleStyle(): SelectionHandleStyle = handleStyle

    var isHighlightCurrentBlock: Boolean
        get() = highlightCurrentBlock
        set(value) {
            highlightCurrentBlock = value
            if (!value) {
                cursorPosition = -1
            } else {
                cursorPosition = findCursorBlock()
            }
            invalidate()
        }

    var isStickyTextSelection: Boolean
        get() = stickyTextSelection
        set(value) {
            stickyTextSelection = value
        }

    var isHighlightCurrentLine: Boolean
        get() = highlightCurrentLine
        set(value) {
            highlightCurrentLine = value
            invalidate()
        }

    fun getEditorLanguage(): Language = editorLanguage

    fun setEditorLanguage(lang: Language?) {
        val newLang = lang ?: EmptyLanguage()

        // Destroy old one
        if (::editorLanguage.isInitialized) {
            val old = editorLanguage
            val formatter = old.formatter
            formatter.setReceiver(null)
            formatter.destroy()
            old.analyzeManager.setReceiver(null)
            old.analyzeManager.destroy()
            old.destroy()
        }

        styleDelegate.reset()
        editorLanguage = newLang
        textStyles = null
        diagnostics = null

        // Setup new one
        val mgr = newLang.analyzeManager
        mgr.setReceiver(styleDelegate)
        if (::text.isInitialized) {
            mgr.reset(ContentReference(text), extraArguments)
        }

        // Symbol pairs
        if (::languageSymbolPairs.isInitialized) {
            languageSymbolPairs.parent = null
        }
        languageSymbolPairs = editorLanguage.symbolPairs
        if (languageSymbolPairs == null) {
            Log.w(LOG_TAG, "Language(${editorLanguage}) returned null for symbol pairs. It is a mistake.")
            languageSymbolPairs = SymbolPairMatch()
        }
        languageSymbolPairs.parent = props.overrideSymbolPairs

        if (::snippetController.isInitialized) {
            snippetController.stopSnippet()
        }
        renderContext.invalidateRenderNodes()
        invalidate()

        // reset inlay hints (partially re-layout required)
        if (inlayHints != null) {
            setInlayHints(null)
        }
        if (highlightTextContainer != null) {
            setHighlightTexts(null)
        }
    }

    /**
     * Replace the built-in component to the given one.
     * The new component's enabled state will extend the old one.
     *
     * @param clazz       Built-in class type. Such as {@code EditorAutoCompletion.class}
     * @param replacement The new component to apply
     * @param <T>         Type of built-in component
     */
    fun <T : EditorBuiltinComponent> replaceComponent(@NonNull clazz: Class<T>, @NonNull replacement: T) {
        val old = getComponent(clazz)
        val isEnabled = old.isEnabled
        old.isEnabled = false
        when (clazz) {
            EditorAutoCompletion::class.java -> completionWindow = replacement as EditorAutoCompletion
            Magnifier::class.java -> touchHandler.magnifier = replacement as Magnifier
            EditorTextActionWindow::class.java -> textActionWindow = replacement as EditorTextActionWindow
            EditorDiagnosticTooltipWindow::class.java -> diagnosticTooltip = replacement as EditorDiagnosticTooltipWindow
            EditorContextMenuCreator::class.java -> contextMenuCreator = replacement as EditorContextMenuCreator
            else -> throw IllegalArgumentException("Unknown component type")
        }
        replacement.isEnabled = isEnabled
    }

    fun registerInlayHintRenderers(vararg renderers: InlayHintRenderer) {
        var needLayout = false
        for (renderer in renderers) {
            val oldValue = inlayHintRendererMap.put(renderer.typeName, renderer)
            needLayout = needLayout || oldValue !== renderer
        }
        if (needLayout) {
            createLayout()
        }
    }

    fun registerInlayHintRenderer(@NonNull renderer: InlayHintRenderer) {
        val oldValue = inlayHintRendererMap.put(renderer.typeName, renderer)
        if (oldValue !== renderer) {
            createLayout()
        }
    }

    fun removeInlayHintRenderer(@NonNull renderer: InlayHintRenderer) {
        val oldValue = inlayHintRendererMap[renderer.typeName]
        if (oldValue === renderer) {
            inlayHintRendererMap.remove(renderer.typeName)
            createLayout()
        }
    }

    @NonNull
    fun getInlayHintRenderers(): List<InlayHintRenderer> {
        return ArrayList(inlayHintRendererMap.values)
    }


    /**
     * Get KeyMetaStates, which manages alt/shift state in editor
     */
    @NonNull
    fun getKeyMetaStates(): KeyMetaStates {
        return keyEventHandler.keyMetaStates
    }

    /**
     * Cancel the next animation for {@link CodeEditor#ensurePositionVisible(int, int)}
     */
    protected fun cancelAnimation() {
        lastMakeVisible = System.currentTimeMillis()
    }

    /**
     * Get the width of line number and divider line
     *
     * @return The width
     */
    fun measureTextRegionOffset(): Float {
        return if (isLineNumberEnabled) {
            measureLineNumber() + dividerMarginLeft + dividerMarginRight + dividerWidth +
                    (if (renderer.hasSideHintIcons()) rowHeight.toFloat() else 0f)
        } else {
            dividerMarginLeft + dividerMarginRight
        }
    }

    /**
     * Get the rect of left selection handle painted on view
     *
     * @return Descriptor of left handle
     */
    fun getLeftHandleDescriptor(): SelectionHandleStyle.HandleDescriptor {
        return handleDescLeft
    }

    /**
     * Get the rect of right selection handle painted on view
     *
     * @return Descriptor of right handle
     */
    fun getRightHandleDescriptor(): SelectionHandleStyle.HandleDescriptor {
        return handleDescRight
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getOffset(line: Int, column: Int): Float {
        return layout?.getCharLayoutOffset(line, column)!![1] + measureTextRegionOffset() - offsetX
    }

    /**
     * Get the character's x offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getCharOffsetX(line: Int, column: Int): Float {
        return layout?.getCharLayoutOffset(line, column)!![1] + measureTextRegionOffset() - offsetX
    }

    /**
     * Get the character's y offset on view
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    fun indentSelection() {
        if (!isEditable) {
            return
        }
        val cur = getCursor()
        indentLines(cur.leftLine, cur.rightLine)
    }

    fun indentLines(startLine: Int, endLine: Int) {
        if (!isEditable) {
            return
        }
        text.beginBatchEdit()
        val tab = createTabString()
        for (i in startLine..endLine) {
            text.insert(i, 0, tab)
        }
        text.endBatchEdit()
    }

    fun unindentSelection() {
        if (!isEditable) {
            return
        }
        val cur = getCursor()
        val startLine = cur.leftLine
        val endLine = cur.rightLine
        text.beginBatchEdit()
        for (i in startLine..endLine) {
            var unindentSize = 0
            val lineSize = text.getColumnCount(i)
            if (lineSize > 0) {
                if (text.charAt(i, 0) == '\t') {
                    unindentSize = 1
                } else if (text.charAt(i, 0) == ' ') {
                    unindentSize = 1
                    while (unindentSize < tabWidth && unindentSize < lineSize && text.charAt(i, unindentSize) == ' ') {
                        unindentSize++
                    }
                }
            }
            if (unindentSize > 0) {
                text.delete(i, 0, i, unindentSize)
            }
        }
        text.endBatchEdit()
    }

    fun commitTab() {
        if (!isEditable) {
            return
        }
        val cur = getCursor()
        if (cur.isSelected) {
            deleteText()
        }
        val tab = createTabString()
        text.insert(cur.rightLine, cur.rightColumn, tab)
    }

    fun indentOrCommitTab() {
        if (!isEditable) {
            return
        }
        if (getCursor().isSelected) {
            indentSelection()
        } else {
            commitTab()
        }
    }

    fun createTabString(): String {
        return if (props.useSpacesForTab) {
            " ".repeat(tabWidth)
        } else {
            "\t"
        }
    }

    fun updateCursorAnchor() {
        if (inputMethodManager == null || !isFocused) {
            return
        }
        val cur = getCursor()
        val line = cur.rightLine
        val column = cur.rightColumn
        val offset = layout!!.getCharLayoutOffset(line, column)
        val x = offset[1] + measureTextRegionOffset() - offsetX
        val y = offset[0] - offsetY
        anchorInfoBuilder.reset()
        anchorInfoBuilder.setSelectionRange(cur.left, cur.right)
        val rowHeight = getRowHeight(layout!!.getRowIndexForY(offset[0].toInt()))
        val top = y + getLineSpacingPixels() / 2f
        val bottom = top + getRowHeightOfText()

        anchorInfoBuilder.setInsertionMarkerLocation(x, top, bottom, bottom, CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION)

        matrix.reset()
        val location = IntArray(2)
        getLocationOnScreen(location)
        matrix.postTranslate(location[0].toFloat(), location[1].toFloat())
        inputMethodManager.updateCursorAnchorInfo(this, anchorInfoBuilder.build())
    }

    var isHighlightBracketPair: Boolean
        get() = highlightBracketPair
        set(value) {
            highlightBracketPair = value
            if (!value) {
                styleDelegate.clearFoundBracketPair()
            } else {
                styleDelegate.postUpdateBracketPair()
            }
            invalidate()
        }

    fun setLineSeparator(@NonNull lineSeparator: LineSeparator) {
        if (lineSeparator == LineSeparator.NONE) {
            throw IllegalArgumentException()
        }
        this.lineSeparator = lineSeparator
    }

    fun getLineSeparator(): LineSeparator = lineSeparator

    var inputTypeMode: Int
        get() = inputType
        set(value) {
            inputType = value
            restartInput()
        }

    fun setInputType(inputType: Int) {
        this.inputType = inputType
        restartInput()
    }

    fun getInputType(): Int = inputType

    fun undo() {
        val range = text.undo()
        if (range != null) {
            try {
                setSelectionRegion(
                    range.start.line, range.start.column,
                    range.end.line, range.end.column,
                    true, SelectionChangeEvent.CAUSE_TEXT_MODIFICATION
                )
            } catch (e: IndexOutOfBoundsException) {
                // Suppressed
            }
        }
        notifyIMEExternalCursorChange()
    }

    fun redo() {
        text.redo()
        notifyIMEExternalCursorChange()
    }

    fun canUndo(): Boolean = text.canUndo()

    fun canRedo(): Boolean = text.canRedo()

    var isUndoEnabled: Boolean
        get() = undoEnabled
        set(value) {
            undoEnabled = value
            if (::text.isInitialized) {
                text.isUndoEnabled = value
            }
        }

    fun getDiagnosticIndicatorStyle(): DiagnosticIndicatorStyle = diagnosticStyle

    fun setDiagnosticIndicatorStyle(@NonNull diagnosticIndicatorStyle: DiagnosticIndicatorStyle) {
        this.diagnosticStyle = diagnosticIndicatorStyle
        invalidate()
    }

    fun beginSearchMode() {
        val callback: ActionMode.Callback = object : ActionMode.Callback {
            override fun onCreateActionMode(p1: ActionMode, p2: Menu): Boolean {
                startedActionMode = ACTION_MODE_SEARCH_TEXT
                p2.add(0, 0, 0, I18nConfig.getResourceId(R.string.sora_editor_next))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                p2.add(0, 1, 0, I18nConfig.getResourceId(R.string.sora_editor_last))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                p2.add(0, 2, 0, I18nConfig.getResourceId(R.string.sora_editor_replace))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                p2.add(0, 3, 0, I18nConfig.getResourceId(R.string.sora_editor_replaceAll))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                val sv = SearchView(context)
                sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(text: String): Boolean {
                        getSearcher().gotoNext()
                        return false
                    }

                    override fun onQueryTextChange(text: String?): Boolean {
                        if (text == null || text.isEmpty()) {
                            getSearcher().stopSearch()
                            return false
                        }
                        getSearcher().search(text, EditorSearcher.SearchOptions(false, false))
                        return false
                    }
                })
                p1.customView = sv
                sv.performClick()
                sv.queryHint = I18nConfig.getString(context, R.string.sora_editor_text_to_search)
                sv.isIconifiedByDefault = false
                sv.isIconified = false
                return true
            }

            override fun onPrepareActionMode(p1: ActionMode, p2: Menu): Boolean = true

            override fun onActionItemClicked(am: ActionMode, p2: MenuItem): Boolean {
                if (!getSearcher().hasQuery()) {
                    return false
                }
                when (p2.itemId) {
                    1 -> getSearcher().gotoPrevious()
                    0 -> getSearcher().gotoNext()
                    2, 3 -> {
                        val replaceAll = p2.itemId == 3
                        val et = EditText(context)
                        et.setHint(I18nConfig.getResourceId(R.string.sora_editor_replacement))
                        AlertDialog.Builder(context)
                            .setTitle(I18nConfig.getResourceId(if (replaceAll) R.string.sora_editor_replaceAll else R.string.sora_editor_replace))
                            .setView(et)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(I18nConfig.getResourceId(R.string.sora_editor_replace)) { dialog, _ ->
                                if (replaceAll) {
                                    getSearcher().replaceAll(et.text.toString(), { am.finish() })
                                } else {
                                    getSearcher().replaceCurrentMatch(et.text.toString())
                                    am.finish()
                                }
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
                return false
            }

            override fun onDestroyActionMode(p1: ActionMode) {
                startedActionMode = ACTION_MODE_NONE
                getSearcher().stopSearch()
            }
        }

        startActionMode(callback)
    }

    fun getSearcher(): EditorSearcher = editorSearcher

    fun getEventHandler(): EditorTouchEventHandler = touchHandler

    fun getExtraMarginRight(): Float = extraMarginRight

    fun setExtraMarginRight(extraMarginRight: Float) {
        this.extraMarginRight = extraMarginRight
        requestLayoutIfNeeded()
        createLayout()
        invalidate()
    }

    @Px
    fun getDividerMarginLeft(): Float = dividerMarginLeft

    @Px
    fun getDividerMarginRight(): Float = dividerMarginRight

    fun setDividerMargin(@Px marginLeft: Float, @Px marginRight: Float) {
        if (marginLeft < 0 || marginRight < 0) {
            throw IllegalArgumentException("margin can not be under zero")
        }
        dividerMarginLeft = marginLeft
        dividerMarginRight = marginRight
        requestLayoutIfNeeded()
        createLayout()
        invalidate()
    }

    fun setDividerMargin(@Px margin: Float) {
        setDividerMargin(margin, margin)
    }

    fun setLineNumberMarginLeft(@Px lineNumberMarginLeft: Float) {
        this.lineNumberMarginLeft = lineNumberMarginLeft
        requestLayoutIfNeeded()
        createLayout()
        invalidate()
    }

    @Px
    fun getLineNumberMarginLeft(): Float = lineNumberMarginLeft

    @Px
    fun getDividerWidth(): Float = dividerWidth

    fun setDividerWidth(@Px dividerWidth: Float) {
        if (dividerWidth < 0) {
            throw IllegalArgumentException("width can not be under zero")
        }
        this.dividerWidth = dividerWidth
        requestLayoutIfNeeded()
        invalidate()
    }

    fun getTypefaceLineNumber(): Typeface? = renderer.paintOther.typeface

    fun setTypefaceLineNumber(typefaceLineNumber: Typeface?) {
        renderer.setTypefaceLineNumber(typefaceLineNumber)
        requestLayoutIfNeeded()
    }

    fun getTypefaceText(): Typeface? = renderer.paint.typeface

    fun setTypefaceText(typefaceText: Typeface?) {
        renderer.setTypefaceText(typefaceText)
        requestLayoutIfNeeded()
    }

    fun getTextScaleX(): Float = renderer.paint.textScaleX

    fun setTextScaleX(textScaleX: Float) {
        renderer.setTextScaleX(textScaleX)
    }

    fun getTextLetterSpacing(): Float = renderer.paint.letterSpacing

    fun setTextLetterSpacing(textLetterSpacing: Float) {
        renderer.setLetterSpacing(textLetterSpacing)
        requestLayoutIfNeeded()
    }

    fun setLineNumberAlign(align: android.graphics.Paint.Align?) {
        lineNumberAlign = align ?: android.graphics.Paint.Align.LEFT
        invalidate()
    }

    fun setCursorWidth(@Px width: Float) {
        if (width < 0) {
            throw IllegalArgumentException("width can not be under zero")
        }
        insertSelectionWidth = width
        invalidate()
    }

    @Px
    fun getInsertSelectionWidth(): Float = insertSelectionWidth

    fun setTextBorderWidth(@Px width: Float) {
        if (width < 0) {
            throw IllegalArgumentException("width can not be under zero")
        }
        textBorderWidth = width
        invalidate()
    }

    @Px
    fun getTextBorderWidth(): Float = textBorderWidth

    fun getCursor(): Cursor = cursor

    fun getLineCount(): Int = text.lineCount

    val lineCount: Int
        get() = text.lineCount

    fun getFirstVisibleLine(): Int {
        return try {
            layout?.getLineNumberForRow(getFirstVisibleRow()) ?: 0
        } catch (e: IndexOutOfBoundsException) {
            0
        }
    }

    fun getFirstVisibleRow(): Int {
        if (layout == null) return offsetY / logicalRowHeight
        return layout!!.getRowIndexForY(offsetY)
    }

    fun getLastVisibleRow(): Int {
        if (layout == null) return (offsetY + height) / logicalRowHeight
        return max(0, min(layout!!.rowCount - 1, layout!!.getRowIndexForY(offsetY + height)))
    }

    fun getLastVisibleLine(): Int {
        return try {
            layout?.getLineNumberForRow(getLastVisibleRow()) ?: (lineCount - 1)
        } catch (e: IndexOutOfBoundsException) {
            lineCount - 1
        }
    }

    fun isRowVisible(row: Int): Boolean {
        return getFirstVisibleRow() <= row && row <= getLastVisibleRow()
    }

    fun setLineSpacing(add: Float, mult: Float) {
        lineSpacingAdd = add
        lineSpacingMultiplier = mult
        requestLayout()
        invalidate()
    }

    fun setWrapLineSpacing(add: Float, mult: Float) {
        wrapLineSpacingAdd = add
        wrapLineSpacingMultiplier = mult
        requestLayout()
        invalidate()
    }

    fun getLineSpacingExtra(): Float = lineSpacingAdd

    fun setLineSpacingExtra(lineSpacingExtra: Float) {
        lineSpacingAdd = lineSpacingExtra
        invalidate()
    }

    fun getLineSpacingMultiplier(): Float = lineSpacingMultiplier

    fun setLineSpacingMultiplier(lineSpacingMultiplier: Float) {
        this.lineSpacingMultiplier = lineSpacingMultiplier
        invalidate()
    }

    fun getRowTopOfText(row: Int): Int {
        return getRowTop(row) + getLineSpacingPixels() / 2
    }

    fun getRowBottomOfText(row: Int): Int {
        return getRowBottom(row) - getLineSpacingPixels() / 2
    }

    fun getRowHeightOfText(): Int {
        val metrics = renderer.metricsText
        return metrics.descent - metrics.ascent
    }

    @UnsupportedUserUsage
    fun setLayoutBusy(busy: Boolean) {
        if (isLayoutBusy && !busy) {
            if (wordwrap && touchHandler.positionNotApplied) {
                touchHandler.positionNotApplied = false
                val line = IntPair.getFirst(touchHandler.memoryPosition)
                val column = IntPair.getSecond(touchHandler.memoryPosition)
                // Compute new scroll position
                val row = (layout as WordwrapLayout).findRow(line, column)
                val afterScrollY = row * rowHeight - touchHandler.focusY
                val scroller = touchHandler.scroller
                dispatchEvent(ScrollEvent(this, scroller.currX, scroller.currY, 0, afterScrollY.toInt(), ScrollEvent.CAUSE_SCALE_TEXT))
                scroller.startScroll(0, afterScrollY.toInt(), 0, 0, 0)
                scroller.abortAnimation()
            }
            // IMPORTANT restart input after clearing the busy flag
            // otherwise, the connection may fallback to inactive mode
            this.isLayoutBusy = false
            restartInput()
            postInvalidate()
            dispatchEvent(LayoutStateChangeEvent(this, false))
            return
        }
        if (isLayoutBusy == busy) {
            return
        }
        this.isLayoutBusy = busy
        dispatchEvent(LayoutStateChangeEvent(this, busy))
    }

    fun isEditable(): Boolean {
        return editable && !isLayoutBusy && !isFormatting
    }

    fun isWordwrap(): Boolean = wordwrap
    fun isHighlightCurrentLine(): Boolean = highlightCurrentLine
    fun setHighlightCurrentLine(enabled: Boolean) {
        highlightCurrentLine = enabled
        invalidate()
    }
    fun isDisplayLnPanel(): Boolean = displayLnPanel
    fun setDisplayLnPanel(enabled: Boolean) {
        displayLnPanel = enabled
        invalidate()
    }
    
    override fun isHorizontalScrollBarEnabled(): Boolean = horizontalScrollBarEnabled
    override fun setHorizontalScrollBarEnabled(horizontalScrollBarEnabled: Boolean) {
        this.horizontalScrollBarEnabled = horizontalScrollBarEnabled
        invalidate()
    }
    
    override fun isVerticalScrollBarEnabled(): Boolean = verticalScrollBarEnabled
    override fun setVerticalScrollBarEnabled(verticalScrollBarEnabled: Boolean) {
        this.verticalScrollBarEnabled = verticalScrollBarEnabled
        invalidate()
    }

    fun getTabWidth(): Int = tabWidth
    fun setTabWidth(width: Int) {
        this.tabWidth = width
        invalidate()
    }

    fun getScrollMaxX(): Int {
        return (layout?.width ?: 0).toInt() - width + paddingLeft + paddingRight
    }

    fun getScrollMaxY(): Int {
        return (layout?.height ?: 0).toInt() - height + paddingTop + paddingBottom
    }

    fun isTextSelected(): Boolean = cursor.isSelected

    fun setSelectionAround(line: Int, column: Int) {
        setSelection(line, column)
    }

    fun getEditable(): Boolean = editable

    fun setEditable(editable: Boolean) {
        this.editable = editable
        if (!editable) {
            hideSoftInput()
            snippetController.stopSnippet()
        }
    }

    var isScalable: Boolean
        get() = scalable
        set(value) {
            scalable = value
        }

    fun isBlockLineEnabled(): Boolean = blockLineEnabled

    fun setBlockLineEnabled(enabled: Boolean) {
        blockLineEnabled = enabled
        invalidate()
    }

    fun beginComposingTextRejection() {
        rejectComposingCount++
    }

    fun acceptsComposingText(): Boolean {
        return rejectComposingCount == 0
    }

    fun endComposingTextRejection() {
        rejectComposingCount--
        if (rejectComposingCount < 0) {
            rejectComposingCount = 0
        }
    }

    fun hasMouseHovering(): Boolean = mouseHover

    fun hasMousePressed(): Boolean = mouseButtonPressed

    fun isInMouseMode(): Boolean {
        when (props.mouseMode) {
            DirectAccessProps.MOUSE_MODE_ALWAYS -> return true
            DirectAccessProps.MOUSE_MODE_NEVER -> return false
        }
        return hasMouseHovering() || hasMousePressed()
    }

    protected fun getSelectingTarget(): CharPosition {
        return if (cursor.left().equals(selectionAnchor)) {
            cursor.right()
        } else {
            cursor.left()
        }
    }

    protected fun ensureSelectingTargetVisible() {
        if (cursor.left().equals(selectionAnchor)) {
            ensureSelectionVisible()
        } else {
            ensurePositionVisible(cursor.leftLine, cursor.leftColumn)
        }
    }

    protected fun ensureSelectionAnchorAvailable() {
        if (selectionAnchor == null || !text.isValidPosition(selectionAnchor!!)) {
            selectionAnchor = cursor.right()
        }
    }

    fun moveOrExtendSelection(@NonNull movement: SelectionMovement, extend: Boolean) {
        if (extend) {
            extendSelection(movement)
        } else {
            moveSelection(movement)
        }
    }

    fun extendSelection(@NonNull movement: SelectionMovement) {
        ensureSelectionAnchorAvailable()
        val sel = movement.getPositionAfterMovement(this, getSelectingTarget())
        setSelectionRegion(selectionAnchor!!.line, selectionAnchor!!.column, sel.line, sel.column, false, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
        if (movement === SelectionMovement.PAGE_UP) {
            touchHandler.scrollBy(0f, (-height).toFloat(), true)
        } else if (movement === SelectionMovement.PAGE_DOWN) {
            touchHandler.scrollBy(0f, height.toFloat(), true)
        }
        ensureSelectingTargetVisible()
    }

    fun moveSelection(@NonNull movement: SelectionMovement) {
        if (cursor.isSelected) {
            if (movement === SelectionMovement.LEFT) {
                setSelection(cursor.leftLine, cursor.leftColumn, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
                return
            }
            if (movement === SelectionMovement.RIGHT) {
                setSelection(cursor.rightLine, cursor.rightColumn, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
                return
            }
        }
        val pos = when (movement.basePosition) {
            SelectionMovement.BasePosition.LEFT_SELECTION -> cursor.left()
            SelectionMovement.BasePosition.RIGHT_SELECTION -> cursor.right()
            else -> {
                ensureSelectionAnchorAvailable()
                selectionAnchor!!
            }
        }
        val sel = movement.getPositionAfterMovement(this, pos)
        if (movement === SelectionMovement.PAGE_UP) {
            touchHandler.scrollBy(0f, (-height).toFloat(), true)
        } else if (movement === SelectionMovement.PAGE_DOWN) {
            touchHandler.scrollBy(0f, height.toFloat(), true)
        }
        setSelection(sel.line, sel.column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
    }

    private object SelectionMovement_BasePosition {
        const val LEFT_SELECTION = 0
        const val RIGHT_SELECTION = 1
    }

    fun setSelection(line: Int, column: Int) {
        setSelection(line, column, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    fun setSelection(line: Int, column: Int, cause: Int) {
        setSelection(line, column, true, cause)
    }

    fun setSelection(line: Int, column: Int, makeItVisible: Boolean) {
        setSelection(line, column, makeItVisible, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    fun setSelection(line: Int, column: Int, makeItVisible: Boolean, cause: Int) {
        cursorAnimator.markStartPos()
        var col = column
        if (col > 0 && Character.isHighSurrogate(text.charAt(line, col - 1))) {
            col++
            if (col > text.getColumnCount(line)) {
                col--
            }
        }
        cursor.set(line, col)
        if (highlightCurrentBlock) {
            cursorPosition = findCursorBlock()
        }
        updateCursor()
        updateSelection()
        if (isEditable() && !touchHandler.hasAnyHeldHandle() && acceptsComposingText()) {
            cursorAnimator.markEndPos()
            cursorAnimator.start()
        }

        selectionAnchor = cursor.right()

        renderContext.invalidateRenderNodes()
        if (makeItVisible) {
            ensurePositionVisible(line, col)
        } else {
            invalidate()
        }
        onSelectionChanged(cause)
    }

    private fun findCursorBlock(): Int {
        val blocks = if (textStyles == null) null else textStyles!!.blocks
        if (blocks == null || blocks.isEmpty()) {
            return -1
        }
        return findCursorBlock(blocks)
    }

    private fun findCursorBlock(blocks: List<CodeBlock>): Int {
        var left = 0
        var right = blocks.size - 1
        val cursorIndex = cursor.left
        while (left <= right) {
            val mid = (left + right) / 2
            val block = blocks[mid]
            if (cursorIndex < block.startOffset) {
                right = mid - 1
            } else if (cursorIndex > block.endOffset) {
                left = mid + 1
            } else {
                return mid
            }
        }
        return -1
    }

    fun selectAll() {
        setSelectionRegion(0, 0, lineCount - 1, text.getColumnCount(lineCount - 1))
    }

    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int, cause: Int) {
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, cause)
    }

    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int) {
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int, makeRightVisible: Boolean) {
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, makeRightVisible, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int, makeRightVisible: Boolean, cause: Int) {
        requestFocus()
        val start = text.getCharIndex(lineLeft, columnLeft)
        val end = text.getCharIndex(lineRight, columnRight)
        if (start == end) {
            setSelection(lineLeft, columnLeft, makeRightVisible, cause)
            return
        }
        if (start > end) {
            setSelectionRegion(lineRight, columnRight, lineLeft, columnLeft, makeRightVisible, cause)
            Log.w(LOG_TAG, "setSelectionRegion() error: start > end:start = $start end = $end lineLeft = $lineLeft columnLeft = $columnLeft lineRight = $lineRight columnRight = $columnRight")
            return
        }
        cursorAnimator.cancel()
        var colLeft = columnLeft
        var colRight = columnRight
        if (colLeft > 0) {
            val ch = text.charAt(lineLeft, colLeft - 1)
            if (Character.isHighSurrogate(ch)) {
                colLeft++
                if (colLeft > text.getColumnCount(lineLeft)) {
                    colLeft--
                }
            }
        }
        if (colRight > 0) {
            val ch = text.charAt(lineRight, colRight - 1)
            if (Character.isHighSurrogate(ch)) {
                colRight++
                if (colRight > text.getColumnCount(lineRight)) {
                    colRight--
                }
            }
        }
        cursor.setLeft(lineLeft, colLeft)
        cursor.setRight(lineRight, colRight)
        updateCursor()
        updateSelection()
        renderContext.invalidateRenderNodes()

        // Update selection anchor
        if (!cursor.left().equals(selectionAnchor) && !cursor.right().equals(selectionAnchor)) {
            selectionAnchor = cursor.right()
        }

        if (makeRightVisible) {
            if (cause == SelectionChangeEvent.CAUSE_SEARCH) {
                ensurePositionVisible(lineLeft, colLeft)
                lastMakeVisible = 0
                ensurePositionVisible(lineRight, colRight)
            } else {
                ensurePositionVisible(lineRight, colRight)
            }
        } else {
            invalidate()
        }
        onSelectionChanged(cause)
    }

    fun getClipboardManager(): ClipboardManager = clipboardManager

    fun pasteText() {
        try {
            val clip: ClipData? = if (!clipboardManager.hasPrimaryClip()) null else clipboardManager.primaryClip
            if (clip == null) {
                return
            }
            pasteText(ClipDataUtils.clipDataToString(clip))
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error pasting text to editor", e)
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteText(@Nullable text: CharSequence?) {
        if (text != null && inputConnection != null) {
            inputConnection.commitText(text, 1)
            if (props.formatPastedText) {
                formatCodeAsync(lastInsertion!!.start, lastInsertion!!.end)
            }
            notifyIMEExternalCursorChange()
        }
    }

    fun copyText() {
        copyText(true)
    }

    fun copyText(shouldCopyLine: Boolean) {
        if (cursor.isSelected) {
            copyTextToClipboard(text, cursor.left, cursor.right)
        } else if (shouldCopyLine) {
            copyLine()
        } else {
            val separator = getLineSeparator().content
            copyTextToClipboard(separator, 0, separator.length)
        }
    }

    protected fun copyTextToClipboard(@NonNull text: CharSequence, start: Int, end: Int) {
        if (end < start) {
            return
        }
        if (end - start > props.clipboardTextLengthLimit) {
            Toast.makeText(context, I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val clip = if (text is Content) text.substring(start, end) else text.subSequence(start, end).toString()
            clipboardManager.setPrimaryClip(ClipData.newPlainText(clip, clip))
        } catch (e: RuntimeException) {
            if (e.cause is TransactionTooLargeException) {
                Toast.makeText(context, I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large), Toast.LENGTH_SHORT).show()
            } else {
                Log.w(LOG_TAG, e)
                Toast.makeText(context, e.javaClass.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyLine() {
        val cur = getCursor()
        if (cur.isSelected) {
            copyText()
            return
        }

        val line = cur.leftLine
        setSelectionRegion(line, 0, line, text.getColumnCount(line))
        copyText(false)
    }

    fun cutText() {
        if (cursor.isSelected) {
            copyText()
            deleteText()
            notifyIMEExternalCursorChange()
        } else {
            cutLine()
        }
    }

    fun cutLine() {
        val cur = getCursor()
        if (cur.isSelected) {
            cutText()
            return
        }

        val left = cur.left()
        val line = left.line

        if (line + 1 == lineCount) {
            val columnCount = text.getColumnCount(line)
            if (columnCount == 0) {
                // copy line separator
                copyText(false)
                return
            }
            setSelectionRegion(line, 0, line, text.getColumnCount(line))
        } else {
            setSelectionRegion(line, 0, line + 1, 0)
        }

        cutText()
        if (props.placeSelOnPreviousLineAfterCut) {
            moveSelection(SelectionMovement.LEFT)
        }
    }

    fun duplicateLine() {
        val cur = getCursor()
        if (cur.isSelected) {
            duplicateSelection()
            return
        }

        val left = cur.left()
        setSelectionRegion(left.line, 0, left.line, text.getColumnCount(left.line), true)
        duplicateSelection("\n", false)
    }

    fun duplicateSelection() {
        duplicateSelection(true)
    }

    fun duplicateSelection(selectDuplicate: Boolean) {
        duplicateSelection("", selectDuplicate)
    }

    fun duplicateSelection(prefix: String, selectDuplicate: Boolean) {
        val cur = getCursor()
        if (!cur.isSelected) {
            return
        }

        val left = cur.left()
        val right = cur.right().fromThis()
        val sub = text.subContent(left.line, left.column, right.line, right.column)

        setSelection(right.line, right.column)
        commitText(prefix + sub, false)

        if (selectDuplicate) {
            val r = cur.right()
            setSelectionRegion(right.line, right.column, r.line, r.column)
        }
    }

    fun selectCurrentWord() {
        val left = getCursor().left()
        selectWord(left.line, left.column)
    }

    fun selectWord(line: Int, column: Int) {
        val range = getWordRange(line, column)
        val start = range.start
        val end = range.end
        setSelectionRegion(start.line, start.column, end.line, end.column, SelectionChangeEvent.CAUSE_LONG_PRESS)
    }

    fun getWordRange(line: Int, column: Int): TextRange {
        return getWordRange(line, column, props.useICULibToSelectWords)
    }

    fun getWordRange(line: Int, column: Int, useIcu: Boolean): TextRange {
        return Chars.getWordRange(text, line, column, useIcu)
    }

    @NonNull
    fun getText(): Content = text

    fun setText(@Nullable text: CharSequence?) {
        setText(text, true, null)
    }

    @NonNull
    fun getExtraArguments(): Bundle {
        if (extraArguments == null) {
            extraArguments = Bundle()
        }
        return extraArguments!!
    }

    fun setText(@Nullable text: CharSequence?, @Nullable extraArguments: Bundle?) {
        setText(text, true, extraArguments)
    }

    fun setText(@Nullable text: CharSequence?, reuseContentObject: Boolean, @Nullable extraArguments: Bundle?) {
        var newText = text ?: ""

        if (::text.isInitialized) {
            this.text.removeContentListener(this)
            this.text.resetBatchEdit()
        }
        this.extraArguments = extraArguments ?: Bundle()
        lastInsertion = null
        if (reuseContentObject && newText is Content) {
            this.text = newText
            this.text.resetBatchEdit()
            renderer.updateTimestamp()
        } else {
            this.text = Content(newText)
        }
        styleDelegate.reset()
        textStyles = null
        cursor = this.text.cursor
        selectionAnchor = cursor.right()
        touchHandler.reset()
        this.text.addContentListener(this)
        this.text.isUndoEnabled = undoEnabled
        this.text.isBidiEnabled = true
        renderContext.reset(this.text.lineCount)
        renderer.onEditorFullTextUpdate()

        if (::editorLanguage.isInitialized) {
            editorLanguage.analyzeManager.reset(ContentReference(this.text), this.extraArguments!!)
            editorLanguage.formatter.cancel()
        }
        inlayHints = null

        dispatchEvent(
            ContentChangeEvent(
                this, ContentChangeEvent.ACTION_SET_NEW_TEXT, CharPosition(),
                this.text.indexer.getCharPosition(lineCount - 1, this.text.getColumnCount(lineCount - 1)),
                this.text, false
            )
        )
        createLayout()
        if (inputMethodManager != null) {
            inputMethodManager.restartInput(this)
        }
        requestLayout()
    }

    fun deleteText() {
        inputConnection.deleteText()
    }

    fun commitText(text: CharSequence, selectionOffset: Int) {
        inputConnection.commitText(text, selectionOffset)
    }
    fun setTextSize(textSize: Float) {
        val res = context?.resources ?: Resources.getSystem()
        setTextSizePx(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, res.displayMetrics))
    }

    fun setTextSizePx(size: Float) {
        renderer.setTextSize(size)
        requestLayoutIfNeeded()
    }

    var textSizePx: Float
        get() = renderer.paint.textSize
        set(value) = setTextSizePx(value)

    var isRenderFunctionCharacters: Boolean
        get() = renderFunctionCharacters
        set(value) {
            if (this.renderFunctionCharacters != value) {
                this.renderFunctionCharacters = value
                renderer.onTextStyleUpdate()
                requestLayoutIfNeeded()
                createLayout()
                invalidate()
            }
        }

    fun <T : Event> subscribeEvent(eventType: Class<T>, receiver: EventReceiver<T>): SubscriptionReceipt<T> {
        return eventManager.subscribeEvent(eventType, receiver)
    }

    fun <T : Event> subscribeAlways(eventType: Class<T>, receiver: EventManager.NoUnsubscribeReceiver<T>): SubscriptionReceipt<T> {
        return eventManager.subscribeAlways(eventType, receiver)
    }

    fun <T : Event> dispatchEvent(event: T): Int {
        return eventManager.dispatchEvent(event)
    }

    fun createSubEventManager(): EventManager {
        return EventManager(eventManager)
    }

    val isFormatting: Boolean
        get() = ::editorLanguage.isInitialized && editorLanguage.formatter.isRunning

    var isLineNumberEnabled: Boolean
        get() = lineNumberEnabled
        set(value) {
            if (value != this.lineNumberEnabled && isWordwrap) {
                createLayout()
            }
            this.lineNumberEnabled = value
            invalidate()
        }

    @NonNull
    fun getTextPaint(): Paint = renderer.paint

    fun getOtherPaint(): Paint = renderer.paintOther

    fun getGraphPaint(): Paint = renderer.paintGraph

    @NonNull
    fun getColorScheme(): EditorColorScheme = colorScheme

    fun setColorScheme(@NonNull colors: EditorColorScheme) {
        if (::colorScheme.isInitialized) {
            colorScheme.detachEditor(this)
        }
        colorScheme = colors
        colors.attachEditor(this)
        invalidate()
    }

    fun jumpToLine(line: Int) {
        setSelection(line, 0)
    }

    fun beginLongSelect() {
        if (!isEditable()) {
            return
        }
        if (cursor.isSelected) {
            setSelection(cursor.leftLine, cursor.leftColumn)
        }
        isInLongSelect = true
        invalidate()
    }

    fun endLongSelect() {
        isInLongSelect = false
        invalidate()
    }

    fun setInlayHints(inlayHints: InlayHintsContainer?) {
        val affectedLines = MutableIntSet()
        val oldInlayHints = this.inlayHints
        if (oldInlayHints != null) {
            affectedLines.addAll(oldInlayHints.lineNumbers)
        }
        if (inlayHints != null) {
            affectedLines.addAll(inlayHints.lineNumbers)
        }
        this.inlayHints = inlayHints
        val range = IntSetUpdateRange(affectedLines)
        if (!isLayoutBusy) {
            layout?.invalidateLines(range)
        } else {
            createLayout()
        }
        renderContext.invalidateRenderNodes()
    }

    @Nullable
    fun getInlayHints(): List<InlayHint>? = inlayHints

    fun setHighlightTexts(highlightTexts: HighlightTextContainer?) {
        val affectedLines = MutableIntSet()
        val oldHighlights = this.highlightTextContainer
        if (oldHighlights != null) {
            val lines = oldHighlights.lineNumbers
            for (line in lines) {
                affectedLines.add(line)
            }
        }
        this.highlightTextContainer = highlightTexts
        if (highlightTexts != null) {
            val lines = highlightTexts.lineNumbers
            for (line in lines) {
                affectedLines.add(line)
            }
        }
        if (affectedLines.isEmpty()) {
            return
        }
        if (layout == null) {
            invalidate()
            return
        }
        val range = IntSetUpdateRange(affectedLines)
        if (!isLayoutBusy) {
            layout!!.invalidateLines(range)
        } else {
            createLayout()
        }
        renderContext.invalidateRenderNodes()
        invalidate()
    }

    @Nullable
    fun getHighlightTexts(): List<HighlightText>? = highlightTextContainer

    @Nullable
    fun getLayout(): EditorLayout? = layout

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (wordwrap) {
            createLayout()
        }
        touchHandler.onSizeChanged(w, h, oldw, oldh)
    }

    protected fun createLayout() {
        if (layout != null) {
            layout!!.destroy()
        }
        val factory = layoutFactory ?: if (wordwrap) wordwrapLayoutFactory else simpleLayoutFactory
        layout = factory.createLayout(this, text)
        renderContext.reset(text.lineCount)
        requestLayout()
        invalidate()
        dispatchEvent(LayoutStateChangeEvent(this, false))
    }

    protected fun requestLayoutIfNeeded() {
        if (!wordwrap) {
            requestLayout()
        }
    }

    fun ensurePositionVisible(line: Int, column: Int) {
        if (layout == null) {
            return
        }
        val layoutOffset = layout!!.getCharLayoutOffset(line, column)
        val x = layoutOffset[1]
        val y = layoutOffset[0]
        val width = width
        val height = height
        val textRegionOffset = measureTextRegionOffset()
        val scrollMaxX = getScrollMaxX()
        val scrollMaxY = getScrollMaxY()

        var scrollX = offsetX
        var scrollY = offsetY

        val row = layout!!.getRowIndexForY(y.toInt())
        val rowHeight = getRowHeight(row)
        val rowTop = getRowTop(row)
        val rowBottom = getRowBottom(row)

        if (x + textRegionOffset < offsetX + textRegionOffset) {
            scrollX = x.toInt()
        } else if (x + textRegionOffset + insertSelectionWidth > offsetX + width) {
            scrollX = (x + textRegionOffset + insertSelectionWidth - width).toInt()
        }

        if (rowTop < offsetY) {
            scrollY = rowTop
        } else if (rowBottom > offsetY + height) {
            scrollY = rowBottom - height
        }

        scrollX = max(0, min(scrollMaxX, scrollX))
        scrollY = max(0, min(scrollMaxY, scrollY))

        if (scrollX != offsetX || scrollY != offsetY) {
            val scroller = touchHandler.scroller
            if (System.currentTimeMillis() - lastMakeVisible < 100) {
                scroller.startScroll(scroller.currX, scroller.currY, scrollX - scroller.currX, scrollY - scroller.currY, 0)
            } else {
                scroller.startScroll(scroller.currX, scroller.currY, scrollX - scroller.currX, scrollY - scroller.currY)
            }
            lastMakeVisible = System.currentTimeMillis()
            invalidate()
        }
    }

    fun ensureSelectionVisible() {
        ensurePositionVisible(cursor.rightLine, cursor.rightColumn)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        dispatchEvent(ScrollEvent(this, oldl, oldt, l, t, ScrollEvent.CAUSE_USER_DRAG))
    }

    override fun computeHorizontalScrollRange(): Int = (layout?.layoutWidth ?: 0f).toInt() + measureTextRegionOffset().toInt()

    override fun computeVerticalScrollRange(): Int = (layout?.layoutHeight ?: 0f).toInt()

    override fun computeHorizontalScrollOffset(): Int = offsetX

    override fun computeVerticalScrollOffset(): Int = offsetY

    fun endLongSelect() {
        isInLongSelect = false
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            cursorBlink.valid = true
            postInLifecycle(cursorBlink)
        } else {
            cursorBlink.valid = false
            removeFromLifecycle(cursorBlink)
        }
        invalidate()
        if (inputMethodManager != null && focused) {
            inputMethodManager.showSoftInput(this, 0)
        }
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!isEditable()) {
            return null
        }
        outAttrs.inputType = inputType
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outAttrs.hintLocales = context.resources.configuration.locales
        }
        inputConnection = EditorInputConnection(this)
        return inputConnection
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (inputMethodManager != null) {
            inputMethodManager.restartInput(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isFocused) {
            cursorBlink.valid = true
            postInLifecycle(cursorBlink)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cursorBlink.valid = false
        removeFromLifecycle(cursorBlink)
    }

    fun postInLifecycle(runnable: Runnable): Boolean {
        return post(runnable)
    }

    fun removeFromLifecycle(runnable: Runnable): Boolean {
        return removeCallbacks(runnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var measuredWidth = 0
        var measuredHeight = 0

        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = min(measuredWidth, widthSize)
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = min(measuredHeight, heightSize)
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun measureLineNumber(): Float {
        if (!isLineNumberEnabled) {
            return 0f
        }
        val count = lineCount
        val text = count.toString()
        val metrics = renderer.metricsOther
        return renderer.paintOther.measureText(text) + lineNumberMarginLeft
    }

    fun setWordwrap(wordwrap: Boolean) {
        setWordwrap(wordwrap, false, true)
    }

    fun setWordwrap(wordwrap: Boolean, wordwrapSingleLineTerminated: Boolean) {
        setWordwrap(wordwrap, wordwrapSingleLineTerminated, true)
    }

    fun setWordwrap(wordwrap: Boolean, wordwrapSingleLineTerminated: Boolean, recreateLayout: Boolean) {
        this.wordwrap = wordwrap
        this.wordwrapSingleLineTerminated = wordwrapSingleLineTerminated
        if (recreateLayout) {
            createLayout()
        }
    }

    fun isWordwrap(): Boolean = wordwrap

    fun isWordwrapSingleLineTerminated(): Boolean = wordwrapSingleLineTerminated

    fun setFormatTipLineCountThreshold(threshold: Int) {
        formatTipLineCountThreshold = threshold
    }

    fun rerunAnalysis() {
        if (::editorLanguage.isInitialized) {
            editorLanguage.analyzeManager.rerun()
        }
    }

    fun getStyles(): Styles? = textStyles

    fun setStyles(styles: Styles?) {
        textStyles = styles
        if (highlightCurrentBlock) {
            cursorPosition = findCursorBlock()
        }
        renderContext.invalidateRenderNodes()
        renderer.updateTimestamp()
        invalidate()
    }

    fun updateStyles(styles: Styles, range: StyleUpdateRange?) {
        if (textStyles !== styles || range == null) {
            setStyles(styles)
            return
        }
        if (highlightCurrentBlock) {
            cursorPosition = findCursorBlock()
        }
        renderContext.updateForRange(range)
        renderer.updateTimestamp()
        invalidate()
    }

    fun hideAutoCompleteWindow() {
        completionWindow.hide()
    }

    fun getBlockIndex(): Int = cursorPosition

    fun showSoftInput() {
        if (isEditable() && isEnabled) {
            if (isInTouchMode && !isFocused) {
                requestFocusFromTouch()
            }
            if (!isFocused) {
                requestFocus()
            }

            if (checkSoftInputEnabled()) {
                inputMethodManager.showSoftInput(this, 0)
            }
        }
        invalidate()
    }

    override fun hideSoftInput() {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    protected fun checkSoftInputEnabled(): Boolean {
        if (isDisableSoftKbdIfHardKbdAvailable() && KeyboardUtils.isHardKeyboardConnected(context)) {
            return false
        }
        return isSoftKeyboardEnabled()
    }

    fun setSoftKeyboardEnabled(isEnabled: Boolean) {
        if (isSoftKbdEnabled == isEnabled) {
            return
        }
        isSoftKbdEnabled = isEnabled
        hideSoftInput()
        restartInput()
    }

    fun isSoftKeyboardEnabled(): Boolean = isSoftKbdEnabled

    fun setDisableSoftKbdIfHardKbdAvailable(isDisabled: Boolean) {
        if (isDisableSoftKbdOnHardKbd == isDisabled) {
            return
        }
        isDisableSoftKbdOnHardKbd = isDisabled
        hideSoftInput()
        restartInput()
    }

    protected fun updateSelection() {
        if (props.disallowSuggestions) {
            val index = Random().nextInt()
            inputMethodManager.updateSelection(this, index, index, -1, -1)
            return
        }
        if (inputConnection.composingText.preSetComposing) {
            return
        }
        var candidatesStart = -1
        var candidatesEnd = -1
        if (inputConnection.composingText.isComposing) {
            try {
                candidatesStart = inputConnection.composingText.startIndex
                candidatesEnd = inputConnection.composingText.endIndex
            } catch (e: IndexOutOfBoundsException) {
                // Ignored
            }
        }
        inputMethodManager.updateSelection(this, cursor.left, cursor.right, candidatesStart, candidatesEnd)
    }

    protected fun updateExtractedText() {
        if (extractingTextRequest != null) {
            val text = extractText(extractingTextRequest!!)
            inputMethodManager.updateExtractedText(this, extractingTextRequest!!.token, text)
        }
    }

    protected fun setExtracting(request: ExtractedTextRequest?) {
        if (props.disallowSuggestions) {
            extractingTextRequest = null
            return
        }
        extractingTextRequest = request
    }

    protected fun extractText(request: ExtractedTextRequest): ExtractedText? {
        if (props.disallowSuggestions || props.disableTextExtracting) {
            return null
        }
        val cur = getCursor()
        val textResult = ExtractedText()
        val selBegin = cur.left
        val selEnd = cur.right
        var startOffset = 0
        if (request.hintMaxChars == 0) {
            request.hintMaxChars = props.maxIPCTextLength
        }
        if (startOffset + request.hintMaxChars < selBegin) {
            startOffset = selBegin - request.hintMaxChars / 2
            startOffset = max(0, startOffset)
        }
        textResult.text = inputConnection.getTextRegion(startOffset, startOffset + request.hintMaxChars, request.flags)
        textResult.startOffset = startOffset
        textResult.selectionStart = selBegin - startOffset
        textResult.selectionEnd = selEnd - startOffset
        if (keyEventHandler.keyMetaStates.isSelecting) {
            textResult.flags = textResult.flags or ExtractedText.FLAG_SELECTING
        }
        return textResult
    }

    fun notifyIMEExternalCursorChange() {
        updateExtractedText()
        updateSelection()
        updateCursorAnchor()
        if (inputConnection.composingText.isComposing) {
            restartInput()
        }
    }

    fun restartInput() {
        if (::inputConnection.isInitialized) {
            inputConnection.reset()
        }
        if (inputMethodManager != null) {
            inputMethodManager.restartInput(this)
        }
    }

    fun updateCursor() {
        updateCursorAnchor()
        updateExtractedText()
        if (text.nestedBatchEdit <= 1 && !inputConnection.composingText.isComposing) {
            updateSelection()
        }
    }

    fun release() {
        hideEditorWindows()
        if (!released) {
            dispatchEvent(EditorReleaseEvent(this))
        } else {
            return
        }
        released = true
        if (::editorLanguage.isInitialized) {
            editorLanguage.analyzeManager.destroy()
            val formatter = editorLanguage.formatter
            formatter.setReceiver(null)
            formatter.destroy()
            editorLanguage.destroy()
            editorLanguage = EmptyLanguage()
        }

        textStyles = null
        diagnostics = null
        styleDelegate.reset()

        if (::text.isInitialized) {
            text.removeContentListener(this)
        }
        colorScheme.detachEditor(this)
    }

    fun isReleased(): Boolean = released

    fun hideEditorWindows() {
        completionWindow.cancelCompletion()
        completionWindow.hide()
        textActionWindow.dismiss()
        touchHandler.magnifier.dismiss()
        diagnosticTooltip.dismiss()
    }

    fun onColorUpdated(type: Int) {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext.invalidateRenderNodes()
        invalidate()
    }

    fun onColorFullUpdate() {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext.invalidateRenderNodes()
        invalidate()
    }

    protected fun getInputMethodManager(): InputMethodManager? = inputMethodManager

    protected fun onCloseConnection() {
        setExtracting(null)
        invalidate()
    }

    protected fun onCreateRenderer(): EditorRenderer {
        return EditorRenderer(this)
    }

    protected fun onSelectionChanged(cause: Int) {
        var oldLeft: CharPosition? = null
        var oldRight: CharPosition? = null
        val lastTextRange = this.lastSelectedTextRange
        if (lastTextRange != null) {
            oldLeft = lastTextRange.start
            oldRight = lastTextRange.end
        }
        dispatchEvent(SelectionChangeEvent(this, oldLeft, oldRight, cause))
        this.lastSelectedTextRange = getCursorRange()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas)

        // Update magnifier
        val scroller = touchHandler.scroller
        if ((lastCursorState != cursorBlink.visibility || !scroller.isFinished) && touchHandler.magnifier.isShowing) {
            lastCursorState = cursorBlink.visibility
            postInLifecycle { touchHandler.magnifier.updateDisplay() }
        }
    }

    override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo {
        val info = super.createAccessibilityNodeInfo()
        if (isEnabled) {
            info.isEditable = isEditable()
            info.setTextSelection(cursor.left, cursor.right)
            info.inputType = InputType.TYPE_CLASS_TEXT
            info.isMultiLine = true
            info.text = text.toStringBuilder()
            info.isLongClickable = true
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
            val scrollRange = getScrollMaxY()
            if (scrollRange > 0) {
                info.isScrollable = true
                val scrollY = offsetY
                if (scrollY > 0) {
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP)
                    }
                }
                if (scrollY < scrollRange) {
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN)
                    }
                }
            }
        }
        return info
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        val maxScrollY = getScrollMaxY()
        event.isScrollable = maxScrollY > 0
        event.maxScrollX = getScrollMaxX()
        event.maxScrollY = maxScrollY
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_COPY -> {
                copyText()
                return true
            }
            AccessibilityNodeInfo.ACTION_CUT -> {
                cutText()
                return true
            }
            AccessibilityNodeInfo.ACTION_PASTE -> {
                pasteText()
                return true
            }
            AccessibilityNodeInfo.ACTION_SET_TEXT -> {
                setText(arguments?.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE))
                return true
            }
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                moveSelection(SelectionMovement.PAGE_DOWN)
                return true
            }
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                moveSelection(SelectionMovement.PAGE_UP)
                return true
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (action) {
                android.R.id.accessibilityActionScrollDown -> {
                    moveSelection(SelectionMovement.PAGE_UP)
                    return true
                }
                android.R.id.accessibilityActionScrollUp -> {
                    moveSelection(SelectionMovement.PAGE_DOWN)
                    return true
                }
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun getAccessibilityClassName(): CharSequence {
        return CodeEditor::class.java.name
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                if (forceHorizontalScrollable) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - downX
                if (forceHorizontalScrollable && !touchHandler.hasAnyHeldHandle()) {
                    if ((deltaX > 0 && touchHandler.scroller.currX == 0)
                        || (deltaX < 0 && touchHandler.scroller.currX == getScrollMaxX())
                    ) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): PointerIcon? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                if (isFormatting || isLayoutBusy) {
                    return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_WAIT)
                }
                if (touchHandler.hasAnyHeldHandle()) {
                    return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_GRABBING)
                }
                if (getLeftHandleDescriptor().position.contains(event.x, event.y)
                    || getRightHandleDescriptor().position.contains(event.x, event.y)
                    || getInsertHandleDescriptor().position.contains(event.x, event.y)
                ) {
                    return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_GRAB)
                }
                val res = RegionResolver.resolveTouchRegion(this, event, pointerIndex)
                val region = IntPair.getFirst(res)
                val inbound = IntPair.getSecond(res) == RegionResolver.IN_BOUND
                if (region == RegionResolver.REGION_TEXT && inbound) {
                    if (touchHandler.mouseCanMoveText && !touchHandler.mouseClick) {
                        return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_GRABBING)
                    }
                    if (renderer.lastStickyLines != null) {
                        val stickyLineCount = renderer.lastStickyLines.size
                        if (stickyLineCount > 0 && event.y < getRowBottom(stickyLineCount - 1)) {
                            return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_HAND)
                        }
                    }
                    return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_TEXT)
                } else if (region == RegionResolver.REGION_LINE_NUMBER) {
                    when (props.actionWhenLineNumberClicked) {
                        DirectAccessProps.LN_ACTION_SELECT_LINE,
                        DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> {
                            return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_HAND)
                        }
                    }
                }
                return super.onResolvePointerIcon(event, pointerIndex)
            }
        }
        return super.onResolvePointerIcon(event, pointerIndex)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) && props.mouseMode != DirectAccessProps.MOUSE_MODE_NEVER) {
            return touchHandler.onMouseEvent(event)
        }
        if (isFormatting) {
            touchHandler.reset2()
            scaleDetector.onTouchEvent(event)
            return basicDetector.onTouchEvent(event)
        }
        val handlingBefore = touchHandler.handlingMotions()
        val res = touchHandler.onTouchEvent(event)
        val handling = touchHandler.handlingMotions()
        var res2 = false
        val res3 = scaleDetector.onTouchEvent(event)
        if (!handling && !handlingBefore) {
            res2 = basicDetector.onTouchEvent(event)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            edgeEffectVertical.onRelease()
            edgeEffectHorizontal.onRelease()
        }
        return res3 || res2 || res
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyUp(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyMultiple(keyCode, repeatCount, event)
    }

    fun onSuperKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    fun onSuperKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    fun onSuperKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var wSpec = widthMeasureSpec
        var hSpec = heightMeasureSpec
        if (MeasureSpec.getMode(wSpec) != MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(hSpec) != MeasureSpec.EXACTLY
        ) {
            Log.w(LOG_TAG, "use wrap_content in editor may cause layout lags")
            val specs = ViewMeasureHelper.getDesiredSize(
                wSpec, hSpec, measureTextRegionOffset(),
                rowHeight, wordwrap, tabWidth, text, renderer.paintGeneral
            )
            wSpec = IntPair.getFirst(specs)
            hSpec = IntPair.getSecond(specs)
            anyWrapContentSet = true
        } else {
            anyWrapContentSet = false
        }
        super.onMeasure(wSpec, hSpec)
    }

    override fun onDragEvent(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> return true
            DragEvent.ACTION_DRAG_LOCATION -> {
                val pos = getPointPositionOnScreen(event.x, event.y)
                val line = IntPair.getFirst(pos)
                val column = IntPair.getSecond(pos)
                touchHandler.draggingSelection = text.indexer.getCharPosition(line, column)
                postInvalidate()
                touchHandler.scrollIfReachesEdge(null, event.x, event.y)
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                touchHandler.draggingSelection = null
                postInvalidate()
                return true
            }
            DragEvent.ACTION_DROP -> {
                val targetPos = touchHandler.draggingSelection
                if (targetPos == null) {
                    return false
                }
                touchHandler.draggingSelection = null
                setSelection(targetPos.line, targetPos.column)
                pasteText(ClipDataUtils.clipDataToString(event.clipData))
                requestFocus()
                postInvalidate()
                // Call super for notifying listeners
                super.onDragEvent(event)
                return true
            }
        }
        return super.onDragEvent(event)
    }

    override fun onCreateContextMenu(menu: ContextMenu) {
        super.onCreateContextMenu(menu)
        val pos = touchHandler.lastContextClickPosition
        if (pos == null) {
            return
        }
        val charPosRes = getPointPositionOnScreen(pos.x, pos.y)
        dispatchEvent(
            CreateContextMenuEvent(
                this, menu, text.indexer.getCharPosition(
                    IntPair.getFirst(charPosRes), IntPair.getSecond(charPosRes)
                )
            )
        )
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                mouseHover = true
            } else if (event.action == MotionEvent.ACTION_HOVER_EXIT) {
                mouseHover = false
            }
            if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
                || event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE
            ) {
                mouseButtonPressed = event.buttonState != 0
            }
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER,
                MotionEvent.ACTION_HOVER_MOVE,
                MotionEvent.ACTION_HOVER_EXIT -> {
                    touchHandler.dispatchEditorMotionEvent({ e -> HoverEvent(this, e) }, null, event)
                    return true
                }
            }
        }
        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER) && !keyEventHandler.keyMetaStates.isCtrlPressed) {
            val vScroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val hScroll = -event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            var distanceX = hScroll * verticalScrollFactor * props.mouseWheelScrollFactor
            var distanceY = vScroll * verticalScrollFactor * props.mouseWheelScrollFactor
            if (keyEventHandler.keyMetaStates.isAltPressed) {
                val multiplier = props.fastScrollSensitivity
                distanceX *= multiplier
                distanceY *= multiplier
            }
            touchHandler.scrollBy(distanceX, distanceY)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun beforeReplace(content: Content) {
        waitForNextChange = true
        layout?.beforeReplace(content)
    }

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        renderContext.updateForInsertion(startLine, endLine)
        renderer.updateTimestamp()
        styleDelegate.onTextChange()
        val start = text.indexer.getCharPosition(startLine, startColumn)
        val end = text.indexer.getCharPosition(endLine, endColumn)

        try {
            textStyles?.adjustOnInsert(start, end)
            diagnostics?.shiftOnInsert(start.index, end.index)
            inlayHints?.updateOnInsertion(startLine, startColumn, endLine, endColumn)
            highlightTextContainer?.updateOnInsertion(startLine, startColumn, endLine, endColumn)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Update failure", e)
        }

        layout?.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        renderer.buildMeasureCacheForLines(startLine, endLine)
        checkForRelayoutIfNeeded()

        if (::editorLanguage.isInitialized) {
            editorLanguage.analyzeManager.insert(start, end, insertedContent)
        }
        touchHandler.hideInsertHandle()
        if (editable && !cursor.isSelected && !inputConnection.composingText.isComposing && acceptsComposingText()) {
            cursorAnimator.markEndPos()
            cursorAnimator.start()
        }
        selectionAnchor = if (lastAnchorIsSelLeft) cursor.left() else cursor.right()
        dispatchEvent(ContentChangeEvent(this, ContentChangeEvent.ACTION_INSERT, start, end, insertedContent, text.isUndoManagerWorking))
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)
        lastInsertion = TextRange(start.fromThis(), end.fromThis())
        waitForNextChange = false
        ensureSelectionVisible()
        updateCursor()
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        renderContext.updateForDeletion(startLine, endLine)
        renderer.updateTimestamp()
        styleDelegate.onTextChange()
        val start = text.indexer.getCharPosition(startLine, startColumn)
        val end = start.fromThis()
        end.column = endColumn
        end.line = endLine
        end.index = start.index + deletedContent.length

        try {
            textStyles?.adjustOnDelete(start, end)
            diagnostics?.shiftOnDelete(start.index, end.index)
            inlayHints?.updateOnDeletion(startLine, startColumn, endLine, endColumn)
            highlightTextContainer?.updateOnDeletion(startLine, startColumn, endLine, endColumn)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Update failure", e)
        }

        layout?.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        renderer.buildMeasureCacheForLines(startLine, startLine + 1)
        checkForRelayoutIfNeeded()

        if (editable && !cursor.isSelected && !waitForNextChange && !inputConnection.composingText.isComposing && acceptsComposingText()) {
            cursorAnimator.markEndPos()
            cursorAnimator.start()
        }
        if (::editorLanguage.isInitialized) {
            editorLanguage.analyzeManager.delete(start, end, deletedContent)
        }
        selectionAnchor = if (lastAnchorIsSelLeft) cursor.left() else cursor.right()
        dispatchEvent(ContentChangeEvent(this, ContentChangeEvent.ACTION_DELETE, start, end, deletedContent, text.isUndoManagerWorking))
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)

        if (!waitForNextChange) {
            updateCursor()
            ensureSelectionVisible()
            touchHandler.hideInsertHandle()
        }
    }

    override fun beforeModification(content: Content) {
        if (props.checkModificationThread && isAttachedToWindow) {
            val handler = handler
            if (handler != null) {
                if (handler.looper.thread !== Thread.currentThread()) {
                    throw RuntimeException("text is changed in wrong thread")
                }
            }
        }
        cursorAnimator.markStartPos()
        lastAnchorIsSelLeft = cursor.left() == selectionAnchor
    }

    override fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?) {
        postInLifecycle {
            val line = cursor.leftLine
            val column = cursor.leftColumn
            val x = offsetX
            val y = offsetY
            val string = if (applyContent is Content) applyContent.toStringBuilder() else applyContent
            text.beginBatchEdit()
            text.delete(0, 0, text.lineCount - 1, text.getColumnCount(text.lineCount - 1))
            text.insert(0, 0, string)
            text.endBatchEdit()
            inputConnection.markInvalid()
            if (cursorRange == null) {
                setSelectionAround(line, column)
            } else {
                try {
                    val start = cursorRange.start
                    val end = cursorRange.end
                    setSelectionRegion(start.line, start.column, end.line, end.column)
                } catch (e: IndexOutOfBoundsException) {
                    Log.w(LOG_TAG, e)
                }
            }
            touchHandler.scroller.forceFinished(true)
            touchHandler.scroller.startScroll(x, y, 0, 0, 0)
            touchHandler.scroller.abortAnimation()
            touchHandler.scrollBy(0, 0)
            inputConnection.reset()
            restartInput()
            dispatchEvent(EditorFormatEvent(this, true))
        }
    }

    override fun onFormatFail(throwable: Throwable?) {
        postInLifecycle {
            Toast.makeText(context, "Format:$throwable", Toast.LENGTH_SHORT).show()
            dispatchEvent(EditorFormatEvent(this, false))
        }
    }

    private fun checkForRelayoutIfNeeded() {
        if (!isLayoutBusy && wordwrap) {
            createLayout()
        }
    }
    override fun computeScroll() {
        val scroller = touchHandler.scroller
        if (scroller.computeScrollOffset()) {
            if (!scroller.isFinished && (scroller.startX != scroller.finalX || scroller.startY != scroller.finalY)) {
                scrollerFinalX = scroller.finalX.toFloat()
                scrollerFinalY = scroller.finalY.toFloat()
                horizontalAbsorb = abs(scroller.startX - scroller.finalX) > dpUnit * 5
                verticalAbsorb = abs(scroller.startY - scroller.finalY) > dpUnit * 5
            }
            if (scroller.currX <= 0 && scrollerFinalX <= 0 && edgeEffectHorizontal.isFinished && horizontalAbsorb) {
                edgeEffectHorizontal.onAbsorb(scroller.currVelocity.toInt())
                touchHandler.glowLeftOrRight = false
            } else {
                val max = getScrollMaxX()
                if (scroller.currX >= max && scrollerFinalX >= max && edgeEffectHorizontal.isFinished && horizontalAbsorb) {
                    edgeEffectHorizontal.onAbsorb(scroller.currVelocity.toInt())
                    touchHandler.glowLeftOrRight = true
                }
            }
            if (scroller.currY <= 0 && scrollerFinalY <= 0 && edgeEffectVertical.isFinished && verticalAbsorb) {
                edgeEffectVertical.onAbsorb(scroller.currVelocity.toInt())
                touchHandler.glowTopOrBottom = false
            } else {
                val max = getScrollMaxY()
                if (scroller.currY >= max && scrollerFinalY >= max && edgeEffectVertical.isFinished && verticalAbsorb) {
                    edgeEffectVertical.onAbsorb(scroller.currVelocity.toInt())
                    touchHandler.glowTopOrBottom = true
                }
            }
            postInvalidateOnAnimation()
        }
    }
}
