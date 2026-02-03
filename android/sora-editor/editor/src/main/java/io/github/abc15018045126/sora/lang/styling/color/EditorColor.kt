
package io.github.abc15018045126.sora.lang.styling.color

import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


class EditorColor
 (private val colorId: Int) : ResolvableColor {
    override fun resolve(colorScheme: EditorColorScheme): Int {
        return colorScheme.getColor(colorId)
    }
}
