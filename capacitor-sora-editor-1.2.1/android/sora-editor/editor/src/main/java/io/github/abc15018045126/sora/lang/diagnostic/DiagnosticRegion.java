
package io.github.abc15018045126.sora.lang.diagnostic;

/**
 * Class for describing a diagnostic region.
 *
 * @author abc15018045126
 */
public final class DiagnosticRegion implements Comparable<DiagnosticRegion> {

    public final static short SEVERITY_NONE = 0;
    public final static short SEVERITY_TYPO = 1;
    public final static short SEVERITY_WARNING = 2;
    public final static short SEVERITY_ERROR = 3;

    /**
     * Id specified by diagnostic provider
     */
    public long id;
    /*
     * The detail of the problem
     **/
    public DiagnosticDetail detail;
    /**
     * The start index of the diagnostic
     */
    public int startIndex;
    /**
     * The end index of the diagnostic
     */
    public int endIndex;
    /**
     * One diagnostic has only one severity specification
     *
     * @see #SEVERITY_NONE
     * @see #SEVERITY_TYPO
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_ERROR
     */
    public short severity;

    public DiagnosticRegion(int startIndex, int endIndex, short severity) {
        this(startIndex, endIndex, severity, 0, null);
    }

    public DiagnosticRegion(int startIndex, int endIndex, short severity, long id) {
        this(startIndex, endIndex, severity, id, null);
    }

    public DiagnosticRegion(int startIndex, int endIndex, short severity, long id, DiagnosticDetail detail) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.severity = severity;
        this.detail = detail;
        this.id = id;
    }

    @Override
    public int compareTo(DiagnosticRegion o) {
        var cmp = Integer.compare(startIndex, o.startIndex);
        if (cmp == 0) {
            cmp = Integer.compare(endIndex, o.endIndex);
        }
        if (cmp == 0) {
            cmp = Short.compare(severity, o.severity);
        }
        if (cmp == 0) {
            cmp = Long.compare(id, o.id);
        }
        return cmp;
    }
}

