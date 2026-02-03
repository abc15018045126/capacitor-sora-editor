package io.github.abc15018045126.sora

import android.content.Context
import android.util.SparseIntArray


object I18nConfig {

    private val mapping = SparseIntArray()


    @JvmStatic
    fun mapTo(originalResId: Int, newResId: Int) {
        mapping.put(originalResId, newResId)
    }


    @JvmStatic
    fun getResourceId(resId: Int): Int {
        val newResource = mapping.get(resId)
        return if (newResource == 0) resId else newResource
    }


    @JvmStatic
    fun getString(context: Context, resId: Int): String {
        return context.getString(getResourceId(resId))
    }
}
