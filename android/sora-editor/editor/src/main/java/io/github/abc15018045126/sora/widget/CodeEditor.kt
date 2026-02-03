package io.github.abc15018045126.sora.widget

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Paint.FontMetricsInt
import android.graphics.Paint.FontMetrics
import android.graphics.Paint as AndroidPaint
import android.text.InputType
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.HandleDescriptor

import android.view.*
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.EdgeEffect
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.Px
import io.github.abc15018045126.sora.I18nConfig
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.event.*
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRenderer
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRendererProvider
import io.github.abc15018045126.sora.lang.EmptyLanguage
import io.github.abc15018045126.sora.lang.Language
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer
import io.github.abc15018045126.sora.lang.format.Formatter
import io.github.abc15018045126.sora.lang.styling.HighlightTextContainer
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.abc15018045126.sora.text.*
import io.github.abc15018045126.sora.text.method.KeyMetaStates
import io.github.abc15018045126.sora.lang.styling.CodeBlock
import io.github.abc15018045126.sora.util.*
import io.github.abc15018045126.sora.widget.component.*
import io.github.abc15018045126.sora.widget.layout.Layout
import io.github.abc15018045126.sora.widget.rendering.RenderContext
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.snippet.SnippetController
import io.github.abc15018045126.sora.widget.style.*
import io.github.abc15018045126.sora.widget.style.builtin.*
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import android.graphics.Canvas
import android.graphics.Rect
import android.content.res.Configuration
import android.os.TransactionTooLargeException
import android.widget.SearchView
import android.widget.EditText
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.ExtractedText
import androidx.annotation.UiThread
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import io.github.abc15018045126.sora.lang.QuickQuoteHandler
import io.github.abc15018045126.sora.lang.QuickQuoteHandler.HandleResult
import io.github.abc15018045126.sora.widget.EditorSearcher.SearchOptions
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.inlayHint.IntSetUpdateRange
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.REGION_OUTBOUND
import io.github.abc15018045126.sora.widget.REGION_LINE_NUMBER
import io.github.abc15018045126.sora.widget.REGION_SIDE_ICON
import io.github.abc15018045126.sora.widget.REGION_DIVIDER_MARGIN
import io.github.abc15018045126.sora.widget.REGION_DIVIDER
import io.github.abc15018045126.sora.widget.REGION_TEXT
import io.github.abc15018045126.sora.widget.IN_BOUND
import io.github.abc15018045126.sora.widget.OUT_BOUND
import io.github.abc15018045126.sora.widget.DirectAccessProps.Companion.MOUSE_MODE_NEVER
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.widget.layout.LineBreakLayout
import io.github.abc15018045126.sora.widget.layout.WordwrapLayout
import io.github.abc15018045126.sora.widget.layout.ViewMeasureHelper
import android.graphics.PointF

