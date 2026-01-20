package com.abc15018045126.notes;

import android.content.Intent;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SoraEditor")
public class SoraEditorPlugin extends Plugin {

    @PluginMethod
    public void open(PluginCall call) {
        String path = call.getString("path");
        String content = call.getString("content");
        String title = call.getString("title");

        Intent intent = new Intent(getContext(), SoraEditorActivity.class);
        intent.putExtra("path", path);
        
        // 如果内容太大，就在 Intent 中传 null，让 Activity 自己去读文件
        if (content != null && content.length() > 500000) {
            intent.putExtra("content", (String)null);
        } else {
            intent.putExtra("content", content);
        }
        
        intent.putExtra("title", title);

        startActivityForResult(call, intent, "editorCallback");
    }

    @ActivityCallback
    private void editorCallback(PluginCall call, ActivityResult result) {
        if (call == null) return;

        JSObject ret = new JSObject();
        if (result.getResultCode() == android.app.Activity.RESULT_OK) {
            // 注意：不再传回内容，直接让前端 reloadNotes 读文件
            ret.put("success", true);
            call.resolve(ret);
        } else {
            call.reject("Cancelled or failed");
        }
    }
}
