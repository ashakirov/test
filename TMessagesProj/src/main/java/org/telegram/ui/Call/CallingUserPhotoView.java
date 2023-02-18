package org.telegram.ui.Call;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;

import org.telegram.ui.Components.BackupImageView;

public class CallingUserPhotoView extends BackupImageView {

    int blackoutColor = ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.3f));

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(blackoutColor);
    }
}
