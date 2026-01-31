
package io.github.abc15018045126.sora.widget

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.OverScroller
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import io.github.abc15018045126.sora.annotations.Experimental
import io.github.abc15018045126.sora.annotations.InvalidateRequired
import java.io.Serializable

/**
 * Direct-access properties.
 *
 * This object saves some feature settings of editor. These features are not accessed unless the user
 * does something that requires to check the state of the feature. So we save them here by public fields
 * so that you can modify them easily and do not have to call so many methods.
 */
class DirectAccessProps : Serializable {

    companion object {
        /**
         * Rendering behavior for [cursorLineBgOverlapBehavior].
         *
         * If the cursor line has a custom background set, then draw the cursor line background on top
         * of the custom background. For backwards compatibility, this is the default behavior.
         */
        const val CURSOR_LINE_BG_OVERLAP_CUSTOM: Int = 0

        /**
         * Rendering behavior for [cursorLineBgOverlapBehavior].
         *
         * If the cursor line has a custom background set, then don't draw the cursor line background.
         */
        const val CURSOR_LINE_BG_OVERLAP_CURSOR: Int = 1

        /**
         * Rendering behavior for [cursorLineBgOverlapBehavior].
         *
         * If the cursor line has a custom background set, then draw the cursor line background on top
         * of the custom background, but make the cursor line background partly transparent so that both
         * background colors are visible.
         */
        const val CURSOR_LINE_BG_OVERLAP_MIXED: Int = 2

        /**
         * The default rendering behavior for [cursorLineBgOverlapBehavior].
         */
        const val CURSOR_LINE_BG_OVERLAP_DEFAULT: Int = CURSOR_LINE_BG_OVERLAP_CUSTOM

        /**
         * Do nothing
         */
        const val LN_ACTION_NOTHING: Int = 0

        /**
         * Select the whole line
         */
        const val LN_ACTION_SELECT_LINE: Int = 1

        /**
         * Set selection to line start
         */
        const val LN_ACTION_PLACE_SELECTION_HOME: Int = 2

        /**
         * Enable mouse mode if a mouse is currently hovering in editor
         */
        const val MOUSE_MODE_AUTO: Int = 0

        /**
         * Always use mouse mode
         */
        const val MOUSE_MODE_ALWAYS: Int = 1

        /**
         * Do not use mouse mode
         */
        const val MOUSE_MODE_NEVER: Int = 2

        /**
         * Show divider line
         */
        const val STICKY_LINE_INDICATOR_LINE: Int = 1

        /**
         * Show shadow
         */
        const val STICKY_LINE_INDICATOR_SHADOW: Int = 1
    }

    /**
     * Define symbol pairs for any language,
     * Override language settings.
     */
    @JvmField
    val overrideSymbolPairs: SymbolPairMatch = SymbolPairMatch()

    /**
     * If set to be true, the editor will delete the whole line if the current line is empty (only tabs or spaces)
     * when the users press the DELETE key.
     *
     * Default value is `true`
     */
    @JvmField
    var deleteEmptyLineFast: Boolean = true

    /**
     * Delete multiple spaces at a time when the user press the DELETE key.
     * This only takes effect when selection is in leading spaces.
     *
     * Default Value: `1`  -> The editor will always delete only 1 space.
     * Special Value: `-1` -> Follow tab size
     */
    @JvmField
    var deleteMultiSpaces: Int = 1

    /**
     * Set to `false` if you don't want the editor to go fullscreen on devices with smaller screen size.
     * Otherwise, set to `true`
     *
     * Default value is `false`
     */
    @JvmField
    var allowFullscreen: Boolean = false

    /**
     * Control whether auto-completes for symbol pairs.
     *
     * Such as automatically adding a ')' when '(' is entered
     */
    @JvmField
    var symbolPairAutoCompletion: Boolean = true

    /**
     * Show auto-completion even when there is composing text set by the IME in editor.
     *
     * Note: composing text is usually a small piece of text you are typing. It is displayed with an
     * underline in editor.
     * This is useful when the user uses an input method that does not support the attitude [EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS].
     * When this switch is set to false, the editor will not provide auto-completion
     * when there is any composing text in editor.
     */
    @JvmField
    var autoCompletionOnComposing: Boolean = true

