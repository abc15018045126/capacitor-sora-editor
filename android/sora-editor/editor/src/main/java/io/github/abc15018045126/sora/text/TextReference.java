
package io.github.abc15018045126.sora.text;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Reference of a {@link CharSequence} object, which allows
 * to access the original sequence in read-only mode, and attach a
 * {@link Validator} to validate access to check whether reject the
 * read access.
 * This can be useful when reading text in multiple threads, with the ability
 * to interrupt that thread when the actual text changes.
 *
 * @author abc15018045126
 */
public class TextReference implements CharSequence {

    private final CharSequence ref;
    private final int start, end;
    private Validator validator;

    public TextReference(@NonNull CharSequence ref) {
        this(ref, 0, ref.length());
    }

    public TextReference(@NonNull CharSequence ref, int start, int end) {
        this.ref = Objects.requireNonNull(ref);
        this.start = start;
        this.end = end;
        if (start > end) {
            throw new IllegalArgumentException("start > end");
        }
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > ref.length()) {
            throw new StringIndexOutOfBoundsException(end);
        }
    }

    /**
     * Get original text of the reference
     */
    @NonNull
    public CharSequence getReference() {
        return ref;
    }

    @Override
    public int length() {
        validateAccess();
        return end - start;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new StringIndexOutOfBoundsException(index);
        }
        validateAccess();
        return ref.charAt(start + index);
    }

    @NonNull
    @Override
    public String toString() {
        return ref.subSequence(start, end).toString();
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || start >= length()) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end < 0 || end >= length()) {
            throw new StringIndexOutOfBoundsException(end);
        }
        validateAccess();
        return new TextReference(ref, this.start + start, this.start + end).setValidator(validator);
    }

    public TextReference setValidator(Validator validator) {
        this.validator = validator;
        return this;
    }

    public void validateAccess() {
        if (validator != null)
            validator.validate();
    }

    public interface Validator {
        void validate();
    }

    public static class ValidateFailedException extends RuntimeException {

        public ValidateFailedException() {
        }

        public ValidateFailedException(String message) {
            super(message);
        }

    }
}

