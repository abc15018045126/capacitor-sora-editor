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
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

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
        
        activity.runOnUiThread {
            if (editor == null) {
                editor = CodeEditor(context).apply {
                    setTextSize(fontSize)
                    setTypefaceText(Typeface.MONOSPACE)
                    isLineNumberEnabled = true
                }
                
                try {
                    // Try to load a theme (optional)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                activity.addContentView(editor, params)
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
        }
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
            editor?.setSelection(line, column)
        }
        call.resolve()
    }
}
