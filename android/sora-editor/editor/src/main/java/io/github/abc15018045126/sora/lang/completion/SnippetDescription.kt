

package io.github.abc15018045126.sora.lang.completion

import io.github.abc15018045126.sora.lang.completion.snippet.CodeSnippet


data class SnippetDescription(
    val selectedLength: Int,
    val snippet: CodeSnippet,
    val deleteSelected: Boolean = true
)