    /**
     * Set whether auto indent should be executed when user enters
     * a NEWLINE.
     *
     * Enabling this will automatically copy the leading spaces on this line to the new line.
     */
    @JvmField
    var autoIndent: Boolean = true

    /**
     * Disallow suggestions from keyboard forcibly by preventing
     * [android.view.inputmethod.InputConnection.setComposingText] and
     * [android.view.inputmethod.InputConnection.setComposingRegion] taking effects.
     *
     * This may not be always good for all IMEs, as keyboards' strategy varies.
     *
     * Update: this will cause input connection to be negative and forcibly reject composing texts by
     * restarting inputs.
     */
    @JvmField
    var disallowSuggestions: Boolean = false

    /**
     * Max text length that can be extracted by [android.view.inputmethod.InputConnection.getExtractedText]
     * and other methods related to text content.
     *
     * Usually you need to make it big enough so that the IME does it work for its symbol pair match (at least
     * some Chinese keyboards need it).
     * Text exceeds the limit will be cut, but editor will make sure the selection region is in the extracted text.
     * Some IMEs ignore the [android.view.inputmethod.ExtractedText.startOffset] and if the length exceeds this
     * limit, they may not work properly.
     *
     * Set it to 0 to send no text to IME.
     */
    @JvmField
    @IntRange(from = 0)
    var maxIPCTextLength: Int = 32768

    /**
     * Whether over scroll is permitted.
     * When over scroll is enabled, the user will be able to scroll out of displaying
     * bounds if the user scroll fast enough.
     * This is implemented by [OverScroller.fling]
     */
    @JvmField
    var overScrollEnabled: Boolean = false

    /**
     * Allow fling scroll
     */
    @JvmField
    var scrollFling: Boolean = true

    /**
     * Duration in milliseconds for smooth scrolling animations triggered by the editor.
     * Controls how long programmatic scrolls take to reach their destination.
     * Default value is `250`.
     */
    @JvmField
    @IntRange(from = 0)
    var scrollAnimationDurationMs: Int = 250

    /**
     * If the two completion requests are sent within this time, the completion will not
     * show.
     */
    @JvmField
    var cancelCompletionNs: Long = 70L * 1000000L

    /**
     * Whether the editor should adjust its scroll position to make selection visible when its
     * layout height decreases.
     */
    @JvmField
    var adjustToSelectionOnResize: Boolean = true

    /**
     * Show scroll bars even when the scroll is caused by editor's adjustment but not user interaction
     */
    @JvmField
    var awareScrollbarWhenAdjust: Boolean = false

    /**
     * Wave length of problem indicators.
     *
     * Unit DIP.
     */
    @JvmField
    @InvalidateRequired
    @FloatRange(from = 0.0, fromInclusive = false)
    var indicatorWaveLength: Float = 18f

    /**
     * Wave width of problem indicators.
     *
     * Unit DIP.
     */
    @JvmField
    @InvalidateRequired
    @FloatRange(from = 0.0, fromInclusive = false)
    var indicatorWaveWidth: Float = 0.9f

    /**
     * Wave amplitude of problem indicators.
     *
     * Unit DIP.
     */
    @JvmField
    @InvalidateRequired
    @FloatRange(from = 0.0, fromInclusive = false)
    var indicatorWaveAmplitude: Float = 4f

    /**
     * Compare the text to commit with composing text.
     *
     * See detailed issue: #155
     */
    @JvmField
    var trackComposingTextOnCommit: Boolean = true

    /**
     * Try to simplify composing text update as a single insertion or deletion.
     *
     * See detailed issue: #357
     */
    @JvmField
    var minimizeComposingTextUpdate: Boolean = true

    /**
     * Draw side block line when in wordwrap mode
     */
    @JvmField
    @InvalidateRequired
    var drawSideBlockLine: Boolean = true

    /**
     * Cache RenderNode of long text lines
     * This costs some memory, but improves performance when the line is not too long.
     */
    @JvmField
    var cacheRenderNodeForLongLines: Boolean = false

