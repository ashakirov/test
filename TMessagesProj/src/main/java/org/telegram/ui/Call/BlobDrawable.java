package org.telegram.ui.Call;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

import org.telegram.messenger.SharedConfig;

import java.util.Random;

public class BlobDrawable {

    private int minRadius;
    private int maxAmlitude;

    private Path path = new Path();
    public Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float[] radius;
    private float[] angle;
    private float[] radiusNext;
    private float[] angleNext;
    private float[] progress;

    //milliseconds from blob1 to blob2
    private final float waveTime = 1000;
    public int amplitude;

    private float[] pointStart = new float[4];
    private float[] pointEnd = new float[4];

    final Random random = new Random();

    private final float N;
    private final float L;
    public float cubicBezierK = 1f;

    private final Matrix m = new Matrix();

    public BlobDrawable(int n, int minRadius, int maxAmlitude) {
        N = n;
        L = (float) ((4.0 / 3.0) * Math.tan(Math.PI / (2 * N)));
        radius = new float[n];
        angle = new float[n];
        this.minRadius = minRadius;
        this.maxAmlitude = maxAmlitude;
        this.amplitude = maxAmlitude;
        radiusNext = new float[n];
        angleNext = new float[n];
        progress = new float[n];

        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }

    private void generateBlob(float[] radius, float[] angle, int i) {
        float angleDif = 360f / N * 0.05f;
        radius[i] = amplitude == 0 ? minRadius : minRadius + random.nextInt((int) amplitude);
        angle[i] = 360f / N * i + ((random.nextInt() % 100f) / 100f) * angleDif;
    }

    public void setAmplitude(int amplitude) {
        if (amplitude < 0) {
            this.amplitude = 0;
        } else if (amplitude > maxAmlitude) {
            this.amplitude = maxAmlitude;
        } else{
            this.amplitude = amplitude;
        }
    }

    public void update(long dt) {
        if(dt == 0){
            return;
        }
        if (SharedConfig.getLiteMode().enabled()) {
            return;
        }
        for (int i = 0; i < N; i++) {
            float dist = Math.abs(radiusNext[i] - radius[i]);
            float dX = dist * dt / waveTime;

            float prog = dist == 0 ? 1 : dX / dist;
            progress[i] += prog;
            if (progress[i] >= 1f) {
                progress[i] = 0;
                radius[i] = radiusNext[i];
                angle[i] = angleNext[i];
                generateBlob(radiusNext, angleNext, i);
            }
        }
    }

    public void draw(float cX, float cY, Canvas canvas) {
        if (SharedConfig.getLiteMode().enabled()) {
            return;
        }

        path.reset();

        for (int i = 0; i < N; i++) {
            float progress = this.progress[i];
            int nextIndex = i + 1 < N ? i + 1 : 0;
            float progressNext = this.progress[nextIndex];
            float r1 = radius[i] * (1f - progress) + radiusNext[i] * progress;
            float r2 = radius[nextIndex] * (1f - progressNext) + radiusNext[nextIndex] * progressNext;
            float angle1 = angle[i] * (1f - progress) + angleNext[i] * progress;
            float angle2 = angle[nextIndex] * (1f - progressNext) + angleNext[nextIndex] * progressNext;

            float l = L * (Math.min(r1, r2) + (Math.max(r1, r2) - Math.min(r1, r2)) / 2f) * cubicBezierK;
            m.reset();
            m.setRotate(angle1, cX, cY);

            pointStart[0] = cX;
            pointStart[1] = cY - r1;
            pointStart[2] = cX + l;
            pointStart[3] = cY - r1;

            m.mapPoints(pointStart);

            pointEnd[0] = cX;
            pointEnd[1] = cY - r2;
            pointEnd[2] = cX - l;
            pointEnd[3] = cY - r2;

            m.reset();
            m.setRotate(angle2, cX, cY);

            m.mapPoints(pointEnd);

            if (i == 0) {
                path.moveTo(pointStart[0], pointStart[1]);
            }

            path.cubicTo(
                    pointStart[2], pointStart[3],
                    pointEnd[2], pointEnd[3],
                    pointEnd[0], pointEnd[1]
            );
        }

        canvas.save();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void generateBlob() {
        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }

    public void setMinRadius(int minRadius) {
        float diff = minRadius - this.minRadius;
        this.minRadius = minRadius;
        for (int i = 0; i < N; i++) {
            radius[i] += diff;
            radiusNext[i] += diff;
        }
    }

    public boolean isCircle() {
        if (amplitude == 0) {
            for (int i = 0; i < N; i++) {
                if (radius[i] != minRadius || radiusNext[i] != minRadius) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
