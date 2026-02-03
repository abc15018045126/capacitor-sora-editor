

package io.github.abc15018045126.sora.lang.completion.snippet

data class PlaceholderDefinition @JvmOverloads constructor(
    var id: Int,
    var choices: List<String>? = null,
    var elements: MutableList<PlaceHolderElement> = mutableListOf(),
    var transform: Transform? = null
) {
    internal var text: String? = null
}

