package com.abc15018045126.notes

import android.content.Intent
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.abc15018045126.notes.compose.ComposeEditorActivity

@CapacitorPlugin(name = "ComposeEditor")
class ComposeEditorPlugin : Plugin() {

    @PluginMethod
    fun openEditor(call: PluginCall) {
        val filePath = call.getString("filePath") ?: ""
        
        android.util.Log.d("ComposeEditorPlugin", "openEditor called with filePath: $filePath")
        
        if (filePath.isEmpty()) {
            android.util.Log.e("ComposeEditorPlugin", "File path is empty")
            call.reject("File path is required")
            return
        }

        activity.runOnUiThread {
            val intent = Intent(context, ComposeEditorActivity::class.java)
            intent.putExtra("FILE_PATH", filePath)
            android.util.Log.d("ComposeEditorPlugin", "Starting ComposeEditorActivity")
            activity.startActivity(intent)
            call.resolve()
        }
    }
}
