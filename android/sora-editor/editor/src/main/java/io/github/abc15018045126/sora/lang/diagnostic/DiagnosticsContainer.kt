package io.github.abc15018045126.sora.lang.diagnostic


class DiagnosticsContainer @JvmOverloads constructor(
    private val shiftEnabled: Boolean = true
) {

    private val regions = mutableListOf<DiagnosticRegion>()


    @Synchronized
    fun addDiagnostics(regions: Collection<DiagnosticRegion>) {
        this.regions.addAll(regions)
    }


    @Synchronized
    fun addDiagnostic(diagnostic: DiagnosticRegion) {
        regions.add(diagnostic)
    }


    @Synchronized
    fun queryInRegion(result: MutableList<DiagnosticRegion>, startIndex: Int, endIndex: Int) {
        for (region in regions) {
            if (region.endIndex > startIndex && region.startIndex <= endIndex) {
                result.add(region)
            }
        }
    }

    @Synchronized
    fun shiftOnInsert(insertStart: Int, insertEnd: Int) {
        if (!shiftEnabled) {
            return
        }
        val length = insertEnd - insertStart
        for (region in regions) {

            if (region.startIndex <= insertStart && region.endIndex >= insertStart) {
                region.endIndex += length
            }

            if (region.startIndex > insertStart) {
                region.startIndex += length
                region.endIndex += length
            }
        }
    }

    @Synchronized
    fun shiftOnDelete(deleteStart: Int, deleteEnd: Int) {
        if (!shiftEnabled) {
            return
        }
        val length = deleteEnd - deleteStart
        val garbage = mutableListOf<DiagnosticRegion>()
        for (region in regions) {

            val sharedStart = maxOf(deleteStart, region.startIndex)
            val sharedEnd = minOf(deleteEnd, region.endIndex)
            if (sharedEnd <= sharedStart) {

                if (region.startIndex >= deleteEnd) {

                    region.startIndex -= length
                    region.endIndex -= length
                }
            } else {

                val sharedLength = sharedEnd - sharedStart
                region.endIndex -= sharedLength
                if (region.startIndex > deleteStart) {

                    val shiftLeftCount = region.startIndex - deleteStart
                    region.startIndex -= shiftLeftCount
                    region.endIndex -= shiftLeftCount
                }

                if (region.startIndex == region.endIndex) {
                    garbage.add(region)
                }
            }
        }
        regions.removeAll(garbage)
    }


    @Synchronized
    fun reset() {
        regions.clear()
    }
}