@SuppressWarnings("unused")
open class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.codeEditorStyle,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), ContentListener, Formatter.FormatResultReceiver,
    InlayHintRendererProvider {


    protected val keyEventHandler: EditorKeyEventHandler = EditorKeyEventHandler(this)
    protected var languageSymbolPairs: SymbolPairMatch? = null
    protected var textActionWindow: EditorTextActionWindow? = null
    protected var diagnosticTooltip: EditorDiagnosticTooltipWindow? = null
    protected var contextMenuCreator: EditorContextMenuCreator? = null
    internal var defaultSpans: MutableList<Span?> = ArrayList(2)
    protected var styleDelegate: EditorStyleDelegate? = null
    var startedActionMode: Int = 0
    var selectionAnchor: CharPosition? = null

    internal var inputConnection: EditorInputConnection? = null
    var eventManager: EventManager? = null

    private var _layout: Layout? = null
    var layout: Layout
        get() = _layout!!
        set(value) { _layout = value }

    private var _tabWidth = 0
    var tabWidth: Int
        get() = _tabWidth
        set(w) {
            require(w > 0) { "tab width must > 0" }
            if (_tabWidth != w) { _tabWidth = w; if (!isWordwrap) invalidate() else { requestLayoutIfNeeded(); createLayout(); invalidate() } }
        }

    private var _blockIndex = -1
    var blockIndex: Int
        get() = _blockIndex
        set(v) { _blockIndex = v }

    private var downX = 0; private var inputType = 0; private var nonPrintableOptions = 0; private var completionWndPosMode = 0; private var availableFloatArrayRegion: Long = 0

    var dpUnit: Float = 0f; private set

    private var _dividerWidth = 0f
    var dividerWidth: Float
        get() = _dividerWidth
        set(v) { if (_dividerWidth != v) { _dividerWidth = v; requestLayoutIfNeeded(); invalidate() } }

    @get:Px
    internal var dividerMarginLeft: Float = 0f
        set(v) { field = v; requestLayoutIfNeeded(); createLayout(); invalidate() }

    @get:Px
    internal var dividerMarginRight: Float = 0f
        set(v) { field = v; requestLayoutIfNeeded(); createLayout(); invalidate() }

    fun getDividerMarginLeft() = dividerMarginLeft
    fun getDividerMarginRight() = dividerMarginRight

    var extraMarginRight: Float = 0f
        set(v) { field = v; requestLayoutIfNeeded(); createLayout(); invalidate() }

    @get:Px
    var insertSelectionWidth: Float = 0f; private set

    private var blockLineWidth = 0f; private var textBorderWidth = 0f; private var verticalScrollFactor = 0f

    var lineInfoTextSize: Float = 0f
        set(v) { require(v > 0); field = v }

    private var lineSpacingMultiplier = 1f; private var lineSpacingAdd = 0f; private var wrapLineSpacingMultiplier = 1f; private var wrapLineSpacingAdd = 0f; private var lineNumberMarginLeft = 0f; private var verticalExtraSpaceFactor = 0.5f; private var waitForNextChange = false

    var isScalable: Boolean = false
    private var _editable = true
    var isEditable: Boolean
        get() = _editable && !layoutBusy && !isFormatting
        set(editable) { _editable = editable; if (!isEditable) { hideSoftInput(); snippetController!!.stopSnippet() } }

    private var undoEnabled = false; private var mouseHover = false; private var mouseButtonPressed = false; private var lastAnchorIsSelLeft = false

    @Volatile
    internal var layoutBusy = false

    fun isLayoutBusy() = layoutBusy

    var isDisplayLnPanel: Boolean = false
        set(v) { field = v; invalidate() }

    var lnPanelPosition: Int = 0
        set(v) { field = v; invalidate() }

    var lnPanelPositionMode: Int = 0
        set(v) { field = v; invalidate() }

    private var rejectComposingCount = 0

    var isReleased: Boolean = false; private set

    var isLineNumberEnabled: Boolean = false
        set(v) { if (v != field && isWordwrap) createLayout(); field = v; invalidate() }

    private var blockLineEnabled = false

    var isInterceptParentHorizontalScrollEnabled: Boolean = false; private set
    private var highlightCurrentBlock = false

    var isHighlightCurrentLine: Boolean = false
        set(v) { field = v; invalidate() }

    override fun isVerticalScrollBarEnabled() = super.isVerticalScrollBarEnabled()
    override fun setVerticalScrollBarEnabled(enabled: Boolean) = super.setVerticalScrollBarEnabled(enabled)
    override fun isHorizontalScrollBarEnabled() = super.isHorizontalScrollBarEnabled()
    override fun setHorizontalScrollBarEnabled(enabled: Boolean) = super.setHorizontalScrollBarEnabled(enabled)

    private var cursorAnimation = false; private var initialPreviewLines = 20
    @JvmField var forceSyncBreakLines = false; private var isLineNumberRightOfDivider = false

    var isLineNumberPinned: Boolean = false; private set

    private var _wordwrap = false
    var isWordwrap: Boolean
        get() = _wordwrap
        set(v) = setWordwrap(v, isAntiWordBreaking, isWordwrapRtlDisplaySupport)

    var isAntiWordBreaking: Boolean = false
        set(v) = setWordwrap(isWordwrap, v, isWordwrapRtlDisplaySupport)

    var isWordwrapRtlDisplaySupport: Boolean = false
        set(v) = setWordwrap(isWordwrap, isAntiWordBreaking, v)

    var isFirstLineNumberAlwaysVisible: Boolean = false
        set(v) { field = v; if (isWordwrap) invalidate() }

    var isLigatureEnabled: Boolean = false
        set(v) { field = v; setFontFeatureSettings(if (v) null else "'liga' 0,'calt' 0,'hlig' 0,'dlig' 0,'clig' 0") }

    private var lastCursorState = false

    var isStickyTextSelection: Boolean = false
    private var highlightBracketPair = false

    var isInLongSelect: Boolean = false; private set
    private var anyWrapContentSet = false

    private var _renderFunctionCharacters = false
    var isRenderFunctionCharacters: Boolean
        get() = _renderFunctionCharacters
        set(v) { if (_renderFunctionCharacters != v) { _renderFunctionCharacters = v; renderer.onTextStyleUpdate(); requestLayoutIfNeeded(); createLayout(); invalidate() } }

    var isSoftKbdEnabled = false; var isDisableSoftKbdOnHardKbd = false
    var handleDescLeft: SelectionHandleStyle.HandleDescriptor? = null; var handleDescRight: SelectionHandleStyle.HandleDescriptor? = null; var handleDescInsert: SelectionHandleStyle.HandleDescriptor? = null
    var clipboardManager: ClipboardManager? = null; var inputMethodManager: InputMethodManager? = null

    private var _cursor: Cursor? = null
    var cursor: Cursor
        get() = _cursor!!
        set(v) { _cursor = v }

    private var _text: Content? = null
    var text: Content
        get() = _text!!
        set(v) = setText(v)

    private var matrix: Matrix? = null
    var colorScheme: EditorColorScheme = EditorColorScheme.getDefault()
        set(colors) { field.detachEditor(this); field = colors; colors.attachEditor(this); invalidate() }
    internal var lineNumberTipTextProvider: LineNumberTipTextProvider? = null
    internal var formatTip: String? = null
    private var _editorLanguage: Language? = null
    var editorLanguage: Language?
        get() = _editorLanguage
        set(lang) {
            val language = lang ?: EmptyLanguage()
            _editorLanguage?.let { old ->
                old.formatter.apply { setReceiver(null); destroy() }
                old.analyzeManager.apply { receiver = null; destroy() }
                old.destroy()
            }
            styleDelegate!!.reset(); _editorLanguage = language; textStyles = null; _diagnostics = null; _inlayHints = null; _searcher.stopSearch()
            if (isAttachedToWindow) language.analyzeManager.receiver = styleDelegate!!
            _text?.let { it.removeContentListener(this); it.addContentListener(this) }
            languageSymbolPairs?.parent = null; languageSymbolPairs = language.symbolPairs
            if (languageSymbolPairs == null) { Log.w(LOG_TAG, "Language(${language}) returned null for symbol pairs. It is a mistake."); languageSymbolPairs = SymbolPairMatch() }
            languageSymbolPairs?.parent = props!!.overrideSymbolPairs; snippetController?.stopSnippet(); renderContext?.invalidateRenderNodes(); invalidate()
            if (inlayHints != null) inlayHints = null; if (highlightTextContainer != null) highlightTextContainer = null
        }

    private var diagnosticStyle: DiagnosticIndicatorStyle? = DiagnosticIndicatorStyle.WAVY_LINE
    private var lastMakeVisible: Long = 0
    private var completionWindow: EditorAutoCompletion? = null; var touchHandler: EditorTouchEventHandler? = null

    internal var lineNumberAlign: android.graphics.Paint.Align? = null
    private var basicDetector: GestureDetector? = null; private var scaleDetector: ScaleGestureDetector? = null; private var anchorInfoBuilder: CursorAnchorInfo.Builder? = null
    var edgeEffectVertical: EdgeEffect? = null; var edgeEffectHorizontal: EdgeEffect? = null

    private var extractingTextRequest: ExtractedTextRequest? = null
    protected lateinit var _searcher: EditorSearcher

    private var _cursorAnimator: CursorAnimator? = null

    var handleStyle: SelectionHandleStyle? = null

    internal var cursorBlink: CursorBlink? = null; var props: DirectAccessProps? = null; private var extraArguments: Bundle? = null; private var textStyles: Styles? = null

    private var _diagnostics: DiagnosticsContainer? = null
    var diagnostics: DiagnosticsContainer?
        get() = _diagnostics
        set(v) { _diagnostics = v; invalidate() }

    private var _inlayHints: InlayHintsContainer? = null
    var inlayHints: InlayHintsContainer?
        get() = _inlayHints
        set(v) {
            val affectedLines = MutableIntSet(); _inlayHints?.let { affectedLines.addAll(it.getLineNumbers()) }; v?.let { affectedLines.addAll(it.getLineNumbers()) }
            _inlayHints = v; if (!layoutBusy) layout.invalidateLines(IntSetUpdateRange(affectedLines)) else createLayout()
            renderContext?.invalidateRenderNodes(); invalidate()
        }

    private var _highlightTextContainer: HighlightTextContainer? = null
    var highlightTextContainer: HighlightTextContainer?
        get() = _highlightTextContainer
        set(v) {
            val lines = MutableIntSet(); _highlightTextContainer?.let { lines.addAll(it.getLineNumbers()) }; v?.let { lines.addAll(it.getLineNumbers()) }
            _highlightTextContainer = v; if (!layoutBusy) layout.invalidateLines(IntSetUpdateRange(lines)) else createLayout()
            renderContext?.invalidateRenderNodes(); invalidate()
        }
    var renderContext: RenderContext? = null

    lateinit var renderer: EditorRenderer
    private var hardwareAccAllowed = false; private var scrollerFinalX = 0f; private var scrollerFinalY = 0f; private var verticalAbsorb = false; private var horizontalAbsorb = false

    private var _lineSeparator: LineSeparator? = null
    var lineSeparator: LineSeparator?
        get() = _lineSeparator
        set(v) { require(v != null && v !== LineSeparator.NONE); _lineSeparator = v }

    private var lastInsertion: TextRange? = null; private var lastSelectedTextRange: TextRange? = null
    private var _snippetController: SnippetController? = null
    var snippetController: SnippetController?
        get() = _snippetController
        set(v) { _snippetController = v }

    private val inlayHintRendererMap: MutableMap<String?, InlayHintRenderer?> = HashMap()

    init { initialize(attrs, defStyleAttr, defStyleRes); applyAttributeSets(attrs, defStyleAttr, defStyleRes) }

    @Suppress("UNCHECKED_CAST")
    fun <T : EditorBuiltinComponent> getComponent(clazz: Class<T>): T = when (clazz) {
        EditorAutoCompletion::class.java -> completionWindow as T
        Magnifier::class.java -> touchHandler!!.editorMagnifier as T
        EditorTextActionWindow::class.java -> textActionWindow as T
        EditorDiagnosticTooltipWindow::class.java -> diagnosticTooltip as T
        EditorContextMenuCreator::class.java -> contextMenuCreator as T
        else -> throw IllegalArgumentException("Unknown component type")
    }

    fun <T : EditorBuiltinComponent> replaceComponent(clazz: Class<T>, replacement: T) {
        val old = getComponent(clazz); val isEnabled = old.isEnabled; old.isEnabled = false
        when (clazz) {
            EditorAutoCompletion::class.java -> completionWindow = replacement as EditorAutoCompletion
            Magnifier::class.java -> touchHandler!!.editorMagnifier = replacement as Magnifier
            EditorTextActionWindow::class.java -> textActionWindow = replacement as EditorTextActionWindow
            EditorDiagnosticTooltipWindow::class.java -> diagnosticTooltip = replacement as EditorDiagnosticTooltipWindow
            EditorContextMenuCreator::class.java -> contextMenuCreator = replacement as EditorContextMenuCreator
            else -> throw IllegalArgumentException("Unknown component type")
        }
        replacement.isEnabled = isEnabled
    }

    fun registerInlayHintRenderers(vararg renderers: InlayHintRenderer) {
        var needLayout = false; for (renderer in renderers) { val old = inlayHintRendererMap.put(renderer.typeName, renderer); needLayout = needLayout or (old != renderer) }
        if (needLayout) createLayout()
    }

    fun registerInlayHintRenderer(@NonNull renderer: InlayHintRenderer) { if (inlayHintRendererMap.put(renderer.typeName, renderer) != renderer) createLayout() }

    fun removeInlayHintRenderer(@NonNull renderer: InlayHintRenderer) { if (inlayHintRendererMap[renderer.typeName] == renderer) { inlayHintRendererMap.remove(renderer.typeName); createLayout() } }

    val inlayHintRenderers: List<InlayHintRenderer> get() = ArrayList(inlayHintRendererMap.values.filterNotNull())

    override fun getInlayHintRendererForType(type: String) = inlayHintRendererMap[type]


    fun getKeyMetaStates() = keyEventHandler.getKeyMetaStates()

    @UnsupportedUserUsage
    open fun cancelAnimation() { lastMakeVisible = System.currentTimeMillis() }

    fun measureTextRegionOffset() = if (isLineNumberEnabled) (measureLineNumber() + dividerMarginLeft + dividerMarginRight + dividerWidth + (if (renderer.hasSideHintIcons()) rowHeight else 0)) else dividerMarginLeft + dividerMarginRight

    val leftHandleDescriptor get() = handleDescLeft
    val rightHandleDescriptor get() = handleDescRight

    fun getOffset(line: Int, column: Int) = layout.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - offsetX
    fun getCharOffsetX(line: Int, column: Int) = layout.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - offsetX
    fun getCharOffsetY(line: Int, column: Int) = layout.getCharLayoutOffset(line, column)[0] - offsetY

    protected fun initialize(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        Log.v(LOG_TAG, COPYRIGHT); eventManager = EventManager(); styleDelegate = EditorStyleDelegate(this); touchHandler = EditorTouchEventHandler(this)
        _text = Content(""); _cursor = _text!!.cursor; renderContext = RenderContext(this); renderer = onCreateRenderer(); isRenderFunctionCharacters = true
        verticalScrollFactor = ViewUtils.getVerticalScrollFactor(context); lineSeparator = LineSeparator.LF; lineNumberTipTextProvider = DefaultLineNumberTip
        formatTip = I18nConfig.getString(context, R.string.sora_editor_editor_formatting); props = DirectAccessProps()
        dpUnit = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, Resources.getSystem().displayMetrics) / 10f
        dividerWidth = dpUnit; insertSelectionWidth = dpUnit * 1.5f; textBorderWidth = dpUnit; extraArguments = Bundle()
        dividerMarginRight = dpUnit * 2; dividerMarginLeft = dividerMarginRight; matrix = Matrix(); handleStyle = HandleStyleSideDrop(context)
        _searcher = EditorSearcher(this); _cursorAnimator = MoveCursorAnimator(this); cursorBlink = CursorBlink(this, DEFAULT_CURSOR_BLINK_PERIOD)
        setCursorBlinkPeriod(DEFAULT_CURSOR_BLINK_PERIOD); anchorInfoBuilder = CursorAnchorInfo.Builder(); startedActionMode = ACTION_MODE_NONE
        setTextSize(DEFAULT_TEXT_SIZE.toFloat()); lineInfoTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_LINE_INFO_TEXT_SIZE.toFloat(), Resources.getSystem().displayMetrics)
        colorScheme = EditorColorScheme.getDefault(); colorScheme.attachEditor(this); basicDetector = GestureDetector(context, touchHandler!!)
        basicDetector!!.setOnDoubleTapListener(touchHandler!!); scaleDetector = ScaleGestureDetector(context, touchHandler!!); handleDescInsert = HandleDescriptor()
        handleDescLeft = HandleDescriptor(); handleDescRight = HandleDescriptor(); lineNumberAlign = android.graphics.Paint.Align.RIGHT; waitForNextChange = false
        blockLineEnabled = true; blockLineWidth = 1f; inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; setUndoEnabled(true); blockIndex = -1; isScalable = true
        isFocusable = true; isFocusableInTouchMode = true; highlightBracketPair = true; inputConnection = EditorInputConnection(this); snippetController = SnippetController(this)
        completionWindow = EditorAutoCompletion(this); edgeEffectVertical = EdgeEffect(context); edgeEffectHorizontal = EdgeEffect(context); textActionWindow = EditorTextActionWindow(this)
        diagnosticTooltip = EditorDiagnosticTooltipWindow(this); contextMenuCreator = EditorContextMenuCreator(this); editorLanguage = null; setText(null); tabWidth = 4
        isHighlightCurrentLine = true; isVerticalScrollBarEnabled = true; setHighlightCurrentBlock(true); isDisplayLnPanel = true; isHorizontalScrollBarEnabled = true
        isFirstLineNumberAlwaysVisible = true; isCursorAnimationEnabled = true; isEditable = true; isLineNumberEnabled = true; isHardwareAcceleratedDrawAllowed = true
        setInterceptParentHorizontalScrollIfNeeded(false); setTypefaceText(Typeface.DEFAULT)
        this.isSoftKeyboardEnabled = true
        this.isDisableSoftKbdIfHardKbdAvailable = true


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false)
        }
        if (getContext() is ContextThemeWrapper) {
            this.edgeEffectColor = ThemeUtils.getColorPrimary(getContext() as ContextThemeWrapper)
        }


        scaleDetector!!.setQuickScaleEnabled(false)
    }


    protected fun applyAttributeSets(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val array = getContext().obtainStyledAttributes(attrs, R.styleable.CodeEditor, defStyleAttr, defStyleRes)

        setHorizontalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbHorizontal))
        setHorizontalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackHorizontal))
        setVerticalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbVertical))
        setVerticalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackVertical))

        this.lnPanelPositionMode =
            array.getInt(R.styleable.CodeEditor_lnPanelPositionMode, LineInfoPanelPositionMode.FOLLOW)
        this.lnPanelPosition = array.getInt(R.styleable.CodeEditor_lnPanelPosition, LineInfoPanelPosition.CENTER)

        _dividerWidth = array.getDimension(R.styleable.CodeEditor_dividerWidth, _dividerWidth)
        setDividerMargin(
            array.getDimension(R.styleable.CodeEditor_dividerMargin, this.dividerMarginLeft),
            array.getDimension(R.styleable.CodeEditor_dividerMargin, this.dividerMarginRight)
        )
        setPinLineNumber(array.getBoolean(R.styleable.CodeEditor_pinLineNumber, false))

        setHighlightCurrentBlock(array.getBoolean(R.styleable.CodeEditor_highlightCurrentBlock, true))
        this.isHighlightCurrentLine = array.getBoolean(R.styleable.CodeEditor_highlightCurrentLine, true)
        setHighlightBracketPair(array.getBoolean(R.styleable.CodeEditor_highlightBracketPair, true))

        this.isLigatureEnabled = array.getBoolean(R.styleable.CodeEditor_ligatures, true)
        this.isLineNumberEnabled = array.getBoolean(R.styleable.CodeEditor_lineNumberVisible, this.isLineNumberEnabled)
        getComponent<EditorAutoCompletion>(EditorAutoCompletion::class.java)?.isEnabled =
            array.getBoolean(
                R.styleable.CodeEditor_autoCompleteEnabled,
                true
            )
        props!!.symbolPairAutoCompletion = array.getBoolean(R.styleable.CodeEditor_symbolCompletionEnabled, true)
        isRenderFunctionCharacters = array.getBoolean(
            R.styleable.CodeEditor_renderFunctionChars,
            isRenderFunctionCharacters
        )
        this.isScalable = array.getBoolean(R.styleable.CodeEditor_scalable, this.isScalable)

        this.textSizePx = array.getDimension(R.styleable.CodeEditor_textSize, this.textSizePx)
        setCursorBlinkPeriod(array.getInt(R.styleable.CodeEditor_cursorBlinkPeriod, cursorBlink?.period ?: 500))
        this.tabWidth = array.getInt(R.styleable.CodeEditor_tabWidth, tabWidth)

        val wordwrapMode: Int = array.getInt(R.styleable.CodeEditor_wordwrapMode, 0)
        if (wordwrapMode != 0) {
            setWordwrap(true, wordwrapMode > 1, false)
        }

        setText(array.getString(R.styleable.CodeEditor_text))

        array.recycle()
    }





    fun getFormatTip() = formatTip
    fun setFormatTip(@NonNull formatTip: String?) { this.formatTip = Objects.requireNonNull(formatTip) }
    fun setPinLineNumber(pinLineNumber: Boolean) { this.isLineNumberPinned = pinLineNumber; if (isLineNumberEnabled) invalidate() }


    var textActionMenuOrder: List<String>? = null
        set(v) { field = v; textActionWindow?.updateMenuOrderAndVisibility() }
    var textActionMenuHidden: List<String>? = null
        set(v) { field = v; textActionWindow?.updateMenuOrderAndVisibility() }

    fun insertText(text: String, selectionOffset: Int) {
        require(selectionOffset in 0..text.length) { "selectionOffset is invalid" }
        val cur = cursor ?: return
        if (cur.isSelected()) { deleteText(); notifyIMEExternalCursorChange() }
        this.text.insert(cur.rightLine, cur.rightColumn, text); notifyIMEExternalCursorChange()
        if (selectionOffset != text.length) { val pos = this.text.indexer.getCharPosition(cur.right - (text.length - selectionOffset)); setSelection(pos.line, pos.column) }
    }


    fun setCursorBlinkPeriod(period: Int) { cursorBlink?.apply { onSelectionChanged(); val b = this.period; setPeriod(period); if (b <= 0 && valid && isAttachedToWindow) postInLifecycle(this) } }
    fun setFontFeatureSettings(f: String?) { renderer.paint.setFontFeatureSettingsWrapped(f); renderer.paintOther.setFontFeatureSettings(f); renderer.paintGraph.setFontFeatureSettings(f); renderer.updateTimestamp(); invalidate() }
    fun setSelectionHandleStyle(@NonNull style: SelectionHandleStyle) { handleStyle = style; invalidate() }

    @NonNull


    fun isHighlightCurrentBlock() = highlightCurrentBlock
    fun setHighlightCurrentBlock(v: Boolean) { highlightCurrentBlock = v; blockIndex = if (v) findCursorBlock() else -1; invalidate() }

    internal fun canHandleKeyBinding(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
        val dpad = keyCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT; val homeEnd = keyCode == KeyEvent.KEYCODE_MOVE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_END
        return if (ctrl) (if (shift) dpad || homeEnd || keyCode == KeyEvent.KEYCODE_J else if (alt) keyCode == KeyEvent.KEYCODE_ENTER else dpad || homeEnd || keyCode in intArrayOf(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_ENTER))
               else if (shift) dpad || homeEnd || keyCode == KeyEvent.KEYCODE_ENTER else false
    }


    fun getBlockLineWidth() = blockLineWidth
    fun setBlockLineWidth(dp: Float) { blockLineWidth = dp; invalidate() }










    private fun setWordwrap(wordwrap: Boolean, antiBreaking: Boolean, supportRtl: Boolean) {
        if (_wordwrap != wordwrap || isAntiWordBreaking != antiBreaking || isWordwrapRtlDisplaySupport != supportRtl) {
            _wordwrap = wordwrap; isAntiWordBreaking = antiBreaking; isWordwrapRtlDisplaySupport = supportRtl
            requestLayoutIfNeeded(); createLayout(); if (!wordwrap) renderContext?.invalidateRenderNodes(); invalidate()
        }
    }

    var isCursorAnimationEnabled: Boolean
        get() = cursorAnimation
        set(v) { if (!v) _cursorAnimator!!.cancel(); cursorAnimation = v }


    fun getInitialPreviewLines() = initialPreviewLines
    fun setInitialPreviewLines(lines: Int) { initialPreviewLines = lines }


    fun setLineNumberRightOfDivider(v: Boolean) { if (isLineNumberRightOfDivider != v) { isLineNumberRightOfDivider = v; invalidate() } }
    fun isLineNumberRightOfDivider() = isLineNumberRightOfDivider




    fun setCursorAnimator(@NonNull animator: CursorAnimator) { _cursorAnimator = animator }


    fun setScrollBarEnabled(v: Boolean) { isHorizontalScrollBarEnabled = v; isVerticalScrollBarEnabled = v; invalidate() }

    override fun getHorizontalScrollbarThumbDrawable() = renderer.getHorizontalScrollbarThumbDrawable()
    override fun setHorizontalScrollbarThumbDrawable(d: Drawable?) = super.setHorizontalScrollbarThumbDrawable(d)
    override fun getHorizontalScrollbarTrackDrawable() = renderer.getHorizontalScrollbarTrackDrawable()
    override fun setHorizontalScrollbarTrackDrawable(d: Drawable?) = super.setHorizontalScrollbarTrackDrawable(d)
    override fun getVerticalScrollbarThumbDrawable() = renderer.getVerticalScrollbarThumbDrawable()
    override fun setVerticalScrollbarThumbDrawable(d: Drawable?) = super.setVerticalScrollbarThumbDrawable(d)
    override fun getVerticalScrollbarTrackDrawable() = renderer.getVerticalScrollbarTrackDrawable()
    override fun setVerticalScrollbarTrackDrawable(d: Drawable?) = super.setVerticalScrollbarTrackDrawable(d)
    fun getLineNumberTipTextProvider() = lineNumberTipTextProvider
    fun setLineNumberTipTextProvider(p: LineNumberTipTextProvider) { lineNumberTipTextProvider = p; invalidate() }

    val insertHandleDescriptor get() = handleDescInsert
    @get:Px var textSizePx: Float
        get() = renderer.paint.textSize
        set(v) { setTextSizePxDirect(v); requestLayoutIfNeeded(); createLayout(); invalidate() }


    @UnsupportedUserUsage
    open fun setTextSizePxDirect(@Px size: Float) {

        val oldTextSize = this.textSizePx
        renderer.setTextSizePxDirect(size)
        val layout = _layout
        if (layout is io.github.abc15018045126.sora.widget.layout.WordwrapLayout) {
            layout.refreshHeights()
        }
        dispatchEvent(TextSizeChangeEvent(this, oldTextSize, size))
    }

    internal fun requestLayoutIfNeeded() {
        if (anyWrapContentSet) {
            requestLayout()
        }
    }

    protected fun checkForRelayout() {
        if (anyWrapContentSet) {
            val params =
                getLayoutParams()
            if (params != null) {
                if (params.width === ViewGroup.LayoutParams.WRAP_CONTENT) {
                    requestLayout()
                } else if (params.height === ViewGroup.LayoutParams.WRAP_CONTENT) {
                    if (height !== layout!!.layoutHeight) {
                        requestLayout()
                    }
                }
            }
        }
    }



    val lineNumberMetrics: android.graphics.Paint.FontMetricsInt
        get() = renderer.lineNumberMetrics


    internal fun shouldInitializeNonPrintable() = Numbers.clearBits(nonPrintableOptions, FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE or FLAG_DRAW_TAB_SAME_AS_SPACE or FLAG_DRAW_LINE_SEPARATOR or FLAG_DRAW_SOFT_WRAP) != 0

    var isHardwareAcceleratedDrawAllowed: Boolean
        get() = hardwareAccAllowed
        set(v) { hardwareAccAllowed = v; if (v && !isWordwrap) renderContext?.invalidateRenderNodes() }

    internal fun findLeadingAndTrailingWhitespacePos(line: ContentLine): Long {
        val buffer = line.backingCharArray; val column = line.length; var leading = 0; var trailing = column
        while (leading < column && Character.isWhitespace(buffer[leading])) leading++
        if (leading != column && (nonPrintableOptions and (FLAG_DRAW_WHITESPACE_INNER or FLAG_DRAW_WHITESPACE_TRAILING)) != 0) {
            while (trailing > 0 && Character.isWhitespace(buffer[trailing - 1])) trailing--
        }
        return IntPair.pack(leading, trailing)
    }

    private fun Character.isWhitespace(ch: Char) = ch == '\t' || ch == ' '

    internal fun computeMatchedPositions(line: Int, positions: LongArrayList) {
        positions.clear(); val pattern = _searcher.currentPattern ?: return; val options = _searcher.searchOptions ?: return
        if (!_searcher.isResultValid()) return; val res = _searcher.lastResults ?: return
        val lineLeft = text.getCharIndex(line, 0); val lineRight = lineLeft + text.getColumnCount(line)
        for (i in max(0, res.lowerBoundByFirst(lineLeft) - 1)..<res.size()) {
            val region = res.get(i); val start = IntPair.getFirst(region); val end = IntPair.getSecond(region)
            val hStart = max(start, lineLeft); val hEnd = min(end, lineRight)
            if (hStart < hEnd) positions.add(IntPair.pack(hStart - lineLeft, hEnd - lineLeft))
            if (start > lineRight) break
        }
    }

    internal fun computeHighlightPositions(line: Int, positions: MutableLongLongMap) {
        positions.clear(); val container = highlightTextContainer ?: return; val highlights = container.getForLine(line)
        if (highlights.isEmpty()) return; val lineColumnCount = text.getColumnCount(line)
        for (h in highlights) {
            if (line < h.startLine || line > h.endLine) continue
            var start = if (line == h.startLine) h.startColumn else 0; var end = if (line == h.endLine) h.endColumn else lineColumnCount
            start = max(0, min(start, lineColumnCount)); end = max(0, min(end, lineColumnCount))
            if (lineColumnCount == 0) continue
            if (start < end) positions.put(IntPair.pack(start, end), IntPair.pack(h.color.resolve(colorScheme), h.borderColor.resolve(colorScheme)))
        }
    }

    var edgeEffectColor: Int
        get() = edgeEffectVertical!!.color
        set(v) { edgeEffectVertical!!.setColor(v); edgeEffectHorizontal!!.setColor(v) }

    val verticalEdgeEffect: EdgeEffect get() = edgeEffectVertical!!
    val horizontalEdgeEffect: EdgeEffect get() = edgeEffectHorizontal!!

    private fun findCursorBlock(): Int {
        val blocks = textStyles?.blocks ?: return -1; if (blocks.isEmpty()) return -1
        return findCursorBlock(blocks)
    }

    private fun findCursorBlock(blocks: List<CodeBlock>): Int {
        val line = cursor!!.leftLine; var min = binarySearchEndBlock(line, blocks); if (min == -1) min = 0
        var minDis = Int.MAX_VALUE; var found = -1; var invalidCount = 0; val maxCount = textStyles?.getSuppressSwitch() ?: Int.MAX_VALUE
        for (i in min until blocks.size) {
            val b = blocks[i] ?: continue
            if (b.endLine >= line && b.startLine <= line) { val dis = b.endLine - b.startLine; if (dis < minDis) { minDis = dis; found = i } }
            else if (minDis != Int.MAX_VALUE) { if (++invalidCount >= maxCount) break }
        }
        return found
    }

    fun binarySearchEndBlock(firstVis: Int, blocks: List<CodeBlock>?) = CodeBlock.binarySearchEndBlock(firstVis, blocks)

    @NonNull fun getSpansForLine(line: Int): List<Span?> {
        val spanMap = textStyles?.spans ?: return defaultSpans.apply { if (isEmpty()) add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())) }
        return try { spanMap.read().getSpansOnLine(line) } catch (e: Exception) { defaultSpans }
    }

    fun measureLineNumber(): Float {
        if (!isLineNumberEnabled) return 0f
        var count = 0; var lc = lineCount
        while (lc > 0) { count++; lc /= 10 }
        val buffer = TemporaryFloatBuffer.obtain(NUMBER_DIGITS.length); renderer.paintOther.getTextWidths(NUMBER_DIGITS, buffer)
        var single = 0f; for (i in 0 until NUMBER_DIGITS.length step 2) single = max(single, buffer[i])
        TemporaryFloatBuffer.recycle(buffer); return single * count + lineNumberMarginLeft
    }

    internal fun createLayout(clearWordwrapCache: Boolean = true) {
        val text = _text ?: return; val layout = _layout
        if (layout != null) {
            if (layout is LineBreakLayout && !isWordwrap) { layout.reuse(text); return }
            if (layout is WordwrapLayout && isWordwrap) { val nl = WordwrapLayout(this, text, isAntiWordBreaking, isWordwrapRtlDisplaySupport, layout, clearWordwrapCache); layout.destroyLayout(); _layout = nl; return }
            layout.destroyLayout()
        }
        if (isWordwrap) { renderer.setCachedLineNumberWidth(measureLineNumber().toInt()); _layout = WordwrapLayout(this, text, isAntiWordBreaking, isWordwrapRtlDisplaySupport, null, false) }
        else _layout = LineBreakLayout(this, text)
        touchHandler?.scrollBy(0f, 0f)
    }

    fun indentSelection() = indentLines(true)

    fun indentLines(onlyIfSelected: Boolean) {
        val cursor = cursor; if (onlyIfSelected && !cursor.isSelected()) return
        val tabString = createTabString(); val text = text; val tw = tabWidth
        text.beginBatchEdit()
        for (i in cursor.leftLine..cursor.rightLine) {
            val line = text.getLine(i); val res = TextUtils.countLeadingSpacesAndTabs(line); val sc = IntPair.getFirst(res); val tc = IntPair.getSecond(res)
            val spaces = sc + (tc * tw); val endCol = sc + tc; val req = tw - (spaces % tw)
            if (sc > 0 && tc > 0) { text.replace(i, 0, i, endCol, tabString.repeat((((if (req == 0) tw else req) + spaces) / tw))); continue }
            if (req == tw) text.insert(i, endCol, tabString) else text.insert(i, endCol, " ".repeat(req))
        }
        text.endBatchEdit()
    }

    fun unindentSelection() {
        val cursor = cursor; val text = text; val tw = tabWidth; val tabString = createTabString()
        text.beginBatchEdit()
        for (i in cursor.leftLine..cursor.rightLine) {
            val line = text.getLineString(i); val res = TextUtils.countLeadingSpacesAndTabs(line); val sc = IntPair.getFirst(res); val tc = IntPair.getSecond(res)
            val spaces = sc + (tc * tw); if (spaces == 0) continue
            val endCol = sc + tc; val extra = spaces % tw
            if (sc > 0 && tc > 0) { text.replace(i, 0, i, endCol, tabString.repeat(abs(spaces - (if (extra == 0) tw else extra)) / tw)); continue }
            if (extra == 0) text.delete(i, endCol - (if (tc > 0) 1 else tw), i, endCol) else text.delete(i, endCol - extra, i, endCol)
        }
        text.endBatchEdit()
    }

    protected fun commitTab() {
        if (inputConnection != null && isEditable) {
            if (inputConnection!!.composingText.isComposing()) restartInput()
            inputConnection!!.commitTextInternal(createTabString(), true)
        }
    }

    fun indentOrCommitTab() {
        val cursor = cursor; if (cursor.isSelected()) { indentSelection(); return }
        val left = cursor.left(); val line = text.getLine(left.line); val count = TextUtils.countLeadingSpacesAndTabs(line)
        if (left.column > IntPair.getFirst(count) + IntPair.getSecond(count)) commitTab() else indentLines(false)
    }

    protected fun createTabString() = TextUtils.createIndent(tabWidth, tabWidth, editorLanguage!!.useTab())

    fun updateCursorAnchor(): Float {
        val l = cursor!!.rightLine; val column = cursor!!.rightColumn; var visible = true; var x = measureTextRegionOffset() + layout.getCharLayoutOffset(l, column)[1] - offsetX
        if (x < 0) { visible = false; x = 0f }
        val composingText = inputConnection!!.composingText; if (composingText.preSetComposing) return x
        if (props!!.reportCursorAnchor) {
            val builder = anchorInfoBuilder!!; builder.reset(); matrix!!.set(getMatrix()); val b = IntArray(2); getLocationOnScreen(b)
            matrix!!.postTranslate(b[0].toFloat(), b[1].toFloat()); builder.setMatrix(matrix); builder.setSelectionRange(cursor!!.left, cursor!!.right)
            if (composingText.isComposing()) builder.setComposingText(composingText.startIndex, text.substring(composingText.startIndex, composingText.endIndex))
            builder.setInsertionMarkerLocation(x, getRowTop(l) - offsetY.toFloat(), getRowBaseline(l) - offsetY.toFloat(), getRowBottom(l) - offsetY.toFloat(), if (visible) CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION else CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION)
            inputMethodManager!!.updateCursorAnchorInfo(this, builder.build())
        }
        return x
    }

    fun deleteText() {
        val cur = cursor!!
        if (cur.isSelected()) text.delete(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn)
        else {
            val col = cur.leftColumn; val line = cur.leftLine
            if (props!!.deleteEmptyLineFast || (props!!.deleteMultiSpaces !== 1 && col > 0 && text.getLineString(line)[col - 1] === ' ')) {
                val t = text.getLine(cur.leftLine).backingCharArray; var inLeading = true
                for (i in col - 1 downTo 0) { if (t[i] != ' ' && t[i] != '\t') { inLeading = false; break } }
                if (inLeading) {
                    var emptyLine = true; val max = text.getColumnCount(line)
                    for (i in col until max) { if (t[i] != ' ' && t[i] != '\t') { emptyLine = false; break } }
                    if (props!!.deleteEmptyLineFast && emptyLine) {
                        if (line == 0) text.delete(line, 0, line, col) else text.delete(line - 1, text.getColumnCount(line - 1), line, max)
                        return
                    }
                    if (props!!.deleteMultiSpaces !== 1 && col > 0 && text.getLineString(line)[col - 1] === ' ') {
                        text.delete(line, max(0, col - (if (props!!.deleteMultiSpaces === -1) tabWidth else props!!.deleteMultiSpaces)), line, col)
                        return
                    }
                }
            }
            var begin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) TextUtilsP.getOffsetForBackspaceKey(text.getLine(cur.leftLine), col) else TextLayoutHelper.get().getCurPosLeft(col, text.getLine(cur.leftLine))
            var end = cur.leftColumn; if (begin > end) { val tmp = begin; begin = end; end = tmp }
            if (begin == end) { if (cur.leftLine > 0 && begin == 0) text.delete(cur.leftLine - 1, text.getColumnCount(cur.leftLine - 1), cur.leftLine, 0) }
            else text.delete(cur.leftLine, begin, cur.leftLine, end)
        }
    }

    fun commitText(@NonNull text: CharSequence) = commitText(text, true)
    fun commitText(@NonNull text: CharSequence, applyAutoIndent: Boolean) = commitText(text, applyAutoIndent, true)

    fun commitText(@NonNull text: CharSequence, applyAutoIndent: Boolean, applySymbolCompletion: Boolean) {
        var t = text; var pair: SymbolPairMatch.SymbolPair? = null
        if (applySymbolCompletion && props!!.symbolPairAutoCompletion && t.isNotEmpty()) {
            val endChar = t[t.length - 1]; val inputText = if (t.length > 1) t.toString().toCharArray() else null
            pair = languageSymbolPairs?.matchBestPair(this, cursor!!.left(), inputText, endChar)
        }
        val cur = cursor!!; val editorText = this.text; val quoteHandler = LanguageHelper.getQuickQuoteHandler(editorLanguage!!)
        if (pair != null && pair !== SymbolPairMatch.SymbolPair.EMPTY_SYMBOL_PAIR) {
            if (pair.shouldDoAutoSurround(editorText) && quoteHandler == null) {
                editorText.beginBatchEdit(); editorText.insert(cur.leftLine, cur.leftColumn, pair.open); editorText.insert(cur.rightLine, cur.rightColumn, pair.close); editorText.endBatchEdit()
                setSelectionRegion(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn - pair.close.length); return
            } else if (cur.isSelected() && quoteHandler != null) {
                if (t.length == 1) {
                    val res = quoteHandler.onHandleTyping(t.toString(), editorText, cursorRange, styles!!)
                    if (res != null && res.isConsumed()) { res.getNewCursorRange()?.let { setSelectionRegion(it.start.line, it.start.column, it.end.line, it.end.column) }; return }
                }
            } else {
                editorText.beginBatchEdit(); val insPos = editorText.indexer.getCharPosition(pair.insertOffset)
                editorText.replace(insPos.line, insPos.column, cur.rightLine, cur.rightColumn, pair.open)
                editorText.insert(insPos.line, insPos.column + pair.open.length, pair.close); editorText.endBatchEdit()
                val cursorPos = editorText.indexer.getCharPosition(pair.cursorOffset); setSelection(cursorPos.line, cursorPos.column); return
            }
        }
        if (cur.isSelected()) editorText.replace(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn, t)
        else {
            if (props!!.autoIndent && t.isNotEmpty() && applyAutoIndent) {
                val first = t[0]; if (first == '\n' || first == '\r') {
                    val line = editorText.getLineString(cur.leftLine); var p = 0; var sc = 0; var tc = 0
                    while (p < cur.leftColumn) { if (Character.isWhitespace(line[p])) { if (line[p] == '\t') tc++ else sc++; p++ } else break }
                    var cnt = sc + (tc * tabWidth)
                    try { cnt += LanguageHelper.getIndentAdvance(editorLanguage!!, ContentReference(editorText), cur.leftLine, cur.leftColumn, sc, tc) }
                    catch (e: Exception) { Log.w(LOG_TAG, "Language object error", e) }
                    var idx = if (first == '\r' && t.length >= 2 && t[1] == '\n') 2 else 1
                    t = StringBuilder(t).insert(idx, TextUtils.createIndent(cnt, tabWidth, editorLanguage!!.useTab()))
                }
            }
            editorText.insert(cur.leftLine, cur.leftColumn, t)
        }
    }

    var nonPrintablePaintingFlags: Int
        get() = nonPrintableOptions
        set(v) { val old = nonPrintableOptions; nonPrintableOptions = v; if ((old and FLAG_DRAW_SOFT_WRAP) != (v and FLAG_DRAW_SOFT_WRAP)) createLayout(); invalidate() }

    fun hasComposingText() = inputConnection!!.composingText.isComposing()

    fun ensureSelectionVisible() = ensurePositionVisible(cursor.rightLine, cursor.rightColumn)

    @JvmOverloads fun ensurePositionVisible(line: Int, column: Int, noAnimation: Boolean = false) {
        val scroller = scroller; val layoutOffset = layout!!.getCharLayoutOffset(line, column); val xOff = layoutOffset[1] + measureTextRegionOffset(); val yOff = layoutOffset[0]
        val cFinalY = if (scroller.isFinished) offsetY.toFloat() else scroller.getFinalY().toFloat(); val cFinalX = if (scroller.isFinished) offsetX.toFloat() else scroller.getFinalX().toFloat()
        var tY = cFinalY; var tX = cFinalX; val topLines = if (props!!.stickyScroll) props!!.stickyScrollMaxLines else 2
        if (yOff - rowHeight * topLines < cFinalY) tY = yOff - rowHeight * topLines.toFloat()
        if (yOff > height + cFinalY) tY = yOff - height + rowHeight * 1f
        val charW = if (column == 0) 0f else textPaint.measureText("a")
        if (xOff < cFinalX + (if (isLineNumberPinned) measureTextRegionOffset() else 0f)) {
            val bX = tX; val slopX = width / 2; tX = xOff + (if (isLineNumberPinned) -measureTextRegionOffset() else 0f) - charW
            if (abs(tX - bX) < slopX) tX = max(1f, bX - slopX)
        }
        if (xOff + charW > cFinalX + width) tX = xOff + charW * 0.8f - width
        tX = max(0f, min(scrollMaxX.toFloat(), tX)); tY = max(0f, min(scrollMaxY.toFloat(), tY))
        if (Floats.withinDelta(tX, offsetX.toFloat(), 1f) && Floats.withinDelta(tY, offsetY.toFloat(), 1f)) { invalidate(); return }
        val animation = System.currentTimeMillis() - lastMakeVisible >= 100; lastMakeVisible = System.currentTimeMillis()
        if (animation && !noAnimation) {
            scroller.forceFinished(true); scroller.startScroll(offsetX, offsetY, (tX - offsetX).toInt(), (tY - offsetY).toInt())
            if (props!!.awareScrollbarWhenAdjust && abs(offsetY - tY) > dpUnit * 100) touchHandler!!.notifyScrolled()
        } else { scroller.startScroll(offsetX, offsetY, (tX - offsetX).toInt(), (tY - offsetY).toInt(), 0); scroller.abortAnimation() }
        dispatchEvent(ScrollEvent(this, offsetX, offsetY, tX.toInt(), tY.toInt(), ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE))
        invalidate()
    }


    fun hasClip(): Boolean {
        return clipboardManager!!.hasPrimaryClip()
    }

    val scroller: EditorScroller

        get() = touchHandler!!.getScroller()


    fun isOverMaxY(posOnScreen: Float): Boolean {
        return posOnScreen + this.offsetY > layout!!.layoutHeight
    }


    fun isScreenPointOnText(x: Float, y: Float): Boolean {
        val pos = getPointPositionOnScreen(x, y)
        val rowIdx: Int =
            layout!!.getRowIndexForPosition(text.getCharIndex(IntPair.getFirst(pos), IntPair.getSecond(pos)))
        val layoutMax: Float =
            renderer.getRowWidth(rowIdx)
        val textRegionX = measureTextRegionOffset()
        val rowRegionRightX: Float = textRegionX + layoutMax

        val offset = this.offsetX + x
        return offset >= textRegionX && offset <= rowRegionRightX
    }


    fun getPointPosition(xOffset: Float, yOffset: Float): Long {
        return layout!!.getCharPositionForLayoutOffset(xOffset - measureTextRegionOffset(), yOffset)
    }


    fun getPointPositionOnScreen(x: Float, y: Float): Long {
        var y = y
        y = kotlin.math.max(0f, y)
        val stuckLines: List<CodeBlock?>? =
            renderer.lastStuckLines
        if (stuckLines != null) {

            val index = kotlin.math.max(0f, (y / this.rowHeight)).toInt()
            if (y < stuckLines.size * this.rowHeight && index < stuckLines.size) {
                return getPointPosition(
                    x,
                    layout!!.getCharLayoutOffset(stuckLines!![index]!!.startLine, 0)[0] - this.rowHeight / 2f
                )
            }
        }
        return getPointPosition(x + this.offsetX, y + this.offsetY)
    }

    val scrollMaxY: Int

        get() {
            val params: ViewGroup.LayoutParams? =
                getLayoutParams()
            return kotlin.math.max(
                0,
                layout!!.layoutHeight - (if (params == null || params.height === ViewGroup.LayoutParams.WRAP_CONTENT) height else (height * (1 - verticalExtraSpaceFactor)).toInt())
            )
        }

    val scrollMaxX: Int

        get() = kotlin.math.max(0, (layout!!.layoutWidth + measureTextRegionOffset() - width / 2f).toInt())


    fun setVerticalExtraSpaceFactor(extraSpaceFactor: Float) {
        kotlin.require(!(extraSpaceFactor < 0 || extraSpaceFactor > 1.0f)) { "the factor should be in range [0.0, 1.0]" }
        this.verticalExtraSpaceFactor = extraSpaceFactor

        touchHandler!!.scrollBy(0f, 0f)
    }


    fun getVerticalExtraSpaceFactor(): Float {
        return verticalExtraSpaceFactor
    }

    val cursorAnimator: CursorAnimator

        get() = _cursorAnimator!!

    val searcher: EditorSearcher

        get() = _searcher



    internal fun setSelectionAround(line: Int, column: Int) {

        var column = column
        if (line < this.lineCount) {
            val columnCount: Int = text.getColumnCount(line)
            if (column > columnCount) {
                column = columnCount
            }
            setSelection(line, column)
        } else {
            setSelection(this.lineCount - 1, text.getColumnCount(this.lineCount - 1))
        }
    }


    @Synchronized
    fun formatCodeAsync(): Boolean {
        if (this.isFormatting) {
            return false
        }
        val formatter: Formatter? =
            editorLanguage!!.formatter
        formatter?.setReceiver(this)
        val formatContent: Content? =
            text.copyText(false)
        formatContent?.isUndoEnabled = false
        formatter?.format(formatContent!!, this.cursorRange)
        postInvalidate()
        return true
    }


    @Synchronized
    fun formatCodeAsync(start: CharPosition, end: CharPosition): Boolean {
        kotlin.require(!(start.index > end.index)) { "start > end" }
        if (this.isFormatting) {
            return false
        }
        val formatter: Formatter? =
            editorLanguage!!.formatter
        formatter?.setReceiver(this)
        val formatContent: Content? =
            text.copyText(false)
        formatContent?.isUndoEnabled = false
        formatter?.formatRegion(formatContent!!, TextRange(start, end), this.cursorRange)
        postInvalidate()
        return true
    }

    val cursorRange: TextRange

        get() = cursor!!.getRange()

    val isTextSelected: Boolean

        get() = cursor!!.isSelected()





    fun setScaleTextSizes(minSize: Float, maxSize: Float) {
        kotlin.require(!(minSize > maxSize)) { "min size can not be bigger than max size" }
        kotlin.require(!(minSize < 2f)) { "min size must be at least 2px" }
        touchHandler!!.scaleMinSize = minSize
        touchHandler!!.scaleMaxSize = maxSize
    }


    fun setInterceptParentHorizontalScrollIfNeeded(forceHorizontalScrollable: Boolean) {
        this.isInterceptParentHorizontalScrollEnabled = forceHorizontalScrollable
        if (!forceHorizontalScrollable) {
            val parent: ViewParent? = getParent()
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }


    fun isHighlightBracketPair(): Boolean {
        return highlightBracketPair
    }


    fun setHighlightBracketPair(highlightBracketPair: Boolean) {
        this.highlightBracketPair = highlightBracketPair
        if (!highlightBracketPair) {
            styleDelegate!!.clearFoundBracketPair()
        } else {
            styleDelegate!!.postUpdateBracketPair()
        }
        invalidate()
    }






    fun getInputType(): Int {
        return inputType
    }


    fun setInputType(inputType: Int) {
        this.inputType = inputType
        restartInput()
    }


    fun undo() {
        val range: TextRange? = text.undo()
        if (range != null) {
            try {
                setSelectionRegion(
                    range.start.line,
                    range.start.column,
                    range.end.line,
                    range.end.column,
                    true,
                    SelectionChangeEvent.CAUSE_TEXT_MODIFICATION
                )
            } catch (e: IndexOutOfBoundsException) {

            }
        }
        notifyIMEExternalCursorChange()
    }


    fun redo() {
        text.redo()
        notifyIMEExternalCursorChange()
    }


    fun canUndo(): Boolean {
        return text.canUndo()
    }


    fun canRedo(): Boolean {
        return text.canRedo()
    }


    fun isUndoEnabled(): Boolean {
        return undoEnabled
    }


    fun setUndoEnabled(enabled: Boolean) {
        undoEnabled = enabled
        if (text != null) {
            text.isUndoEnabled = enabled
        }
    }

    val diagnosticIndicatorStyle: DiagnosticIndicatorStyle?
        get() = diagnosticStyle

    fun setDiagnosticIndicatorStyle(@NonNull diagnosticIndicatorStyle: DiagnosticIndicatorStyle?) {
        this.diagnosticStyle = diagnosticIndicatorStyle
        invalidate()
    }


    fun beginSearchMode() {
        class SearchActionMode : ActionMode.Callback {

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
                val sv: SearchView = SearchView(getContext())
                sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(text: String?): Boolean {
                        _searcher.gotoNext()
                        return false
                    }


                    override fun onQueryTextChange(text: String?): Boolean {
                        if (text == null || text.isEmpty()) {
                            _searcher.stopSearch()
                            return false
                        }
                        _searcher.search(text, SearchOptions(false, false))
                        return false
                    }
                })
                p1.setCustomView(sv)
                sv.performClick()
                sv.setQueryHint(I18nConfig.getString(getContext(), R.string.sora_editor_text_to_search))
                sv.setIconifiedByDefault(false)
                sv.setIconified(false)
                return true
            }


            override fun onPrepareActionMode(p1: ActionMode?, p2: Menu?): Boolean {
                return true
            }


            override fun onActionItemClicked(am: ActionMode, p2: MenuItem): Boolean {
                if (!_searcher.hasQuery()) {
                    return false
                }
                when (p2.getItemId()) {
                    1 -> _searcher.gotoPrevious()
                    0 -> _searcher.gotoNext()
                    2, 3 -> {
                        val replaceAll = p2.getItemId() === 3
                        val et: EditText = EditText(getContext())
                        et.setHint(I18nConfig.getResourceId(R.string.sora_editor_replacement))
                        android.app.AlertDialog.Builder(context)
                            .setTitle(I18nConfig.getResourceId(if (replaceAll) R.string.sora_editor_replaceAll else R.string.sora_editor_replace))
                            .setView(et)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(
                                I18nConfig.getResourceId(R.string.sora_editor_replace)
                            ) { dialog, which ->
                                if (replaceAll) {
                                    _searcher.replaceAll(et.text.toString(), am::finish)
                                } else {
                                    _searcher.replaceCurrentMatch(et.text.toString())
                                    am.finish()
                                }
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
                return false
            }


            override fun onDestroyActionMode(p1: ActionMode?) {
                startedActionMode = ACTION_MODE_NONE
                _searcher.stopSearch()
            }
        }

        val callback: ActionMode.Callback = SearchActionMode()
        startActionMode(callback)
    }

    val eventHandler: EditorTouchEventHandler?

        get() = touchHandler!!


    fun setDividerMargin(@Px marginLeft: Float, @Px marginRight: Float) {
        kotlin.require(!(marginLeft < 0 || marginRight < 0)) { "margin can not be under zero" }
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
    fun getLineNumberMarginLeft(): Float {
        return lineNumberMarginLeft
    }

    val typefaceLineNumber: Typeface

        get() = renderer.getPaintOther().typeface


    fun setTypefaceLineNumber(typefaceLineNumber: Typeface?) {
        renderer.setTypefaceLineNumber(typefaceLineNumber)
        requestLayoutIfNeeded()
    }

    val typefaceText: Typeface


        get() = renderer.paint.typeface


    fun setTypefaceText(typefaceText: Typeface?) {
        renderer.setTypefaceText(typefaceText)
        requestLayoutIfNeeded()
    }

    var textScaleX: Float

        get() = renderer.paint.getTextScaleX()

        set(textScaleX) {
            renderer.setTextScaleX(textScaleX)
        }

    var textLetterSpacing: Float

        get() = renderer.paint.getLetterSpacing()

        set(textLetterSpacing) {
            renderer.setLetterSpacing(textLetterSpacing)
            requestLayoutIfNeeded()
        }


    fun getLineNumberAlign(): AndroidPaint.Align? {
        return lineNumberAlign
    }


    fun setLineNumberPaintAlign(align: AndroidPaint.Align?) {
        var align: AndroidPaint.Align? = align
        if (align == null) {
            align = AndroidPaint.Align.LEFT
        }
        lineNumberAlign = align
        invalidate()
    }


    fun setCursorWidth(@Px width: Float) {
        kotlin.require(!(width < 0)) { "width can not be under zero" }
        insertSelectionWidth = width
        invalidate()
    }


    fun setTextBorderWidth(@Px width: Float) {
        kotlin.require(!(width < 0)) { "width can not be under zero" }
        textBorderWidth = width
        invalidate()
    }


    @Px
    fun getTextBorderWidth(): Float {
        return textBorderWidth
    }



    val lineCount: Int

        get() = text.lineCount

    val firstVisibleLine: Int

        get() {
            try {
                return layout!!.getLineNumberForRow(this.firstVisibleRow)
            } catch (e: IndexOutOfBoundsException) {
                return 0
            }
        }

    val firstVisibleRow: Int

        get() {
            if (layout!! == null) return (this.offsetY / this.logicalRowHeight).toInt()
            return layout!!.getRowIndexForY(this.offsetY.toFloat())
        }

    val lastVisibleRow: Int

        get() {
            if (layout!! == null) return ((this.offsetY + height) / this.logicalRowHeight).toInt()
            return kotlin.math.max(0, kotlin.math.min(layout!!.rowCount - 1, layout!!.getRowIndexForY((this.offsetY + height).toFloat())))
        }

    val lastVisibleLine: Int

        get() {
            try {
                return layout!!.getLineNumberForRow(this.lastVisibleRow)
            } catch (e: IndexOutOfBoundsException) {
                return this.lineCount - 1
            }
        }


    fun isRowVisible(row: Int): Boolean {
        return (this.firstVisibleRow <= row && row <= this.lastVisibleRow)
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

    var lineSpacingExtra: Float

        get() = lineSpacingAdd

        set(lineSpacingExtra) {
            lineSpacingAdd = lineSpacingExtra
            invalidate()
        }


    fun getLineSpacingMultiplier(): Float {
        return lineSpacingMultiplier
    }


    fun setLineSpacingMultiplier(lineSpacingMultiplier: Float) {
        this.lineSpacingMultiplier = lineSpacingMultiplier
        invalidate()
    }

    val lineSpacingPixels: Int

        get() = getLineSpacingPixels(lineSpacingMultiplier, lineSpacingAdd)


    fun getLineSpacingPixels(multiplier: Float, add: Float): Int {
        val metrics: android.graphics.Paint.FontMetricsInt? =
            renderer.metricsText
        return (((metrics!!.descent - metrics.ascent) * (multiplier - 1f) + add).toInt()) / 2 * 2
    }


    fun getRowBaseline(row: Int): Int {
        val ls = lineSpacingPixels; val m = renderer.metricsText!!; return max(1, m.descent - m.ascent + ls) * (row + 1) - m.descent - ls / 2
    }

    val rowHeight get() = logicalRowHeight

    val logicalRowHeight: Int get() {
        val m = renderer.metricsText!!; return max(1, m.descent - m.ascent + getLineSpacingPixels(lineSpacingMultiplier, lineSpacingAdd))
    }

    val wrapRowHeight: Int get() {
        val m = renderer.metricsText!!; return max(1, m.descent - m.ascent + getLineSpacingPixels(wrapLineSpacingMultiplier, wrapLineSpacingAdd))
    }

    fun getRowHeight(row: Int) = if (layout == null) rowHeight else (if (layout.getRowAt(row).isTrailingRow) logicalRowHeight else wrapRowHeight)
    fun getRowTop(row: Int) = if (layout == null) logicalRowHeight * row else layout.getRowTop(row)
    fun getRowBottom(row: Int) = if (layout == null) logicalRowHeight * (row + 1) else layout.getRowBottom(row)
    fun getRowTopOfText(row: Int) = getRowTop(row) + lineSpacingPixels / 2
    fun getRowBottomOfText(row: Int) = getRowBottom(row) - lineSpacingPixels / 2

    val rowHeightOfText: Int get() { val m = renderer.metricsText!!; return m.descent - m.ascent }

    val offsetX get() = touchHandler!!.getScroller().getCurrX()
    val offsetY get() = touchHandler!!.getScroller().getCurrY()

    @UnsupportedUserUsage fun setLayoutBusy(busy: Boolean) {
        if (layoutBusy && !busy) {
            if (isWordwrap && touchHandler!!.positionNotApplied) {
                touchHandler!!.positionNotApplied = false; val line = IntPair.getFirst(touchHandler!!.memoryPosition); val col = IntPair.getSecond(touchHandler!!.memoryPosition)
                val row = (layout as WordwrapLayout).findRow(line, col); val sc = touchHandler!!.getScroller()
                val afterY = row.toFloat() * rowHeight - touchHandler!!.focusY
                dispatchEvent(ScrollEvent(this, sc.getCurrX(), sc.getCurrY(), 0, afterY.toInt(), ScrollEvent.CAUSE_SCALE_TEXT))
                sc.startScroll(0, afterY.toInt(), 0, 0, 0); sc.abortAnimation()
            }
            layoutBusy = false; restartInput(); postInvalidate(); dispatchEvent(LayoutStateChangeEvent(this, false)); return
        }
        if (layoutBusy == busy) return
        layoutBusy = busy; dispatchEvent(LayoutStateChangeEvent(this, busy))
    }

    fun isBlockLineEnabled() = blockLineEnabled
    fun setBlockLineEnabled(enabled: Boolean) { blockLineEnabled = enabled; invalidate() }

    fun beginComposingTextRejection() { rejectComposingCount++ }
    fun acceptsComposingText() = rejectComposingCount == 0
    fun endComposingTextRejection() { rejectComposingCount = max(0, rejectComposingCount - 1) }

    fun hasMouseHovering() = mouseHover
    fun hasMousePressed() = mouseButtonPressed

    val isInMouseMode: Boolean get() = when (props!!.mouseMode) {
        DirectAccessProps.MOUSE_MODE_ALWAYS -> true
        DirectAccessProps.MOUSE_MODE_NEVER -> false
        else -> hasMouseHovering() || hasMousePressed()
    }

    internal val selectingTarget get() = if (cursor.left().equals(selectionAnchor)) cursor.right() else cursor.left()

    protected fun ensureSelectingTargetVisible() { if (cursor.left().equals(selectionAnchor)) ensureSelectionVisible() else ensurePositionVisible(cursor.leftLine, cursor.leftColumn) }

    protected fun ensureSelectionAnchorAvailable() { if (selectionAnchor == null || !text.isValidPosition(selectionAnchor!!)) selectionAnchor = cursor.right() }

    fun moveOrExtendSelection(@NonNull movement: SelectionMovement, extend: Boolean) = if (extend) extendSelection(movement) else moveSelection(movement)

    fun extendSelection(@NonNull movement: SelectionMovement) {
        ensureSelectionAnchorAvailable(); val sel = movement.getPositionAfterMovement(this, selectingTarget)
        setSelectionRegion(selectionAnchor!!.line, selectionAnchor!!.column, sel.line, sel.column, false, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
        if (movement === SelectionMovement.PAGE_UP) touchHandler!!.scrollBy(0f, -height.toFloat(), true)
        else if (movement === SelectionMovement.PAGE_DOWN) touchHandler!!.scrollBy(0f, height.toFloat(), true)
        ensureSelectingTargetVisible()
    }

    fun moveSelection(@NonNull movement: SelectionMovement) {
        if (cursor.isSelected()) {
            if (movement === SelectionMovement.LEFT) { setSelection(cursor.leftLine, cursor.leftColumn, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE); return }
            if (movement === SelectionMovement.RIGHT) { setSelection(cursor.rightLine, cursor.rightColumn, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE); return }
        }
        val pos = when (movement.basePosition) {
            SelectionMovement.MovingBasePosition.LEFT_SELECTION -> cursor.left()
            SelectionMovement.MovingBasePosition.RIGHT_SELECTION -> cursor.right()
            else -> { selectionAnchor = cursor.right(); selectionAnchor }
        }
        val sel = movement.getPositionAfterMovement(this, pos!!); if (movement === SelectionMovement.PAGE_UP) touchHandler!!.scrollBy(0f, -height.toFloat(), true)
        else if (movement === SelectionMovement.PAGE_DOWN) touchHandler!!.scrollBy(0f, height.toFloat(), true)
        setSelection(sel.line, sel.column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
    }

    fun setSelection(line: Int, column: Int) = setSelection(line, column, SelectionChangeEvent.CAUSE_UNKNOWN)
    fun setSelection(line: Int, column: Int, cause: Int) = setSelection(line, column, true, cause)
    fun setSelection(line: Int, column: Int, makeItVisible: Boolean) = setSelection(line, column, makeItVisible, SelectionChangeEvent.CAUSE_UNKNOWN)

    fun setSelection(line: Int, column: Int, makeItVisible: Boolean, cause: Int) {
        var col = column; _cursorAnimator!!.markStartPos()
        if (col > 0 && Character.isHighSurrogate(text.getLineString(line)[col - 1])) { col++; if (col > text.getColumnCount(line)) col-- }
        cursor.set(line, col); if (highlightCurrentBlock) blockIndex = findCursorBlock(); updateCursor(); updateSelection()
        if (isEditable && !touchHandler!!.hasAnyHeldHandle() && acceptsComposingText()) { _cursorAnimator!!.markEndPos(); _cursorAnimator!!.start() }
        selectionAnchor = cursor.right(); renderContext?.invalidateRenderNodes(); if (makeItVisible) ensurePositionVisible(line, col) else invalidate()
        onSelectionChanged(cause)
    }

    fun selectAll() = setSelectionRegion(0, 0, lineCount - 1, text.getColumnCount(lineCount - 1))

    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int, cause: Int) = setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, cause)
    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int) = setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, SelectionChangeEvent.CAUSE_UNKNOWN)
    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int, makeRightVisible: Boolean) = setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, makeRightVisible, SelectionChangeEvent.CAUSE_UNKNOWN)

    fun setSelectionRegion(lineLeft: Int, columnLeft: Int, lineRight: Int, columnRight: Int, makeRightVisible: Boolean, cause: Int) {
        var (cl, cr) = columnLeft to columnRight; requestFocus(); val start = text.getCharIndex(lineLeft, cl); val end = text.getCharIndex(lineRight, cr)
        if (start == end) { setSelection(lineLeft, cl, makeRightVisible, cause); return }
        if (start > end) { setSelectionRegion(lineRight, cr, lineLeft, cl, makeRightVisible, cause); Log.w(LOG_TAG, "setSelectionRegion() error: start > end:start = $start end = $end lLeft = $lineLeft cLeft = $cl lRight = $lineRight cRight = $cr"); return }
        _cursorAnimator!!.cancel()
        if (cl > 0 && Character.isHighSurrogate(text.getLineString(lineLeft)[cl - 1])) { cl++; if (cl > text.getColumnCount(lineLeft)) cl-- }
        if (cr > 0 && Character.isHighSurrogate(text.getLineString(lineRight)[cr - 1])) { cr++; if (cr > text.getColumnCount(lineRight)) cr-- }
        cursor.setLeft(lineLeft, cl); cursor.setRight(lineRight, cr); updateCursor(); updateSelection(); renderContext?.invalidateRenderNodes()
        if (!cursor.left().equals(selectionAnchor) && !cursor.right().equals(selectionAnchor)) selectionAnchor = cursor.right()
        if (makeRightVisible) { if (cause == SelectionChangeEvent.CAUSE_SEARCH) { ensurePositionVisible(lineLeft, cl); lastMakeVisible = 0; ensurePositionVisible(lineRight, cr) } else ensurePositionVisible(lineRight, cr) }
        else invalidate()
        onSelectionChanged(cause)
    }

    fun pasteText() = try { var clip: ClipData? = null; clipboardManager?.let { if (it.hasPrimaryClip()) clip = it.primaryClip }; clip?.let { pasteText(ClipDataUtils.clipDataToString(it)) } }
                      catch (e: Exception) { Log.w(LOG_TAG, "Error pasting text to editor", e); Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show() }

    fun pasteText(@Nullable text: CharSequence?) = text?.let { inputConnection?.let { ic -> ic.commitText(it, 1); if (props!!.formatPastedText) formatCodeAsync(lastInsertion!!.start, lastInsertion!!.end); notifyIMEExternalCursorChange() } }

    @JvmOverloads fun copyText(shouldCopyLine: Boolean = true) = if (cursor.isSelected()) copyTextToClipboard(text, cursor.left, cursor.right) else if (shouldCopyLine) copyLine() else lineSeparator?.content?.let { copyTextToClipboard(it, 0, it.length) }

    protected fun copyTextToClipboard(@NonNull text: CharSequence, start: Int, end: Int) {
        if (end < start) return
        if (end - start > props!!.clipboardTextLengthLimit) { Toast.makeText(context, I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large), Toast.LENGTH_SHORT).show(); return }
        try { val clip = if (text is Content) text.substring(start, end) else text.subSequence(start, end).toString(); clipboardManager!!.setPrimaryClip(ClipData.newPlainText(clip, clip)) }
        catch (e: RuntimeException) { if (e.cause is TransactionTooLargeException) Toast.makeText(context, I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large), Toast.LENGTH_SHORT).show() else { Log.w(LOG_TAG, e); Toast.makeText(context, e.javaClass.toString(), Toast.LENGTH_SHORT).show() } }
    }

    private fun copyLine() { if (cursor.isSelected()) { copyText(); return }; val line = cursor.left().line; setSelectionRegion(line, 0, line, text.getColumnCount(line)); copyText(false) }

    fun cutText() = if (cursor.isSelected()) { copyText(); deleteText(); notifyIMEExternalCursorChange() } else cutLine()

    fun cutLine() {
        if (cursor.isSelected()) { cutText(); return }; val left = cursor.left(); val line = left.line
        if (line + 1 == lineCount) { if (text.getColumnCount(line) == 0) { copyText(false); return }; setSelectionRegion(line, 0, line, text.getColumnCount(line)) }
        else setSelectionRegion(line, 0, line + 1, 0)
        cutText(); if (props!!.placeSelOnPreviousLineAfterCut) moveSelection(SelectionMovement.LEFT)
    }

    fun duplicateLine() { if (cursor.isSelected()) { duplicateSelection(); return }; val line = cursor.left().line; setSelectionRegion(line, 0, line, text.getColumnCount(line), true); duplicateSelection("\n", false) }

    @JvmOverloads fun duplicateSelection(selectDuplicate: Boolean = true) = duplicateSelection("", selectDuplicate)

    fun duplicateSelection(prefix: String?, selectDuplicate: Boolean) {
        if (!cursor.isSelected()) return
        val (left, right) = cursor.left() to cursor.right().fromThis(); val sub = text.subContent(left.line, left.column, right.line, right.column)
        setSelection(right.line, right.column); commitText(prefix + sub, false)
        if (selectDuplicate) { val r = cursor.right(); setSelectionRegion(right.line, right.column, r.line, r.column) }
    }

    fun selectCurrentWord() { val left = cursor.left(); selectWord(left.line, left.column) }

    fun selectWord(line: Int, column: Int) { val range = getWordRange(line, column); setSelectionRegion(range.start.line, range.start.column, range.end.line, range.end.column, SelectionChangeEvent.CAUSE_LONG_PRESS) }

    fun getWordRange(line: Int, column: Int) = getWordRange(line, column, props!!.useICULibToSelectWords)
    fun getWordRange(line: Int, column: Int, useIcu: Boolean) = Chars.getWordRange(text, line, column, useIcu)

    fun setText(@Nullable text: CharSequence?) = setText(text, true, null)

    @NonNull fun getExtraArguments() = extraArguments

    fun setText(@Nullable text: CharSequence?, @Nullable extraArguments: Bundle?) = setText(text, true, extraArguments)

    fun setText(@Nullable text: CharSequence?, reuseContentObject: Boolean, @Nullable extraArguments: Bundle?) {
        forceSyncBreakLines = true; val t = text ?: ""; _text?.apply { removeContentListener(this@CodeEditor); resetBatchEdit() }
        this.extraArguments = extraArguments ?: Bundle(); lastInsertion = null; _text = if (reuseContentObject && t is Content) t.apply { resetBatchEdit(); renderer.updateTimestamp() } else Content(t)
        styleDelegate?.reset(); textStyles = null; cursor = _text!!.cursor; selectionAnchor = cursor.right(); touchHandler?.reset()
        _text!!.apply { addContentListener(this@CodeEditor); isUndoEnabled = undoEnabled; setBidiEnabled(true) }
        renderContext?.reset(_text!!.lineCount); renderer.onEditorFullTextUpdate()
        editorLanguage?.let { it.analyzeManager.reset(ContentReference(_text!!), this.extraArguments!!); it.formatter.cancel() }; inlayHints = null
        dispatchEvent(ContentChangeEvent(this, ContentChangeEvent.ACTION_SET_NEW_TEXT, CharPosition(), _text!!.indexer.getCharPosition(lineCount - 1, _text!!.getColumnCount(lineCount - 1)), _text!!, false))
        createLayout(); inputMethodManager?.apply { viewClicked(this@CodeEditor); showSoftInput(this@CodeEditor, 0) }; renderContext?.invalidateRenderNodes(); invalidate()
    }

    fun setTextSize(textSize: Float) { val res = context?.resources ?: Resources.getSystem(); textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, res.displayMetrics) }

    fun <T : Event> subscribeEvent(eventType: Class<T>, receiver: EventReceiver<T>) = eventManager!!.subscribeEvent(eventType, receiver)
    fun <T : Event> subscribeAlways(eventType: Class<T>, receiver: EventManager.NoUnsubscribeReceiver<T>) = eventManager!!.subscribeAlways(eventType, receiver)
    fun <T : Event> dispatchEvent(event: T) = eventManager!!.dispatchEvent(event)
    @NonNull fun createSubEventManager() = EventManager(eventManager)

    val isFormatting get() = editorLanguage!!.formatter.isRunning()
    @get:NonNull val textPaint get() = renderer.paint
    val otherPaint get() = renderer.paintOther
    val graphPaint get() = renderer.paintGraph

    fun jumpToLine(line: Int) = setSelection(line, 0)

    fun beginLongSelect() {
        if (!isEditable) return
        if (cursor.isSelected()) setSelection(cursor.leftLine, cursor.leftColumn)
        isInLongSelect = true; invalidate()
    }

    fun endLongSelect() { isInLongSelect = false }

    fun rerunAnalysis() { editorLanguage?.analyzeManager?.rerun() }

    @get:Nullable val styles get() = textStyles

    @UiThread fun setStyles(@Nullable styles: Styles?) {
        textStyles = styles; if (highlightCurrentBlock) blockIndex = findCursorBlock(); renderContext?.invalidateRenderNodes(); renderer.updateTimestamp(); invalidate()
    }

    @UiThread fun updateStyles(@NonNull styles: Styles?, @Nullable range: StyleUpdateRange?) {
        if (textStyles !== styles || range == null) { setStyles(styles); return }
        if (highlightCurrentBlock) this.blockIndex = findCursorBlock()
        renderContext?.updateForRange(range); renderer.updateTimestamp(); invalidate()
    }

    @get:Nullable
    val highlightTexts: HighlightTextContainer?
        get() = highlightTextContainer


    fun hideAutoCompleteWindow() {
        if (completionWindow != null) {
            completionWindow!!.hide()
        }
    }


    fun showSoftInput() {
        if (isEditable && isEnabled()) {

            if (isInTouchMode() && !isFocused()) {
                requestFocusFromTouch()
            }
            if (!isFocused()) {
                requestFocus()
            }

            if (checkSoftInputEnabled()) inputMethodManager!!.showSoftInput(this, 0)
        }
        invalidate()
    }


    fun hideSoftInput() {
        inputMethodManager!!.hideSoftInputFromWindow(windowToken, 0)
    }


    protected fun checkSoftInputEnabled(): Boolean {
        if (this.isDisableSoftKbdIfHardKbdAvailable
            && KeyboardUtils.isHardKeyboardConnected(getContext())
        ) {
            return false
        }
        return this.isSoftKeyboardEnabled
    }

    var isSoftKeyboardEnabled: Boolean

        get() = this.isSoftKbdEnabled

        set(isEnabled) {
            if (isSoftKbdEnabled == isEnabled) {

                return
            }

            this.isSoftKbdEnabled = isEnabled
            hideSoftInput()
            restartInput()
        }

    var isDisableSoftKbdIfHardKbdAvailable: Boolean

        get() = isDisableSoftKbdOnHardKbd

        set(isDisabled) {
            if (isDisableSoftKbdOnHardKbd == isDisabled) {

                return
            }

            this.isDisableSoftKbdOnHardKbd = isDisabled
            hideSoftInput()
            restartInput()
        }


    internal fun updateSelection() {
        if (props!!.disallowSuggestions) {
            val index: Int? =
                Random().nextInt()
            inputMethodManager!!.updateSelection(this, index!!, index, -1, -1)
            return
        }
        if (inputConnection!!.composingText.preSetComposing) {
            return
        }
        var candidatesStart = -1
        var candidatesEnd = -1
        if (inputConnection!!.composingText.isComposing()) {
            try {
                candidatesStart = inputConnection!!.composingText.startIndex
                candidatesEnd = inputConnection!!.composingText.endIndex
            } catch (e: IndexOutOfBoundsException) {

            }
        }
        inputMethodManager!!.updateSelection(this, cursor!!.left, cursor!!.right, candidatesStart, candidatesEnd)
    }


    protected fun updateExtractedText() {
        if (extractingTextRequest != null) {
            val text: ExtractedText? = extractText(extractingTextRequest!!)
            inputMethodManager!!.updateExtractedText(this, extractingTextRequest!!.token, text)
        }
    }





    internal fun setExtracting(@Nullable request: ExtractedTextRequest?) {
        if (props!!.disallowSuggestions) {
            extractingTextRequest = null
            return
        }
        extractingTextRequest = request
    }


    internal fun extractText(@NonNull request: ExtractedTextRequest): ExtractedText? {
        if (props!!.disallowSuggestions || props!!.disableTextExtracting) {
            return null
        }
        val cur: Cursor = cursor
        val text: ExtractedText = ExtractedText()
        val selBegin: Int = cur.left
        val selEnd: Int = cur.right
        var startOffset = 0
        if (request.hintMaxChars === 0) {
            request.hintMaxChars = props!!.maxIPCTextLength
        }
        if (startOffset + request.hintMaxChars < selBegin) {
            startOffset = selBegin - request.hintMaxChars / 2
            startOffset = kotlin.math.min(startOffset, selBegin)
        }
        text.text = inputConnection!!.getTextRegion(startOffset, startOffset + request.hintMaxChars, request.flags)
        text.startOffset = startOffset
        text.selectionStart = selBegin - startOffset
        text.selectionEnd = selEnd - startOffset
        if (this.getKeyMetaStates().isSelecting) {
            text.flags = text.flags or ExtractedText.FLAG_SELECTING
        }
        return text
    }


    fun notifyIMEExternalCursorChange() {
        updateExtractedText()
        updateSelection()
        updateCursorAnchor()

        if (inputConnection!!.composingText.isComposing()) {
            restartInput()
        }
    }


    fun restartInput() {
        inputConnection?.reset()
        inputMethodManager?.restartInput(this)
    }


    fun updateCursor() {
        updateCursorAnchor()
        updateExtractedText()
        if (text.getNestedBatchEdit() <= 1 && !inputConnection!!.composingText.isComposing()) {
            updateSelection()
        }
    }


    fun release() {
        hideEditorWindows()
        if (!this.isReleased) {
            dispatchEvent(EditorReleaseEvent(this))
        } else {
            return
        }
        this.isReleased = true
        editorLanguage?.let { lang ->
            lang.analyzeManager.destroy()
            val formatter: Formatter? = lang.formatter
            formatter?.setReceiver(null)
            formatter?.destroy()
            lang.destroy()
            editorLanguage = EmptyLanguage()
        }


        textStyles = null
        diagnostics = null
        styleDelegate!!.reset()

        val text: Content? = this.text
        if (text != null) {
            text.removeContentListener(this)
        }
        colorScheme.detachEditor(this)
    }


    fun hideEditorWindows() {
        completionWindow?.cancelCompletion()
        completionWindow?.hide()
        textActionWindow?.dismiss()
        touchHandler!!.editorMagnifier.dismiss()
        diagnosticTooltip?.dismiss()
    }


    fun onColorUpdated(type: Int) {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext?.invalidateRenderNodes()
        invalidate()
    }


    fun onColorFullUpdate() {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext?.invalidateRenderNodes()
        invalidate()
    }




    internal fun onCloseConnection() {
        setExtracting(null)
        invalidate()
    }


    @NonNull
    protected fun onCreateRenderer(): EditorRenderer {
        return EditorRenderer(this)
    }


    protected fun onSelectionChanged(cause: Int) {
        var oldLeft: CharPosition? = null
        var oldRight: CharPosition? = null
        val lastTextRange: TextRange? = this.lastSelectedTextRange
        if (lastTextRange != null) {
            oldLeft = lastTextRange.getStart()
            oldRight = lastTextRange.getEnd()
        }
        dispatchEvent(SelectionChangeEvent(this, oldLeft, oldRight, cause))
        this.lastSelectedTextRange = this.cursorRange
    }


    @UnsupportedUserUsage
    open fun releaseEdgeEffects() {

        edgeEffectHorizontal!!.onRelease()
        edgeEffectVertical!!.onRelease()
    }





    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        renderer.draw(canvas)


        if ((lastCursorState != cursorBlink?.visibility ?: false || !touchHandler!!.getScroller()
                .isFinished) && touchHandler!!.editorMagnifier.isShowing()
        ) {
            lastCursorState = cursorBlink?.visibility ?: false
            postInLifecycle(touchHandler!!.editorMagnifier::updateDisplay)
        }
    }


    override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo? {
        val info: AccessibilityNodeInfo? =
            super.createAccessibilityNodeInfo()
        if (isEnabled()) {
            info!!.setEditable(isEditable)
            val cur = cursor
        if (cur != null) {
            info.setTextSelection(cur.left, cur.right)
        }
            info.setInputType(android.text.InputType.TYPE_CLASS_TEXT)
            info.setMultiLine(true)
            info.setText(text)
            info.setLongClickable(true)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
            val scrollRange = this.scrollMaxY
            if (scrollRange > 0) {
                info.setScrollable(true)
                val scrollY = this.offsetY
                if (scrollY > 0) {
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD
                    )
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
        val maxScrollY = this.scrollMaxY
        event.setScrollable(maxScrollY > 0)
        event.setMaxScrollX(this.scrollMaxX)
        event.setMaxScrollY(maxScrollY)
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
        return io.github.abc15018045126.sora.widget.CodeEditor::class.java.getName()
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val x = event.getX().toInt()
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                if (this.isInterceptParentHorizontalScrollEnabled) {
                    getParent().requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - downX
                if (this.isInterceptParentHorizontalScrollEnabled && !touchHandler!!.hasAnyHeldHandle()) {
                    if (deltaX > 0 && scroller.getCurrX() == 0
                        || deltaX < 0 && scroller.getCurrX() == scrollMaxX
                    ) {
                        getParent().requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }


    override fun onCheckIsTextEditor(): Boolean {
        return isEnabled() && isEditable
    }


    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!isEditable || !isEnabled()) {
            return null
        }
        if (checkSoftInputEnabled()) {
            outAttrs.inputType =
                if (inputType != 0) inputType else EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            outAttrs.inputType = android.text.InputType.TYPE_NULL
        }
        outAttrs.initialSelStart = if (cursor != null) cursor.left else 0
        outAttrs.initialSelEnd = if (cursor != null) cursor.right else 0
        outAttrs.initialCapsMode = inputConnection!!.getCursorCapsMode(0)



        if (!props!!.allowFullscreen) {
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }

        dispatchEvent(BuildEditorInfoEvent(this, outAttrs))
        inputConnection!!.reset()
        text.resetBatchEdit()
        setExtracting(null)
        return inputConnection
    }


    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): PointerIcon {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                if (this.isFormatting || layoutBusy) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_WAIT)
                }
                if (touchHandler!!.hasAnyHeldHandle()) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING)
                }
                if (this.leftHandleDescriptor?.position?.contains(event.getX(), event.getY()) == true
                    || this.rightHandleDescriptor?.position?.contains(event.getX(), event.getY()) == true
                    || this.insertHandleDescriptor?.position?.contains(event.getX(), event.getY()) == true
                ) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRAB)
                }
                val res: Long =
                    resolveTouchRegion(event, pointerIndex)
                val region = IntPair.getFirst(res)
                val inbound = IntPair.getSecond(res) == IN_BOUND
                if (region == REGION_TEXT && inbound) {
                    if (touchHandler!!.mouseCanMoveText && !touchHandler!!.mouseClick) {
                        return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING)
                    }
                    if (renderer.lastStuckLines != null) {
                        val stickyLineCount: Int? =
                            renderer.lastStuckLines!!.size
                        if (stickyLineCount!! > 0 && event.getY() < getRowBottom(stickyLineCount - 1)) {
                            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND)
                        }
                    }
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TEXT)
                } else if (region == REGION_LINE_NUMBER) {
                    when (props!!.actionWhenLineNumberClicked) {
                        DirectAccessProps.LN_ACTION_SELECT_LINE, DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> {
                            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND)
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
        if (!isEnabled()) {
            return false
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) && props!!.mouseMode !== DirectAccessProps.MOUSE_MODE_NEVER) {
            return touchHandler!!.onMouseEvent(event)
        }
        if (this.isFormatting) {
            touchHandler!!.reset2()
            scaleDetector?.onTouchEvent(event)
            return basicDetector?.onTouchEvent(event) ?: false
        }
        val handlingBefore: Boolean = touchHandler!!.handlingMotions()
        val res: Boolean = touchHandler!!.onTouchEvent(event)
        val handling: Boolean = touchHandler!!.handlingMotions()
        var res2 = false
        val res3: Boolean = scaleDetector?.onTouchEvent(event) ?: false
        if (!handling && !handlingBefore) {
            res2 = basicDetector?.onTouchEvent(event) ?: false
        }
        if (event.getAction() === MotionEvent.ACTION_UP) {
            edgeEffectVertical!!.onRelease()
            edgeEffectHorizontal!!.onRelease()
        }
        return (res3 || res2 || res)
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


    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (MeasureSpec.getMode(widthMeasureSpec) !== MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(heightMeasureSpec) !== MeasureSpec.EXACTLY
        ) {
            Log.w(
                LOG_TAG,
                "use wrap_content in editor may cause layout!! lags"
            )
            val specs: Long = ViewMeasureHelper.getDesiredSize(
                widthMeasureSpec, heightMeasureSpec, measureTextRegionOffset(),
                this.rowHeight.toFloat(), isWordwrap, tabWidth, text, renderer.paintGeneral
            )
            widthMeasureSpec = IntPair.getFirst(specs)
            heightMeasureSpec = IntPair.getSecond(specs)
            anyWrapContentSet = true
        } else {
            anyWrapContentSet = false
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }



    override fun onDragEvent(event: DragEvent) = when (event.action) {
        DragEvent.ACTION_DRAG_STARTED -> event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
        DragEvent.ACTION_DRAG_LOCATION -> { touchHandler!!.draggingSelection = getPointPositionOnScreen(event.x, event.y).let { text.indexer.getCharPosition(IntPair.getFirst(it).toInt(), IntPair.getSecond(it).toInt()) }; postInvalidate(); true }
        DragEvent.ACTION_DROP -> touchHandler!!.draggingSelection?.let { pos -> touchHandler!!.draggingSelection = null; setSelection(pos.line, pos.column); pasteText(ClipDataUtils.clipDataToString(event.clipData)); requestFocus(); postInvalidate(); super.onDragEvent(event); true } ?: false
        else -> super.onDragEvent(event)
    }

    protected override fun onCreateContextMenu(menu: ContextMenu?) {
        super.onCreateContextMenu(menu); touchHandler!!.lastContextClickPosition?.let { pos -> val charPos = getPointPositionOnScreen(pos.x, pos.y); dispatchEvent(CreateContextMenuEvent(this, menu!!, text.getIndexer().getCharPosition(IntPair.getFirst(charPos).toInt(), IntPair.getSecond(charPos).toInt()))) }
    }

    protected override fun onConfigurationChanged(newConfig: Configuration?) { super.onConfigurationChanged(newConfig); touchHandler!!.resetMouse(); mouseButtonPressed = false; mouseHover = false }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) mouseHover = true else if (event.action == MotionEvent.ACTION_HOVER_EXIT) mouseHover = false
            if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS || event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE) mouseButtonPressed = event.buttonState != 0
            when (event.action) { MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_EXIT -> { touchHandler!!.dispatchEditorMotionEvent({ ed, pos, ev, sp, spR, reg, bnd -> HoverEvent(ed, pos, ev, sp, spR, reg, bnd) }, null, event); return true } }
        }
        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER) && !keyEventHandler.getKeyMetaStates().isCtrlPressed) {
            var dx = -event.getAxisValue(MotionEvent.AXIS_HSCROLL) * verticalScrollFactor * props!!.mouseWheelScrollFactor
            var dy = -event.getAxisValue(MotionEvent.AXIS_VSCROLL) * verticalScrollFactor * props!!.mouseWheelScrollFactor
            if (keyEventHandler.getKeyMetaStates().isAltPressed) { dx *= props!!.fastScrollSensitivity; dy *= props!!.fastScrollSensitivity }
            if (keyEventHandler.getKeyMetaStates().isShiftPressed) { val t = dx; dx = dy; dy = t }
            touchHandler!!.onScroll(event, event, dx, dy); return true
        }
        return super.onGenericMotionEvent(event)
    }

    protected override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(w, h, oldWidth, oldHeight); renderer.onSizeChanged(w, h); verticalEdgeEffect.setSize(w, h); horizontalEdgeEffect.setSize(h, w); verticalEdgeEffect.finish(); horizontalEdgeEffect.finish()
        if (layout == null || (isWordwrap && w != oldWidth)) createLayout() else touchHandler!!.scrollBy(if (offsetX > scrollMaxX) (scrollMaxX - offsetX).toFloat() else 0f, if (offsetY > scrollMaxY) (scrollMaxY - offsetY).toFloat() else 0f)
        verticalAbsorb = false; horizontalAbsorb = false; if (oldHeight > h && props!!.adjustToSelectionOnResize) ensureSelectionVisible()
    }

    protected override fun onDetachedFromWindow() { super.onDetachedFromWindow(); dispatchEvent(EditorAttachStateChangeEvent(this, false)); cursorBlink?.let { removeCallbacks(it) } }
    protected override fun onAttachedToWindow() { super.onAttachedToWindow(); dispatchEvent(EditorAttachStateChangeEvent(this, true)) }

    protected override fun onFocusChanged(gainFocus: Boolean, direction: Int, @Nullable previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) { cursorBlink?.let { if (it.valid && it.period > 0) postInLifecycle(it) } }
        else { cursorBlink?.let { it.valid = false; it.visibility = false; removeCallbacks(it) }; touchHandler!!.hideInsertHandle() }
        dispatchEvent(EditorFocusChangeEvent(this, gainFocus)); invalidate()
    }

    override fun computeScroll() {
        val sc = touchHandler!!.getScroller()
        if (sc.computeScrollOffset()) {
            if (!sc.isFinished && (sc.getStartX() != sc.getFinalX() || sc.getStartY() != sc.getFinalY())) { scrollerFinalX = sc.getFinalX().toFloat(); scrollerFinalY = sc.getFinalY().toFloat(); horizontalAbsorb = abs(sc.getStartX() - sc.getFinalX()) > dpUnit * 5; verticalAbsorb = abs(sc.getStartY() - sc.getFinalY()) > dpUnit * 5 }
            if (sc.getCurrX() <= 0 && scrollerFinalX <= 0 && edgeEffectHorizontal!!.isFinished() && horizontalAbsorb) { edgeEffectHorizontal!!.onAbsorb(sc.getCurrVelocity().toInt()); touchHandler!!.glowLeftOrRight = false }
            else if (sc.getCurrX() >= scrollMaxX && scrollerFinalX >= scrollMaxX && edgeEffectHorizontal!!.isFinished() && horizontalAbsorb) { edgeEffectHorizontal!!.onAbsorb(sc.getCurrVelocity().toInt()); touchHandler!!.glowLeftOrRight = true }
            if (sc.getCurrY() <= 0 && scrollerFinalY <= 0 && edgeEffectVertical!!.isFinished() && verticalAbsorb) { edgeEffectVertical!!.onAbsorb(sc.getCurrVelocity().toInt()); touchHandler!!.glowTopOrBottom = false }
            else if (sc.getCurrY() >= scrollMaxY && scrollerFinalY >= scrollMaxY && edgeEffectVertical!!.isFinished() && verticalAbsorb) { edgeEffectVertical!!.onAbsorb(sc.getCurrVelocity().toInt()); touchHandler!!.glowTopOrBottom = true }
            postInvalidateOnAnimation()
        }
    }

    override fun computeVerticalScrollRange() = scrollMaxY
    override fun computeVerticalScrollOffset() = max(0, min(scrollMaxY, offsetY))
    override fun computeHorizontalScrollRange() = scrollMaxX
    override fun computeHorizontalScrollOffset() = max(0, min(scrollMaxX, offsetX))
    override fun computeHorizontalScrollExtent() = 0
    override fun computeVerticalScrollExtent() = 0

    override fun removeCallbacks(action: Runnable?): Boolean { action?.let { EditorHandler.removeCallbacks(it) }; return super.removeCallbacks(action) }

    fun postInLifecycle(action: Runnable) = EditorHandler.post { if (!isReleased) action.run() }
    fun postDelayedInLifecycle(action: Runnable, delayMillis: Long) = EditorHandler.postDelayed({ if (!isReleased) action.run() }, delayMillis)

    override fun beforeReplace(@NonNull content: Content) { waitForNextChange = true; layout!!.beforeReplace(content) }

    override fun afterInsert(@NonNull content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, @NonNull insertedContent: CharSequence) {
        renderContext?.updateForInsertion(startLine, endLine); renderer.updateTimestamp(); styleDelegate!!.onTextChange()
        val start = text.indexer.getCharPosition(startLine, startColumn); val end = text.indexer.getCharPosition(endLine, endColumn)
        try { textStyles?.adjustOnInsert(start, end); diagnostics?.shiftOnInsert(start.index, end.index); inlayHints?.updateOnInsertion(startLine, startColumn, endLine, endColumn); highlightTextContainer?.updateOnInsertion(startLine, startColumn, endLine, endColumn) }
        catch (e: Exception) { Log.w(LOG_TAG, "Update failure", e) }
        layout!!.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent); renderer.buildMeasureCacheForLines(startLine, endLine); checkForRelayout()
        editorLanguage!!.analyzeManager.insert(start, end, insertedContent); touchHandler!!.hideInsertHandle()
        if (isEditable && cursor != null && !cursor.isSelected() && !inputConnection!!.composingText.isComposing() && acceptsComposingText()) { _cursorAnimator!!.markEndPos(); _cursorAnimator!!.start() }
        selectionAnchor = if (lastAnchorIsSelLeft) cursor?.left() else cursor?.right()
        dispatchEvent(ContentChangeEvent(this, ContentChangeEvent.ACTION_INSERT, start, end, insertedContent, text.isUndoManagerWorking()))
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION); lastInsertion = TextRange(start.fromThis(), end.fromThis()); waitForNextChange = false; ensureSelectionVisible(); updateCursor()
    }

    override fun afterDelete(@NonNull content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, @NonNull deletedContent: CharSequence) {
        renderContext?.updateForDeletion(startLine, endLine); renderer.updateTimestamp(); styleDelegate!!.onTextChange()
        val start = text.getIndexer().getCharPosition(startLine, startColumn); val end = start.fromThis().apply { column = endColumn; line = endLine; index = start.index + deletedContent.length }
        try { textStyles?.adjustOnDelete(start, end); diagnostics?.shiftOnDelete(start.index, end.index); inlayHints?.updateOnDeletion(startLine, startColumn, endLine, endColumn); highlightTextContainer?.updateOnDeletion(startLine, startColumn, endLine, endColumn) }
        catch (e: Exception) { Log.w(LOG_TAG, "Update failure", e) }
        layout!!.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent); renderer.buildMeasureCacheForLines(startLine, startLine + 1); checkForRelayout()
        if (isEditable && !cursor.isSelected() && !waitForNextChange && !inputConnection!!.composingText.isComposing() && acceptsComposingText()) { _cursorAnimator!!.markEndPos(); _cursorAnimator!!.start() }
        editorLanguage!!.analyzeManager.delete(start, end, deletedContent); selectionAnchor = if (lastAnchorIsSelLeft) cursor.left() else cursor.right()
        dispatchEvent(ContentChangeEvent(this, ContentChangeEvent.ACTION_DELETE, start, end, deletedContent, text.isUndoManagerWorking()))
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION); if (!waitForNextChange) { updateCursor(); ensureSelectionVisible(); touchHandler!!.hideInsertHandle() }
    }

    override fun beforeModification(@NonNull content: Content) {
        if (props!!.checkModificationThread && isAttachedToWindow) { getHandler()?.let { if (it.looper.thread !== Thread.currentThread()) throw RuntimeException("text is changed in wrong thread") } }
        _cursorAnimator!!.markStartPos(); lastAnchorIsSelLeft = cursor.left() == selectionAnchor
    }

    override fun onFormatSucceed(@NonNull applyContent: CharSequence, @Nullable cursorRange: TextRange?) {
        postInLifecycle {
            val (line, col) = cursor.leftLine to cursor.leftColumn; val (x, y) = offsetX to offsetY
            text.beginBatchEdit(); text.delete(0, 0, text.lineCount - 1, text.getColumnCount(text.lineCount - 1)); text.insert(0, 0, applyContent); text.endBatchEdit()
            inputConnection!!.markInvalid(); if (cursorRange == null) setSelectionAround(line, col) else try { setSelectionRegion(cursorRange.start.line, cursorRange.start.column, cursorRange.end.line, cursorRange.end.column) } catch (e: IndexOutOfBoundsException) { Log.w(LOG_TAG, e) }
            scroller.apply { forceFinished(true); startScroll(x, y, 0, 0, 0); abortAnimation() }
            touchHandler!!.scrollBy(0f, 0f); inputConnection!!.reset(); restartInput(); dispatchEvent(EditorFormatEvent(this, true))
        }
    }

    override fun onFormatFail(throwable: Throwable?) {
        postInLifecycle { Toast.makeText(context, "Format:$throwable", Toast.LENGTH_SHORT).show(); dispatchEvent(EditorFormatEvent(this, false)) }
    }

    companion object {

        const val DEFAULT_TEXT_SIZE: Int = 18


        const val DEFAULT_LINE_INFO_TEXT_SIZE: Int = 21


        const val DEFAULT_CURSOR_BLINK_PERIOD: Int = 500


        const val FLAG_DRAW_WHITESPACE_LEADING: Int = 1
        const val FLAG_DRAW_WHITESPACE_INNER: Int = 1 shl 1
        const val FLAG_DRAW_WHITESPACE_TRAILING: Int = 1 shl 2
        const val FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE: Int = 1 shl 3
        const val FLAG_DRAW_LINE_SEPARATOR: Int = 1 shl 4
        const val FLAG_DRAW_TAB_SAME_AS_SPACE: Int = 1 shl 5
        const val FLAG_DRAW_WHITESPACE_IN_SELECTION: Int = 1 shl 6
        const val FLAG_DRAW_SOFT_WRAP: Int = 1 shl 7

        const val ACTION_MODE_NONE: Int = 0
        const val ACTION_MODE_SEARCH_TEXT: Int = 1
        const val ACTION_MODE_SELECT_TEXT: Int = 2

        @JvmField
        val logger: Logger? = Logger.instance("CodeEditor")


        const val NUMBER_DIGITS = "0 1 2 3 4 5 6 7 8 9"
        const val LOG_TAG = "CodeEditor"
        const val COPYRIGHT =
            "sora-editor\nCopyright (C) abc15018045126 roses2020@qq.com\nThis project is distributed under the LGPL v2.1 license"


        fun hasVisibleRegion(begin: Int, end: Int, first: Int, last: Int): Boolean {
            return (end > first && begin < last)
        }
    }

}

