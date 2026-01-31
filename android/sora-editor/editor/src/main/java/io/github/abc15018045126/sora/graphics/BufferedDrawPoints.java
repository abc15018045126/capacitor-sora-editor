
package io.github.abc15018045126.sora.graphics;


import android.graphics.Canvas;
import android.graphics.Paint;

public class BufferedDrawPoints {

    private int pointCount;
    private float[] points;
    private float offsetX, offsetY;

    public BufferedDrawPoints() {
        points = new float[128];
    }

    public void drawPoint(float cx, float cy) {
        // Check buffer size and grow
        if (points.length < (pointCount + 1) * 2) {
            float[] newBuffer = new float[points.length << 1];
            System.arraycopy(points, 0, newBuffer, 0, pointCount * 2);
            points = newBuffer;
        }
        points[pointCount * 2] = cx + offsetX;
        points[pointCount * 2 + 1] = cy + offsetY;
        pointCount++;
    }

    public void setOffsets(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void commitPoints(Canvas canvas, Paint paint) {
        if (pointCount == 0) {
            return;
        }
        canvas.drawPoints(points, 0, pointCount * 2, paint);
        pointCount = 0;
    }

}

