

package io.github.abc15018045126.sora.lang.styling

import java.util.concurrent.ArrayBlockingQueue


open class SpanPool<SpanT : Span> @JvmOverloads constructor(
  capacity: Int = DEFAULT_CAPACITY,
  private val factory: (column: Int, style: Long) -> SpanT
) {

  private val cacheQueue = ArrayBlockingQueue<SpanT>(capacity)

  companion object {


    const val CAPACITY_SMALL = 8192


    const val CAPACITY_LARGE = CAPACITY_SMALL * 2


    const val DEFAULT_CAPACITY = CAPACITY_LARGE
  }


  open fun offer(span: SpanT): Boolean {
    return cacheQueue.offer(span)
  }


  open fun obtain(column: Int, style: Long): SpanT {
    return cacheQueue.poll()?.also {
      it.column = column
      it.style = style
    } ?: factory(column, style)
  }
}
