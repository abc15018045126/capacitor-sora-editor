

package io.github.abc15018045126.sora.graphics.inlayHint


fun interface InlayHintRendererProvider {

    fun getInlayHintRendererForType(type: String): InlayHintRenderer?

}
