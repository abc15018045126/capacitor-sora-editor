package io.github.abc15018045126.sora.lang.diagnostic


class DiagnosticRegion @JvmOverloads constructor(

    @JvmField var startIndex: Int,

    @JvmField var endIndex: Int,

    @JvmField var severity: Short,

    @JvmField var id: Long = 0,

    @JvmField var detail: DiagnosticDetail? = null
) : Comparable<DiagnosticRegion> {

    override fun compareTo(other: DiagnosticRegion): Int {
        var cmp = startIndex.compareTo(other.startIndex)
        if (cmp == 0) {
            cmp = endIndex.compareTo(other.endIndex)
        }
        if (cmp == 0) {
            cmp = severity.compareTo(other.severity)
        }
        if (cmp == 0) {
            cmp = id.compareTo(other.id)
        }
        return cmp
    }

    companion object {
        const val SEVERITY_NONE: Short = 0
        const val SEVERITY_TYPO: Short = 1
        const val SEVERITY_WARNING: Short = 2
        const val SEVERITY_ERROR: Short = 3
    }
}
