package io.github.abc15018045126.sora.lang.styling

import android.util.Log
import io.github.abc15018045126.sora.text.CharPosition

object StylesUtils {

    private const val LOG_TAG = "StylesUtils"


    @JvmStatic
    fun checkNoCompletion(styles: Styles?, pos: CharPosition): Boolean {
        val span = getSpanForPosition(styles, pos)
        return span == null || TextStyle.isNoCompletion(span.style)
    }


    @JvmStatic
    fun getSpanForPosition(styles: Styles?, pos: CharPosition): Span? {
        return getSpanForPositionImpl(styles, pos, 0)
    }


    @JvmStatic
    fun getFollowingSpanForPosition(styles: Styles?, pos: CharPosition): Span? {
        return getSpanForPositionImpl(styles, pos, 1)
    }

    private fun getSpanForPositionImpl(styles: Styles?, pos: CharPosition, spanIndexOffset: Int): Span? {
        val line = pos.line
        val column = pos.column

        if (styles == null) {
            return null
        }
        val spans = styles.spans ?: return null
        var ex: Exception? = null
        val reader = spans.read()
        try {
            reader.moveToLine(line)
            var index = reader.getSpanCount() - 1
            if (index == -1) {
                return null
            }
            for (i in 0 until reader.getSpanCount()) {
                if (reader.getSpanAt(i).column > column) {
                    index = i - 1
                    break
                }
            }
            index += spanIndexOffset
            if (index < 0 || index >= reader.getSpanCount()) {
                return null
            }
            return reader.getSpanAt(index)
        } catch (e: Exception) {

            ex = e
            return null
        } finally {
            try {
                reader.moveToLine(-1)
            } catch (e1: Exception) {
                if (ex != null) {
                    ex.addSuppressed(e1)
                } else {
                    Log.e(LOG_TAG, "failed to close $reader", e1)
                }
            }
            if (ex != null) {
                Log.e(LOG_TAG, "failed to get spans from $reader at $pos", ex)
            }
        }
    }
}
