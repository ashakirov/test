package org.telegram.ui.Call;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ColoredInsetFrameLayout extends FrameLayout {

    private WindowInsets lastInsets;
    private Paint overlayPaint;
    private Paint overlayBottomPaint;

    public ColoredInsetFrameLayout(@NonNull Context context) {
        super(context);
    }

    public ColoredInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ColoredInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public ColoredInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    public void setInsets(WindowInsets insets) {
        this.lastInsets = insets;
    }

    public void setOverlayPaint(Paint overlayPaint){
        this.overlayPaint = overlayPaint;
    }

    public void setOverlayBottomPaint(Paint overlayBottomPaint){
        this.overlayBottomPaint = overlayBottomPaint;
    }
}
