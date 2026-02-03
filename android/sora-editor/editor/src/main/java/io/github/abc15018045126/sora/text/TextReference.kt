package io.github.abc15018045126.sora.text

open class TextReference(
    @JvmField protected val ref: CharSequence,
    private val start: Int,
    private val end: Int
) : CharSequence {
    private var validator: Validator? = null

    constructor(ref: CharSequence) : this(ref, 0, ref.length)

    init {
        require(start <= end) { "start > end" }
        if (start < 0 || end > ref.length) throw StringIndexOutOfBoundsException()
    }

    open val reference get() = ref
    override val length get() = validateAccess().let { end - start }

    override fun get(index: Int): Char {
        if (index !in 0 until length) throw StringIndexOutOfBoundsException(index)
        return validateAccess().let { ref[start + index] }
    }

    override fun toString() = validateAccess().let { ref.subSequence(start, end).toString() }

    override fun subSequence(s: Int, e: Int): CharSequence {
        if (s !in 0 until length || e !in 0..length) throw StringIndexOutOfBoundsException()
        return validateAccess().let { TextReference(ref, start + s, start + e).setValidator(validator) }
    }

    open fun setValidator(v: Validator?) = apply { validator = v }
    fun validateAccess() = validator?.validate()

    fun interface Validator { fun validate() }
    class ValidateFailedException : RuntimeException {
        constructor()
        constructor(msg: String?) : super(msg)
    }
}
