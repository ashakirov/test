package org.telegram.ui.Call;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.telegram.messenger.SharedConfig;

public class BlobView extends View {

    private int innerWaveRadius = 50;
    private int outerWaveRadius = 70;
    private int maxAmplitude = 20;

    private BlobDrawable innerWave, outerWave;
    public int waveAmplitude;
    private boolean isInited = false;

    public static final Property<BlobView, Integer> PROPERTY_INNER_RADIUS = new Property<BlobView, Integer>(Integer.class, "innerWaveRadius") {
        public Integer get(BlobView object) {
            return object.innerWaveRadius;
        }

        public void set(BlobView object, Integer value) {
            object.setInnerWaveRadius(value);
        }
    };

    public static final Property<BlobView, Integer> PROPERTY_OUTER_RADIUS = new Property<BlobView, Integer>(Integer.class, "outerWaveRadius") {
        public Integer get(BlobView object) {
            return object.outerWaveRadius;
        }

        public void set(BlobView object, Integer value) {
            object.setOuterWaveRadius(value);
        }
    };

    public static final Property<BlobView, Integer> PROPERTY_WAVE_AMPLITUDE = new Property<BlobView, Integer>(Integer.class, "waveAmplitude") {
        public Integer get(BlobView object) {
            return object.waveAmplitude;
        }

        public void set(BlobView object, Integer value) {
            object.setWaveAmplitude(value);
        }
    };

    public BlobView(Context context) {
        super(context);
    }

    public BlobView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BlobView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public BlobView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void init(int innerWaveRadius, int outerWaveRadius, int maxAmlitude, int innerWaveColor, int outerWaveColor) {
        this.innerWaveRadius = innerWaveRadius;
        this.outerWaveRadius = outerWaveRadius;
        this.maxAmplitude = maxAmlitude;
        innerWave = new BlobDrawable(15, innerWaveRadius, maxAmplitude);
        innerWave.generateBlob();
        innerWave.paint.setColor(innerWaveColor);

        outerWave = new BlobDrawable(12, outerWaveRadius, maxAmplitude);
        outerWave.generateBlob();
        outerWave.paint.setColor(outerWaveColor);
        isInited = true;
    }


    float cx = -1f, cy = -1f;
    long dt, lastUpdateTime = System.currentTimeMillis();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInited || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        dt = System.currentTimeMillis() - lastUpdateTime;
        lastUpdateTime = System.currentTimeMillis();

        if (cx < 0f || cy < 0f) {
            cx = getWidth() / 2f;
            cy = getHeight() / 2f;
        }

        innerWave.update(dt);
        innerWave.draw(cx, cy, canvas);

        outerWave.update(dt);
        outerWave.draw(cx, cy, canvas);

        if (!SharedConfig.getLiteMode().enabled()) {
            if (!innerWave.isCircle()) {
                postDelayed(this::invalidate, 16);
            }
        }
    }

    public void setWaveAmplitude(int waveAmplitude) {
        this.waveAmplitude = waveAmplitude;
        innerWave.setAmplitude(waveAmplitude);
        outerWave.setAmplitude(waveAmplitude);
        invalidate();
    }

    public int getWaveAmplitude() {
        return waveAmplitude;
    }

    public int getInnerWaveRadius() {
        return innerWaveRadius;
    }

    public void setInnerWaveRadius(int innerWaveRadius) {
        this.innerWaveRadius = innerWaveRadius;
        innerWave.setMinRadius(innerWaveRadius);
        invalidate();
    }

    public int getOuterWaveRadius() {
        return outerWaveRadius;
    }

    public void setOuterWaveRadius(int outerWaveRadius) {
        this.outerWaveRadius = outerWaveRadius;
        outerWave.setMinRadius(outerWaveRadius);
        invalidate();
    }
}