    /**
     * Use the ICU library to find range of words on double tap or long press.
     */
    @JvmField
    var useICULibToSelectWords: Boolean = true

    /**
     * Highlight matching delimiters. This requires language support.
     */
    @JvmField
    @InvalidateRequired
    var highlightMatchingDelimiters: Boolean = true

    /**
     * Make matching delimiters bold
     */
    @JvmField
    @InvalidateRequired
    var boldMatchingDelimiters: Boolean = true

    /**
     * Whether the editor will use round rectangle for text background
     */
    @JvmField
    @InvalidateRequired
    var enableRoundTextBackground: Boolean = true

    /**
     * The text background wraps the actual text, but not the whole line
     */
    @JvmField
    @InvalidateRequired
    var textBackgroundWrapTextOnly: Boolean = false

    /**
     * The new cursor position when the user exits selecting mode.
     * `true` for the current right cursor
     * `false` for the current left cursor
     */
    @JvmField
    var positionOfCursorWhenExitSelecting: Boolean = true

    /**
     * Draw custom line background color (specified by [io.github.abc15018045126.sora.lang.styling.line.LineBackground])
     * on current line
     */
    @JvmField
    @InvalidateRequired
    var drawCustomLineBgOnCurrentLine: Boolean = false

    /**
     * The factor of round rectangle, affecting the corner radius of the resulting display
     */
    @JvmField
    @InvalidateRequired
    var roundTextBackgroundFactor: Float = 0.13f

    /**
     * Specify the icon size factor. result size = row height * sideIconSizeFactor
     */
    @JvmField
    @InvalidateRequired
    @FloatRange(from = 0.0, to = 1.0)
    var sideIconSizeFactor: Float = 0.7f

    /**
     * Specify the marker text size factor, such as line-break markers.
     * not available for setting now
     */
    @JvmField
    @InvalidateRequired
    @FloatRange(from = 0.0, to = 1.0)
    val miniMarkerSizeFactor: Float = 0.5f

    /**
     * Specify the text size factor for function characters
     * not available for setting now
     */
    @JvmField
    @InvalidateRequired
    @FloatRange(from = 0.0, to = 1.0)
    val functionCharacterSizeFactor: Float = 0.85f

    /**
     * Specify editor behavior when line number is clicked.
     */
    @JvmField
    var actionWhenLineNumberClicked: Int = LN_ACTION_PLACE_SELECTION_HOME

    /**
     * Format pasted text (when text is pasted by [CodeEditor.pasteText])
     */
    @JvmField
    var formatPastedText: Boolean = false

    /**
     * Use enhanced function of home and end. When it is enabled, clicking home will place
     * the selection to actually text start on the line if the selection is currently at the start
     * of line. End works in similar way, too.
     */
    @JvmField
    var enhancedHomeAndEnd: Boolean = true

    /**
     * Show hard wrap marker near the column. (a reminder for starting a new line)
     * Use 0 or negative number for no marker
     */
    @JvmField
    @InvalidateRequired
    var hardwrapColumn: Int = 0

    /**
     * Select words even if some texts are already selected when the editor is
     * long-pressed.
     * If true, new text under the new long-press will be selected. Otherwise, the old text is kept
     * selected.
     */
    @JvmField
    var reselectOnLongPress: Boolean = true

    /**
     * Enable drag-select after a long-press. When true (default), the editor suppresses selection
     * handles during the drag gesture and lets the magnifier follow the finger until the drag
     * completes.
     */
    @JvmField
    var dragSelectAfterLongPress: Boolean = true

    /**
     * Show selection above selection handle when text is selected
     */
    @JvmField
    @InvalidateRequired
    var showSelectionWhenSelected: Boolean = false

    /**
     * Limit length for copying text to clipboard. When the length of copying text exceeded the limit,
     * copying is aborted and a toast tip is shown to notify user that the action is failed.
     *
     * Default size is 512*1024 Java characters, which is 1MB in UTF-16 encoding
     */
    @JvmField
    var clipboardTextLengthLimit: Int = 512 * 1024

    /**
     * Scrolling speed multiplier when ALT key is pressed (for mouse wheel only).
     *
     * 5.0f by default
     */
    @JvmField
    @FloatRange(from = 1.0)
    var fastScrollSensitivity: Float = 5f

