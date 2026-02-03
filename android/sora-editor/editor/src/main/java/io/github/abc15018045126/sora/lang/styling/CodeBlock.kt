package io.github.abc15018045126.sora.lang.styling

import java.util.Objects


class CodeBlock {


    @JvmField
    var startLine: Int = 0


    @JvmField
    var startColumn: Int = 0


    @JvmField
    var endLine: Int = 0


    @JvmField
    var endColumn: Int = 0


    @JvmField
    var toBottomOfEndLine: Boolean = false

    fun clear() {
        startLine = 0
        startColumn = 0
        endLine = 0
        endColumn = 0
        toBottomOfEndLine = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val codeBlock = other as CodeBlock
        return startLine == codeBlock.startLine &&
                startColumn == codeBlock.startColumn &&
                endLine == codeBlock.endLine &&
                endColumn == codeBlock.endColumn &&
                toBottomOfEndLine == codeBlock.toBottomOfEndLine
    }

    override fun hashCode(): Int {
        return Objects.hash(startLine, startColumn, endLine, endColumn, toBottomOfEndLine)
    }

    override fun toString(): String {
        return "BlockLine{" +
                "startLine=$startLine" +
                ", startColumn=$startColumn" +
                ", endLine=$endLine" +
                ", endColumn=$endColumn" +
                ", toBottomOfEndLine=$toBottomOfEndLine" +
                '}'
    }

    companion object {
        @JvmField
        val COMPARATOR_END = Comparator<CodeBlock> { a, b ->
            val res = Integer.compare(a.endLine, b.endLine)
            if (res == 0) {
                Integer.compare(a.endColumn, b.endColumn)
            } else {
                res
            }
        }

        @JvmField
        val COMPARATOR_START = Comparator<CodeBlock> { a, b ->
            val res = Integer.compare(a.startLine, b.startLine)
            if (res == 0) {
                Integer.compare(a.startColumn, b.startColumn)
            } else {
                res
            }
        }


        @JvmStatic
        fun binarySearchEndBlock(line: Int, blocks: List<CodeBlock>?): Int {
            if (blocks == null || blocks.isEmpty()) {
                return -1
            }

            var left = 0
            var right = blocks.size - 1
            val max = right

            while (left <= right) {
                var mid = left + (right - left) / 2
                if (mid < 0 || mid > max) {
                    return -1
                }

                var block = blocks[mid]
                if (block == null) {
                    var nonNullLeft = mid - 1
                    var nonNullRight = mid + 1

                    while (true) {
                        if (nonNullLeft < left && nonNullRight > right) {
                            return -1
                        } else if (nonNullLeft >= left && blocks[nonNullLeft] != null) {
                            mid = nonNullLeft
                            break
                        } else if (nonNullRight <= right && blocks[nonNullRight] != null) {
                            mid = nonNullRight
                            break
                        }
                        nonNullLeft--
                        nonNullRight++
                    }

                    block = blocks[mid]
                }

                val row = block!!.endLine
                when {
                    row > line -> right = mid - 1
                    row < line -> left = mid + 1
                    else -> {
                        left = mid
                        break
                    }
                }
            }

            if (left < 0 || left > max) {
                return -1
            }

            return left
        }
    }
}
