package io.github.abc15018045126.sora.widget.snippet.variable


interface ISnippetVariableResolver {


    fun resolve(name: String): String


    fun getResolvableNames(): Array<String>

}
