package io.github.abc15018045126.sora.lang.styling


object BlocksUpdater {


    @JvmStatic
    fun update(blocks: MutableList<CodeBlock>, restrict: Int, delta: Int) {
        if (delta == 0) {
            return
        }
        val itr = blocks.iterator()
        while (itr.hasNext()) {
            val block = itr.next()
            if (block.startLine >= restrict) {
                block.startLine += delta
            }
            if (block.endLine >= restrict) {
                block.endLine += delta
            }
            if (block.startLine >= block.endLine) {
                itr.remove()
            }
        }
    }
}
