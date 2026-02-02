package io.github.abc15018045126.sora.widget.component

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.event.*
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.EditorTouchEventHandler
import io.github.abc15018045126.sora.widget.base.EditorPopupWindow
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.snippet.SnippetController

/**
 * This window will show when selecting text to present text actions.
 *
 * @author abc15018045126
 */
class EditorTextActionWindow(editor: CodeEditor) :
    EditorPopupWindow(editor, FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED), View.OnClickListener,
    EditorBuiltinComponent {

    private val selectAllBtn: ImageButton
    private val pasteBtn: ImageButton
    private val copyBtn: ImageButton
    private val cutBtn: ImageButton
    private val rootView: View
    private val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!

    private val eventManager = editor.createSubEventManager()
    private var lastScroll: Long = 0
    private var lastPosition: Int = -1
    private var lastCause: Int = 0
    private var lastActionTime: Long = 0
    override var isEnabled = true
        set(value) {
            field = value
            eventManager.isEnabled = value
            if (!value) {
                dismiss()
            }
        }

    companion object {
        private const val DELAY: Long = 200
        private const val CHECK_FOR_DISMISS_INTERVAL: Long = 100
    }

    init {
        @SuppressLint("InflateParams")
        val root = LayoutInflater.from(editor.context).inflate(R.layout.text_compose_panel, null)
        this.rootView = root
        selectAllBtn = root.findViewById(R.id.panel_btn_select_all)
        cutBtn = root.findViewById(R.id.panel_btn_cut)
        copyBtn = root.findViewById(R.id.panel_btn_copy)
        pasteBtn = root.findViewById(R.id.panel_btn_paste)

        selectAllBtn.setOnClickListener(this)
        cutBtn.setOnClickListener(this)
        copyBtn.setOnClickListener(this)
        pasteBtn.setOnClickListener(this)

        applyColorScheme()
        setContentView(root)
        setSize(rootView.measuredWidth, (editor.dpUnit * 48).toInt())
        popup.animationStyle = R.style.text_action_popup_animation

        subscribeEvents()
    }

    private fun applyColorFilter(btn: ImageButton, color: Int) {
        btn.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    fun applyColorScheme() {
        val gd = GradientDrawable()
        gd.cornerRadius = 5 * editor.dpUnit
        try {
            gd.setColor(android.graphics.Color.parseColor(editor.floatMenuBackgroundColor))
        } catch (e: Exception) {
            gd.setColor(editor.colorScheme.getColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND))
        }
        rootView.background = gd
        val color = editor.colorScheme.getColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR)
        applyColorFilter(selectAllBtn, color)
        applyColorFilter(cutBtn, color)
        applyColorFilter(copyBtn, color)
        applyColorFilter(pasteBtn, color)
    }

    private fun subscribeEvents() {
        eventManager.subscribeAlways(SelectionChangeEvent::class.java, this::onSelectionChange)
        eventManager.subscribeAlways(ScrollEvent::class.java, this::onEditorScroll)
        eventManager.subscribeAlways(HandleStateChangeEvent::class.java, this::onHandleStateChange)
        eventManager.subscribeAlways(EditorKeyEvent::class.java, this::onEditorKey)
        eventManager.subscribeAlways(EditorFocusChangeEvent::class.java, this::onEditorFocusChange)
        eventManager.subscribeAlways(EditorReleaseEvent::class.java, this::onEditorRelease)
        eventManager.subscribeAlways(ColorSchemeUpdateEvent::class.java, this::onEditorColorChange)
        eventManager.subscribeAlways(DragSelectStopEvent::class.java, this::onDragSelectingStop)
    }

    private fun onEditorColorChange(event: ColorSchemeUpdateEvent) {
        applyColorScheme()
    }

    private fun onEditorKey(event: EditorKeyEvent) {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.eventType == EditorKeyEvent.Type.DOWN) {
            if (editor.cursor.isSelected() || editor.isInLongSelect) {
                lastActionTime = System.currentTimeMillis()
            }
        }
    }

    private fun onDragSelectingStop(event: DragSelectStopEvent) {
        displayWindow()
    }

    private fun onEditorRelease(event: EditorReleaseEvent) {
        isEnabled = false
    }

    private fun onEditorFocusChange(event: EditorFocusChangeEvent) {
        if (!event.isGainFocus) {
            dismiss()
        }
    }

    private fun onEditorScroll(event: ScrollEvent) {
        val last = lastScroll
        lastScroll = System.currentTimeMillis()
        if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
            postDisplay()
        }
    }

    private fun onHandleStateChange(event: HandleStateChangeEvent) {
        if (event.isHeld) {
            postDisplay()
        }
        if (!event.editor.cursor.isSelected()
            && event.handleType == HandleStateChangeEvent.HANDLE_TYPE_INSERT
            && !event.isHeld
        ) {
            if (System.currentTimeMillis() - lastActionTime < 300) {
                return
            }
            displayWindow()
            // Also, post to hide the window on handle disappearance
            // Also, post to hide the window on handle disappearance
            io.github.abc15018045126.sora.util.EditorHandler.postDelayed(object : Runnable {
                override fun run() {
                    if (editor.isReleased) return
                    val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
                    if (!handler.shouldDrawInsertHandle()
                        && !editor.cursor.isSelected()
                    ) {
                        dismiss()
                    } else if (!editor.cursor.isSelected()) {
                        io.github.abc15018045126.sora.util.EditorHandler.postDelayed(this, CHECK_FOR_DISMISS_INTERVAL)
                    }
                }
            }, CHECK_FOR_DISMISS_INTERVAL)

        }
    }

    private fun onSelectionChange(event: SelectionChangeEvent) {
        if (System.currentTimeMillis() - lastActionTime < 300) {
            if (!event.isSelected) {
                dismiss()
                return
            }
        }
        if (handler.hasAnyHeldHandle() || event.cause == SelectionChangeEvent.CAUSE_DEAD_KEYS) {
            return
        }
        if (handler.isDragSelecting()) {
            dismiss()
            return
        }
        lastCause = event.cause
        if (event.isSelected || event.cause == SelectionChangeEvent.CAUSE_IME
            || event.cause == SelectionChangeEvent.CAUSE_SELECTION_HANDLE
            || event.cause == SelectionChangeEvent.CAUSE_SEARCH || event.cause == SelectionChangeEvent.CAUSE_UNKNOWN
        ) {
            // Always post show. See #193
            val shouldShow = when (event.cause) {
                SelectionChangeEvent.CAUSE_SEARCH -> false
                SelectionChangeEvent.CAUSE_LONG_PRESS -> editor.floatMenuTriggerLongPress
                SelectionChangeEvent.CAUSE_DOUBLE_TAP -> editor.floatMenuTriggerDoubleTap
                else -> true
            }

            if (shouldShow) {
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (editor.isReleased) return@post
                    displayWindow()
                }
            } else {
                dismiss()
            }
            lastPosition = -1
        } else {
            var show = false
            if (event.cause == SelectionChangeEvent.CAUSE_TAP && event.left.index == lastPosition && !isShowing && !editor.text.isInBatchEdit && editor.isEditable) {
                io.github.abc15018045126.sora.util.EditorHandler.post {
                   if (editor.isReleased) return@post
                   displayWindow()
                }
                show = true

            } else {
                dismiss()
            }
            if (event.cause == SelectionChangeEvent.CAUSE_TAP && !show) {
                lastPosition = event.left.index
            } else {
                lastPosition = -1
            }
        }
    }

    /**
     * Get the view root of the panel.
     */
    fun getView(): ViewGroup {
        return popup.contentView as ViewGroup
    }

    private fun postDisplay() {
        if (!isShowing) {
            return
        }
        dismiss()
        if (!editor.cursor.isSelected()) {
            return
        }
        io.github.abc15018045126.sora.util.EditorHandler.postDelayed(object : Runnable {
            override fun run() {
                if (editor.isReleased) return
                val snippetController: io.github.abc15018045126.sora.widget.snippet.SnippetController? = editor.snippetController
                if (!handler.hasAnyHeldHandle() && snippetController?.isInSnippet() != true && System.currentTimeMillis() - lastScroll > DELAY
                    && editor.scroller.isFinished
                ) {
                    displayWindow()
                } else {
                    io.github.abc15018045126.sora.util.EditorHandler.postDelayed(this, DELAY)
                }
            }
        }, DELAY)


    }

    private fun selectTop(rect: RectF): Int {
        val rowHeight = editor.rowHeight
        return if (rect.top - rowHeight * 3 / 2f > height) {
            (rect.top - rowHeight * 3 / 2 - height).toInt()
        } else {
            (rect.bottom + rowHeight / 2).toInt()
        }
    }

    fun displayWindow() {
        updateBtnState()
        var top: Int
        val cursor = editor.cursor
        if (cursor.isSelected()) {
            val leftRect = editor.leftHandleDescriptor!!.position
            val rightRect = editor.rightHandleDescriptor!!.position
            val top1 = selectTop(leftRect)

            val top2 = selectTop(rightRect)
            top = Math.min(top1, top2)
        } else {
            top = selectTop(editor.insertHandleDescriptor!!.position)
        }

        top = Math.max(0, Math.min(top, editor.height - height - 5))
        val handleLeftX = editor.getOffset(editor.cursor.leftLine, editor.cursor.leftColumn)
        val handleRightX =
            editor.getOffset(editor.cursor.rightLine, editor.cursor.rightColumn)
        val panelX = (handleLeftX + handleRightX) / 2f - rootView.measuredWidth / 2f
        setLocationAbsolutely(panelX, top.toFloat())
        show()
    }

    /**
     * Update buttons and ordering
     */
    private fun updateBtnState() {
        val visibleList = editor.floatMenuVisible.split(",")
        val orderList = editor.floatMenuOrder.split(",")
        
        val btnMap = mapOf(
            "select_all" to selectAllBtn,
            "cut" to cutBtn,
            "copy" to copyBtn,
            "paste" to pasteBtn
        )

        // Visibility and basic state
        pasteBtn.isEnabled = editor.hasClip()
        
        btnMap.forEach { (key, btn) ->
            val isUserVisible = visibleList.contains(key)
            val isContextVisible = when(key) {
                "copy" -> editor.cursor.isSelected()
                "paste" -> editor.isEditable
                "cut" -> editor.cursor.isSelected() && editor.isEditable
                "select_all" -> true
                else -> true
            }
            btn.visibility = if (isUserVisible && isContextVisible) View.VISIBLE else View.GONE
        }

        // Ordering Logic (Assuming rootView is a ViewGroup)
        if (rootView is ViewGroup) {
            val rootGroup = rootView as ViewGroup
            // Collect all views currently in the group
            val currentViews = mutableListOf<View>()
            for (i in 0 until rootGroup.childCount) {
                currentViews.add(rootGroup.getChildAt(i))
            }
            
            // Re-add them in order
            rootGroup.removeAllViews()
            orderList.forEach { key ->
                btnMap[key]?.let { btn ->
                    if (currentViews.contains(btn)) {
                        rootGroup.addView(btn)
                        currentViews.remove(btn)
                    }
                }
            }
            // Add any remaining views (that might not be in our btnMap)
            currentViews.forEach { rootGroup.addView(it) }
        }

        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(1000000, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST)
        )
        setSize(
            Math.min(rootView.measuredWidth, (editor.dpUnit * 230).toInt()),
            height
        )
    }

    override fun show() {
        val snippetController: io.github.abc15018045126.sora.widget.snippet.SnippetController? = editor.snippetController
        if (!isEnabled || snippetController?.isInSnippet() == true || !editor.hasFocus() || editor.isInMouseMode) {
            return
        }
        super.show()
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.panel_btn_select_all) {
            editor.selectAll()
            return
        }
        lastActionTime = System.currentTimeMillis()
        if (id == R.id.panel_btn_cut) {
                editor.cutText()
        } else if (id == R.id.panel_btn_paste) {
            editor.pasteText()
            editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
        } else if (id == R.id.panel_btn_copy) {
            editor.copyText()
            editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
        }
        dismiss()
    }

}
