
package io.github.abc15018045126.sora.lang.styling.color

import android.graphics.Color
import androidx.annotation.ColorInt
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


class ConstColor : ResolvableColor {
    private val color: Int


    constructor(@ColorInt color: Int) {
        this.color = color
    }


    constructor(color: String) {
        this.color = Color.parseColor(color)
    }

    override fun resolve(colorScheme: EditorColorScheme): Int {
        return color
    }
}
