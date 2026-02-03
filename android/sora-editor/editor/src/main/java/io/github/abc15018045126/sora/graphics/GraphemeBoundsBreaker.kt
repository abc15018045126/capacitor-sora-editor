package io.github.abc15018045126.sora.graphics


object GraphemeBoundsBreaker {


    @JvmStatic
    fun findGraphemeBreakPoint(advances: FloatArray, length: Int, width: Int, start: Int): Int {
        var currentWidth = 0f
        var next = start
        while (next < length) {
            if (advances[next] == 0f) {

                next++
                continue
            }
            if (currentWidth + advances[next] > width) {
                break
            }
            currentWidth += advances[next]
            next++
        }
        return next
    }
}
