

package io.github.abc15018045126.sora.text


fun Content.batchEdit(block: (Content) -> Unit): Content {
    this.beginBatchEdit()
    block(this)
    this.endBatchEdit()
    return this
}
