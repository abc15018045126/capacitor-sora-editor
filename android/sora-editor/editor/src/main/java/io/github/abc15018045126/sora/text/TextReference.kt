package io.github.abc15018045126.sora.text

import java.util.Objects


open class TextReference(
    ref: CharSequence,
    private val start: Int,
    private val end: Int
) : CharSequence {

    private val ref: CharSequence = Objects.requireNonNull(ref)
    private var validator: Validator? = null

    constructor(ref: CharSequence) : this(ref, 0, ref.length)

    init {
        if (start > end) {
            throw IllegalArgumentException("start > end")
        }
        if (start < 0) {
            throw StringIndexOutOfBoundsException(start)
        }
        if (end > ref.length) {
            throw StringIndexOutOfBoundsException(end)
        }
    }


    open val reference: CharSequence
        get() = ref

    override val length: Int
        get() {
            validateAccess()
            return end - start
        }

    override fun get(index: Int): Char {
        if (index < 0 || index >= length) {
            throw StringIndexOutOfBoundsException(index)
        }
        validateAccess()
        return ref[start + index]
    }

    override fun toString(): String {
        return ref.subSequence(start, end).toString()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex < 0 || startIndex >= length) {
            throw StringIndexOutOfBoundsException(startIndex)
        }
        if (endIndex < 0 || endIndex > length) {
             throw StringIndexOutOfBoundsException(endIndex)
        }

        validateAccess()
        return TextReference(ref, this.start + startIndex, this.start + endIndex).setValidator(validator)
    }

    open fun setValidator(validator: Validator?): TextReference {
        this.validator = validator
        return this
    }

    fun validateAccess() {
        validator?.validate()
    }

    fun interface Validator {
        fun validate()
    }

    class ValidateFailedException : RuntimeException {
        constructor()
        constructor(message: String?) : super(message)
    }
}
