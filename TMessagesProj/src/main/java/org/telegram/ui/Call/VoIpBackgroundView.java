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

    //order [bottomRight, bottomLeft, topLeft, topRight]
    private int[] green = {0xff07a9ac, 0xff07ba63, 0xffa9cc66, 0xff5ab147};
    private int[] blueGreen = {0xff4576e9, 0xff3b7af1, 0xff08b0a3, 0xff17aae4};
    private int[] blueViolet = {0xffb456d8, 0xff8148ec, 0xff20a4d7, 0xff3f8bea};
    private int[] orangeRed = {0xffe86958, 0xffe7618f, 0xffdb904c, 0xffde7238};

    private int[][] colors = {blueViolet, blueGreen, green};
    private int currentColor = 0;
    private ValueAnimator switchColorAnimator;

    private MotionBackgroundDrawable currMotionDrawable;
    private MotionBackgroundDrawable prevMotionDrawable;

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
        currMotionDrawable = createMotionDrawable(colors[currentColor]);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (prevMotionDrawable != null) {
            prevMotionDrawable.setBounds(0, 0, getWidth(), getHeight());
        }
        currMotionDrawable.setBounds(0, 0, getWidth(), getHeight());

        if (prevMotionDrawable != null)
            prevMotionDrawable.drawBackground(canvas);
        currMotionDrawable.drawBackground(canvas);
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
        return drawable;
    }

    public void switchColors() {
        if (switchColorAnimator != null) {
            switchColorAnimator.cancel();
        }
        currentColor = (currentColor + 1) % colors.length;
        prevMotionDrawable = currMotionDrawable;

        currMotionDrawable = createMotionDrawable(colors[currentColor]);
        currMotionDrawable.setBackgroundAlpha(0f);

        switchColorAnimator = ValueAnimator.ofFloat(0f, 1f);
        switchColorAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            currMotionDrawable.setBackgroundAlpha(progress);
        });
        switchColorAnimator.setDuration(3000);
        switchColorAnimator.start();
    }

    public void setAnimationRunning(boolean isRunning){
        if(prevMotionDrawable != null){
            prevMotionDrawable.setIndeterminateAnimation(isRunning);
        }
        currMotionDrawable.setIndeterminateAnimation(isRunning);
    }
}
