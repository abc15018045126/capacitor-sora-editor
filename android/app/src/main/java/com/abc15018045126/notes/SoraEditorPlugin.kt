package com.abc15018045126.notes

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

@CapacitorPlugin(name = "SoraEditor")
class SoraEditorPlugin : Plugin() {

    private var editor: CodeEditor? = null

    @PluginMethod
    fun start(call: PluginCall) {
        val content = call.getString("content") ?: ""
        val topMargin = call.getInt("top") ?: 0
        val leftMargin = call.getInt("left") ?: 0
        val rightMargin = call.getInt("right") ?: 0
        val bottomMargin = call.getInt("bottom") ?: 0
        val width = call.getInt("width") ?: -1 // -1 = MATCH_PARENT
        val height = call.getInt("height") ?: -1 
        val fontSize = call.getFloat("fontSize") ?: 18f
        val showLineNumbers = call.getBoolean("showLineNumbers") ?: true
        val wordWrap = call.getBoolean("wordWrap") ?: false
        val editable = call.getBoolean("editable") ?: true
        val bgColorStr = call.getString("backgroundColor") // e.g. "#FFFFFF"
        
        activity.runOnUiThread {
            if (editor == null) {
                editor = CodeEditor(context).apply {
                    setTextSize(fontSize)
                    setTypefaceText(Typeface.MONOSPACE)
                    isLineNumberEnabled = showLineNumbers
                    isWordwrap = wordWrap
                    setEditable(editable)
                    
                    var startX = 0f
                    var startY = 0f
                    var startTime = 0L
                    
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                startX = event.x
                                startY = event.y
                                startTime = System.currentTimeMillis()
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                val duration = System.currentTimeMillis() - startTime
                                val dist = Math.sqrt(Math.pow((event.x - startX).toDouble(), 2.0) + Math.pow((event.y - startY).toDouble(), 2.0))
                                if (duration < 300 && dist < 20) {
                                    notifyListeners("onEditorClick", JSObject())
                                }
                            }
                        }
                        false // Allow editor to handle the event too
                    }
                    subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { event, _ ->
                        notifyListeners("onContentChange", JSObject())
                    }
                }
                
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                activity.addContentView(editor, params)
            }

            editor!!.isLineNumberEnabled = showLineNumbers
            editor!!.isWordwrap = wordWrap
            editor!!.setEditable(editable)
            
            if (bgColorStr != null) {
                try {
                    val color = Color.parseColor(bgColorStr)
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    
                    editor!!.colorScheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, color)
                    editor!!.colorScheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, color)
                    
                    if (luminance < 0.5) {
                        editor!!.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, Color.WHITE)
                        editor!!.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, Color.GRAY)
                    } else {
                        editor!!.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, Color.BLACK)
                        editor!!.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, Color.DKGRAY)
                    }
                } catch (e: Exception) {}
            }

            val density = context.resources.displayMetrics.density
            val params = editor!!.layoutParams as FrameLayout.LayoutParams
            params.topMargin = (topMargin * density).toInt()
            params.leftMargin = (leftMargin * density).toInt()
            params.rightMargin = (rightMargin * density).toInt()
            params.bottomMargin = (bottomMargin * density).toInt()
            if (width >= 0) params.width = (width * density).toInt() else params.width = FrameLayout.LayoutParams.MATCH_PARENT
            if (height >= 0) params.height = (height * density).toInt() else params.height = FrameLayout.LayoutParams.MATCH_PARENT
            
            editor!!.layoutParams = params

            // Only update text if explicitly provided and different (to avoid cursor jump)?
            // Or if content is passed, assume overwrite.
            if (call.hasOption("content")) {
                val currentText = editor!!.text.toString()
                if (currentText != content) {
                     editor!!.setText(content)
                }
            }
            
            editor!!.setTextSize(fontSize)
            editor!!.visibility = View.VISIBLE
            editor!!.bringToFront()
            
            if (call.hasOption("selectionLine") || call.hasOption("selectionColumn")) {
                 val l = call.getInt("selectionLine") ?: 0
                 val c = call.getInt("selectionColumn") ?: 0
                 editor!!.postDelayed({
                     try {
                         editor!!.setSelection(l, c)
                         editor!!.ensureSelectionVisible()
                         android.widget.Toast.makeText(context, "Jumping to $l", android.widget.Toast.LENGTH_SHORT).show()
                     } catch(e: Exception){
                         android.widget.Toast.makeText(context, "Jump Error: $e", android.widget.Toast.LENGTH_SHORT).show()
                     }
                 }, 150)
            }
        }
        android.widget.Toast.makeText(context, "Start Called", android.widget.Toast.LENGTH_SHORT).show()
        call.resolve()
    }

    @PluginMethod
    fun close(call: PluginCall) {
        activity.runOnUiThread {
            editor?.visibility = View.GONE
        }
        call.resolve()
    }

    @PluginMethod
    fun getText(call: PluginCall) {
        val ret = JSObject()
        activity.runOnUiThread {
            ret.put("content", editor?.text?.toString() ?: "")
            call.resolve(ret)
        }
    }

    @PluginMethod
    fun getSelection(call: PluginCall) {
        val ret = JSObject()
        activity.runOnUiThread {
            if (editor != null) {
                val cursor = editor!!.cursor
                ret.put("line", cursor.leftLine)
                ret.put("column", cursor.leftColumn)
            }
            call.resolve(ret)
        }
    }
    
    @PluginMethod
    fun setText(call: PluginCall) {
        val content = call.getString("content") ?: ""
        activity.runOnUiThread {
            editor?.setText(content)
        }
        call.resolve()
    }

    @PluginMethod
    fun setSelection(call: PluginCall) {
        val line = call.getInt("line") ?: 0
        val column = call.getInt("column") ?: 0
        activity.runOnUiThread {
            editor?.post {
                try {
                    editor?.setSelection(line, column)
                    editor?.ensureSelectionVisible()
                     android.widget.Toast.makeText(context, "SetSel to ${line+1}", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        call.resolve()
    }
    @PluginMethod
    fun redo(call: PluginCall) {
        activity.runOnUiThread {
            editor?.redo()
        }
        call.resolve()
    }

    @PluginMethod
    fun undo(call: PluginCall) {
        activity.runOnUiThread {
            editor?.undo()
        }
        call.resolve()
    }
}
