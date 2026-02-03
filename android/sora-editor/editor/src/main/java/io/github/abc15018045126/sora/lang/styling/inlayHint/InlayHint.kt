

package io.github.abc15018045126.sora.lang.styling.inlayHint

import io.github.abc15018045126.sora.lang.styling.util.PointAnchoredObject


enum class CharacterSide {
    LEFT,
    RIGHT
}

open class InlayHint(
    override var line: Int,
    override var column: Int,
    val type: String,
    val displaySide: CharacterSide = CharacterSide.LEFT
) : PointAnchoredObject {

    init {
        if (line < 0 || column < 0) {
            throw IllegalArgumentException("negative number")
        }
    }

}
