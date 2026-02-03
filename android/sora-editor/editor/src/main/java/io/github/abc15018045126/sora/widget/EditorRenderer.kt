package io.github.abc15018045126.sora.widget

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.SparseArray
import androidx.annotation.RequiresApi
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.graphics.*
import io.github.abc15018045126.sora.lang.completion.snippet.SnippetItem
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticRegion
import io.github.abc15018045126.sora.lang.styling.*
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.lang.styling.line.*
import io.github.abc15018045126.sora.text.*
import io.github.abc15018045126.sora.text.bidi.Directions
import io.github.abc15018045126.sora.util.*
import io.github.abc15018045126.sora.util.Numbers.stringSize
import io.github.abc15018045126.sora.widget.layout.*
import io.github.abc15018045126.sora.widget.rendering.*
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.style.*
import java.util.*
import kotlin.math.*
import android.graphics.Paint as AndroidPaint

class EditorRenderer(internal val editor: CodeEditor) {
    internal val bufferedDrawPoints: BufferedDrawPoints = BufferedDrawPoints()
    @JvmField internal val paintGeneral = Paint()
    @JvmField internal val paintOther = Paint()
    @JvmField internal val paintGraph = Paint()
    @JvmField internal val viewRect = Rect()
    @JvmField internal val verticalScrollBarRect = RectF()
    @JvmField internal val horizontalScrollBarRect = RectF()
    private val tmpRect = RectF(); private val tmpPath = Path()
    private val postDrawLineNumbers = LongArrayList()
    private val postDrawCurrentLines = MutableIntList()
    private val matchedPositions = LongArrayList()
    private val highlightPositions = MutableLongLongMap()
    private val preloadedLines = SparseArray<ContentLine>()
    private val preloadedDirections = SparseArray<Directions>()
    private val collectedDiagnostics = mutableListOf<DiagnosticRegion>()
    @JvmField var lastStuckLines: List<CodeBlock?>? = null
    @JvmField var metricsText: AndroidPaint.FontMetricsInt? = null
    private val sharedTextRow = TextRow()
    private var horizontalScrollbarThumbDrawable: Drawable? = null
    private var horizontalScrollbarTrackDrawable: Drawable? = null
    private var verticalScrollbarThumbDrawable: Drawable? = null
    private var verticalScrollbarTrackDrawable: Drawable? = null
    private val lineBreakGraph: Drawable? = null
    private val softwrapLeftGraph: Drawable? = null
    private val softwrapRightGraph: Drawable? = null
    @Volatile var timestamp: Long = 0; private set
    private var metricsLineNumber = AndroidPaint.FontMetricsInt()
    private var metricsGraph: AndroidPaint.FontMetricsInt? = null
    private var cachedGutterWidth = 0
    private var cursor: Cursor? = null
    protected var lineBuf: ContentLine? = null
    protected var content: Content? = null
    @Volatile private var renderingFlag = false
    internal var forcedRecreateLayout = false

    fun onEditorFullTextUpdate() { cursor = editor.cursor; content = editor.text }
    fun draw(canvas: Canvas) { val c = canvas.save(); canvas.translate(editor.offsetX.toFloat(), editor.offsetY.toFloat()); renderingFlag = true; try { drawView(canvas) } finally { renderingFlag = false }; canvas.restoreToCount(c) }
    fun onSizeChanged(w: Int, h: Int) { viewRect.right = w; viewRect.bottom = h }

    val paint get() = paintGeneral
    fun getPaintOther() = paintOther
    fun getPaintGraph() = paintGraph
    fun setCachedLineNumberWidth(w: Int) { cachedGutterWidth = w }
    fun getVerticalScrollBarRect() = verticalScrollBarRect
    fun getHorizontalScrollBarRect() = horizontalScrollBarRect

    fun setHorizontalScrollbarThumbDrawable(d: Drawable?) { horizontalScrollbarThumbDrawable = d }
    fun getHorizontalScrollbarThumbDrawable() = horizontalScrollbarThumbDrawable
    fun setHorizontalScrollbarTrackDrawable(d: Drawable?) { horizontalScrollbarTrackDrawable = d }
    fun getHorizontalScrollbarTrackDrawable() = horizontalScrollbarTrackDrawable
    fun setVerticalScrollbarThumbDrawable(d: Drawable?) { verticalScrollbarThumbDrawable = d }
    fun getVerticalScrollbarThumbDrawable() = verticalScrollbarThumbDrawable
    fun setVerticalScrollbarTrackDrawable(d: Drawable?) { verticalScrollbarTrackDrawable = d }
    fun getVerticalScrollbarTrackDrawable() = verticalScrollbarTrackDrawable

    fun setTextSizePxDirect(size: Float) {
        paintGeneral.setTextSizeWrapped(size); paintOther.textSize = size; paintGraph.textSize = size * editor.props!!.functionCharacterSizeFactor
        metricsText = paintGeneral.fontMetricsInt; metricsLineNumber = paintOther.fontMetricsInt; metricsGraph = paintGraph.fontMetricsInt
        editor.renderContext!!.invalidateRenderNodes(); updateTimestamp()
    }

    fun setTypefaceText(tf: Typeface?) { paintGeneral.setTypefaceWrapped(tf ?: Typeface.DEFAULT); metricsText = paintGeneral.fontMetricsInt; editor.renderContext!!.invalidateRenderNodes(); updateTimestamp(); editor.createLayout(); editor.invalidate() }
    fun setTypefaceLineNumber(tf: Typeface?) { paintOther.typeface = tf ?: Typeface.MONOSPACE; metricsLineNumber = paintOther.fontMetricsInt; editor.invalidate() }
    fun setTextScaleX(s: Float) { paintGeneral.textScaleX = s; paintOther.textScaleX = s; onTextStyleUpdate() }
    fun setLetterSpacing(s: Float) { paintGeneral.letterSpacing = s; paintOther.letterSpacing = s; onTextStyleUpdate() }

    internal fun onTextStyleUpdate() {
        paintGeneral.isRenderFunctionCharacters = editor.isRenderFunctionCharacters; metricsGraph = paintGraph.fontMetricsInt; metricsLineNumber = paintOther.fontMetricsInt; metricsText = paintGeneral.fontMetricsInt
        editor.renderContext!!.invalidateRenderNodes(); updateTimestamp(); editor.createLayout(); editor.invalidate()
    }

    fun updateTimestamp() { timestamp = SystemClock.elapsedRealtimeNanos() }
    protected fun prepareLine(line: Int) { lineBuf = getLine(line) }

    protected fun getLine(line: Int) = if (!renderingFlag) getLineDirect(line) else preloadedLines[line] ?: content!!.getLine(line).also { preloadedLines.put(line, it) }

    protected fun getLineDirections(line: Int): Directions {
        if (!renderingFlag) return content!!.getLineDirections(line)
        return preloadedDirections[line] ?: content!!.getLineDirections(line).also { preloadedDirections.put(line, it) }
    }

    fun getLineDirect(line: Int) = content!!.getLine(line)
    fun getColumnCount(line: Int) = getLine(line).length


    @RequiresApi(29)
    fun updateLineDisplayList(renderNode: RenderNode, line: Int, spans: Spans.Reader?) {
        val widthLine = drawSingleTextLine(null, line, 0f, 0f, spans, false)
        renderNode.setPosition(0, 0, (widthLine + 0.5f).toInt(), editor.logicalRowHeight)
        val canvas =
            renderNode.beginRecording()
        try {
            drawSingleTextLine(canvas, line, 0f, 0f, spans, false)
        } finally {
            renderNode.endRecording()
        }
    }

    @UnsupportedUserUsage
    fun createTextRow(rowIndex: Int): TextRow {
        val tr = TextRow()
        updateTextRow(tr, rowIndex)
        return tr
    }

    private fun updateTextRow(tr: TextRow, rowIndex: Int) {
        val styles = editor.styles
        val spanMap =
            if (styles != null) styles.spans else null
        var spanReader =
            if (spanMap != null) spanMap.read() else null
        spanReader = if (spanReader == null) EmptyReader.INSTANCE else spanReader
        val row =
            editor.layout!!.getRowAt(rowIndex)
        val line =
            content!!.getLine(row.lineIndex)
        val cache =
            editor.renderContext!!.cache.queryMeasureCache(row.lineIndex)
        var widths =
            if (cache != null && cache.updateTimestamp >= this.timestamp) cache.widths else null
        widths = if (widths != null && widths.size > line.length) widths else null
        tr.set(
            line,
            row.startColumn,
            row.endColumn,
            spanReader.getSpansOnLine(row.lineIndex),
            row.inlayHints,
            content!!.getLineDirections(row.lineIndex),
            paintGeneral,
            widths,
            createTextRowParams()
        )
        applySelectedTextRange(tr, row.lineIndex)
    }

    private fun applySelectedTextRange(tr: TextRow, lineIndex: Int) {
        val cur = cursor ?: return
        if (cur.isSelected() && lineIndex >= cur.leftLine && lineIndex <= cur.rightLine) {
            var startColInLine = if (lineIndex == cur.leftLine) cur.leftColumn else 0
            var endColInLine: Int =
                if (lineIndex == cur.rightLine) cur.rightColumn else (lineBuf?.length ?: 0)
            startColInLine = Math.max(tr.textStart, startColInLine)
            endColInLine = Math.min(tr.textEnd, endColInLine)
            if (startColInLine < endColInLine) {
                tr.setSelectedRange(startColInLine, endColInLine)
            }
        }
    }

    protected fun drawSingleTextLine(
        canvas: Canvas?,
        line: Int,
        offsetX: Float,
        offsetY: Float,
        spans: Spans.Reader?,
        visibleOnly: Boolean
    ): Float {
        var reader: Spans.Reader? = spans
        prepareLine(line)
        val columnCount = getColumnCount(line)
        if (reader == null || reader.getSpanCount() <= 0) {
            reader = EmptyReader.INSTANCE
        }
        val tr: TextRow = TextRow()
        val inlayHints =
            editor.inlayHints
        val lineInlays: List<InlayHint>? =
            if (inlayHints == null) Collections.emptyList() else inlayHints.getForLine(line) as List<InlayHint>?
        val cache =
            editor.renderContext!!.cache.queryMeasureCache(line)
        var widths =
            if (cache != null && cache.updateTimestamp >= this.timestamp) cache.widths else null
        widths = if (widths != null && widths.size > (lineBuf?.length ?: 0)) widths else null
        tr.set(
            lineBuf!!,
            0,
            columnCount,
            reader.getSpansOnLine(line),
            lineInlays,
            getLineDirections(line),
            paintGeneral,
            widths,
            createTextRowParams()!!
        )
        applySelectedTextRange(tr, line)
        if (canvas != null) {
            canvas.save()
            canvas.translate(offsetX, editor.getRowTop(0) + offsetY)
            if (visibleOnly) {
                val visibleStart: Float = Math.max(0f, -offsetX)
                val visibleEnd: Float = Math.max(visibleStart, -offsetX + editor.width)
                tr.draw(canvas, visibleStart, visibleEnd)
            } else {
                tr.draw(canvas, 0f, Float.MAX_VALUE)
            }
            canvas.restore()
        }
        return if (canvas == null) tr.computeRowWidth() else 0f
    }

    fun hasSideHintIcons(): Boolean {
        val styles: Styles? = editor.styles
        if (styles != null) {
            val styleTypeCount = styles.styleTypeCount
            if (styleTypeCount != null) {
                val count =
                    styleTypeCount.get(LineSideIcon::class.java)
                if (count == null) {
                    return false
                }
                return count.value > 0
            }
        }
        return false
    }


