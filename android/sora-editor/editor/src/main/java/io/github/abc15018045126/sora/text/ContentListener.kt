package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage

interface ContentListener {
    fun beforeReplace(content: Content)
    fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence)
    fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence)

    @UnsupportedUserUsage
    fun beforeModification(content: Content) {}
}
