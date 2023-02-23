package org.telegram.ui.Call;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Property;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ColoredInsetFrameLayout extends FrameLayout {

    private WindowInsets lastInsets;
    private Paint overlayPaint = new Paint();
    private Paint overlayBottomPaint = new Paint();
    private int topColor, bottomColor;

    public static final Property<ColoredInsetFrameLayout, Integer> PROPERTY_TOP_COLOR = new Property<ColoredInsetFrameLayout, Integer>(Integer.class, "topColor") {
        public Integer get(ColoredInsetFrameLayout object) {
            return object.topColor;
        }

        public void set(ColoredInsetFrameLayout object, Integer value) {
            object.setTopColor(value);
        }
    };

    public static final Property<ColoredInsetFrameLayout, Integer> PROPERTY_BOTTOM_COLOR = new Property<ColoredInsetFrameLayout, Integer>(Integer.class, "bottomColor") {
        public Integer get(ColoredInsetFrameLayout object) {
            return object.bottomColor;
        }

        public void set(ColoredInsetFrameLayout object, Integer value) {
            object.setBottomColor(value);
        }
    };

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
            overlayPaint.setColor(topColor);
            canvas.drawRect(0, 0, getMeasuredWidth(), lastInsets.getSystemWindowInsetTop(), overlayPaint);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && lastInsets != null) {
            overlayBottomPaint.setColor(bottomColor);
            canvas.drawRect(0, getMeasuredHeight() - lastInsets.getSystemWindowInsetBottom(), getMeasuredWidth(), getMeasuredHeight(), overlayBottomPaint);
        }
    }

    public void setInsets(WindowInsets insets) {
        this.lastInsets = insets;
    }

    public int getTopColor() {
        return topColor;
    }

    public void setTopColor(int topColor) {
        this.topColor = topColor;
        invalidate();
    }

    public int getBottomColor() {
        return bottomColor;
    }

    public void setBottomColor(int bottomColor) {
        this.bottomColor = bottomColor;
        invalidate();
    }
}
