
package io.github.abc15018045126.sora.util;

public abstract class ObjectPool<T> {

    private final Object[] pool;

    public ObjectPool() {
        this(16);
    }

    public ObjectPool(int size) {
        pool = new Object[size];
    }

    public void recycle(T obj) {
        if (obj == null)
            return;
        onRecycleObject(obj);
        synchronized (this) {
            for (int i = 0; i < pool.length; i++) {
                if (pool[i] == null) {
                    pool[i] = obj;
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public T obtain() {
        T result = null;
        synchronized (this) {
            for (int i = pool.length - 1; i >= 0; i--) {
                if (pool[i] != null) {
                    result = (T) pool[i];
                    pool[i] = null;
                    break;
                }
            }
        }
        if (result == null) {
            result = allocateNew();
        }
        return result;
    }

    protected void onRecycleObject(T recycledObj) {

    }

    protected abstract T allocateNew();

}

