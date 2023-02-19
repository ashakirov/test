package org.telegram.ui.Call;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.voip.VoIPService;

public class VoIPStatusLayout extends LinearLayout {

    public VoIPStatusLayout(Context context) {
        super(context);
    }

    public VoIPStatusLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VoIPStatusLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public VoIPStatusLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        final VoIPService service = VoIPService.getSharedInstance();
        final CharSequence callingUserTitleText = ((TextView) findViewById(R.id.callingUserTitle)).getText();
        if (service != null && !TextUtils.isEmpty(callingUserTitleText)) {
            final StringBuilder builder = new StringBuilder(callingUserTitleText);

            builder.append(", ");
            if (service.privateCall != null && service.privateCall.video) {
                builder.append(LocaleController.getString("VoipInVideoCallBranding", R.string.VoipInVideoCallBranding));
            } else {
                builder.append(LocaleController.getString("VoipInCallBranding", R.string.VoipInCallBranding));
            }

            final long callDuration = service.getCallDuration();
            if (callDuration > 0) {
                builder.append(", ");
                builder.append(LocaleController.formatDuration((int) (callDuration / 1000)));
            }

            info.setText(builder);
        }
    }
}
