



package io.github.abc15018045126.sora.lang.styling.line


abstract class LineAnchorStyle(open var line: Int) : Comparable<LineAnchorStyle> {


    var customData: Any? = null

    override fun compareTo(other: LineAnchorStyle) = line.compareTo(other.line)

}
