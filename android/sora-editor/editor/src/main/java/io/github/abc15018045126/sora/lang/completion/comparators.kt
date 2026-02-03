
@file:JvmName("Comparators")


package io.github.abc15018045126.sora.lang.completion

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.util.CharCode
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

private fun CharSequence?.asString(): String {
    return if (this == null) " " else this as? String ?: this.toString()
}

fun defaultComparator(a: CompletionItem, b: CompletionItem): Int {

    val p1Score = (a.extra as? SortedCompletionItem)?.score?.score ?: 0
    val p2Score = (b.extra as? SortedCompletionItem)?.score?.score ?: 0


    if (p1Score < p2Score) {
        return 1;
    } else if (p1Score > p2Score) {
        return -1;
    }

    var p1 = a.sortText.asString()
    var p2 = b.sortText.asString()



    if (p1 < p2) {
        return -1;
    } else if (p1 > p2) {
        return 1;
    }

    p1 = a.label.asString()
    p2 = b.label.asString()


    if (p1 < p2) {
        return -1;
    } else if (p1 > p2) {
        return 1;
    }



    val kind = (b.kind?.value ?: 0) - (a.kind?.value ?: 0)

    return kind
}

fun snippetUpComparator(a: CompletionItem, b: CompletionItem): Int {
    if (a.kind != b.kind) {
        if (a.kind == CompletionItemKind.Snippet) {
            return 1;
        } else if (b.kind == CompletionItemKind.Snippet) {
            return -1;
        }
    }
    return defaultComparator(a, b);
}


fun filterCompletionItems(
    source: ContentReference,
    cursorPosition: CharPosition,
    completionItemList: Collection<CompletionItem>
): List<CompletionItem> {
    val result = mutableListOf<CompletionItem>()

    source.validateAccess()

    val sourceLine = source.reference.getLine(cursorPosition.line)

    var word = ""
    var wordLow = ""





    val scoreFn = FuzzyScorer { pattern,
                                lowPattern,
                                patternPos,
                                wordText,
                                lowWord,
                                wordPos,
                                options ->
        if (sourceLine.length > 2000) {
            fuzzyScore(pattern, lowPattern, patternPos, wordText, lowWord, wordPos, options)
        } else {
            fuzzyScoreGracefulAggressive(
                pattern,
                lowPattern,
                patternPos,
                wordText,
                lowWord,
                wordPos,
                options
            )
        }

    }

    for (originItem in completionItemList) {
        source.validateAccess()

        val overwriteBefore = originItem.prefixLength
        val wordLen = overwriteBefore
        if (word.length != wordLen) {
            word = if (wordLen == 0) "" else sourceLine.substring(
                cursorPosition.column - wordLen,
                cursorPosition.column
            )
            wordLow = word.lowercase()
        }


        val item = SortedCompletionItem(originItem, FuzzyScore.default)






        if (overwriteBefore == 0) {





            item.score = FuzzyScore.default
        } else {


            var wordPos = 0;
            while (wordPos < overwriteBefore) {
                val ch = word[wordPos].code
                if (ch == CharCode.Space || ch == CharCode.Tab) {
                    wordPos += 1;
                } else {
                    break;
                }
            }

            val filterText = originItem.filterText

            if (wordPos >= overwriteBefore) {


                item.score = FuzzyScore.default;
            } else if (filterText?.isNotEmpty() == true) {





                val match = scoreFn.calculateScore(
                    word,
                    wordLow,
                    wordPos,
                    filterText.asString(),
                    filterText.asString().lowercase(),
                    0,
                    FuzzyScoreOptions.default
                ) ?: continue;


                if (filterText.equals(originItem.label.toString(), ignoreCase = true)) {

                    item.score = match;
                } else {


                    val labelMatch = anyScore(
                        word,
                        wordLow,
                        wordPos,
                        originItem.label.asString(),
                        originItem.label.asString().lowercase(),
                        0
                    )
                    item.score = labelMatch
                    labelMatch.matches[0] = match.matches[0]
                }

            } else {

                val match = scoreFn.calculateScore(
                    word,
                    wordLow,
                    wordPos,
                    originItem.label.asString(),
                    originItem.label.asString().lowercase(),
                    0,
                    FuzzyScoreOptions.default
                ) ?: continue;

                item.score = match;
            }

            originItem.extra = item

        }

        result.add(originItem)
    }

    return result
}

fun createCompletionItemComparator(completionItemList: Collection<CompletionItem>): Comparator<CompletionItem> {
    if (completionItemList.isNotEmpty() && completionItemList.first().extra != null && completionItemList.first().extra !is SortedCompletionItem) {
        throw IllegalArgumentException("The completionItemList must run through the filterCompletionItems() method first")
    }


    return Comparator { o1, o2 ->
        snippetUpComparator(o1, o2)
    }
}


@Deprecated("Use filterCompletionItems and createCompletionItemComparator instead")
fun getCompletionItemComparator(
    source: ContentReference,
    cursorPosition: CharPosition,
    completionItemList: Collection<CompletionItem>
): Comparator<CompletionItem> {

    filterCompletionItems(source, cursorPosition, completionItemList)

    return createCompletionItemComparator(completionItemList)
}


fun List<CompletionItem>.highlightMatchLabel(colorSchema: EditorColorScheme?): List<CompletionItem> {
    val notNullColorScheme = colorSchema ?: EditorColorScheme.getDefault()
    val matchedColor = notNullColorScheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_MATCHED)
    forEach { item ->
        val extra = item.extra
        if (extra == null || extra !is SortedCompletionItem) {
            return@forEach
        }


        if (item.label is Spannable) {
            return@forEach
        }


        val score = extra.score
        val spannable = SpannableString(item.label)

        for (index in score.matches.indices.reversed()) {
            val matchIndex = score.matches[index]


            if (matchIndex < 0 || matchIndex >= spannable.length) continue

            val end = (matchIndex + 1).coerceAtMost(spannable.length)
            if (end <= matchIndex) continue

            try {
                spannable.setSpan(
                    ForegroundColorSpan(matchedColor),
                    matchIndex,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        item.label = spannable

    }
    return this
}

data class SortedCompletionItem(
    val completionItem: CompletionItem,
    var score: FuzzyScore
)

