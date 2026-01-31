
package io.github.abc15018045126.sora.langs.textmate.folding;

import android.util.SparseIntArray;

import java.util.Stack;

public class FoldingRegions {
    private final SparseIntArray _startIndexes;
    private final SparseIntArray _endIndexes;
    private boolean _parentsComputed;
    public FoldingRegions(SparseIntArray startIndexes, SparseIntArray endIndexes) throws Exception {
        if (startIndexes.size() != endIndexes.size() || startIndexes.size() > IndentRange.MAX_FOLDING_REGIONS) {
            throw new Exception("invalid startIndexes or endIndexes size");
        }
        this._startIndexes = startIndexes;
        this._endIndexes = endIndexes;
        this._parentsComputed=false;
    }

    public int length() {
        return this._startIndexes.size();
    }

    public int getStartLineNumber(int index) {
        return this._startIndexes.get(index) & IndentRange.MAX_LINE_NUMBER;
    }

    public int getEndLineNumber(int index) {
        return this._endIndexes.get(index) & IndentRange.MAX_LINE_NUMBER;
    }


    public FoldingRegion toRegion(int index) {
        return new FoldingRegion(this, index);
    }

    private boolean isInsideLast(Stack<Integer> parentIndexes,int startLineNumber,int endLineNumber){
        int index = parentIndexes.get(parentIndexes.size() - 1);
        return this.getStartLineNumber(index) <= startLineNumber && this.getEndLineNumber(index) >= endLineNumber;

    }

    private void ensureParentIndices() throws Exception {
        if (!this._parentsComputed) {
            this._parentsComputed = true;
            Stack<Integer> parentIndexes=new Stack<>();
            for (int i = 0, len = this._startIndexes.size(); i < len; i++) {
                int startLineNumber = this._startIndexes.get(i);
                int endLineNumber = this._endIndexes.get(i);
                if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
                    throw new Exception("startLineNumber or endLineNumber must not exceed " + IndentRange.MAX_LINE_NUMBER);
                }
                while (parentIndexes.size() > 0 && !isInsideLast(parentIndexes,startLineNumber, endLineNumber)) {
                    parentIndexes.pop();
                }
                int parentIndex = parentIndexes.size() > 0 ? parentIndexes.get(parentIndexes.size() - 1) : -1;
                parentIndexes.push(i);
                this._startIndexes.put(i,startLineNumber + ((parentIndex & 0xFF) << 24));
                this._endIndexes.put(i,endLineNumber + ((parentIndex & 0xFF00) << 16));
            }
        }
    }

    public int getParentIndex(int index) throws Exception {
        this.ensureParentIndices();
        int parent = ((this._startIndexes.get(index) & IndentRange.MASK_INDENT) >>> 24) + ((this._endIndexes.get(index) & IndentRange.MASK_INDENT) >>> 16);
        if (parent == IndentRange.MAX_FOLDING_REGIONS) {
            return -1;
        }
        return parent;
    }
}

