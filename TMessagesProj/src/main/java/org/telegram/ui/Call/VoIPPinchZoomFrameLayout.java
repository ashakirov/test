package org.telegram.ui.Call;

import static org.telegram.ui.GroupCallActivity.TRANSITION_DURATION;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.voip.VoIPTextureView;
import org.telegram.ui.GroupCallActivity;

public class VoIPPinchZoomFrameLayout extends FrameLayout {

    public interface CallBackgroundViewCallback {
        void onTap(long time);
        VoIPTextureView getFullscreenTextureView();
    }

    /* === pinch to zoom === */
    private float pinchStartCenterX;
    private float pinchStartCenterY;
    private float pinchStartDistance;
    private float pinchTranslationX;
    private float pinchTranslationY;
    private boolean isInPinchToZoomTouchMode;

    private float pinchCenterX;
    private float pinchCenterY;

    private int pointerId1, pointerId2;

    float pinchScale = 1f;
    private boolean zoomStarted;
    private boolean canZoomGesture;
    private ValueAnimator zoomBackAnimator;
    /* === pinch to zoom === */

    private float touchSlop;
    private Paint overlayPaint;
    private Paint overlayBottomPaint;
    private WindowInsets lastInsets;
    private CallBackgroundViewCallback callback;

    public VoIPPinchZoomFrameLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VoIPPinchZoomFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VoIPPinchZoomFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public VoIPPinchZoomFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(@NonNull Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipToPadding(false);
        setClipChildren(false);
        setBackgroundColor(0xff000000);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && lastInsets != null) {
            canvas.drawRect(0, 0, getMeasuredWidth(), lastInsets.getSystemWindowInsetTop(), overlayPaint);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && lastInsets != null) {
            canvas.drawRect(0, getMeasuredHeight() - lastInsets.getSystemWindowInsetBottom(), getMeasuredWidth(), getMeasuredHeight(), overlayBottomPaint);
        }
    }

    float pressedX;
    float pressedY;
    boolean check;
    long pressedTime;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        /* === pinch to zoom === */
        if (!canZoomGesture && !isInPinchToZoomTouchMode && !zoomStarted && ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            finishZoom();
            return false;
        }
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            canZoomGesture = false;
            isInPinchToZoomTouchMode = false;
            zoomStarted = false;
        }
        VoIPTextureView currentTextureView = callback.getFullscreenTextureView();

        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                AndroidUtilities.rectTmp.set(currentTextureView.getX(), currentTextureView.getY(), currentTextureView.getX() + currentTextureView.getMeasuredWidth(), currentTextureView.getY() + currentTextureView.getMeasuredHeight());
                AndroidUtilities.rectTmp.inset((currentTextureView.getMeasuredHeight() * currentTextureView.scaleTextureToFill - currentTextureView.getMeasuredHeight()) / 2, (currentTextureView.getMeasuredWidth() * currentTextureView.scaleTextureToFill - currentTextureView.getMeasuredWidth()) / 2);
                if (!GroupCallActivity.isLandscapeMode) {
                    AndroidUtilities.rectTmp.top = Math.max(AndroidUtilities.rectTmp.top, ActionBar.getCurrentActionBarHeight());
                    AndroidUtilities.rectTmp.bottom = Math.min(AndroidUtilities.rectTmp.bottom, currentTextureView.getMeasuredHeight() - AndroidUtilities.dp(90));
                } else {
                    AndroidUtilities.rectTmp.top = Math.max(AndroidUtilities.rectTmp.top, ActionBar.getCurrentActionBarHeight());
                    AndroidUtilities.rectTmp.right = Math.min(AndroidUtilities.rectTmp.right, currentTextureView.getMeasuredWidth() - AndroidUtilities.dp(90));
                }
                canZoomGesture = AndroidUtilities.rectTmp.contains(ev.getX(), ev.getY());
                if (!canZoomGesture) {
                    finishZoom();
                }
            }
            if (canZoomGesture && !isInPinchToZoomTouchMode && ev.getPointerCount() == 2) {
                pinchStartDistance = (float) Math.hypot(ev.getX(1) - ev.getX(0), ev.getY(1) - ev.getY(0));
                pinchStartCenterX = pinchCenterX = (ev.getX(0) + ev.getX(1)) / 2.0f;
                pinchStartCenterY = pinchCenterY = (ev.getY(0) + ev.getY(1)) / 2.0f;
                pinchScale = 1f;

                pointerId1 = ev.getPointerId(0);
                pointerId2 = ev.getPointerId(1);
                isInPinchToZoomTouchMode = true;
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE && isInPinchToZoomTouchMode) {
            int index1 = -1;
            int index2 = -1;
            for (int i = 0; i < ev.getPointerCount(); i++) {
                if (pointerId1 == ev.getPointerId(i)) {
                    index1 = i;
                }
                if (pointerId2 == ev.getPointerId(i)) {
                    index2 = i;
                }
            }
            if (index1 == -1 || index2 == -1) {
                getParent().requestDisallowInterceptTouchEvent(false);
                finishZoom();
            } else {
                pinchScale = (float) Math.hypot(ev.getX(index2) - ev.getX(index1), ev.getY(index2) - ev.getY(index1)) / pinchStartDistance;
                if (pinchScale > 1.005f && !zoomStarted) {
                    pinchStartDistance = (float) Math.hypot(ev.getX(index2) - ev.getX(index1), ev.getY(index2) - ev.getY(index1));
                    pinchStartCenterX = pinchCenterX = (ev.getX(index1) + ev.getX(index2)) / 2.0f;
                    pinchStartCenterY = pinchCenterY = (ev.getY(index1) + ev.getY(index2)) / 2.0f;
                    pinchScale = 1f;
                    pinchTranslationX = 0f;
                    pinchTranslationY = 0f;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    zoomStarted = true;
                    isInPinchToZoomTouchMode = true;
                }

                float newPinchCenterX = (ev.getX(index1) + ev.getX(index2)) / 2.0f;
                float newPinchCenterY = (ev.getY(index1) + ev.getY(index2)) / 2.0f;

                float moveDx = pinchStartCenterX - newPinchCenterX;
                float moveDy = pinchStartCenterY - newPinchCenterY;
                pinchTranslationX = -moveDx / pinchScale;
                pinchTranslationY = -moveDy / pinchScale;
                invalidate();
            }
        } else if ((ev.getActionMasked() == MotionEvent.ACTION_UP || (ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP && checkPointerIds(ev)) || ev.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
            getParent().requestDisallowInterceptTouchEvent(false);
            finishZoom();
        }
        invalidate();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressedX = ev.getX();
                pressedY = ev.getY();
                check = true;
                pressedTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_CANCEL:
                check = false;
                break;
            case MotionEvent.ACTION_UP:
                if (check) {
                    float dx = ev.getX() - pressedX;
                    float dy = ev.getY() - pressedY;
                    long currentTime = System.currentTimeMillis();
                    if (dx * dx + dy * dy < touchSlop * touchSlop && currentTime - pressedTime < 300){
                        callback.onTap(currentTime);
                    }
                    check = false;
                }
                break;
        }
        return canZoomGesture || check;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        /*if (child == callingUserPhotoView && (currentUserIsVideo || callingUserIsVideo)) {
            return false;
        }
        if (
                child == callingUserPhotoView ||
                        child == callingUserTextureView ||
                        (child == currentUserCameraFloatingLayout && currentUserCameraIsFullscreen)
        ) { */
            if (zoomStarted || zoomBackAnimator != null) {
                canvas.save();
                canvas.scale(pinchScale, pinchScale, pinchCenterX, pinchCenterY);
                canvas.translate(pinchTranslationX, pinchTranslationY);
                boolean b = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
                return b;
            }
       // }
        return super.drawChild(canvas, child, drawingTime);
    }

    private boolean checkPointerIds(MotionEvent ev) {
        if (ev.getPointerCount() < 2) {
            return false;
        }
        if (pointerId1 == ev.getPointerId(0) && pointerId2 == ev.getPointerId(1)) {
            return true;
        }
        if (pointerId1 == ev.getPointerId(1) && pointerId2 == ev.getPointerId(0)) {
            return true;
        }
        return false;
    }

    private void finishZoom() {
        if (zoomStarted) {
            zoomStarted = false;
            zoomBackAnimator = ValueAnimator.ofFloat(1f, 0);

            float fromScale = pinchScale;
            float fromTranslateX = pinchTranslationX;
            float fromTranslateY = pinchTranslationY;
            zoomBackAnimator.addUpdateListener(valueAnimator -> {
                float v = (float) valueAnimator.getAnimatedValue();
                pinchScale = fromScale * v + 1f * (1f - v);
                pinchTranslationX = fromTranslateX * v;
                pinchTranslationY = fromTranslateY * v;
                invalidate();
            });

            zoomBackAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    zoomBackAnimator = null;
                    pinchScale = 1f;
                    pinchTranslationX = 0;
                    pinchTranslationY = 0;
                    invalidate();
                }
            });
            zoomBackAnimator.setDuration(TRANSITION_DURATION);
            zoomBackAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            zoomBackAnimator.start();
        }
        canZoomGesture = false;
        isInPinchToZoomTouchMode = false;
    }

    public void setOverlayPaint(Paint overlayPaint){
        this.overlayPaint = overlayPaint;
    }

    public void setOverlayBottomPaint(Paint overlayBottomPaint){
        this.overlayBottomPaint = overlayBottomPaint;
    }

    public void setInsets(WindowInsets insets){
        this.lastInsets = insets;
    }

    public void setCallback(CallBackgroundViewCallback callback){
        this.callback = callback;
    }
}
