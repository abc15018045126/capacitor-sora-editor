package io.github.abc15018045126.sora.widget.component

import android.content.Context
import android.view.View
import android.widget.AdapterView
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


interface CompletionLayout {


    fun onApplyColorScheme(colorScheme: EditorColorScheme)


    fun setEditorCompletion(completion: EditorAutoCompletion)


    fun inflate(context: Context): View


    fun getCompletionList(): AdapterView<*>


    fun setLoading(loading: Boolean)


    fun ensureListPositionVisible(position: Int, incrementPixels: Int)


    fun setEnabledAnimation(enabledAnimation: Boolean) {

    }
}
