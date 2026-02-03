package io.github.abc15018045126.sora.lang.completion

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import io.github.abc15018045126.sora.text.TextUtils


class MatchHelper {


    @JvmField
    var highlightColor: Int = 0xff3f51b5.toInt()


    @JvmField
    var ignoreCase: Boolean = false


    @JvmField
    var matchFirstCase: Boolean = false

    fun startsWith(name: CharSequence, pattern: CharSequence): Spanned? {
        return startsWith(name, pattern, matchFirstCase, ignoreCase)
    }

    fun startsWith(
        name: CharSequence,
        pattern: CharSequence,
        matchFirstCase: Boolean,
        ignoreCase: Boolean
    ): Spanned? {
        if (name.length >= pattern.length) {
            val len = pattern.length
            var matches = true
            for (i in 0 until len) {
                val a = name[i]
                val b = pattern[i]
                if (!(a == b || ((ignoreCase && (i != 0 || !matchFirstCase)) && a.lowercaseChar() == b.lowercaseChar()))) {
                    matches = false
                    break
                }
            }
            if (matches) {
                val spanned = SpannableString(name)
                spanned.setSpan(ForegroundColorSpan(highlightColor), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return spanned
            }
        }
        return null
    }

    fun contains(name: CharSequence, pattern: CharSequence): Spanned? {
        return contains(name, pattern, ignoreCase)
    }

    fun contains(name: CharSequence, pattern: CharSequence, ignoreCase: Boolean): Spanned? {
        val index = TextUtils.indexOf(name, pattern, ignoreCase, 0)
        if (index != -1) {
            val spanned = SpannableString(name)
            spanned.setSpan(
                ForegroundColorSpan(highlightColor),
                index,
                index + pattern.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spanned
        }
        return null
    }


    fun commonSub(name: CharSequence, pattern: CharSequence): Spanned? {
        return commonSub(name, pattern, ignoreCase)
    }


    fun commonSub(name: CharSequence, pattern: CharSequence, ignoreCase: Boolean): Spanned? {
        if (name.length >= pattern.length) {
            var spanned: SpannableString? = null
            val len = pattern.length
            var j = 0
            for (i in 0 until len) {
                val p = pattern[i]
                var matched = false
                while (j < name.length && !matched) {
                    val s = name[j]
                    if (s == p || (ignoreCase && s.lowercaseChar() == p.lowercaseChar())) {
                        matched = true
                        if (spanned == null) {
                            spanned = SpannableString(name)
                        }
                        spanned.setSpan(
                            ForegroundColorSpan(highlightColor),
                            j,
                            j + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    j++
                }
                if (!matched) {
                    return null
                }
            }
            return spanned
        }
        return null
    }
}
