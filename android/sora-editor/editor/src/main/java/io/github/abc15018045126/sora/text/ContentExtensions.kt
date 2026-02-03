
package io.github.abc15018045126.sora.text

fun Content.batchEdit(block: (Content) -> Unit) = apply {
    beginBatchEdit()
    try { block(this) } finally { endBatchEdit() }
}
