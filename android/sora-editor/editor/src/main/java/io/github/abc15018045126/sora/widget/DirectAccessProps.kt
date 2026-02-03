package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.annotations.Experimental
import io.github.abc15018045126.sora.annotations.InvalidateRequired
import java.io.Serializable

class DirectAccessProps : Serializable {
    companion object {
        const val CURSOR_LINE_BG_OVERLAP_CUSTOM = 0
        const val CURSOR_LINE_BG_OVERLAP_CURSOR = 1
        const val CURSOR_LINE_BG_OVERLAP_MIXED = 2
        const val CURSOR_LINE_BG_OVERLAP_DEFAULT = CURSOR_LINE_BG_OVERLAP_CUSTOM
        const val LN_ACTION_NOTHING = 0
        const val LN_ACTION_SELECT_LINE = 1
        const val LN_ACTION_PLACE_SELECTION_HOME = 2
        const val MOUSE_MODE_AUTO = 0
        const val MOUSE_MODE_ALWAYS = 1
        const val MOUSE_MODE_NEVER = 2
        const val STICKY_LINE_INDICATOR_LINE = 1
        const val STICKY_LINE_INDICATOR_SHADOW = 1
    }

    @JvmField val overrideSymbolPairs = SymbolPairMatch()
    @JvmField var deleteEmptyLineFast = true
    @JvmField var deleteMultiSpaces = 1
    @JvmField var allowFullscreen = false
    @JvmField var symbolPairAutoCompletion = true
    @JvmField var autoCompletionOnComposing = true
    @JvmField var autoIndent = true
    @JvmField var disallowSuggestions = false
    @JvmField var maxIPCTextLength = 32768
    @JvmField var overScrollEnabled = false
    @JvmField var scrollFling = true
    @JvmField var scrollAnimationDurationMs = 250
    @JvmField var cancelCompletionNs = 70L * 1000000L
    @JvmField var adjustToSelectionOnResize = true
    @JvmField var awareScrollbarWhenAdjust = false
    @InvalidateRequired @JvmField var indicatorWaveLength = 18f
    @InvalidateRequired @JvmField var indicatorWaveWidth = 0.9f
    @InvalidateRequired @JvmField var indicatorWaveAmplitude = 4f
    @JvmField var trackComposingTextOnCommit = true
    @JvmField var minimizeComposingTextUpdate = true
    @InvalidateRequired @JvmField var drawSideBlockLine = true
    @JvmField var cacheRenderNodeForLongLines = false
    @JvmField var useICULibToSelectWords = true
    @InvalidateRequired @JvmField var highlightMatchingDelimiters = true
    @InvalidateRequired @JvmField var boldMatchingDelimiters = true
    @InvalidateRequired @JvmField var enableRoundTextBackground = true
    @InvalidateRequired @JvmField var textBackgroundWrapTextOnly = false
    @JvmField var positionOfCursorWhenExitSelecting = true
    @InvalidateRequired @JvmField var drawCustomLineBgOnCurrentLine = false
    @InvalidateRequired @JvmField var roundTextBackgroundFactor = 0.13f
    @InvalidateRequired @JvmField var sideIconSizeFactor = 0.7f
    @InvalidateRequired @JvmField val miniMarkerSizeFactor = 0.5f
    @InvalidateRequired @JvmField val functionCharacterSizeFactor = 0.85f
    @JvmField var actionWhenLineNumberClicked = LN_ACTION_PLACE_SELECTION_HOME
    @JvmField var formatPastedText = false
    @JvmField var enhancedHomeAndEnd = true
    @InvalidateRequired @JvmField var hardwrapColumn = 0
    @JvmField var reselectOnLongPress = true
    @JvmField var dragSelectAfterLongPress = true
    @InvalidateRequired @JvmField var showSelectionWhenSelected = false
    @JvmField var clipboardTextLengthLimit = 512 * 1024
    @JvmField var fastScrollSensitivity = 5f
    @InvalidateRequired @JvmField var stickyScroll = false
    @InvalidateRequired @JvmField var stickyScrollMaxLines = 3
    @InvalidateRequired @JvmField var stickyScrollPreferInnerScope = false
    @JvmField var stickyScrollIterationLimit = 1000
    @InvalidateRequired @JvmField var stickyScrollAutoCollapse = true
    @JvmField var singleDirectionFling = true
    @JvmField var singleDirectionDragging = true
    @JvmField var reportCursorAnchor = true
    @JvmField var placeSelOnPreviousLineAfterCut = false
    @JvmField var mouseMode = MOUSE_MODE_AUTO
    @JvmField var mouseContextMenu = true
    @JvmField var mouseModeAlwaysShowScrollbars = true
    @JvmField var mouseWheelScrollFactor = 1.2f
    @JvmField var disableTextExtracting = false
    @InvalidateRequired @JvmField var cursorLineBgOverlapBehavior = CURSOR_LINE_BG_OVERLAP_DEFAULT
    @JvmField var rowBasedHomeEnd = true
    @Experimental @JvmField var checkModificationThread = false
    @InvalidateRequired @JvmField var showBidiDirectionIndicator = true
    @InvalidateRequired @JvmField var stickyLineIndicator = STICKY_LINE_INDICATOR_LINE or STICKY_LINE_INDICATOR_SHADOW
    @JvmField var moveSelectionToFirstForKeyboard = true
    @JvmField var selectCompletionItemOnEnterForSoftKbd = true
}
