
@file:JvmName("Filters")

package io.github.abc15018045126.sora.lang.completion

import io.github.abc15018045126.sora.util.CharCode
import io.github.abc15018045126.sora.util.MyCharacter




private const val MAX_LEN = 32

private data class Scratch(
    val minWordMatchPosArray: IntArray = IntArray(2 * MAX_LEN),
    val maxWordMatchPosArray: IntArray = IntArray(2 * MAX_LEN),
    val diag: Array<IntArray> = Array(MAX_LEN) { IntArray(MAX_LEN) },
    val table: Array<IntArray> = Array(MAX_LEN) { IntArray(MAX_LEN) },
    val arrows: Array<IntArray> = Array(MAX_LEN) { IntArray(MAX_LEN) },
) {
    fun reset() {
        minWordMatchPosArray.fill(0)
        maxWordMatchPosArray.fill(0)
        for (row in 0 until MAX_LEN) {
            diag[row].fill(0)
            table[row].fill(0)
            arrows[row].fill(0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scratch

        if (!minWordMatchPosArray.contentEquals(other.minWordMatchPosArray)) return false
        if (!maxWordMatchPosArray.contentEquals(other.maxWordMatchPosArray)) return false
        if (!diag.contentDeepEquals(other.diag)) return false
        if (!table.contentDeepEquals(other.table)) return false
        if (!arrows.contentDeepEquals(other.arrows)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minWordMatchPosArray.contentHashCode()
        result = 31 * result + maxWordMatchPosArray.contentHashCode()
        result = 31 * result + diag.contentDeepHashCode()
        result = 31 * result + table.contentDeepHashCode()
        result = 31 * result + arrows.contentDeepHashCode()
        return result
    }
}

private val scratchLocal =
    object : ThreadLocal<Scratch>() {
        override fun initialValue(): Scratch {
            return Scratch()
        }
    };

private inline fun <T> withScratch(block: Scratch.() -> T): T {
    val scratch = scratchLocal.get()
    scratch ?: error("Not Found Scratch")
    scratch.reset()
    return scratch.block()
}

private fun Scratch.isPatternInWord(
    patternLow: String,
    patternPos: Int,
    patternLen: Int,
    wordLow: String,
    wordPos: Int,
    wordLen: Int,
    fillMinWordPosArr: Boolean
): Boolean {
    var patternPosMut = patternPos
    var wordPosMut = wordPos
    while (patternPosMut < patternLen && wordPosMut < wordLen) {
        if (patternLow[patternPosMut] == wordLow[wordPosMut]) {
            if (fillMinWordPosArr) {
                minWordMatchPosArray[patternPosMut] = wordPosMut
            }
            patternPosMut += 1
        }
        wordPosMut += 1
    }
    return patternPosMut == patternLen
}

private fun Scratch.fillInMaxWordMatchPos(
    patternLen: Int,
    wordLen: Int,
    patternStart: Int,
    wordStart: Int,
    patternLow: String,
    wordLow: String
) {
    var patternPos = patternLen - 1
    var wordPos = wordLen - 1
    while (patternPos >= patternStart && wordPos >= wordStart) {
        if (patternLow[patternPos] == wordLow[wordPos]) {
            maxWordMatchPosArray[patternPos] = wordPos
            patternPos--
        }
        wordPos--
    }
}


object Arrow {
    val Diag = 1
    val Left = 2
    val LeftLeft = 3
}

@JvmOverloads
fun isPatternInWord(
    patternLow: String,
    patternPos: Int,
    patternLen: Int,
    wordLow: String,
    wordPos: Int,
    wordLen: Int,
    fillMinWordPosArr: Boolean = false
): Boolean {
    return withScratch {
        this.isPatternInWord(
            patternLow,
            patternPos,
            patternLen,
            wordLow,
            wordPos,
            wordLen,
            fillMinWordPosArr
        )
    }
}


internal fun fillInMaxWordMatchPos(
    patternLen: Int,
    wordLen: Int,
    patternStart: Int,
    wordStart: Int,
    patternLow: String,
    wordLow: String
) {
    withScratch {
        this.fillInMaxWordMatchPos(
            patternLen,
            wordLen,
            patternStart,
            wordStart,
            patternLow,
            wordLow
        )
    }
}

fun isUpperCaseAtPos(pos: Int, word: String, wordLow: String): Boolean {
    return word[pos] != wordLow[pos]
}


fun isSeparatorAtPos(value: String, index: Int): Boolean {
    if (index < 0 || index >= value.length) {
        return false
    }
    return when (val code = java.lang.Character.codePointAt(value, index)) {
        CharCode.Underline,
        CharCode.Dash,
        CharCode.Period,
        CharCode.Space,
        CharCode.Slash,
        CharCode.Backslash,
        CharCode.SingleQuote,
        CharCode.DoubleQuote,
        CharCode.Colon,
        CharCode.DollarSign,
        CharCode.LessThan,
        CharCode.GreaterThan,
        CharCode.OpenParen,
        CharCode.CloseParen,
        CharCode.OpenSquareBracket,
        CharCode.CloseSquareBracket,
        CharCode.OpenCurlyBrace,
        CharCode.CloseCurlyBrace -> true

        else -> MyCharacter.couldBeEmoji(code)

    }
}

fun isWhitespaceAtPos(value: String, index: Int): Boolean {
    if (index < 0 || index >= value.length) {
        return false
    }

    return when (val code = value[index].code) {
        CharCode.Space,
        CharCode.Tab -> true

        else -> false

    }
}


class FuzzyScore(
    var score: Int,
    val wordStart: Int,
    val matches: MutableList<Int> = mutableListOf()
) {

    companion object {

        @JvmStatic
        val default: FuzzyScore = FuzzyScore(-100, 0)

        @JvmStatic
        fun isDefault(score: FuzzyScore?): Boolean {
            return score?.score == -100 && score.wordStart == 0
        }
    }

}

data class FuzzyScoreOptions(
    val firstMatchCanBeWeak: Boolean,
    val boostFullMatch: Boolean,
) {

    companion object {
        @JvmStatic
        val default = FuzzyScoreOptions(boostFullMatch = true, firstMatchCanBeWeak = true)
    }

}

fun interface FuzzyScorer {
    fun calculateScore(
        pattern: String,
        lowPattern: String,
        patternPos: Int,
        word: String,
        lowWord: String,
        wordPos: Int,
        options: FuzzyScoreOptions?
    ): FuzzyScore?
}

fun anyScore(
    pattern: String,
    lowPattern: String,
    patternPos: Int,
    word: String,
    lowWord: String,
    wordPos: Int,
): FuzzyScore {
    val max = 13.coerceAtMost(pattern.length)
    var patternPosMut = patternPos
    while (patternPosMut < max) {
        val result = fuzzyScore(
            pattern, lowPattern, patternPosMut, word, lowWord, wordPos,
            FuzzyScoreOptions(firstMatchCanBeWeak = false, boostFullMatch = true)
        )
        if (result != null) {
            return result
        }
        patternPosMut++
    }

    return FuzzyScore(0, wordPos)
}


@JvmOverloads
fun fuzzyScore(
    pattern: String,
    patternLow: String,
    patternStart: Int,
    word: String,
    wordLow: String,
    wordStart: Int,
    options: FuzzyScoreOptions? = FuzzyScoreOptions.default
): FuzzyScore? {

    val patternLen = if (pattern.length > MAX_LEN) MAX_LEN else pattern.length
    val wordLen = if (word.length > MAX_LEN - 1) MAX_LEN - 1 else word.length

    if (patternStart >= patternLen || wordStart >= wordLen || (patternLen - patternStart) > (wordLen - wordStart)) {
        return null
    }

    return withScratch {
        val minWordPositions = this.minWordMatchPosArray
        val maxWordPositions = this.maxWordMatchPosArray
        val diagMatrix = this.diag
        val tableMatrix = this.table
        val arrowsMatrix = this.arrows




        if (!this.isPatternInWord(
                patternLow,
                patternStart,
                patternLen,
                wordLow,
                wordStart,
                wordLen,
                true
            )
        ) {
            return@withScratch null
        }



        this.fillInMaxWordMatchPos(
            patternLen,
            wordLen,
            patternStart,
            wordStart,
            patternLow,
            wordLow
        )

        var row = 1
        var column = 1
        var patternPos = patternStart
        var wordPos: Int

        val hasStrongFirstMatch = booleanArrayOf(false)


        while (patternPos < patternLen) {


            val minWordMatchPos = minWordPositions[patternPos]
            val maxWordMatchPos = maxWordPositions[patternPos]
            val nextMaxWordMatchPos =
                if (patternPos + 1 < patternLen) maxWordPositions[patternPos + 1] else wordLen

            column = minWordMatchPos - wordStart + 1
            wordPos = minWordMatchPos

            while (wordPos < nextMaxWordMatchPos) {

                var score = Int.MIN_VALUE
                var canComeDiag = false

                if (wordPos <= maxWordMatchPos) {
                    score = doScore(
                        pattern, patternLow, patternPos, patternStart,
                        word, wordLow, wordPos, wordLen, wordStart,
                        diagMatrix[row - 1][column - 1] == 0,
                        hasStrongFirstMatch
                    )
                }

                var diagScore = 0
                if (score != Int.MIN_VALUE) {
                    canComeDiag = true
                    diagScore = score + tableMatrix[row - 1][column - 1]
                }

                val canComeLeft = wordPos > minWordMatchPos
                val leftScore =
                    if (canComeLeft) tableMatrix[row][column - 1] + (if (diagMatrix[row][column - 1] > 0) -5 else 0) else 0

                val canComeLeftLeft =
                    wordPos > minWordMatchPos + 1 && diagMatrix[row][column - 1] > 0
                val leftLeftScore =
                    if (canComeLeftLeft) tableMatrix[row][column - 2] + (if (diagMatrix[row][column - 2] > 0) -5 else 0) else 0

                if (canComeLeftLeft && (!canComeLeft || leftLeftScore >= leftScore) && (!canComeDiag || leftLeftScore >= diagScore)) {

                    tableMatrix[row][column] = leftLeftScore
                    arrowsMatrix[row][column] = Arrow.LeftLeft
                    diagMatrix[row][column] = 0
                } else if (canComeLeft && (!canComeDiag || leftScore >= diagScore)) {

                    tableMatrix[row][column] = leftScore
                    arrowsMatrix[row][column] = Arrow.Left
                    diagMatrix[row][column] = 0
                } else if (canComeDiag) {
                    tableMatrix[row][column] = diagScore
                    arrowsMatrix[row][column] = Arrow.Diag
                    diagMatrix[row][column] = diagMatrix[row - 1][column - 1] + 1
                } else {
                    error("not possible")
                }
                column++
                wordPos++
            }
            row++
            patternPos++
        }

        if (!hasStrongFirstMatch[0] && options?.firstMatchCanBeWeak == false) {
            return@withScratch null
        }

        row--
        column--

        val result = FuzzyScore(tableMatrix[row][column], wordStart)

        var backwardsDiagLength = 0
        var maxMatchColumn = 0

        while (row >= 1) {

            var diagColumn = column
            do {
                val arrow = arrowsMatrix[row][diagColumn]
                if (arrow == Arrow.LeftLeft) {
                    diagColumn -= 2
                } else if (arrow == Arrow.Left) {
                    diagColumn -= 1
                } else {

                    break
                }
            } while (diagColumn >= 1)


            if (
                backwardsDiagLength > 1
                && patternLow[patternStart + row - 1] == wordLow[wordStart + column - 1]
                && !isUpperCaseAtPos(
                    diagColumn + wordStart - 1,
                    word,
                    wordLow
                )
                && backwardsDiagLength + 1 > diagMatrix[row][diagColumn]
            ) {
                diagColumn = column
            }

            if (diagColumn == column) {

                backwardsDiagLength++
            } else {
                backwardsDiagLength = 1
            }

            if (maxMatchColumn == 0) {

                maxMatchColumn = diagColumn
            }

            row--
            column = diagColumn - 1
            result.matches.add(column)
        }

        if (wordLen == patternLen && options?.boostFullMatch == true) {


            result.score += 2
        }


        val skippedCharsCount = maxMatchColumn - patternLen
        result.score -= skippedCharsCount

        return@withScratch result
    }
}


internal fun doScore(
    pattern: String, patternLow: String, patternPos: Int, patternStart: Int,
    word: String, wordLow: String, wordPos: Int, wordLen: Int, wordStart: Int,
    newMatchStart: Boolean,
    outFirstMatchStrong: BooleanArray
): Int {
    if (patternLow[patternPos] != wordLow[wordPos]) {
        return Int.MIN_VALUE
    }

    var score = 1
    var isGapLocation = false
    if (wordPos == patternPos - patternStart) {


        score = if (pattern[patternPos] == word[wordPos]) 7 else 5

    } else if (isUpperCaseAtPos(wordPos, word, wordLow) && (wordPos == 0 || !isUpperCaseAtPos(
            wordPos - 1,
            word,
            wordLow
        ))
    ) {


        score = if (pattern[patternPos] == word[wordPos]) 7 else 5
        isGapLocation = true

    } else if (isSeparatorAtPos(wordLow, wordPos) && (wordPos == 0 || !isSeparatorAtPos(
            wordLow,
            wordPos - 1
        ))
    ) {


        score = 5
    } else if (isSeparatorAtPos(wordLow, wordPos - 1) || isWhitespaceAtPos(wordLow, wordPos - 1)) {


        score = 5
        isGapLocation = true
    }

    if (score > 1 && patternPos == patternStart) {
        outFirstMatchStrong[0] = true
    }

    if (!isGapLocation) {
        isGapLocation = isUpperCaseAtPos(wordPos, word, wordLow) || isSeparatorAtPos(
            wordLow,
            wordPos - 1
        ) || isWhitespaceAtPos(wordLow, wordPos - 1)
    }


    if (patternPos == patternStart) {
        if (wordPos > wordStart) {


            score -= if (isGapLocation) 3 else 5
        }
    } else {
        if (newMatchStart) {

            score += if (isGapLocation) 2 else 0
        } else {

            score += if (isGapLocation) 0 else 1
        }
    }

    if (wordPos + 1 == wordLen) {


        score -= if (isGapLocation) 3 else 5
    }

    return score
}


fun fuzzyScoreGracefulAggressive(
    pattern: String,
    lowPattern: String,
    patternPos: Int,
    word: String,
    lowWord: String,
    wordPos: Int,
    options: FuzzyScoreOptions?
): FuzzyScore? {
    return fuzzyScoreWithPermutations(
        pattern,
        lowPattern,
        patternPos,
        word,
        lowWord,
        wordPos,
        true,
        options
    )
}

fun fuzzyScoreGraceful(
    pattern: String,
    lowPattern: String,
    patternPos: Int,
    word: String,
    lowWord: String,
    wordPos: Int,
    options: FuzzyScoreOptions?
): FuzzyScore? {
    return fuzzyScoreWithPermutations(
        pattern,
        lowPattern,
        patternPos,
        word,
        lowWord,
        wordPos,
        false,
        options
    )
}

internal fun fuzzyScoreWithPermutations(
    pattern: String,
    lowPattern: String,
    patternPos: Int,
    word: String,
    lowWord: String,
    wordPos: Int,
    aggressive: Boolean,
    options: FuzzyScoreOptions?
): FuzzyScore? {
    var top = fuzzyScore(
        pattern,
        lowPattern,
        patternPos,
        word,
        lowWord,
        wordPos,
        options ?: FuzzyScoreOptions.default
    )

    if (top != null && !aggressive) {



        return top
    }

    if (pattern.length >= 3) {




        val tries = 7.coerceAtMost(pattern.length - 1)

        var movingPatternPos = patternPos + 1

        while (movingPatternPos < tries) {
            val newPattern = nextTypoPermutation(pattern, movingPatternPos)
            if (newPattern != null) {
                val candidate = fuzzyScore(
                    newPattern,
                    newPattern.lowercase(),
                    patternPos,
                    word,
                    lowWord,
                    wordPos,
                    options ?: FuzzyScoreOptions.default
                )
                if (candidate != null) {
                    candidate.score -= 3
                    if (top == null || candidate.score > top.score) {
                        top = candidate
                    }
                }
            }
            movingPatternPos++
        }
    }

    return top
}

internal fun nextTypoPermutation(pattern: String, patternPos: Int): String? {
    if (patternPos + 1 >= pattern.length) {
        return null
    }

    val swap1 = pattern[patternPos]
    val swap2 = pattern[patternPos + 1]

    if (swap1 == swap2) {
        return null
    }

    return pattern.take(patternPos) + swap2 + swap1 + pattern.substring(patternPos + 2)
}


