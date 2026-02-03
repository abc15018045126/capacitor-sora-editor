package io.github.abc15018045126.sora.lang.completion

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.widget.component.EditorAutoCompletion


object CompletionHelper {


    @JvmStatic
    fun computePrefix(ref: ContentReference, pos: CharPosition, checker: PrefixChecker): String {
        var begin = pos.column
        val line = ref.getLine(pos.line)
        while (begin > 0) {
            if (!checker.check(line[begin - 1])) {
                break
            }
            begin--
        }
        return line.substring(begin, pos.column)
    }


    @JvmStatic
    fun checkCancelled(): Boolean {
        val thread = Thread.currentThread()
        return if (thread is EditorAutoCompletion.CompletionThread) {
            thread.isCancelled
        } else {
            false
        }
    }

    fun interface PrefixChecker {
        fun check(ch: Char): Boolean
    }
}