    /**
     * Enable/disable sticky scroll mode
     */
    @JvmField
    @InvalidateRequired
    var stickyScroll: Boolean = false

    /**
     * Control the count of lines that can be stuck to the top of the editor
     */
    @JvmField
    @IntRange(from = 1)
    @InvalidateRequired
    var stickyScrollMaxLines: Int = 3

    /**
     * Prefer inner scopes if true.
     * When set to false, editor abandons inner scopes if [stickyScrollMaxLines] is exceeded.
     * When set to true, editor push the top stuck line out to show the new scope
     * if [stickyScrollMaxLines] is exceeded.
     */
    @JvmField
    @InvalidateRequired
    var stickyScrollPreferInnerScope: Boolean = false

    /**
     * Limit for sticky scroll dataset size
     */
    @JvmField
    var stickyScrollIterationLimit: Int = 1000

    /**
     * Hide partially or all of the stuck lines when text is selected
     */
    @JvmField
    @InvalidateRequired
    var stickyScrollAutoCollapse: Boolean = true

    /**
     * Fling scroll in single direction (vertical or horizontal)
     */
    @JvmField
    var singleDirectionFling: Boolean = true

    /**
     * Dragging scroll in single direction (vertical or horizontal)
     */
    @JvmField
    var singleDirectionDragging: Boolean = true

    /**
     * Report cursor anchor info to system.
     *
     * Enable this if the IME needs to get the position of cursor on screen. For example, the
     * IME dialog follows our insert marker (selection).
     */
    @JvmField
    var reportCursorAnchor: Boolean = true

    /**
     * Place selection on previous line after cutting line
     */
    @JvmField
    var placeSelOnPreviousLineAfterCut: Boolean = false

    /**
     * When to enable mouse mode. This affects editor windows and selection handles.
     */
    @JvmField
    var mouseMode: Int = MOUSE_MODE_AUTO

    /**
     * Try to show context menu for mouse
     */
    @JvmField
    var mouseContextMenu: Boolean = true

    /**
     * Always show scrollbars when the editor is in mouse mode
     */
    @JvmField
    var mouseModeAlwaysShowScrollbars: Boolean = true

    /**
     * Adjust scrolling speed in mouse wheel scrolling
     */
    @JvmField
    var mouseWheelScrollFactor: Float = 1.2f

    /**
     * Disable [android.view.inputmethod.InputConnection.getExtractedText]
     * for IME
     */
    @JvmField
    var disableTextExtracting: Boolean = false

    /**
     * Specifies the cursor line background rendering behavior when the cursor is at a line which
     * also has a custom line background set.
     */
    @JvmField
    @InvalidateRequired
    var cursorLineBgOverlapBehavior: Int = CURSOR_LINE_BG_OVERLAP_DEFAULT

    /**
     * If `true`, Home and End shortcuts will be based on visual lines (editor rows)
     * instead of physical lines.
     */
    @JvmField
    var rowBasedHomeEnd: Boolean = true

    /**
     * Check thread when the text in editor is changed. Note that the text should be modified from
     * UI thread only, because the editor need to update itself in UI thread.
     *
     * You may set it to `true` for debugging purpose to detect possible violations
     */
    @JvmField
    @Experimental
    var checkModificationThread: Boolean = false

    /**
     * Show direction indicator on selection for bidirectional text.
     */
    @JvmField
    @InvalidateRequired
    var showBidiDirectionIndicator: Boolean = true

    /**
     * How to show the sticky line divider
     */
    @JvmField
    @InvalidateRequired
    var stickyLineIndicator: Int = STICKY_LINE_INDICATOR_LINE or STICKY_LINE_INDICATOR_SHADOW

    /**
     * The completion window will automatically move selection to first item if physical
     * keyboard is connected when it is going to show up.
     */
    @JvmField
    var moveSelectionToFirstForKeyboard: Boolean = true

    /**
     * Select the first completion item on enter for software keyboard
     */
    @JvmField
    var selectCompletionItemOnEnterForSoftKbd: Boolean = true

}
