

package io.github.abc15018045126.sora.event

import android.os.Bundle
import android.view.ContextMenu
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import io.github.abc15018045126.sora.lang.Language
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.widget.CodeEditor


class EditorLanguageChangeEvent(editor: CodeEditor, val newLanguage: Language) : Event(editor)


class EditorFormatEvent(editor: CodeEditor, val isSuccess: Boolean) : Event(editor)


class EditorReleaseEvent(editor: CodeEditor) : Event(editor)

class ImePrivateCommandEvent(editor: CodeEditor, val action: String, val data: Bundle?) :
    Event(editor)


class BuildEditorInfoEvent(editor: CodeEditor, val editorInfo: EditorInfo) : Event(editor)

class EditorFocusChangeEvent(editor: CodeEditor, val isGainFocus: Boolean) : Event(editor)


class EditorAttachStateChangeEvent(editor: CodeEditor, val isAttachedToWindow: Boolean) :
    Event(editor)


class ContextClickEvent(
    editor: CodeEditor,
    position: CharPosition,
    event: MotionEvent,
    span: Span?,
    spanRange: TextRange?,
    motionRegion: Int,
    motionBound: Int,
) : EditorMotionEvent(editor, position, event, span, spanRange, motionRegion, motionBound)


class HoverEvent(
    editor: CodeEditor,
    position: CharPosition,
    event: MotionEvent,
    span: Span?,
    spanRange: TextRange?,
    motionRegion: Int,
    motionBound: Int,
) : EditorMotionEvent(editor, position, event, span, spanRange, motionRegion, motionBound)


class DragSelectStopEvent(
    editor: CodeEditor
) : Event(editor)


class CreateContextMenuEvent(
    editor: CodeEditor,
    val menu: ContextMenu,
    val position: CharPosition
) : Event(editor)


class TextSizeChangeEvent(
    editor: CodeEditor,
    val oldTextSize: Float,
    val newTextSize: Float
) : Event(editor)


class PublishSearchResultEvent(editor: CodeEditor) : Event(editor) {

    fun getSearcher() = editor.searcher

}

class LayoutStateChangeEvent(editor: CodeEditor, val isLayoutBusy: Boolean) : Event(editor)
