package io.github.abc15018045126.sora.lang.completion

import android.os.Build
import android.os.Bundle
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.text.TextUtils
import io.github.abc15018045126.sora.util.MutableInt
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class IdentifierAutoComplete {

    companion object {

        @Deprecated("")
        private val COMPARATOR = Comparator<CompletionItem> { p1, p2 ->
            val cmp1 = asString(p1.desc).compareTo(asString(p2.desc))
            if (cmp1 < 0) {
                return@Comparator 1
            } else if (cmp1 > 0) {
                return@Comparator -1
            }
            asString(p1.label).compareTo(asString(p2.label))
        }

        private fun asString(str: CharSequence?): String {
            return if (str is String) str else str.toString()
        }
    }

    private var keywords: Array<String>? = null
    private var keywordsAreLowCase: Boolean = false
    private var keywordMap: Map<String, Any>? = null

    constructor()

    constructor(keywords: Array<String>) {
        setKeywords(keywords, true)
    }

    fun setKeywords(keywords: Array<String>?, lowCase: Boolean) {
        this.keywords = keywords
        keywordsAreLowCase = lowCase
        val map = HashMap<String, Any>()
        if (keywords != null) {
            for (keyword in keywords) {
                map[keyword] = true
            }
        }
        keywordMap = map
    }

    fun getKeywords(): Array<String>? {
        return keywords
    }


    fun requireAutoComplete(
        reference: ContentReference,
        position: CharPosition,
        prefix: String,
        publisher: CompletionPublisher,
        userIdentifiers: Identifiers?
    ) {
        val completionItemList = filterCompletionItems(
            reference, position, createCompletionItemList(prefix, userIdentifiers)
        )

        val comparator = createCompletionItemComparator(completionItemList)

        publisher.addItems(completionItemList)

        publisher.setComparator(comparator)
    }

    fun createCompletionItemList(
        prefix: String,
        userIdentifiers: Identifiers?
    ): List<CompletionItem> {
        val prefixLength = prefix.length
        if (prefixLength == 0) {
            return Collections.emptyList()
        }
        val result = ArrayList<CompletionItem>()
        val keywordArray = keywords
        val lowCase = keywordsAreLowCase
        val keywordMap = this.keywordMap
        val match = prefix.lowercase(Locale.ROOT)

        if (keywordArray != null) {
            if (lowCase) {
                for (kw in keywordArray) {
                    val fuzzyScore = fuzzyScoreGracefulAggressive(
                        prefix,
                        prefix.lowercase(Locale.ROOT),
                        0, kw, kw.lowercase(Locale.ROOT), 0, FuzzyScoreOptions.default
                    )

                    val score = fuzzyScore?.score ?: -100

                    if (kw.startsWith(match) || score >= -20) {
                        result.add(
                            SimpleCompletionItem(kw, "Keyword", prefixLength, kw)
                                .kind(CompletionItemKind.Keyword)
                        )
                    }
                }
            } else {
                for (kw in keywordArray) {
                    val fuzzyScore = fuzzyScoreGracefulAggressive(
                        prefix,
                        prefix.lowercase(Locale.ROOT),
                        0, kw, kw.lowercase(Locale.ROOT), 0, FuzzyScoreOptions.default
                    )

                    val score = fuzzyScore?.score ?: -100

                    if (kw.lowercase(Locale.ROOT).startsWith(match) || score >= -20) {
                        result.add(
                            SimpleCompletionItem(kw, "Keyword", prefixLength, kw)
                                .kind(CompletionItemKind.Keyword)
                        )
                    }
                }
            }
        }
        if (userIdentifiers != null) {
            val dest = ArrayList<String>()

            userIdentifiers.filterIdentifiers(prefix, dest)
            for (word in dest) {
                if (keywordMap == null || !keywordMap.containsKey(word))
                    result.add(
                        SimpleCompletionItem(word, "Identifier", prefixLength, word)
                            .kind(CompletionItemKind.Identifier)
                    )
            }
        }
        return result
    }


    @Deprecated("")
    fun requireAutoComplete(
        prefix: String,
        publisher: CompletionPublisher,
        userIdentifiers: Identifiers?
    ) {
        publisher.setComparator(COMPARATOR)
        publisher.setUpdateThreshold(0)
        publisher.addItems(createCompletionItemList(prefix, userIdentifiers))
    }


    interface Identifiers {


        fun filterIdentifiers(prefix: String, dest: MutableList<String>)
    }


    class DisposableIdentifiers : Identifiers {

        companion object {
            private val SIGN = Any()
        }

        private val identifiers = ArrayList<String>(128)
        private var cache: HashMap<String, Any>? = null

        fun addIdentifier(identifier: String) {
            if (cache == null) {
                throw IllegalStateException("begin() has not been called")
            }
            if (cache!!.put(identifier, SIGN) == SIGN) {
                return
            }
            identifiers.add(identifier)
        }


        fun beginBuilding() {
            cache = HashMap()
        }


        fun finishBuilding() {
            cache?.clear()
            cache = null
        }

        override fun filterIdentifiers(prefix: String, dest: MutableList<String>) {
            for (identifier in identifiers) {
                val fuzzyScore = fuzzyScoreGracefulAggressive(
                    prefix,
                    prefix.lowercase(Locale.ROOT),
                    0, identifier, identifier.lowercase(Locale.ROOT), 0, FuzzyScoreOptions.default
                )

                val score = fuzzyScore?.score ?: -100

                if ((TextUtils.startsWith(identifier, prefix, true) || score >= -20) && !(prefix.length == identifier.length && TextUtils.startsWith(
                        prefix,
                        identifier,
                        false
                    ))
                ) {
                    dest.add(identifier)
                }
            }
        }
    }

    class SyncIdentifiers : Identifiers {

        private val lock: Lock = ReentrantLock(true)
        private val identifierMap: MutableMap<String, MutableInt> = HashMap()

        fun clear() {
            lock.lock()
            try {
                identifierMap.clear()
            } finally {
                lock.unlock()
            }
        }

        fun identifierIncrease(identifier: String) {
            lock.lock()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    identifierMap.computeIfAbsent(identifier) { MutableInt(0) }.increase()
                } else {
                    var counter = identifierMap[identifier]
                    if (counter == null) {
                        counter = MutableInt(0)
                        identifierMap[identifier] = counter
                    }
                    counter.increase()
                }
            } finally {
                lock.unlock()
            }
        }

        fun identifierDecrease(identifier: String) {
            lock.lock()
            try {
                val count = identifierMap[identifier]
                if (count != null) {
                    if (count.decreaseAndGet() <= 0) {
                        identifierMap.remove(identifier)
                    }
                }
            } finally {
                lock.unlock()
            }
        }

        override fun filterIdentifiers(prefix: String, dest: MutableList<String>) {
            filterIdentifiers(prefix, dest, false)
        }

        fun filterIdentifiers(prefix: String, dest: MutableList<String>, waitForLock: Boolean) {
            val acquired: Boolean
            if (waitForLock) {
                lock.lock()
                acquired = true
            } else {
                acquired = try {
                    lock.tryLock(3, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    false
                }
            }
            if (acquired) {
                try {
                    for (s in identifierMap.keys) {
                        val fuzzyScore = fuzzyScoreGracefulAggressive(
                            prefix,
                            prefix.lowercase(Locale.ROOT),
                            0, s, s.lowercase(Locale.ROOT), 0, FuzzyScoreOptions.default
                        )

                        val score = fuzzyScore?.score ?: -100

                        if ((TextUtils.startsWith(s, prefix, true) || score >= -20) && !(prefix.length == s.length && TextUtils.startsWith(
                                prefix,
                                s,
                                false
                            ))
                        ) {
                            dest.add(s)
                        }
                    }
                } finally {
                    lock.unlock()
                }
            }
        }
    }
}