    fun drawView(canvas: Canvas) {
        cursor?.updateCache(editor.firstVisibleLine); val color = editor.colorScheme; drawColor(canvas, color.getColor(EditorColorScheme.WHOLE_BACKGROUND), viewRect)
        val lnWidth = editor.measureLineNumber(); val sideIconW = if (hasSideHintIcons()) editor.logicalRowHeight.toFloat() else 0f
        var oX = -editor.offsetX.toFloat() + editor.measureTextRegionOffset(); val textOff = oX; val gutterW = (lnWidth + sideIconW + editor.dividerWidth + editor.dividerMarginLeft + editor.dividerMarginRight).toInt()
        if (editor.isWordwrap) { if (cachedGutterWidth == 0 || (cachedGutterWidth != gutterW && !editor.touchHandler!!.isScaling)) { cachedGutterWidth = gutterW; editor.postInLifecycle(editor::requestLayoutIfNeeded); editor.createLayout(false) } else if (forcedRecreateLayout) { editor.createLayout(); editor.postInLifecycle(editor::requestLayoutIfNeeded) } }
        else { cachedGutterWidth = 0; if (forcedRecreateLayout) editor.createLayout() }
        forcedRecreateLayout = false; prepareLines(editor.firstVisibleLine, editor.lastVisibleLine); buildMeasureCacheForLines(editor.firstVisibleLine, editor.lastVisibleLine, timestamp, true)
        val stuckLines = stuckCodeBlocks; val cur = editor.cursor ?: return; if (cur.isSelected()) editor.handleDescInsert!!.setEmpty() else { editor.handleDescLeft!!.setEmpty(); editor.handleDescRight!!.setEmpty() }
        val notPinned = editor.isLineNumberEnabled && !editor.isLineNumberPinned; postDrawLineNumbers.clear(); postDrawCurrentLines.clear()
        val postDrawCursor = mutableListOf<DrawCursorTask?>(); val firstLn = if (editor.isFirstLineNumberAlwaysVisible && editor.isWordwrap && !editor.isLineNumberPinned) MutableInt(-1) else null
        canvas.save(); val stuckLB = getStuckLineBottom(stuckLines); canvas.clipRect(0f, stuckLB, editor.width.toFloat(), editor.height.toFloat()); drawRows(canvas, textOff, postDrawLineNumbers, postDrawCursor, postDrawCurrentLines, firstLn); patchHighlightedDelimiters(canvas, textOff); drawDiagnosticIndicators(canvas, oX); canvas.restore()
        oX = -editor.offsetX.toFloat(); val curLnNum = if (cur.isSelected()) -1 else cur.leftLine
        if (notPinned) {
            drawLineNumberBackground(canvas, oX, lnWidth + sideIconW + editor.dividerMarginLeft, color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND))
            val lnColor = color.getColor(EditorColorScheme.LINE_NUMBER); val curLnBgColor = color.getColor(EditorColorScheme.CURRENT_LINE)
            if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable) { tmpRect.bottom = editor.cursorAnimator.animatedLineBottom() - editor.offsetY; tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight(); tmpRect.left = 0f; tmpRect.right = textOff - editor.dividerMarginRight; drawColor(canvas, curLnBgColor, tmpRect) }
            canvas.save(); canvas.clipRect(0f, stuckLB, editor.width.toFloat(), editor.height.toFloat()); repeat(postDrawCurrentLines.size) { drawRowBackground(canvas, curLnBgColor, postDrawCurrentLines.get(it), (textOff - editor.dividerMarginRight).toInt()) }
            drawUserGutterBackground(canvas, (textOff - editor.dividerMarginRight).toInt()); drawSideIcons(canvas, oX + lnWidth); canvas.restore()
            val divX = if (editor.isLineNumberRightOfDivider()) oX else oX + lnWidth + sideIconW + editor.dividerMarginLeft; drawDivider(canvas, divX, color.getColor(EditorColorScheme.LINE_DIVIDER))
            val numX = if (editor.isLineNumberRightOfDivider()) oX + editor.dividerWidth + editor.dividerMarginRight + sideIconW else oX; canvas.save(); canvas.clipRect(0f, stuckLB, editor.width.toFloat(), editor.height.toFloat())
            if (firstLn?.value != -1) {
                val b = editor.getRowBottom(0); val y = if (postDrawLineNumbers.size == 0 || editor.getRowTop(IntPair.getSecond(postDrawLineNumbers.get(0))) - editor.offsetY > b) (editor.getRowBottom(0) + editor.getRowTop(0)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent else (editor.getRowBottom(IntPair.getSecond(postDrawLineNumbers.get(0)) - 1) + editor.getRowTop(IntPair.getSecond(postDrawLineNumbers.get(0)) - 1)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.offsetY
                paintOther.textAlign = editor.lineNumberAlign; paintOther.color = if (firstLn!!.value == curLnNum) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lnColor; val txt = (firstLn.value + 1).toString()
                when (editor.lineNumberAlign) { AndroidPaint.Align.LEFT -> canvas.drawText(txt, numX, y, paintOther); AndroidPaint.Align.RIGHT -> canvas.drawText(txt, numX + lnWidth, y, paintOther); AndroidPaint.Align.CENTER -> canvas.drawText(txt, numX + (lnWidth + editor.dividerMarginLeft) / 2f, y, paintOther); else -> {} }
            }
            repeat(postDrawLineNumbers.size) { val p = postDrawLineNumbers.get(it); drawLineNumber(canvas, IntPair.getFirst(p), IntPair.getSecond(p), numX, lnWidth, if (IntPair.getFirst(p) == curLnNum) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lnColor) }; canvas.restore()
        }
        if (editor.isBlockLineEnabled()) { canvas.save(); canvas.clipRect(0f, stuckLB, editor.width.toFloat(), editor.height.toFloat()); if (editor.isWordwrap) drawSideBlockLine(canvas) else drawBlockLines(canvas, textOff); canvas.restore() }
        if (!editor.cursorAnimator.isRunning()) postDrawCursor.forEach { it?.execute(canvas) } else drawSelectionOnAnimation(canvas)
        drawStuckLines(canvas, stuckLines, textOff)
        if (editor.isLineNumberEnabled && !notPinned) {
            drawLineNumberBackground(canvas, 0f, lnWidth + sideIconW + editor.dividerMarginLeft, color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND)); canvas.save(); canvas.clipRect(0f, stuckLB, editor.width.toFloat(), editor.height.toFloat())
            val lnColor = color.getColor(EditorColorScheme.LINE_NUMBER); val curLnBgColor = color.getColor(EditorColorScheme.CURRENT_LINE)
            if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable) { tmpRect.bottom = editor.cursorAnimator.animatedLineBottom() - editor.offsetY; tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight(); tmpRect.left = 0f; tmpRect.right = textOff - editor.dividerMarginRight + editor.offsetX; drawColor(canvas, curLnBgColor, tmpRect) }
            repeat(postDrawCurrentLines.size) { drawRowBackground(canvas, curLnBgColor, postDrawCurrentLines.get(it), (textOff - editor.dividerMarginRight + editor.offsetX).toInt()) }
            drawUserGutterBackground(canvas, (textOff - editor.dividerMarginRight + editor.offsetX).toInt()); drawSideIcons(canvas, lnWidth); canvas.restore()
            drawDivider(canvas, lnWidth + sideIconW + editor.dividerMarginLeft, color.getColor(EditorColorScheme.LINE_DIVIDER)); canvas.save(); canvas.clipRect(0f, stuckLB, editor.width.toFloat(), editor.height.toFloat())
            repeat(postDrawLineNumbers.size) { val p = postDrawLineNumbers.get(it); drawLineNumber(canvas, IntPair.getFirst(p), IntPair.getSecond(p), 0f, lnWidth, if (IntPair.getFirst(p) == curLnNum) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lnColor) }; canvas.restore()
        }
        drawStuckLineNumbers(canvas, stuckLines, oX, lnWidth, color.getColor(EditorColorScheme.LINE_NUMBER)); drawScrollBars(canvas); drawEdgeEffect(canvas); releasePreloadedData(); lastStuckLines = stuckLines; drawFormatTip(canvas)
    }

    protected fun drawUserGutterBackground(canvas: Canvas, right: Int) {
        for (line in editor.firstVisibleLine..editor.lastVisibleLine) {
            getUserGutterBackgroundForLine(line)?.let { bg ->
                val bgColor = bg.resolve(editor.colorScheme); val top = (editor.layout!!.getCharLayoutOffset(line, 0)[0] / editor.logicalRowHeight).toInt() - 1
                repeat(editor.layout!!.getRowCountForLine(line)) { drawRowBackground(canvas, bgColor, top + it, right) }
            }
        }
    }

    protected fun drawStuckLineNumbers(canvas: Canvas, candidates: List<CodeBlock?>?, offset: Float, lnWidth: Float, lnColor: Int) {
        if (candidates.isNullOrEmpty() || !editor.isLineNumberEnabled) return
        val cur = editor.cursor; val curLn = if (cur?.isSelected() == true) -1 else (cur?.leftLine ?: -1)
        canvas.save(); canvas.translate(0f, editor.offsetY.toFloat())
        candidates.forEachIndexed { i, block ->
            if (block == null) return@forEachIndexed
            val line = block.startLine; val bg = getUserGutterBackgroundForLine(line); val color = bg?.resolve(editor.colorScheme) ?: 0
            val bOffset = editor.getRowBottom(i); val endTop = editor.getRowTop(block.endLine) - editor.offsetY
            val st = endTop < bOffset && endTop >= bOffset - editor.logicalRowHeight
            if (st) { canvas.save(); canvas.clipRect(0f, (editor.getRowTop(i) - editor.offsetY).toFloat(), editor.width.toFloat(), editor.height.toFloat()); canvas.translate(0f, (endTop - bOffset).toFloat()) }
            if (curLn == line || color != 0) {
                tmpRect.top = (editor.getRowTop(i) - editor.offsetY).toFloat(); tmpRect.bottom = editor.getRowBottom(i) - editor.offsetY - editor.dpUnit
                tmpRect.left = if (editor.isLineNumberPinned) 0f else offset; tmpRect.right = tmpRect.left + editor.measureTextRegionOffset()
                if (curLn == line && editor.isHighlightCurrentLine) drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE), tmpRect)
                if (color != 0) drawColor(canvas, color, tmpRect)
            }
            drawLineNumber(canvas, line, i, if (editor.isLineNumberPinned) 0f else offset, lnWidth, if (curLn == line) editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lnColor)
            if (st) canvas.restore()
        }
        canvas.restore()
    }

    protected fun getStuckLineBottom(candidates: List<CodeBlock?>?): Float {
        if (candidates.isNullOrEmpty()) return 0f
        var bOffset = 0f; var offLn = 0; var prevLn = -1
        candidates.forEach { block ->
            if (block == null) return@forEach
            if (block.startLine > prevLn) {
                bOffset = editor.getRowBottom(offLn).toFloat(); val endTop = editor.getRowTop(block.endLine) - editor.offsetY
                if (endTop < bOffset && endTop >= bOffset - editor.logicalRowHeight) bOffset += (endTop - bOffset).toFloat()
                prevLn = block.startLine; offLn++
            }
        }
        return bOffset
    }

    protected fun drawStuckLines(canvas: Canvas, candidates: List<CodeBlock?>?, offset: Float) {
        if (candidates.isNullOrEmpty()) return
        val reader = editor.styles?.spans?.read(); var prevLn = -1; var offLn = 0
        val cur = editor.cursor; val curLn = if (cur?.isSelected() == true) -1 else (cur?.leftLine ?: -1); var bOffset = 0f
        candidates.forEach { block ->
            if (block == null || block.startLine <= prevLn) return@forEach
            tmpRect.top = editor.getRowTop(offLn).toFloat(); tmpRect.bottom = editor.getRowBottom(offLn).toFloat(); bOffset = tmpRect.bottom
            tmpRect.left = offset; tmpRect.right = editor.width.toFloat()
            val endTop = editor.getRowTop(block.endLine) - editor.offsetY; val st = endTop < tmpRect.bottom && endTop >= tmpRect.top
            if (st) { canvas.save(); canvas.clipRect(0f, tmpRect.top, editor.width.toFloat(), editor.height.toFloat()); canvas.translate(0f, (endTop - tmpRect.bottom).toFloat()); bOffset += (endTop - tmpRect.bottom).toFloat() }
            drawColor(canvas, editor.colorScheme.getColor(if (block.startLine == curLn && editor.isHighlightCurrentLine) EditorColorScheme.CURRENT_LINE else EditorColorScheme.WHOLE_BACKGROUND), tmpRect)
            if (canvas.isHardwareAccelerated && editor.isHardwareAcceleratedDrawAllowed && Build.VERSION.SDK_INT >= 29 && editor.renderContext?.renderNodeHolder != null && !editor.touchHandler!!.isScaling && (editor.props!!.cacheRenderNodeForLongLines || getLine(block.startLine).length < 128)) editor.renderContext?.renderNodeHolder?.drawLineHardwareAccelerated(canvas, block.startLine, offset, (offLn * editor.logicalRowHeight).toFloat())
            else { reader?.moveToLine(block.startLine); drawSingleTextLine(canvas, block.startLine, offset, (offLn * editor.logicalRowHeight).toFloat(), reader, true); reader?.moveToLine(-1) }
            prevLn = block.startLine; offLn++
            if (st) canvas.restore()
        }
        if (bOffset > 0f) {
            tmpRect.top = bOffset - editor.dpUnit; tmpRect.bottom = bOffset; tmpRect.left = 0f; tmpRect.right = editor.width.toFloat()
            val shadow = (editor.props!!.stickyLineIndicator and DirectAccessProps.STICKY_LINE_INDICATOR_SHADOW) != 0
            var show = (editor.props!!.stickyLineIndicator and DirectAccessProps.STICKY_LINE_INDICATOR_LINE) != 0; val lnColor = editor.colorScheme.getColor(EditorColorScheme.STICKY_SCROLL_DIVIDER); show = show && lnColor != 0
            if (!shadow && !show) return
            if (shadow) { canvas.save(); canvas.clipRect(0f, if (show) tmpRect.top else tmpRect.bottom, editor.width.toFloat(), editor.height.toFloat()); paintGeneral.setShadowLayer(editor.dpUnit * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP, 0f, 0f, Color.BLACK) }
            drawColor(canvas, if (!show && shadow) Color.BLACK else lnColor, tmpRect); if (shadow) { paintGeneral.setShadowLayer(0f, 0f, 0f, 0); canvas.restore() }
        }
    }

    protected fun drawHardwrapMarker(canvas: Canvas?, offset: Float) {
        val col = editor.props!!.hardwrapColumn
        if (!editor.isWordwrap && col > 0) {
            tmpRect.left = offset + paintGeneral.measureText("a") * col; tmpRect.right = tmpRect.left + editor.dpUnit * 2f; tmpRect.top = 0f; tmpRect.bottom = viewRect.bottom.toFloat()
            drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.HARD_WRAP_MARKER), tmpRect)
        }
    }

    protected fun drawSideIcons(canvas: Canvas?, offset: Float) {
        if (!hasSideHintIcons()) return
        var row = editor.firstVisibleRow; val itr = editor.layout!!.obtainRowIterator(row); val factor = editor.props!!.sideIconSizeFactor
        val size = (editor.logicalRowHeight * factor).toInt(); val off = (editor.logicalRowHeight * (1 - factor) / 2f).toInt()
        while (row <= editor.lastVisibleRow && itr.hasNext()) {
            val rowInf = itr.next()
            if (rowInf.isLeadingRow) getLineStyle(rowInf.lineIndex, LineSideIcon::class.java)?.let { hint ->
                hint.drawable.apply { bounds = Rect(offset.toInt() + off, (editor.getRowTop(row) - editor.offsetY + off).toInt(), offset.toInt() + off + size, (editor.getRowTop(row) - editor.offsetY + off).toInt() + size); draw(canvas!!) }
            }
            row++
        }
    }

    protected fun drawFormatTip(canvas: Canvas) {
        if (editor.isFormatting) {
            val text = editor.formatTip; val baseline = editor.getRowBaseline(0).toFloat(); val rightX = editor.width.toFloat()
            paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.TEXT_NORMAL); paintGeneral.isFakeBoldText = true; paintGeneral.textAlign = AndroidPaint.Align.RIGHT
            if (text != null) canvas.drawText(text, rightX, baseline, paintGeneral); paintGeneral.textAlign = AndroidPaint.Align.LEFT; paintGeneral.isFakeBoldText = false
        }
    }

    protected fun drawColor(canvas: Canvas?, color: Int, rect: RectF?) { if (canvas != null && color != 0 && rect != null) { paintGeneral.color = color; canvas.drawRect(rect, paintGeneral) } }
    protected fun drawColorRound(canvas: Canvas, color: Int, rect: RectF) { if (color != 0) { paintGeneral.color = color; val r = rect.height() * RenderingConstants.ROUND_RECT_FACTOR; canvas.drawRoundRect(rect, r, r, paintGeneral) } }
    protected fun drawColor(canvas: Canvas?, color: Int, rect: Rect?) { if (canvas != null && color != 0 && rect != null) { paintGeneral.color = color; canvas.drawRect(rect, paintGeneral) } }
    protected fun drawRowBackground(canvas: Canvas, color: Int, row: Int) = drawRowBackground(canvas, color, row, viewRect.right)
    protected fun drawRowBackground(canvas: Canvas, color: Int, row: Int, right: Int) { tmpRect.top = (editor.getRowTop(row) - editor.offsetY).toFloat(); tmpRect.bottom = (editor.getRowBottom(row) - editor.offsetY).toFloat(); tmpRect.left = 0f; tmpRect.right = right.toFloat(); drawColor(canvas, color, tmpRect) }

    protected fun drawLineNumber(canvas: Canvas, line: Int, row: Int, oX: Float, width: Float, color: Int) {
        if (width + oX <= 0) return
        if (paintOther.textAlign != editor.lineNumberAlign) paintOther.textAlign = editor.lineNumberAlign
        paintOther.color = color; val y = (editor.getRowBottom(row) + editor.getRowTop(row)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.offsetY
        val buffer = TemporaryCharBuffer.obtain(20); val realLine = line + 1; val i = stringSize(realLine); io.github.abc15018045126.sora.util.Numbers.getChars(realLine, i, buffer)
        when (editor.lineNumberAlign) {
            AndroidPaint.Align.LEFT -> canvas.drawText(buffer, 0, i, oX, y, paintOther)
            AndroidPaint.Align.RIGHT -> canvas.drawText(buffer, 0, i, oX + width, y, paintOther)
            AndroidPaint.Align.CENTER -> canvas.drawText(buffer, 0, i, oX + (width + editor.dividerMarginLeft) / 2f, y, paintOther)
            else -> {}
        }
        TemporaryCharBuffer.recycle(buffer)
    }


    protected fun drawLineNumberBackground(canvas: Canvas, oX: Float, width: Float, color: Int) {
        val right = oX + width; if (right < 0) return
        val left = max(0f, oX); tmpRect.bottom = editor.height.toFloat(); tmpRect.top = 0f; val offY = editor.offsetY
        if (offY < 0) { tmpRect.bottom -= offY.toFloat(); tmpRect.top -= offY.toFloat() }
        tmpRect.left = left; tmpRect.right = right; drawColor(canvas, color, tmpRect)
    }

    protected fun drawDivider(canvas: Canvas, oX: Float, color: Int) {
        val shadow = editor.isLineNumberPinned && !editor.isWordwrap && editor.offsetX > 0; val right = oX + editor.dividerWidth
        if (right < 0) return
        val left = max(0f, oX); tmpRect.bottom = editor.height.toFloat(); tmpRect.top = 0f; val offY = editor.offsetY
        if (offY < 0) { tmpRect.bottom -= offY.toFloat(); tmpRect.top -= offY.toFloat() }
        tmpRect.left = left; tmpRect.right = right
        if (shadow) { canvas.save(); canvas.clipRect(tmpRect.left, tmpRect.top, editor.width.toFloat(), tmpRect.bottom); paintGeneral.setShadowLayer(min((editor.dpUnit * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP).toFloat(), editor.offsetX.toFloat()), 0f, 0f, Color.BLACK) }
        drawColor(canvas, color, tmpRect); if (shadow) { canvas.restore(); paintGeneral.setShadowLayer(0f, 0f, 0f, 0) }
    }

    private fun prepareLines(start: Int, end: Int) {
        val content = this.content ?: return; releasePreloadedData()
        content.runReadActionsOnLines(max(0, start - 5), min(content.lineCount - 1, end + 5), object : Content.ContentLineConsumer { override fun accept(i: Int, line: ContentLine, dirs: Directions) { preloadedLines.put(i, line); preloadedDirections.put(i, dirs) } })
    }

    private fun releasePreloadedData() { preloadedLines.clear(); preloadedDirections.clear() }

    protected val stuckCodeBlocks: List<CodeBlock>?
        get() {
            if (editor.isWordwrap || !editor.props!!.stickyScroll) {
                return null
            }
            val styles = editor.styles ?: return null
            val codeBlocks = styles.blocksByStart ?: return null

            var startLine = editor.firstVisibleLine
            var offsetY = editor.offsetY
            val rowHeight = editor.logicalRowHeight
            val size = codeBlocks.size
            if (size == 0) {
                return null
            }
            val candidates = mutableListOf<CodeBlock>()
            val limit = editor.props!!.stickyScrollIterationLimit
            val maxLine = content!!.lineCount
            var i = 0
            while (i < size && i < limit) {
                val block = codeBlocks[i]
                if (block == null || block.startLine > block.endLine || block.startLine > maxLine || block.endLine > maxLine || block.startLine < 0) {
                    i++
                    continue
                }
                if (block.startLine > startLine) {
                    break
                }
                if (block.endLine > startLine && editor.getRowTop(block.startLine) - offsetY < 0) {
                    candidates.add(block)
                    startLine++
                    offsetY += rowHeight
                }
                i++
            }

            val maxLines = editor.props!!.stickyScrollMaxLines
            var finalCandidates: List<CodeBlock> = candidates
            if (finalCandidates.size > maxLines) {
                if (maxLines <= 0) {
                    return null
                }
                finalCandidates = if (editor.props!!.stickyScrollPreferInnerScope) {
                    finalCandidates.subList(finalCandidates.size - maxLines, finalCandidates.size)
                } else {
                    finalCandidates.subList(0, maxLines)
                }
            }
            val cur = editor.cursor
            if (cur != null && cur.isSelected() && editor.props!!.stickyScrollAutoCollapse) {
                val limitLine = cur.leftLine
                val firstVis = editor.firstVisibleLine
                val lastSelectionLine = cur.rightLine
                if (lastSelectionLine >= firstVis) {
                    val mutableCandidates = finalCandidates.toMutableList()
                    while (mutableCandidates.isNotEmpty() && firstVis + mutableCandidates.size >= limitLine) {
                        mutableCandidates.removeAt(mutableCandidates.size - 1)
                    }
                    finalCandidates = mutableCandidates
                }
            }
            return if (finalCandidates.isEmpty()) null else finalCandidates
        }

    private val coordinateLine = LineStyles(0)

    init {
        paintGeneral.isFilterBitmap = editor.isRenderFunctionCharacters
        paintGeneral.isAntiAlias = true
        paintOther.setStrokeWidth(editor.dpUnit * 1.8f)
        paintOther.strokeCap = AndroidPaint.Cap.ROUND
        paintOther.typeface = Typeface.MONOSPACE
        paintOther.isAntiAlias = true
        paintGraph.isAntiAlias = true
        metricsText = paintGeneral.fontMetricsInt
        metricsLineNumber = paintOther.fontMetricsInt
        onEditorFullTextUpdate()
    }

    
    protected fun getLineStyles(line: Int): LineStyles? {
        val styles = editor.styles ?: return null
        val lineStylesList = styles.lineStyles ?: return null
        coordinateLine.line = line
        val index =
            Collections.binarySearch(lineStylesList, coordinateLine)
        if (index >= 0 && index < lineStylesList.size) {
            return lineStylesList[index]
        }
        return null
    }

    
    internal fun <T : LineAnchorStyle> getLineStyle(line: Int, type: Class<T>): T? {
        val lineStyles: LineStyles? = getLineStyles(line)
        if (lineStyles != null) {
            return lineStyles.findOne(type)
        }
        return null
    }

    
    protected fun getUserBackgroundForLine(line: Int): ResolvableColor? {
        val bg: LineBackground? = getLineStyle(line, LineBackground::class.java)
        if (bg != null) {
            return bg.color
        }
        return null
    }

    
    protected fun getUserGutterBackgroundForLine(line: Int): ResolvableColor? {
        val bg: LineGutterBackground? = getLineStyle(line, LineGutterBackground::class.java)
        if (bg != null) {
            return bg.color
        }
        return null
    }


    protected fun drawAnimatedCurrentLineBackground(canvas: Canvas, currentLineBgColor: Int) {
        tmpRect.bottom = (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
        tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
        tmpRect.left = 0f
        tmpRect.right = viewRect.right.toFloat()
        drawColor(canvas, currentLineBgColor, tmpRect)
    }

    fun createTextRowParams(): TextRowParams {
        return TextRowParams(
            editor.tabWidth, this.metricsText!!, editor.getRowTopOfText(0),
            editor.getRowBottomOfText(0), editor.logicalRowHeight, editor.getRowBaseline(0),
            editor.getRowTop(0), editor.getRowBottom(0),
            editor.logicalRowHeight, editor.props!!.roundTextBackgroundFactor,
            editor, editor.colorScheme, paintOther, paintGraph, metricsGraph!!
        )
    }


    protected fun drawRows(canvas: Canvas, offset: Float, postDrawLineNumbers: LongArrayList, postDrawCursor: MutableList<DrawCursorTask?>, postDrawCurrentLines: MutableIntList, requiredFirstLn: MutableInt?) {
        val cur = cursor ?: return; val content = this.content ?: return
        val firstVis = editor.firstVisibleRow; val rowItr = editor.layout!!.obtainRowIterator(firstVis, preloadedLines)
        val spans = editor.styles?.spans; matchedPositions.clear(); highlightPositions.clear()
        val curLn = if (cur.isSelected()) -1 else cur.leftLine; val curLnBg = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
        val curRow = if (cur.isSelected()) -1 else editor.layout!!.getRowIndexForPosition(cur.left); val curRowBorder = editor.colorScheme.getColor(EditorColorScheme.CURRENT_ROW_BORDER)
        var lastPrepLn = -1; var leadingWSEnd = 0; var trailingWSStart = 0; var circleRadius = 0f
        val miniGraphW = if (editor.isWordwrap && (editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) this.miniGraphW else 0f
        val compPos = editor.inputConnection?.composingText?.let { t -> if (t.isComposing() && t.startIndex in 0 until content.length) content.getIndexer().getCharPosition(t.startIndex) else null }
        val compLen = (editor.inputConnection?.composingText?.endIndex ?: 0) - (editor.inputConnection?.composingText?.startIndex ?: 0)
        val draggingSel = editor.touchHandler?.draggingSelection
        if (editor.shouldInitializeNonPrintable()) circleRadius = min(editor.logicalRowHeight.toFloat(), paintGeneral.spaceWidth) * RenderingConstants.NON_PRINTABLE_CIRCLE_RADIUS_FACTOR
        if (Build.VERSION.SDK_INT >= 29 && !editor.isWordwrap && canvas.isHardwareAccelerated && editor.isHardwareAcceleratedDrawAllowed) editor.renderContext?.renderNodeHolder?.keepCurrentInDisplay(firstVis, editor.lastVisibleRow)
        val off2 = editor.offsetX - editor.measureTextRegionOffset()



        val trParams = createTextRowParams()
        val behavior = editor.props!!.cursorLineBgOverlapBehavior
        val isAnimating = editor.cursorAnimator.isRunning()
        if (isAnimating && editor.isHighlightCurrentLine && editor.isEditable && (behavior == CURSOR_LINE_BG_OVERLAP_CURSOR || behavior == CURSOR_LINE_BG_OVERLAP_MIXED)) drawAnimatedCurrentLineBackground(canvas, curLnBg)

        var r = firstVis
        while (r <= editor.lastVisibleRow && rowItr.hasNext()) {
            val rowInf = rowItr.next(); val ln = rowInf.lineIndex
            if (lastPrepLn != ln) { prepareLine(ln); lastPrepLn = ln }
            var drawCurLnBg = ln == curLn && !isAnimating && editor.isHighlightCurrentLine && editor.isEditable
            val drawCustomBg = !drawCurLnBg || (editor.props!!.drawCustomLineBgOnCurrentLine && behavior != CURSOR_LINE_BG_OVERLAP_CUSTOM)
            var overlapping = false
            if (drawCustomBg) getUserBackgroundForLine(ln)?.let { bg -> val col = bg.resolve(editor.colorScheme); if (ln == curLn) overlapping = true; drawRowBackground(canvas, col, r) }
            if (overlapping) drawCurLnBg = drawCurLnBg && (behavior != CURSOR_LINE_BG_OVERLAP_CURSOR)
            if (drawCurLnBg) { val commitBg = if (overlapping && behavior == CURSOR_LINE_BG_OVERLAP_MIXED) (curLnBg and 0x00FFFFFF) or -0x80000000 else curLnBg; drawRowBackground(canvas, commitBg, r); postDrawCurrentLines.add(r) }
            r++
        }
        if (isAnimating && editor.isHighlightCurrentLine && behavior == CURSOR_LINE_BG_OVERLAP_CUSTOM) drawAnimatedCurrentLineBackground(canvas, curLnBg)
        rowItr.reset(); r = firstVis
        while (r <= editor.lastVisibleRow && rowItr.hasNext()) {
            val rowInf = rowItr.next(); val ln = rowInf.lineIndex; val cols = getColumnCount(ln); canvas.save(); canvas.translate(rowInf.renderTranslateX, 0f)
            if (lastPrepLn != ln) { editor.computeMatchedPositions(ln, matchedPositions); editor.computeHighlightPositions(ln, highlightPositions); prepareLine(ln); lastPrepLn = ln }
            var pOff = -off2; if (!rowInf.isLeadingRow) pOff += miniGraphW
            if (matchedPositions.size > 0) { updateTextRow(sharedTextRow, r); repeat(matchedPositions.size) { val pos = matchedPositions.get(it); drawRowRegionBackground(canvas, r, sharedTextRow, IntPair.getFirst(pos), IntPair.getSecond(pos), rowInf.startColumn, rowInf.endColumn, editor.colorScheme.getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND), editor.colorScheme.getColor(EditorColorScheme.MATCHED_TEXT_BORDER)) } }
            if (highlightPositions.size > 0) {
                val rFinal = r; updateTextRow(sharedTextRow, r); highlightPositions.forEach(object : MutableLongLongMap.Consumer {
                    override fun accept(k: Long, v: Long): Any? { drawRowRegionBackground(canvas, rFinal, sharedTextRow, IntPair.getFirst(k), IntPair.getSecond(k), rowInf.startColumn, rowInf.endColumn, IntPair.getFirst(v), IntPair.getSecond(v)); return null }
                })
            }
            if (cur.isSelected() && ln in cur.leftLine..cur.rightLine) {
                val selStart = if (ln == cur.leftLine) cur.leftColumn else 0; val selEnd = if (ln == cur.rightLine) cur.rightColumn else cols
                if (cols == 0 && ln != cur.rightLine) {
                    tmpRect.top = (getRowTopForBackground(r) - editor.offsetY).toFloat(); tmpRect.bottom = (getRowBottomForBackground(r) - editor.offsetY).toFloat(); tmpRect.left = pOff; tmpRect.right = pOff + paintGeneral.spaceWidth * 2
                    drawRowBackgroundRectWithBorder(canvas, tmpRect, editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND), editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BORDER))
                } else if (selStart < selEnd) { updateTextRow(sharedTextRow, r); drawRowRegionBackground(canvas, r, sharedTextRow, selStart, selEnd, rowInf.startColumn, rowInf.endColumn, editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND), editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BORDER)) }
            }
            canvas.restore()
            if (r == curRow && curRowBorder != 0) {
                tmpRect.top = (editor.getRowTop(r) - editor.offsetY).toFloat(); tmpRect.bottom = (editor.getRowBottom(r) - editor.offsetY).toFloat(); tmpRect.left = max(0f, -off2); tmpRect.right = editor.width.toFloat()
                paintGeneral.color = curRowBorder; paintGeneral.style = AndroidPaint.Style.STROKE; paintGeneral.strokeWidth = editor.dpUnit; canvas.drawRect(tmpRect, paintGeneral); paintGeneral.style = AndroidPaint.Style.FILL
            }
            r++
        }
        rowItr.reset(); patchSnippetRegions(canvas, offset); drawHardwrapMarker(canvas, offset)
        var reader: Spans.Reader? = null; lastPrepLn = -1; var lnCache: TextAdvancesCache? = null; r = firstVis
        while (r <= editor.lastVisibleRow && rowItr.hasNext()) {
            val rowInf = rowItr.next(); val ln = rowInf.lineIndex; val cLine = getLine(ln); val cols = cLine.length
            if (r == firstVis && requiredFirstLn != null) requiredFirstLn.value = ln else if (rowInf.isLeadingRow) postDrawLineNumbers.add(IntPair.pack(ln, r))
            if (lastPrepLn != ln) {
                lastPrepLn = ln; lnCache = editor.renderContext!!.cache.queryMeasureCache(ln)?.let { if (it.updateTimestamp == timestamp && it.widths != null && it.widths!!.size > cols) it.widths else null }; prepareLine(ln)
                reader?.let { try { it.moveToLine(-1) } catch (e: Exception) { Log.w(LOG_TAG, "Failed to release SpanReader", e) } }
                reader = (spans?.read() ?: EmptyReader.INSTANCE).apply { try { moveToLine(ln) } catch (e: Exception) { Log.w(LOG_TAG, "Failed to read span", e) } }.let { if (it.getSpanCount() == 0) EmptyReader.INSTANCE else it }
                if (editor.shouldInitializeNonPrintable()) { val pos = editor.findLeadingAndTrailingWhitespacePos(lineBuf!!); leadingWSEnd = IntPair.getFirst(pos); trailingWSStart = IntPair.getSecond(pos) }
            }
            var pOff = -off2; var offCpy = off2; pOff += rowInf.renderTranslateX; offCpy -= rowInf.renderTranslateX; val flags = editor.nonPrintablePaintingFlags
            if (!rowInf.isLeadingRow && (flags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) { drawMiniGraph(canvas, offset, r, softwrapLeftGraph); pOff += miniGraphW; offCpy -= miniGraphW }
            val backOff = pOff
            if (!editor.isHardwareAcceleratedDrawAllowed || editor.touchHandler!!.isScaling || !canvas!!.isHardwareAccelerated || editor.isWordwrap || Build.VERSION.SDK_INT < 29 || (rowInf.endColumn - rowInf.startColumn > 128 && !editor.props!!.cacheRenderNodeForLongLines)) {
                sharedTextRow.set(lineBuf!!, rowInf.startColumn, rowInf.endColumn, reader!!.getSpansOnLine(ln), rowInf.inlayHints, getLineDirections(ln)!!, paintGeneral, lnCache, trParams); applySelectedTextRange(sharedTextRow, ln)
                canvas.save(); canvas.translate(-offCpy, (editor.getRowTop(r) - editor.offsetY).toFloat()); val result = sharedTextRow.draw(canvas, max(0f, offCpy), max(0f, offCpy) + editor.width); canvas.restore()
                val exhausted = IntPair.getFirst(result) == 1; pOff += IntPair.getSecondAsFloat(result)
                if (exhausted && rowInf.isTrailingRow && (flags and CodeEditor.FLAG_DRAW_LINE_SEPARATOR) != 0) drawMiniGraph(canvas, pOff, r, lineBreakGraph)
                else if (!rowInf.isTrailingRow && editor.isWordwrap && (flags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) drawMiniGraph(canvas, pOff, r, softwrapRightGraph)
            } else {
                pOff = editor.renderContext!!.renderNodeHolder?.drawLineHardwareAccelerated(canvas, ln, offset, (editor.getRowTop(r) - editor.offsetY).toFloat())?.toFloat() ?: 0f
                if (rowInf.isTrailingRow && (flags and CodeEditor.FLAG_DRAW_LINE_SEPARATOR) != 0) drawMiniGraph(canvas, pOff, r, lineBreakGraph)
            }
            pOff = backOff
            if (circleRadius != 0f && (leadingWSEnd != cols || (flags and CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE) != 0)) {
                sharedTextRow.set(lineBuf!!, rowInf.startColumn, rowInf.endColumn, reader!!.getSpansOnLine(ln), rowInf.inlayHints, getLineDirections(ln)!!, paintGeneral, lnCache, trParams)
                canvas!!.save(); val top = (editor.getRowTopOfText(r) - editor.offsetY).toFloat(); canvas.translate(pOff, top); bufferedDrawPoints.setOffsets(pOff, top); paintOther.color = editor.colorScheme.getColor(EditorColorScheme.NON_PRINTABLE_CHAR)
                sharedTextRow.iterateDrawTextRegions(rowInf.startColumn, rowInf.endColumn, canvas, max(0f, pOff), max(0f, pOff) + editor.width, false, object : TextRow.DrawTextConsumer {
                    override fun drawText(canvas: Canvas?, text: CharArray?, idx: Int, cnt: Int, cIdx: Int, cCnt: Int, rtl: Boolean, hOff: Float, w: Float, params: TextRowParams?, span: Span?) {
                        if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_LEADING) != 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, 0, leadingWSEnd)
                        if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_INNER) != 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, leadingWSEnd, trailingWSStart)
                        if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING) != 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, trailingWSStart, cols)
                        if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION) != 0 && cur.isSelected() && ln in cur.leftLine..cur.rightLine) {
                            val sStart = if (ln == cur.leftLine) cur.leftColumn else 0; val sEnd = if (ln == cur.rightLine) cur.rightColumn else cols
                            if ((flags and 14) == 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, sStart, sEnd)
                            else {
                                if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_LEADING) == 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, sStart, min(leadingWSEnd, sEnd))
                                if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_INNER) == 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, max(leadingWSEnd, sStart), min(trailingWSStart, sEnd))
                                if ((flags and CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING) == 0) drawWhitespaces(canvas!!, sharedTextRow, text!!, idx, cnt, cIdx, cCnt, rtl, hOff, w, max(trailingWSStart, sStart), sEnd)
                            }
                        }
                    }
                })
                canvas.restore(); bufferedDrawPoints.setOffsets(0f, 0f)
            }
            if (compPos != null && ln == compPos.line) {
                val cStart = compPos.column; val cEnd = cStart + compLen; val pStart = min(max(cStart, rowInf.startColumn), rowInf.endColumn); val pEnd = min(max(cEnd, rowInf.startColumn), rowInf.endColumn)
                if (pStart < pEnd) {
                    sharedTextRow.set(lineBuf!!, rowInf.startColumn, rowInf.endColumn, reader!!.getSpansOnLine(ln), rowInf.inlayHints, content!!.getLineDirections(ln), paintGeneral, lnCache, trParams)
                    tmpRect.top = (editor.getRowBottom(r) - editor.offsetY).toFloat(); tmpRect.bottom = tmpRect.top + editor.logicalRowHeight * 0.06f; val fOff = pOff
                    sharedTextRow.iterateBackgroundRegions(pStart, pEnd, false, false, object : TextRow.BackgroundRegionConsumer {
                        override fun handleRegion(left: Float, right: Float): Boolean {
                            tmpRect.left = fOff + left; tmpRect.right = fOff + right
                            if (tmpRect.right > 0f && tmpRect.left < editor.width) drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.UNDERLINE), tmpRect)
                            return tmpRect.right < editor.width
                        }
                    })
                }
            }
            val layout = editor.layout!!
            if (cur.isSelected()) {
                if (cur.leftLine == ln && isInside(cur.leftColumn, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    val cX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(cur.leftLine, cur.leftColumn)[1] - editor.offsetX
                    val type = if (content!!.isRtlAt(cur.leftLine, cur.leftColumn)) SelectionHandleStyle.HANDLE_TYPE_RIGHT else SelectionHandleStyle.HANDLE_TYPE_LEFT
                    postDrawCursor.add(DrawCursorTask(cX, (getRowBottomForBackground(r) - editor.offsetY).toFloat(), type, editor.handleDescLeft!!).also { applyBidiIndicatorAttrs(it, cur.leftLine, cur.leftColumn) })
                }
                if (cur.rightLine == ln && isInside(cur.rightColumn, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    val cX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(cur.rightLine, cur.rightColumn)[1] - editor.offsetX
                    val type = if (content!!.isRtlAt(cur.rightLine, cur.rightColumn)) SelectionHandleStyle.HANDLE_TYPE_LEFT else SelectionHandleStyle.HANDLE_TYPE_RIGHT
                    postDrawCursor.add(DrawCursorTask(cX, (getRowBottomForBackground(r) - editor.offsetY).toFloat(), type, editor.handleDescRight!!).also { applyBidiIndicatorAttrs(it, cur.rightLine, cur.rightColumn) })
                }
            } else if (cur.leftLine == ln && isInside(cur.leftColumn, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                val cX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(cur.leftLine, cur.leftColumn)[1] - editor.offsetX
                postDrawCursor.add(DrawCursorTask(cX, (getRowBottomForBackground(r) - editor.offsetY).toFloat(), SelectionHandleStyle.HANDLE_TYPE_INSERT, editor.handleDescInsert!!).also { applyBidiIndicatorAttrs(it, cur.leftLine, cur.leftColumn) })
            }
            val draggingSelection = editor.touchHandler!!.draggingSelection
            if (draggingSelection != null) {
                if (draggingSelection.line == ln && isInside(draggingSelection.column, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    val cX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(draggingSelection.line, draggingSelection.column)[1] - editor.offsetX
                    postDrawCursor.add(DrawCursorTask(cX, (getRowBottomForBackground(r) - editor.offsetY).toFloat(), SelectionHandleStyle.HANDLE_TYPE_UNDEFINED, null).also { applyBidiIndicatorAttrs(it, draggingSelection.line, draggingSelection.column) })
                }
            } else if (editor.isInMouseMode && editor.isTextSelected) {
                editor.selectingTarget?.let { target ->
                    if (target.line == ln && isInside(target.column, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                        val cX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(target.line, target.column)[1] - editor.offsetX
                        postDrawCursor.add(DrawCursorTask(cX, (getRowBottomForBackground(r) - editor.offsetY).toFloat(), SelectionHandleStyle.HANDLE_TYPE_UNDEFINED, null).also { applyBidiIndicatorAttrs(it, target.line, target.column) })
                    }
                }
            }
            r++
        }
        reader?.let { try { it.moveToLine(-1) } catch (e: Exception) { Log.w(LOG_TAG, "Failed to release SpanReader", e) } }
        paintGeneral.isFakeBoldText = false; paintGeneral.textSkewX = 0f; paintOther.strokeWidth = circleRadius * 2; bufferedDrawPoints.commitPoints(canvas, paintOther)
    }

    private fun getBidiIndicatorAttrs(line: Int, column: Int): Long {
        val dirs = getLineDirections(line); if (dirs.runCount == 1) return IntPair.pack(0, if (dirs.isRunRtl(0)) 1 else 0)
        repeat(dirs.runCount) { if (it + 1 == dirs.runCount || (dirs.getRunStart(it) <= column && column < dirs.getRunEnd(it))) return IntPair.pack(if (editor.props!!.showBidiDirectionIndicator) 1 else 0, if (dirs.isRunRtl(it)) 1 else 0) }
        return IntPair.pack(0, 0)
    }

    private fun applyBidiIndicatorAttrs(task: DrawCursorTask, line: Int, column: Int) { val attrs = getBidiIndicatorAttrs(line, column); task.isBidiIndicatorRequired = IntPair.getFirst(attrs) == 1; task.isRightToLeft = IntPair.getSecond(attrs) == 1 }

    private fun drawBidiSelectionIndicator(canvas: Canvas, x: Float, topY: Float, selectionHeight: Float, isRtl: Boolean) {
        val h = selectionHeight * 0.2f; val dX = h * 0.866f; tmpPath.reset(); tmpPath.moveTo(x, topY); tmpPath.lineTo(x + (if (isRtl) -dX else dX), topY + h / 2f); tmpPath.lineTo(x, topY + h); tmpPath.close(); canvas.drawPath(tmpPath, paintGeneral)
    }

    protected fun drawDiagnosticIndicator(canvas: Canvas, style: DiagnosticIndicatorStyle, i: Int, startX: Float, endX: Float) {
        val waveLength = editor.dpUnit * editor.props!!.indicatorWaveLength; val amplitude = editor.dpUnit * editor.props!!.indicatorWaveAmplitude; val waveWidth = editor.dpUnit * editor.props!!.indicatorWaveWidth; val centerY = (editor.getRowBottom(i) - editor.offsetY).toFloat()
        when (style) {
            DiagnosticIndicatorStyle.NONE -> {}
            DiagnosticIndicatorStyle.WAVY_LINE -> {
                val lineWidth = endX - startX; canvas.save(); canvas.clipRect(startX, 0f, endX, canvas.height.toFloat()); canvas.translate(startX, centerY); tmpPath.reset(); tmpPath.moveTo(0f, 0f)
                val waveCount = ceil(((if (startX < 0) 0f else (waveLength * ceil((startX / waveLength).toDouble()).toInt() - startX) + lineWidth) / waveLength).toDouble()).toInt()
                repeat(waveCount) { tmpPath.quadTo(waveLength * it + waveLength / 4, amplitude, waveLength * it + waveLength / 2, 0f); tmpPath.quadTo(waveLength * it + waveLength * 3 / 4, -amplitude, waveLength * it + waveLength, 0f) }
                paintOther.strokeWidth = waveWidth; paintOther.style = AndroidPaint.Style.STROKE; canvas.drawPath(tmpPath, paintOther); canvas.restore(); paintOther.style = AndroidPaint.Style.FILL
            }
            DiagnosticIndicatorStyle.LINE -> { paintOther.strokeWidth = waveWidth; canvas.drawLine(startX, centerY, endX, centerY, paintOther) }
            DiagnosticIndicatorStyle.DOUBLE_LINE -> { paintOther.strokeWidth = waveWidth / 3f; canvas.drawLine(startX, centerY, endX, centerY, paintOther); canvas.drawLine(startX, centerY - waveWidth, endX, centerY - waveWidth, paintOther) }
            else -> {}
        }
    }

    protected fun drawDiagnosticIndicators(canvas: Canvas, offset: Float) {
        val container = editor.diagnostics; val style = editor.diagnosticIndicatorStyle; if (container == null || style == null || style == DiagnosticIndicatorStyle.NONE) return
        val text = content!!; val firstVisRow = editor.firstVisibleRow; val lastVisRow = editor.lastVisibleRow; val firstIdx = text.getCharIndex(editor.firstVisibleLine, 0)
        val lastLn = min(text.lineCount - 1, editor.lastVisibleLine + 1); val lastIdx = text.getCharIndex(lastLn, 0) + text.getColumnCount(lastLn); container.queryInRegion(collectedDiagnostics, firstIdx, lastIdx)
        if (collectedDiagnostics.isEmpty()) return
        val start = CharPosition(); val end = CharPosition(); val localCur = cursor ?: return; val indexer = localCur.getIndexer()
        for (region in collectedDiagnostics) {
            val sIdx = max(firstIdx, region.startIndex); val eIdx = min(lastIdx, region.endIndex); indexer.getCharPosition(sIdx, start); indexer.getCharPosition(eIdx, end)
            val sRow = editor.layout!!.getRowIndexForPosition(sIdx); val eRow = editor.layout!!.getRowIndexForPosition(eIdx); val severity = region.severity.toInt(); val colorId = if (severity in 0..3) sDiagnosticsColorMapping[severity] else 0
            if (colorId == 0) continue
            paintOther.color = editor.colorScheme.getColor(colorId)
            for (i in max(firstVisRow, sRow)..min(lastVisRow, eRow)) {
                val row = editor.layout!!.getRowAt(i); updateTextRow(sharedTextRow, i); val sCol = if (i == sRow) start.column else row.startColumn; val eCol = if (i == eRow) end.column else row.endColumn
                val fOff = offset + row.renderTranslateX + if (editor.isWordwrap && !row.isLeadingRow && (editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) miniGraphW else 0f
                if (sCol == eCol) { val sX = fOff + sharedTextRow.getCursorOffsetForIndex(sCol); drawDiagnosticIndicator(canvas, style, i, sX, sX + paintGeneral.measureText("a")) }
                else { val rIdx = i; sharedTextRow.iterateBackgroundRegions(sCol, eCol, false, false, object : TextRow.BackgroundRegionConsumer { override fun handleRegion(left: Float, right: Float): Boolean { if (right > 0f) drawDiagnosticIndicator(canvas, style, rIdx, fOff + left, fOff + right); return fOff + right < editor.width } }) }
            }
        }
        collectedDiagnostics.clear()
    }

    private fun drawWhitespaces(canvas: Canvas, tr: TextRow, chars: CharArray, idx: Int, cnt: Int, cIdx: Int, cCnt: Int, rtl: Boolean, hOff: Float, w: Float, minV: Int, maxV: Int) {
        val pStart = max(idx, min(idx + cnt, minV)); val pEnd = max(idx, min(idx + cnt, maxV)); if (pStart >= pEnd) return
        val spW = paintGeneral.spaceWidth; val rowC = editor.logicalRowHeight / 2f + editor.getRowTopOfText(0); var off = if (rtl) hOff + w else hOff; var curr = pStart
        while (curr < pEnd) {
            val ch = chars[curr]; var pCnt = 0; var pLn = false
            if (ch == ' ' || ch == '\t') { val adv = tr.measureAdvanceInRun(curr, idx, curr, cIdx, cIdx + cCnt, rtl); off = if (rtl) hOff + w - adv else hOff + adv }
            if (ch == ' ') pCnt = 1 else if (ch == '\t') if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE) != 0) pCnt = editor.tabWidth else pLn = true
            repeat(pCnt) { val startO = off + spW * it; val cO = if (rtl) (startO + startO + spW) / 2f - spW else (startO + startO + spW) / 2f; bufferedDrawPoints.drawPoint(cO, rowC) }
            if (pLn) { val cW = editor.tabWidth * spW; val d = cW * 0.05f; val rD = if (rtl) -cW else 0f; canvas.drawLine(off + d + rD, rowC, off + cW + rD - d, rowC, paintOther) }
            if (ch == ' ' || ch == '\t') off += if (rtl) -(if (ch == ' ') spW else spW * editor.tabWidth) else (if (ch == ' ') spW else spW * editor.tabWidth)
            curr++
        }
    }

    val miniGraphW: Float get() = editor.props!!.let { props -> editor.context.getDrawable(R.drawable.line_break)?.let { g -> val h = editor.logicalRowHeight * props.miniMarkerSizeFactor; val w = g.intrinsicWidth; val ih = g.intrinsicHeight; if (w <= 0 || ih <= 0 || h <= 0) 0f else h * (w.toFloat() / ih) } ?: 0f }

    protected fun drawMiniGraph(canvas: Canvas?, off: Float, row: Int, graph: Drawable?) {
        if (canvas == null || graph == null) return
        val gBottom = (if (row == -1) editor.getRowBottomOfText(0) else (editor.getRowBottomOfText(row) - editor.offsetY)).toFloat()
        val h = editor.logicalRowHeight * editor.props!!.miniMarkerSizeFactor; val w = graph.intrinsicWidth; val ih = graph.intrinsicHeight; if (h <= 0 || w <= 0 || ih <= 0) return
        val wd = h * (w.toFloat() / ih); graph.setColorFilter(editor.colorScheme.getColor(EditorColorScheme.NON_PRINTABLE_CHAR), PorterDuff.Mode.SRC_ATOP)
        graph.setBounds(off.toInt(), (gBottom - h).toInt(), (off + wd).toInt(), gBottom.toInt()); graph.draw(canvas)
    }

    protected fun getRowTopForBackground(row: Int) = if (!editor.props!!.textBackgroundWrapTextOnly) editor.getRowTop(row) else editor.getRowTopOfText(row)
    protected fun getRowBottomForBackground(row: Int) = if (!editor.props!!.textBackgroundWrapTextOnly) editor.getRowBottom(row) else editor.getRowBottomOfText(row)

    protected fun drawRowRegionBackground(canvas: Canvas, row: Int, tr: TextRow?, hStart: Int, hEnd: Int, rStart: Int, rEnd: Int, color: Int, borderColor: Int) {
        val s = max(hStart, rStart); val e = min(hEnd, rEnd); if (s >= e) return
        tmpRect.top = (getRowTopForBackground(row) - editor.offsetY).toFloat(); tmpRect.bottom = (getRowBottomForBackground(row) - editor.offsetY).toFloat()
        var off = editor.measureTextRegionOffset() - editor.offsetX.toFloat(); if (editor.isWordwrap && !editor.layout!!.getRowAt(row).isLeadingRow && (editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) off += miniGraphW
        val fOff = off; val textR = tr ?: createTextRow(row); val width = editor.width.toFloat()
        textR.iterateBackgroundRegions(s, e, false, false, object : TextRow.BackgroundRegionConsumer { override fun handleRegion(left: Float, right: Float): Boolean { tmpRect.left = fOff + left; tmpRect.right = fOff + right; if (tmpRect.right < 0 || tmpRect.left > width) return false; drawRowBackgroundRectWithBorder(canvas, tmpRect, color, borderColor); return true } })
    }

    protected fun drawRowBackgroundRectWithBorder(canvas: Canvas, rect: RectF?, bgColor: Int, borderColor: Int) {
        paintGeneral.color = bgColor; drawRowBackgroundRect(canvas, rect)
        if (borderColor != 0) { paintGeneral.color = borderColor; paintGeneral.style = AndroidPaint.Style.STROKE; paintGeneral.strokeWidth = editor.getTextBorderWidth(); drawRowBackgroundRect(canvas, rect); paintGeneral.style = AndroidPaint.Style.FILL }
    }

    protected fun drawRowBackgroundRect(canvas: Canvas, rect: RectF?) = drawRowBackgroundRect(canvas, rect, paintGeneral)
    protected fun drawRowBackgroundRect(canvas: Canvas?, rect: RectF?, p: Paint?) {
        if (rect == null || p == null || canvas == null) return
        if (editor.props!!.enableRoundTextBackground) { val r = editor.logicalRowHeight * editor.props!!.roundTextBackgroundFactor; canvas.drawRoundRect(rect, r, r, p) }
        else canvas.drawRect(rect, p)
    }

    private fun isInside(idx: Int, start: Int, end: Int, isLast: Boolean) = if (idx == end && !isLast) false else idx in start..end
    val lineNumberMetrics: android.graphics.Paint.FontMetricsInt get() = metricsLineNumber
    val textMetrics: android.graphics.Paint.FontMetricsInt? get() = metricsText

    protected fun drawEdgeEffect(canvas: Canvas) {
        var postDraw = false; val vEff = editor.verticalEdgeEffect!!; val hEff = editor.horizontalEdgeEffect!!
        if (!vEff.isFinished) {
            val bottom = editor.touchHandler!!.glowTopOrBottom; if (bottom) { canvas.save(); canvas.translate(-editor.measuredWidth.toFloat(), editor.measuredHeight.toFloat()); canvas.rotate(180f, editor.measuredWidth.toFloat(), 0f) }
            postDraw = vEff.draw(canvas); if (bottom) canvas.restore()
        }
        if (editor.isWordwrap) hEff.finish()
        if (!hEff.isFinished) {
            canvas.save(); val right = editor.touchHandler!!.glowLeftOrRight
            if (right) { canvas.rotate(90f); canvas.translate(0f, -editor.measuredWidth.toFloat()) } else { canvas.translate(0f, editor.measuredHeight.toFloat()); canvas.rotate(-90f) }
            postDraw = hEff.draw(canvas) || postDraw; canvas.restore()
        }
        val scroller = editor.scroller
        if (scroller.isOverScrolled()) {
            if (vEff.isFinished && (scroller.getCurrY() < 0 || scroller.getCurrY() > editor.scrollMaxY)) { editor.eventHandler!!.glowTopOrBottom = scroller.getCurrY() >= editor.scrollMaxY; vEff.onAbsorb(scroller.getCurrVelocity().toInt()); postDraw = true }
            if (hEff.isFinished && (scroller.getCurrX() < 0 || scroller.getCurrX() > editor.scrollMaxX)) { editor.eventHandler!!.glowLeftOrRight = scroller.getCurrX() >= editor.scrollMaxX; hEff.onAbsorb(scroller.getCurrVelocity().toInt()); postDraw = true }
        }
        if (postDraw) editor.postInvalidate()
    }


    protected fun drawBlockLines(canvas: Canvas?, offsetX: Float) {
        if (canvas == null) {
            return
        }
        val styles = editor.styles
        val blocks: List<CodeBlock?>? = if (styles == null) null else styles.blocks
        val indentMode = styles != null && styles.isIndentCountMode()
        if (blocks == null || blocks.isEmpty()) {
            return
        }
        val firstLine: Int = editor.firstVisibleLine
        val lastLine: Int = editor.lastVisibleLine
        var mark = false
        var invalidCount = 0
        val maxCount: Int = styles!!.getSuppressSwitch()
        var mm: Int = editor.binarySearchEndBlock(firstLine, blocks as List<CodeBlock>)
        if (mm == -1) {
            mm = 0
        }
        val cursorIdx: Int = editor.blockIndex
        for (curr in mm until blocks.size) {
            val block: CodeBlock? = blocks[curr]
            if (block == null) {
                continue
            }
            if (io.github.abc15018045126.sora.widget.CodeEditor.hasVisibleRegion(block.startLine, block.endLine, firstLine, lastLine)) {
                try {
                    var lineContent: ContentLine = getLine(block.endLine)
                    val offsetEnd: Float =
                        if (indentMode) paintGeneral.spaceWidth * block.endColumn else createTextRow(block.endLine).getCursorOffsetForIndex(
                            Math.min(block.endColumn, lineContent.length)
                        )
                    lineContent = getLine(block.startLine)
                    val offsetStart: Float =
                        if (indentMode) paintGeneral.spaceWidth * block.startColumn else createTextRow(block.startLine).getCursorOffsetForIndex(
                            Math.min(block.startColumn, lineContent.length)
                        )
                    val offset: Float = min(offsetEnd, offsetStart)
                    val centerX = offset + offsetX
                    tmpRect.top = max(0f, (editor.getRowBottom(block.startLine) - editor.offsetY).toFloat())
                    tmpRect.bottom = min(
                        editor.height.toFloat(),
                        ((if (block.toBottomOfEndLine) editor.getRowBottom(block.endLine) else editor.getRowTop(block.endLine)) - editor.offsetY).toFloat()
                    )
                    tmpRect.left = centerX - editor.dpUnit * editor.getBlockLineWidth() / 2
                    tmpRect.right = centerX + editor.dpUnit * editor.getBlockLineWidth() / 2
                    drawColor(
                        canvas,
                        editor.colorScheme
                            .getColor(if (curr == cursorIdx) EditorColorScheme.BLOCK_LINE_CURRENT else EditorColorScheme.BLOCK_LINE),
                        tmpRect
                    )
                } catch (e: IndexOutOfBoundsException) {


                }
                mark = true
            } else if (mark) {
                if (invalidCount >= maxCount) break
                invalidCount++
            }
        }
    }

    protected fun drawSideBlockLine(canvas: Canvas) {
        if (!editor.props!!.drawSideBlockLine) {
            return
        }
        val styles = editor.styles
        val blocks: List<CodeBlock?>? = styles?.blocks
        if (blocks == null || blocks.isEmpty()) {
            return
        }
        val current =
            editor.blockIndex
        if (current >= 0 && current < blocks.size) {
            val block =
                blocks[current]
            if (block != null) {
                val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
                try {
                    val top: Float = layout.getCharLayoutOffset(
                        block.startLine,
                        block.startColumn
                    )[0] - editor.logicalRowHeight - editor.offsetY
                    val bottom: Float = layout.getCharLayoutOffset(block.endLine, block.endColumn)[0] - editor.offsetY
                    val left: Float = editor.measureLineNumber()
                    val right: Float = left + editor.dividerMarginLeft
                    val center: Float = (left + right) / 2 - editor.offsetX
                    paintGeneral.setColor(editor.colorScheme.getColor(EditorColorScheme.SIDE_BLOCK_LINE))
                    paintGeneral.setStrokeWidth(editor.dpUnit * editor.getBlockLineWidth())
                    canvas.drawLine(center, top, center, bottom, paintGeneral)
                } catch (e: IndexOutOfBoundsException) {

                }
            }
        }
    }


    protected fun drawScrollBars(canvas: Canvas) {
        verticalScrollBarRect.setEmpty()
        horizontalScrollBarRect.setEmpty()
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (!handler.shouldDrawScrollBarForTouch() && !(editor.isInMouseMode && editor.props!!.mouseModeAlwaysShowScrollbars)) {
            return
        }
        var percentage =
            handler.getScrollBarFadeOutPercentageForTouch()
        if (editor.isInMouseMode && editor.props!!.mouseModeAlwaysShowScrollbars) {
            percentage = 0f
        }
        val size =
            editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        if (editor.isHorizontalScrollBarEnabled() && !editor.isWordwrap && editor.scrollMaxX > editor.width * 3 / 4) {
            canvas.save()
            canvas.translate(0f, size * percentage)

            drawScrollBarTrackHorizontal(canvas)
            drawScrollBarHorizontal(canvas)

            canvas.restore()
        }
        if (editor.isVerticalScrollBarEnabled() && editor.scrollMaxY > editor.height / 2) {
            canvas.save()
            canvas.translate(size * percentage, 0f)

            drawScrollBarTrackVertical(canvas)
            drawScrollBarVertical(canvas)

            canvas.restore()
        }
    }


    protected fun drawScrollBarTrackVertical(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (handler.holdVerticalScrollBar()) {
            tmpRect.right = editor.width.toFloat()
            tmpRect.left = editor.width - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
            tmpRect.top = 0f
            tmpRect.bottom = editor.height.toFloat()
            val track = verticalScrollbarTrackDrawable
            if (track != null) {
                track.setBounds(
                    tmpRect.left.toInt(),
                    tmpRect.top.toInt(),
                    tmpRect.right.toInt(),
                    tmpRect.bottom.toInt()
                )
                track.draw(canvas)
            } else {
                drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_TRACK), tmpRect)
            }
        }
    }


    protected fun drawScrollBarVertical(canvas: Canvas) {
        val height: Int = editor.height
        val all: Float = (editor.scrollMaxY + height).toFloat()
        val length: Float =
            max(height / all * height, editor.dpUnit * RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP)
        val topY: Float = editor.offsetY * 1.0f / editor.scrollMaxY * (height - length)
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (handler.holdVerticalScrollBar()) {
            drawLineInfoPanel(canvas, topY, length)
        }
        tmpRect.right = editor.width.toFloat()
        tmpRect.left = editor.width - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        tmpRect.top = topY
        tmpRect.bottom = topY + length
        verticalScrollBarRect.set(tmpRect)
        val thumb = verticalScrollbarThumbDrawable
        if (thumb != null) {
            thumb.setState(
                if (handler
                        .holdVerticalScrollBar()
                ) PRESSED_DRAWABLE_STATE else DEFAULT_DRAWABLE_STATE
            )
            thumb.setBounds(
                tmpRect.left.toInt(),
                tmpRect.top.toInt(),
                tmpRect.right.toInt(),
                tmpRect.bottom.toInt()
            )
            thumb.draw(canvas)
        } else {
            drawColor(
                canvas,
                editor.colorScheme.getColor(
                    if (handler
                            .holdVerticalScrollBar()
                    ) EditorColorScheme.SCROLL_BAR_THUMB_PRESSED else EditorColorScheme.SCROLL_BAR_THUMB
                ),
                tmpRect
            )
        }
    }


    protected fun drawLineInfoPanel(canvas: Canvas, topY: Float, length: Float) {
        if (!editor.isDisplayLnPanel) return
        val mode = editor.lnPanelPositionMode; val position = editor.lnPanelPosition; val text = editor.lineNumberTipTextProvider!!.getCurrentText(editor)
        val backupSize = paintGeneral.textSize; paintGeneral.textSize = editor.lineInfoTextSize; val backupMetrics = metricsText; metricsText = paintGeneral.fontMetricsInt
        val expand = editor.dpUnit * 8; val textWidth = paintGeneral.measureText(text); var baseline = 0f; var textOffset = 0f
        if (mode == LineInfoPanelPositionMode.FIXED) {
            tmpRect.top = editor.height / 2f - editor.logicalRowHeight / 2f - expand; tmpRect.bottom = editor.height / 2f + editor.logicalRowHeight / 2f + expand
            tmpRect.left = editor.width / 2f - textWidth / 2f - expand; tmpRect.right = editor.width / 2f + textWidth / 2f + expand; baseline = editor.height / 2f + 2 * expand
            val offset = 10 * editor.dpUnit
            if (position != LineInfoPanelPosition.CENTER) {
                if ((position and LineInfoPanelPosition.TOP) != 0) { tmpRect.top = offset; tmpRect.bottom = offset + editor.logicalRowHeight + 2 * expand; baseline = offset + editor.getRowBaseline(0) + expand }
                if ((position and LineInfoPanelPosition.BOTTOM) != 0) { tmpRect.top = editor.height - offset - 2 * expand - editor.logicalRowHeight; tmpRect.bottom = editor.height - offset; baseline = editor.height - editor.logicalRowHeight + editor.getRowBaseline(0) - offset - expand }
                if ((position and LineInfoPanelPosition.LEFT) != 0) { tmpRect.left = offset; tmpRect.right = offset + 2 * expand + textWidth }
                if ((position and LineInfoPanelPosition.RIGHT) != 0) { tmpRect.right = editor.width - offset; tmpRect.left = editor.width - offset - expand * 2 - textWidth }
            }
            drawColorRound(canvas, editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL), tmpRect)
        } else {
            var radii: FloatArray? = null; tmpRect.right = editor.width - 30 * editor.dpUnit; tmpRect.left = editor.width - 30 * editor.dpUnit - expand * 2 - textWidth
            if (position == LineInfoPanelPosition.TOP) {
                tmpRect.top = topY; tmpRect.bottom = topY + editor.logicalRowHeight + 2 * expand; baseline = topY + editor.getRowBaseline(0) + expand; radii = FloatArray(8).apply { val r = tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR; repeat(8) { if (it != 5) this[it] = r } }
            } else if (position == LineInfoPanelPosition.BOTTOM) {
                tmpRect.top = topY + length - editor.logicalRowHeight - 2 * expand; tmpRect.bottom = topY + length; baseline = topY + length - editor.getRowBaseline(0) / 2f; radii = FloatArray(8).apply { val r = tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR; repeat(8) { if (it != 3) this[it] = r } }
            } else {
                val centerY = topY + length / 2f; tmpRect.top = centerY - editor.logicalRowHeight / 2f - expand; tmpRect.bottom = centerY + editor.logicalRowHeight / 2f + expand; baseline = centerY - editor.logicalRowHeight / 2f + editor.getRowBaseline(0)
            }
            if (radii != null) { tmpPath.reset(); tmpPath.addRoundRect(tmpRect, radii, Path.Direction.CW) }
            else { tmpRect.offset(-expand, 0f); tmpRect.right += expand; textOffset = -expand / 2f; BubbleHelper.buildBubblePath(tmpPath, tmpRect) }
            paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL); canvas.drawPath(tmpPath, paintGeneral)
        }
        val centerX = (tmpRect.left + tmpRect.right) / 2 + textOffset; paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT); paintGeneral.textAlign = AndroidPaint.Align.CENTER
        if (text != null) canvas.drawText(text, centerX, baseline, paintGeneral)
        paintGeneral.textAlign = AndroidPaint.Align.LEFT; paintGeneral.textSize = backupSize; metricsText = backupMetrics
    }

    protected fun drawScrollBarTrackHorizontal(canvas: Canvas?) {
        if (canvas == null) return
        val handler = editor.touchHandler!!
        if (handler.holdHorizontalScrollBar()) {
            tmpRect.set(0f, editor.height - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP, editor.width.toFloat(), editor.height.toFloat())
            horizontalScrollbarTrackDrawable?.let { it.bounds = Rect(tmpRect.left.toInt(), tmpRect.top.toInt(), tmpRect.right.toInt(), tmpRect.bottom.toInt()); it.draw(canvas) }
            ?: drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_TRACK), tmpRect)
        }
    }

    protected fun patchSnippetRegions(canvas: Canvas, textOffset: Float) {
        val controller = editor.snippetController!!; if (!controller.isInSnippet()) return
        controller.getEditingTabStop()?.let { patchTextRegionWithColor(canvas, textOffset, it.startIndex, it.endIndex, 0, editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING), 0) }
        controller.getEditingRelatedTabStops().forEach { patchTextRegionWithColor(canvas, textOffset, it.startIndex, it.endIndex, 0, editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED), 0) }
        controller.getInactiveTabStops().forEach { patchTextRegionWithColor(canvas, textOffset, it.startIndex, it.endIndex, 0, editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE), 0) }
    }

    protected fun patchHighlightedDelimiters(canvas: Canvas, textOffset: Float) {
        if (true) return
        val paired = object { val leftIndex = 0; val leftLength = 0; val rightIndex = 0; val rightLength = 0 }
        val color = editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND); var backgroundColor = editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND)
        val underlineColor = editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE); val borderColor = editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER)
        val borderWidth = editor.getTextBorderWidth(); if (isInvalidTextBounds(paired.leftIndex, paired.leftLength) || isInvalidTextBounds(paired.rightIndex, paired.rightLength)) return
        val continuous = paired.leftIndex + paired.leftLength == paired.rightIndex
        if (color != 0 || underlineColor != 0) {
            if (continuous) patchTextRegionWithColor(canvas, textOffset, paired.leftIndex, paired.rightIndex + paired.rightLength, color, backgroundColor, underlineColor)
            else { patchTextRegionWithColor(canvas, textOffset, paired.leftIndex, paired.leftIndex + paired.leftLength, color, backgroundColor, underlineColor); patchTextRegionWithColor(canvas, textOffset, paired.rightIndex, paired.rightIndex + paired.rightLength, color, backgroundColor, underlineColor) }
            backgroundColor = 0
        }
        if (backgroundColor != 0 || (borderColor != 0 && borderWidth > 0)) {
            if (continuous) patchTextBackgroundRegions(canvas, textOffset, paired.leftIndex, paired.rightIndex + paired.rightLength, backgroundColor, borderWidth, borderColor)
            else { patchTextBackgroundRegions(canvas, textOffset, paired.leftIndex, paired.leftIndex + paired.leftLength, backgroundColor, borderWidth, borderColor); patchTextBackgroundRegions(canvas, textOffset, paired.rightIndex, paired.rightIndex + paired.rightLength, backgroundColor, borderWidth, borderColor) }
        }
    }

    protected fun isInvalidTextBounds(index: Int, length: Int) = index < 0 || length < 0 || index + length > (content?.length ?: 0)

    protected fun patchTextRegionWithColor(canvas: Canvas, textOffset: Float, start: Int, end: Int, color: Int, backgroundColor: Int, underlineColor: Int) {
        paintGeneral.color = color; paintOther.strokeWidth = editor.logicalRowHeight * RenderingConstants.MATCHING_DELIMITERS_UNDERLINE_WIDTH_FACTOR
        val useBold = editor.props!!.boldMatchingDelimiters; paintGeneral.style = if (useBold) AndroidPaint.Style.FILL_AND_STROKE else AndroidPaint.Style.FILL; paintGeneral.isFakeBoldText = useBold
        patchTextRegions(canvas, textOffset, start, end, object : TextRow.DrawTextConsumer {
            override fun drawText(canvasLocal: Canvas?, text: CharArray?, index: Int, count: Int, contextIndex: Int, contextCount: Int, isRtl: Boolean, hOff: Float, width: Float, params: TextRowParams?, span: Span?) {
                if (span == null) return
                if (backgroundColor != 0) { tmpRect.top = getRowTopForBackground(0).toFloat(); tmpRect.bottom = getRowBottomForBackground(0).toFloat(); tmpRect.left = hOff; tmpRect.right = hOff + width; paintOther.color = backgroundColor; drawRowBackgroundRect(canvas, tmpRect, paintOther) }
                if (color != 0) { paintGeneral.textSkewX = if (TextStyle.isItalics(span.style)) RenderingConstants.TEXT_SKEW_X else 0f; paintGeneral.isStrikeThruText = TextStyle.isStrikeThrough(span.style); GraphicsCompat.drawTextRun(canvas, text!!, index, count, contextIndex, contextCount, hOff, params!!.textBaseline.toFloat(), isRtl, paintGeneral) }
                if (underlineColor != 0) { paintOther.color = underlineColor; val b = params!!.textBottom - params.textHeight * 0.05f; canvas.drawLine(hOff, b, hOff + width, b, paintOther) }
            }
        }, null)
        paintGeneral.style = AndroidPaint.Style.FILL; paintGeneral.isFakeBoldText = false; paintGeneral.textSkewX = 0f; paintGeneral.isStrikeThruText = false
    }

    protected fun patchTextBackgroundRegions(canvas: Canvas, textOffset: Float, start: Int, end: Int, backgroundColor: Int, borderWidth: Float, borderColor: Int) {
        if (backgroundColor == 0 && (borderWidth <= 0 || borderColor == 0)) return
        patchTextRegions(canvas, textOffset, start, end, null, object : TextRow.BackgroundRegionConsumer {
            override fun handleRegion(left: Float, right: Float): Boolean {
                if (textOffset + left < 0) return true
                tmpRect.top = getRowTopForBackground(0).toFloat(); tmpRect.bottom = getRowBottomForBackground(0).toFloat(); tmpRect.left = left; tmpRect.right = right
                if (backgroundColor != 0) { paintOther.color = backgroundColor; drawRowBackgroundRect(canvas, tmpRect, paintOther) }
                if (borderWidth > 0 && borderColor != 0) { paintOther.style = AndroidPaint.Style.STROKE; paintOther.color = borderColor; paintOther.strokeWidth = borderWidth; drawRowBackgroundRect(canvas, tmpRect, paintOther); paintOther.style = AndroidPaint.Style.FILL }
                return textOffset + right > editor.width
            }
        })
    }

    protected fun patchTextRegions(canvas: Canvas, textOffset: Float, start: Int, end: Int, patch: TextRow.DrawTextConsumer?, bgPatch: TextRow.BackgroundRegionConsumer?) {
        if (patch == null && bgPatch == null) return
        val firstVisRow = editor.firstVisibleRow; val lastVisRow = editor.lastVisibleRow; val layout = editor.layout!!; val startRow = layout.getRowIndexForPosition(start); val endRow = layout.getRowIndexForPosition(end)
        val cur = cursor ?: return; val posStart = cur.getIndexer().getCharPosition(start); val posEnd = cur.getIndexer().getCharPosition(end); val itr = layout.obtainRowIterator(startRow, preloadedLines as SparseArray<ContentLine>); var i = startRow
        while (i <= endRow && itr.hasNext()) {
            val row = itr.next(); if (i !in firstVisRow..lastVisRow) { i++; continue }
            val sOnRow = if (i == startRow) posStart.column else row.startColumn; val eOnRow = if (i == endRow) posEnd.column else row.endColumn
            var hOff = textOffset; if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0 && !row.isLeadingRow) hOff += miniGraphW
            val minH = max(0f, -hOff); val maxH = minH + editor.width; canvas.save(); canvas.translate(hOff + row.renderTranslateX, (editor.getRowTop(i) - editor.offsetY).toFloat())
            if (bgPatch != null) createTextRow(i).iterateBackgroundRegions(sOnRow, eOnRow, false, false, bgPatch)
            if (patch != null) createTextRow(i).iterateDrawTextRegions(sOnRow, eOnRow, canvas, minH, maxH, true, patch); canvas.restore(); i++
        }
    }

    protected fun drawSelectionOnAnimation(canvas: Canvas) {
        if (!editor.isEditable) return
        tmpRect.bottom = editor.cursorAnimator.animatedY() - editor.offsetY; tmpRect.top = tmpRect.bottom - editor.logicalRowHeight; val cX = editor.cursorAnimator.animatedX() - editor.offsetX; tmpRect.left = cX - editor.insertSelectionWidth / 2; tmpRect.right = cX + editor.insertSelectionWidth / 2
        drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SELECTION_INSERT), tmpRect); val bidiAttrs = getBidiIndicatorAttrs(cursor!!.leftLine, cursor!!.leftColumn)
        if (IntPair.getFirst(bidiAttrs) == 1) drawBidiSelectionIndicator(canvas, cX, tmpRect.top, tmpRect.height(), IntPair.getSecond(bidiAttrs) == 1)
        if (editor.touchHandler!!.shouldDrawInsertHandle() && !editor.isInMouseMode) editor.handleStyle!!.draw(canvas, SelectionHandleStyle.HANDLE_TYPE_INSERT, cX, tmpRect.bottom, editor.logicalRowHeight, editor.colorScheme.getColor(EditorColorScheme.SELECTION_HANDLE), editor.handleDescInsert!!)
    }

    protected fun drawScrollBarHorizontal(canvas: Canvas?) {
        if (canvas == null) return
        val page = editor.width; val all = editor.scrollMaxX.toFloat(); var length = page / (all + editor.width) * editor.width; val minL = RenderingConstants.SCROLLBAR_WIDTH_DIP * editor.dpUnit; if (length <= minL) length = minL
        val leftX = editor.offsetX / all * (editor.width - length); tmpRect.top = editor.height - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP; tmpRect.bottom = editor.height.toFloat(); tmpRect.right = leftX + length; tmpRect.left = leftX; horizontalScrollBarRect.set(tmpRect)
        val thumb = horizontalScrollbarThumbDrawable; val handler = editor.touchHandler!!
        if (thumb != null) { thumb.setState(if (handler.holdHorizontalScrollBar()) PRESSED_DRAWABLE_STATE else DEFAULT_DRAWABLE_STATE); thumb.setBounds(tmpRect.left.toInt(), tmpRect.top.toInt(), tmpRect.right.toInt(), tmpRect.bottom.toInt()); thumb.draw(canvas) }
        else drawColor(canvas, editor.colorScheme.getColor(if (handler.holdHorizontalScrollBar()) EditorColorScheme.SCROLL_BAR_THUMB_PRESSED else EditorColorScheme.SCROLL_BAR_THUMB), tmpRect)
    }

    @JvmOverloads
    fun buildMeasureCacheForLines(start: Int, end: Int, ts: Long = timestamp, cached: Boolean = false) {
        val text = content!!; val ctx = editor.renderContext!!; var cur = start
        while (cur <= end && cur < text.lineCount) {
            val line = if (cached) getLine(cur) else getLineDirect(cur); val cache = ctx.cache.getOrCreateMeasureCache(cur)
            if (cache.updateTimestamp < ts) {
                var forced = false; if (cache.widths == null || cache.widths!!.size < line.length) { cache.widths = TextAdvancesCache(max(line.length + 8, 90)); forced = true }
                val spans = editor.getSpansForLine(cur); val h = Objects.hash(spans?.filterNotNull(), line.length, editor.tabWidth, paintGeneral.flags, paintGeneral.textSize, paintGeneral.textScaleX, paintGeneral.letterSpacing, paintGeneral.fontFeatureSettings, paintGeneral.typeface?.hashCode() ?: 0)
                if (ctx.cache.getStyleHash(cur) != h || forced) {
                    ctx.cache.setStyleHash(cur, h); val layout = editor.layout!!; val bRow = layout.getRowIndexForPosition(text.getCharIndex(cur, 0)); val itr = layout.obtainRowIterator(bRow); val tr = TextRow(); val txt = text.getLine(cur); val dirs = text.getLineDirections(cur); val req = txt.length + 10; var w = cache.widths
                    if (w == null || w.size < req) { w = TextAdvancesCache(req); cache.widths = w }
                    while (itr.hasNext()) { val row = itr.next(); if (row.lineIndex != cur) break; tr.set(txt, row.startColumn, row.endColumn, spans?.filterNotNull(), row.inlayHints, dirs, paintGeneral, null, createTextRowParams()!!); tr.buildMeasureCacheStep(w) }
                    tr.setRange(0, txt.length); tr.buildMeasureCacheTailor(w); cache.updateTimestamp = ts
                }
            }
            cur++
        }
    }

    internal fun getRowWidth(row: Int) = createTextRow(row).computeRowWidth()

    protected inner class DrawCursorTask(protected var x: Float, protected var y: Float, protected var handleType: Int, descriptor: SelectionHandleStyle.HandleDescriptor?) {
        protected var descriptor: SelectionHandleStyle.HandleDescriptor? = descriptor
        var isBidiIndicatorRequired = false; var isRightToLeft = false
        private val actualHandleType get() = if (isRightToLeft) (if (handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT) SelectionHandleStyle.HANDLE_TYPE_RIGHT else if (handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) SelectionHandleStyle.HANDLE_TYPE_LEFT else handleType) else handleType
        private fun drawSelForLeftRight() = (handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) && editor.props!!.showSelectionWhenSelected && !editor.isInMouseMode
        private fun drawSelForInsert() = (handleType != SelectionHandleStyle.HANDLE_TYPE_LEFT && handleType != SelectionHandleStyle.HANDLE_TYPE_RIGHT) && (editor.cursorBlink!!.visibility || editor.touchHandler!!.holdInsertHandle() || editor.isInLongSelect)
        private val isSelForLongSelect get() = editor.isInLongSelect && handleType !in SelectionHandleStyle.HANDLE_TYPE_LEFT..SelectionHandleStyle.HANDLE_TYPE_RIGHT
        internal fun execute(canvas: Canvas) {
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && (editor.inputConnection!!.imeConsumingInput || !editor.isFocused())) return
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && !editor.isEditable) return
            val desc = descriptor ?: TMP_DESC
            if (!desc.position.isEmpty() && !editor.isStickyTextSelection) {
                val h = editor.touchHandler!!; if (h.getTouchedHandleType() == actualHandleType && handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && h.isHandleMoving()) {
                    x = h.motionX + (if (desc.alignment != SelectionHandleStyle.ALIGN_CENTER) desc.position.width().toFloat() else 0f) * (if (desc.alignment == SelectionHandleStyle.ALIGN_LEFT) 1 else -1); y = h.motionY - desc.position.height() * 2 / 3f
                }
            }
            if (drawSelForLeftRight() || drawSelForInsert() || handleType == SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                val sY = y - editor.logicalRowHeight; paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.SELECTION_INSERT); paintGeneral.strokeWidth = editor.insertSelectionWidth; paintGeneral.style = AndroidPaint.Style.STROKE
                if (isSelForLongSelect) { val d = (y - sY) / 8f; paintGeneral.pathEffect = DashPathEffect(floatArrayOf(d, d), d / 2f); paintGeneral.strokeWidth = editor.insertSelectionWidth * 1.5f }
                canvas.drawLine(x, sY, x, y, paintGeneral); paintGeneral.style = AndroidPaint.Style.FILL; paintGeneral.pathEffect = null
                if (drawSelForInsert() && isBidiIndicatorRequired) drawBidiSelectionIndicator(canvas, x, sY, y - sY, isRightToLeft)
            }
            var hType = handleType; val h = editor.touchHandler!!; if (hType == SelectionHandleStyle.HANDLE_TYPE_INSERT && (editor.isInLongSelect || !h.shouldDrawInsertHandle())) hType = SelectionHandleStyle.HANDLE_TYPE_UNDEFINED
            if (hType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && !editor.isInMouseMode) { editor.handleStyle!!.draw(canvas, hType, x, y, editor.logicalRowHeight, editor.colorScheme.getColor(EditorColorScheme.SELECTION_HANDLE), desc); if (desc === TMP_DESC) desc.setEmpty() } else desc.setEmpty()
        }
    }

    companion object {
        internal val TMP_DESC = SelectionHandleStyle.HandleDescriptor()
        private val PRESSED_DRAWABLE_STATE = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        private val DEFAULT_DRAWABLE_STATE = intArrayOf(android.R.attr.state_enabled)
        internal const val LOG_TAG = "EditorRenderer"
        private val sDiagnosticsColorMapping = intArrayOf(0, EditorColorScheme.PROBLEM_TYPO, EditorColorScheme.PROBLEM_WARNING, EditorColorScheme.PROBLEM_ERROR)
        const val CURSOR_LINE_BG_OVERLAP_CURSOR = 0; const val CURSOR_LINE_BG_OVERLAP_MIXED = 1; const val CURSOR_LINE_BG_OVERLAP_CUSTOM = 2
    }
}

