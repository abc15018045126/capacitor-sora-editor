

package io.github.abc15018045126.sora.widget.ext

import android.content.Intent
import android.net.Uri
import io.github.abc15018045126.sora.event.ClickEvent
import io.github.abc15018045126.sora.event.DoubleClickEvent
import io.github.abc15018045126.sora.event.EditorMotionEvent
import io.github.abc15018045126.sora.event.LongPressEvent
import io.github.abc15018045126.sora.event.subscribeAlways
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.span.SpanClickableUrl
import io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs
import io.github.abc15018045126.sora.lang.styling.span.SpanInteractionInfo
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.IN_BOUND
import io.github.abc15018045126.sora.widget.REGION_TEXT
import io.github.abc15018045126.sora.widget.resolveTouchRegion


open class EditorSpanInteractionHandler(val editor: CodeEditor) {

    val eventManager = editor.createSubEventManager()

    init {
        eventManager.subscribeAlways<ClickEvent> { event ->
            if (!event.isFromMouse || (event.isFromMouse && editor.getKeyMetaStates().isCtrlPressed)) {
                handleInteractionEvent(
                    event,
                    SpanInteractionInfo::isClickable,
                    ::handleSpanClick,
                    !event.isFromMouse
                )
            }
        }
        eventManager.subscribeAlways<DoubleClickEvent> { event ->
            handleInteractionEvent(
                event,
                SpanInteractionInfo::isDoubleClickable,
                ::handleSpanDoubleClick,
                !event.isFromMouse
            )
        }
        eventManager.subscribeAlways<LongPressEvent> { event ->
            handleInteractionEvent(
                event,
                SpanInteractionInfo::isLongClickable,
                ::handleSpanLongClick,
                !event.isFromMouse
            )
        }
    }

    private fun handleInteractionEvent(
        event: EditorMotionEvent,
        predicate: (interactionInfo: SpanInteractionInfo) -> Boolean,
        handler: (Span, SpanInteractionInfo, TextRange) -> Boolean,
        checkCursorRange: Boolean = true
    ) {
        val regionInfo = editor.resolveTouchRegion(event.causingEvent)
        val span = event.span
        val spanRange = event.spanRange
        if (IntPair.getFirst(regionInfo) == REGION_TEXT &&
            IntPair.getSecond(regionInfo) == IN_BOUND &&
            span != null && spanRange != null
        ) {
            if (!checkCursorRange || spanRange.isPositionInside(editor.cursor!!.left())) {
                span.getSpanExt<SpanInteractionInfo>(SpanExtAttrs.EXT_INTERACTION_INFO)?.let {
                    if (predicate(it)) {
                        if (handler(span, it, spanRange)) {
                            event.intercept()
                        }
                    }
                }
            }
        }
    }

    open fun handleSpanClick(
        span: Span,
        interactionInfo: SpanInteractionInfo,
        spanRange: TextRange
    ): Boolean {
        return false
    }

    open fun handleSpanDoubleClick(
        span: Span,
        interactionInfo: SpanInteractionInfo,
        spanRange: TextRange
    ): Boolean {
        when (interactionInfo) {
            is SpanClickableUrl -> {
                val uri = interactionInfo.getData()
                runCatching {
                    Uri.parse(uri)
                }.onSuccess {
                    val intent = Intent(Intent.ACTION_VIEW, it)
                    editor.context.startActivity(intent)
                }
                return true
            }
        }
        return false
    }

    open fun handleSpanLongClick(
        span: Span,
        interactionInfo: SpanInteractionInfo,
        spanRange: TextRange
    ): Boolean {
        return false
    }

    fun isEnabled() = eventManager.isEnabled

    fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
    }


}
