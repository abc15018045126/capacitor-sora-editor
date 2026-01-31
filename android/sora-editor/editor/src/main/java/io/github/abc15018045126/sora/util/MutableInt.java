
package io.github.abc15018045126.sora.util;

public class MutableInt {

    public int value;

    public MutableInt(int v) {
        value = v;
    }

    public int decreaseAndGet() {
        return --value;
    }

    public void increase() {
        value++;
    }

}

