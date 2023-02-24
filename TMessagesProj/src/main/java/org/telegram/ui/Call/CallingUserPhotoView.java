package org.telegram.ui.Call;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.telegram.messenger.SharedConfig;

public class CallingUserPhotoView extends View {

    private int innerWaveRadius = 50;
    private int outerWaveRadius = 70;
    private int maxAmplitude = 20;

    private BlobDrawable innerWave, outerWave;
    public int waveAmplitude;
    public int blobExtraSize;

    public static final Property<CallingUserPhotoView, Integer> PROPERTY_BLOB_EXTRA_SIZE = new Property<CallingUserPhotoView, Integer>(Integer.class, "blobExtraSize") {
        public Integer get(CallingUserPhotoView object) {
            return object.blobExtraSize;
        }

        public void set(CallingUserPhotoView object, Integer value) {
            object.setBlobExtraSize(value);
        }
    };

    public static final Property<CallingUserPhotoView, Integer> PROPERTY_WAVE_AMPLITUDE = new Property<CallingUserPhotoView, Integer>(Integer.class, "waveAmplitude") {
        public Integer get(CallingUserPhotoView object) {
            return object.waveAmplitude;
        }

        public void set(CallingUserPhotoView object, Integer value) {
            object.setWaveAmplitude(value);
        }
    };

    public CallingUserPhotoView(Context context) {
        super(context);
    }

    public CallingUserPhotoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CallingUserPhotoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public CallingUserPhotoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
    }


    float cx = -1f, cy = -1f;
    long dt, lastUpdateTime = System.currentTimeMillis();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) {
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

    public void setBlobExtraSize(int blobExtraSize) {
        this.blobExtraSize = blobExtraSize;
        innerWave.setMinRadius(innerWaveRadius + blobExtraSize);
        outerWave.setMinRadius(outerWaveRadius + blobExtraSize);
        invalidate();
    }

    public int getBlobExtraSize() {
        return blobExtraSize;
    }
}
