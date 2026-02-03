

package io.github.abc15018045126.sora.lang.styling.line


class LineStyles(override var line: Int) : LineAnchorStyle(line) {

    private val styles = mutableListOf<LineAnchorStyle>()


    fun addStyle(style: LineAnchorStyle): Int {
        if (style is LineStyles) {
            throw IllegalArgumentException("Can not add LineStyles object")
        }
        if (style.line != line) {
            throw IllegalArgumentException("target line differs from this object")
        }
        var result = 1
        if (findOne(style.javaClass) != null) {
            eraseStyle(style.javaClass)
            result = 0
        }
        styles.add(style)
        return result
    }


    fun <T : LineAnchorStyle> eraseStyle(type: Class<T>): Int {
        val all = findAll(type)
        styles.removeAll(all)
        return all.size
    }

    fun updateElements() {
        styles.forEach {
            it.line = line
        }
    }

    fun getElementCount() = styles.size

    fun getElementAt(index: Int) = styles[index]

    fun <T : LineAnchorStyle> findOne(type: Class<T>): T? {
        return styles.find { type.isInstance(it) } as T?
    }

    fun <T : LineAnchorStyle> findAll(type: Class<T>) = styles.filter { type.isInstance(it) }

    fun typedElementCount(type: Class<Any>): Int {
        return styles.filter { type.isInstance(it) }.size
    }

}
