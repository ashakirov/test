package org.telegram.ui.Call;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.R;

public class ArrowView extends TextView {

    private Path path = new Path();
    private Paint paint = new Paint();
    private int color;

    public ArrowView(Context context) {
        super(context);
        init(context);
    }

    public ArrowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArrowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public ArrowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        color = ContextCompat.getColor(context, R.color.call_voip_tooltip_bg);
        paint.setColor(color);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        path.rewind();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(0,  getHeight());
        path.lineTo(getWidth() / 2f, 0);
        path.lineTo(getWidth(), getHeight());
        path.lineTo(0,  getHeight());
        path.close();

        canvas.drawPath(path, paint);
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        paint.setColor(ColorUtils.setAlphaComponent(color, (int) (255 * getAlpha())));
    }
}
