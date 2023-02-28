package org.telegram.ui.Call;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.telegram.ui.Components.MotionBackgroundDrawable;

public class VoIpBackgroundView extends View {

    public enum State {
        NOT_ESTABLISHED,
        CALL_ESTABLISHED,
        WEAK_SIGNAL
    }

    private final int SHOW_WEAK_SIGNAL_DURATION = 400;
    private State state = State.NOT_ESTABLISHED;

    //order [bottomRight, bottomLeft, topLeft, topRight]
    private final int[] green = {0xff07a9ac, 0xff07ba63, 0xffa9cc66, 0xff5ab147};
    private final int[] blueGreen = {0xff4576e9, 0xff3b7af1, 0xff08b0a3, 0xff17aae4};
    private final int[] blueViolet = {0xffb456d8, 0xff8148ec, 0xff20a4d7, 0xff3f8bea};
    private final int[] orangeRed = {0xffe86958, 0xffe7618f, 0xffdb904c, 0xffde7238};

    private final int[][] COLORS = {blueViolet, blueGreen, green};

    private int currentColor = 0;
    private ValueAnimator switchColorAnimator;
    private ValueAnimator weakSignalAnimator;

    private MotionBackgroundDrawable currMotionDrawable;
    private MotionBackgroundDrawable prevMotionDrawable;

    private MotionBackgroundDrawable weakSignalDrawable;

    public VoIpBackgroundView(Context context) {
        super(context);
        init();
    }

    public VoIpBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VoIpBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public VoIpBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        currMotionDrawable = createMotionDrawable(COLORS[0]);

        weakSignalDrawable = createMotionDrawable(orangeRed);
        weakSignalDrawable.setBackgroundAlpha(0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (weakSignalDrawable.getBackgroundAlpha() < 1f) {
            if (prevMotionDrawable != null) {
                prevMotionDrawable.setBounds(0, 0, getWidth(), getHeight());
                prevMotionDrawable.drawBackground(canvas);
            }
            currMotionDrawable.setBounds(0, 0, getWidth(), getHeight());
            currMotionDrawable.drawBackground(canvas);
        }

        if (weakSignalDrawable.getBackgroundAlpha() > 0f) {
            weakSignalDrawable.setBounds(0, 0, getWidth(), getHeight());
            weakSignalDrawable.drawBackground(canvas);
        }
        super.onDraw(canvas);
    }

    private MotionBackgroundDrawable createMotionDrawable(int[] colors) {
        MotionBackgroundDrawable drawable = new MotionBackgroundDrawable();
        drawable.setCallback(this);
        drawable.setColors(colors[0], colors[1], colors[2], colors[3]);
        drawable.setBackgroundAlpha(1f);
        drawable.setParentView(this);
        drawable.setPatternAlpha(1f);
        drawable.setIndeterminateAnimation(true);
        drawable.setIndeterminateSpeedScale(0.5f);
        return drawable;
    }

    public void switchColors(long duration) {
        if (switchColorAnimator != null) {
            switchColorAnimator.cancel();
        }
        currentColor = (currentColor + 1) % getColorMaxIndex();
        prevMotionDrawable = currMotionDrawable;

        currMotionDrawable = createMotionDrawable(COLORS[currentColor]);
        currMotionDrawable.setBackgroundAlpha(0f);

        switchColorAnimator = ValueAnimator.ofFloat(0f, 1f);
        switchColorAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            currMotionDrawable.setBackgroundAlpha(progress);
        });
        switchColorAnimator.setDuration(duration);
        switchColorAnimator.start();
    }

    private int getColorMaxIndex() {
        if (state == State.CALL_ESTABLISHED) {
            return COLORS.length;
        } else {
            return COLORS.length - 1;
        }
    }

    private void showWeakSignalDrawable(boolean isVisible) {
        if (weakSignalAnimator != null) {
            weakSignalAnimator.cancel();
        }
        float from = weakSignalDrawable.getBackgroundAlpha();
        float to = isVisible ? 1f : 0f;
        weakSignalAnimator = ValueAnimator.ofFloat(from, to);
        weakSignalAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            weakSignalDrawable.setBackgroundAlpha(progress);
        });
        weakSignalAnimator.setDuration(SHOW_WEAK_SIGNAL_DURATION);
        weakSignalAnimator.start();
    }

    public void setAnimationRunning(boolean isRunning) {
        if (prevMotionDrawable != null) {
            prevMotionDrawable.setIndeterminateAnimation(isRunning);
        }
        currMotionDrawable.setIndeterminateAnimation(isRunning);
        invalidate();
    }

    public void setState(State newState) {
        State oldState = this.state;
        if (oldState == newState) {
            return;
        }
        this.state = newState;

        if (newState == State.WEAK_SIGNAL) {
            showWeakSignalDrawable(true);
        } else if (oldState == State.WEAK_SIGNAL) {
            showWeakSignalDrawable(false);
        }
    }
}
