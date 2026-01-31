
package io.github.abc15018045126.sora.lang.styling;

import java.util.ArrayList;
import java.util.List;

import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme;

public class EmptyReader implements Spans.Reader {

    private final static EmptyReader INSTANCE = new EmptyReader();

    private final List<Span> spans;

    public EmptyReader() {
        spans = new ArrayList<>(1);
        spans.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL));
    }

    public static EmptyReader getInstance() {
        return INSTANCE;
    }

    @Override
    public void moveToLine(int line) {

    }

    @Override
    public Span getSpanAt(int index) {
        return spans.get(index);
    }

    @Override
    public int getSpanCount() {
        return 1;
    }

    @Override
    public List<Span> getSpansOnLine(int line) {
        return new ArrayList<>(spans);
    }
}

