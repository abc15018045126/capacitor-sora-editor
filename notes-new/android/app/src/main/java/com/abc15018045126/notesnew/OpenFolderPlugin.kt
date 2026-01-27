package com.abc15018045126.notesnew

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.io.File

@CapacitorPlugin(name = "OpenFolder")
class OpenFolderPlugin : Plugin() {

    @PluginMethod
    fun open(call: PluginCall) {
        try {
            val context = context
            
            // 获取公共的 Documents 目录
            val docFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val quickNotesFolder = File(docFolder, "Notes")
            
            if (!quickNotesFolder.exists()) {
                quickNotesFolder.mkdirs()
            }

            // 获取 FileProvider URI
            val contentUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", quickNotesFolder)
            
            // 尝试多种 Intent 协议
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
                call.resolve()
            } catch (e1: Exception) {
                try {
                    val intent2 = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "resource/folder")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent2)
                    call.resolve()
                } catch (e2: Exception) {
                    val intent3 = Intent(Intent.ACTION_GET_CONTENT).apply {
                        setDataAndType(contentUri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent3)
                    call.resolve()
                }
            }
        } catch (e: Exception) {
            call.reject("无法打开管理器: ${e.message}")
        }
    }

    @PluginMethod
    fun requestAllFilesAccess(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                call.resolve()
            } else {
                call.resolve()
            }
        } else {
            call.resolve()
        }
    }
}
