
package io.github.abc15018045126.sora.util;

public class TemporaryFloatBuffer {

    private static final FloatArrayCache sCache = new FloatArrayCache();

    public static float[] obtain(int len) {
        return sCache.obtain(len);
    }

    public static void recycle(float[] temp) {
        sCache.recycle(temp);
    }

    public static class FloatArrayCache {

        private float[] temp = null;

        public float[] obtain(int len) {
            float[] buf;

            synchronized (this) {
                buf = temp;
                temp = null;
            }

            if (buf == null || buf.length < len) {
                buf = new float[len];
            }

            return buf;
        }

        public void recycle(float[] temp) {
            if (temp.length > 1000) return;

            synchronized (this) {
                this.temp = temp;
            }
        }

    }

}

