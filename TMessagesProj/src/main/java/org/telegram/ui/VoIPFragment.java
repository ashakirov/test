package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.FloatRange;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.voip.EncryptionKeyEmojifier;
import org.telegram.messenger.voip.Instance;
import org.telegram.messenger.voip.VideoCapturerDevice;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.DarkAlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Call.BlobView;
import org.telegram.ui.Call.ColoredInsetConstraintLayout;
import org.telegram.ui.Call.VoIPPinchZoomFrameLayout;
import org.telegram.ui.Call.VoIPPinchZoomFrameLayout.CallBackgroundViewCallback;
import org.telegram.ui.Call.VoIPTransitions;
import org.telegram.ui.Call.VoIpBackgroundView;
import org.telegram.ui.Call.VoIpBackgroundView.State;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.HintView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.voip.PrivateVideoPreviewDialog;
import org.telegram.ui.Components.voip.VoIPButtonsLayout;
import org.telegram.ui.Components.voip.VoIPFloatingLayout;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.Components.voip.VoIPNotificationsLayout;
import org.telegram.ui.Components.voip.VoIPPiPView;
import org.telegram.ui.Components.voip.VoIPStatusTextView;
import org.telegram.ui.Components.voip.VoIPTextureView;
import org.telegram.ui.Components.voip.VoIPToggleButton;
import org.telegram.ui.Components.voip.VoIPWindowView;
import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.RendererCommon;
import org.webrtc.TextureViewRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class VoIPFragment implements VoIPService.StateListener, NotificationCenter.NotificationCenterDelegate {

    private final static int STATE_GONE = 0;
    private final static int STATE_FULLSCREEN = 1;
    private final static int STATE_FLOATING = 2;

    private final static int topNavigationColor = 0x66000000;
    private final static int bottomNavigationColor = 0x7f000000;
    private final static int EMOJI_SIZE = 32;
    private final static int EMOJI_SIZE_BIG = 48;
    private final static int EMOJI_COUNT = 4;
    private final static int EMOJI_BIG_HOR_PADDING = 12;
    private final static int EMOJI_HOR_PADDING = 4;
    private final static int EMOJI_BIG_MARGIN_TOP = 120;
    private final static int EMOJI_MARGIN_TOP = 12;

    private final int USER_PHOTO_BLOB_INNER_RADIUS = AndroidUtilities.dp(78);
    private final int USER_PHOTO_BLOB_OUTER_RADIUS = AndroidUtilities.dp(88);
    private final int USER_PHOTO_BLOB_AMPLITUDE = AndroidUtilities.dp(8);

    private final int currentAccount;
    private final int BG_CHANGE_DURATION = 4000;
    private final int BG_GREEN_CIRCLE_DURATION = 800;

    private static final int BTN_OFF_COLOR = 0xED000000;
    private final int ACCEPT_INNNER_RADIUS = AndroidUtilities.dp(30);
    private final int ACCEPT_OUTER_RADIUS = AndroidUtilities.dp(36);

    Activity activity;

    TLRPC.User currentUser;
    TLRPC.User callingUser;

    VoIPToggleButton[] bottomButtons = new VoIPToggleButton[4];

    private ColoredInsetConstraintLayout fragmentView;
    private VoIpBackgroundView mainBackgroundView;
    private View topInsetView, bottomInsetView;

    private BlobView callingUserPhotoBlobView;
    private BackupImageView callingUserPhoto;
    private BackupImageView callingUserPhotoViewMini;
    @FloatRange(from = 0f, to = 1f)
    private float micAmplitude = 0;
    private float maxMicAmlitude = 1f;
    private Transition userPhotoBlobTransition;

    private TextView callingUserTitle;

    private VoIPStatusTextView statusTextView;
    private ImageView backIcon;
    private ImageView speakerPhoneIcon;

    private VoIPPinchZoomFrameLayout pinchZoomLayout;
    private ViewGroup emojiFrame;
    private View emojiBackground, btnHideEmoji;
    private LinearLayout emojiLayout;
    private TextView emojiRationalTextView, emojiEncriptionTextView;
    private ImageView[] emojiViews = new ImageView[EMOJI_COUNT];
    private Emoji.EmojiDrawable[] emojiDrawables = new Emoji.EmojiDrawable[EMOJI_COUNT];
    private LinearLayout statusLayout;
    private VoIPFloatingLayout currentUserCameraFloatingLayout;
    private VoIPFloatingLayout callingUserMiniFloatingLayout;
    private boolean currentUserCameraIsFullscreen;

    private TextureViewRenderer callingUserMiniTextureRenderer;
    private VoIPTextureView callingUserTextureView;
    private VoIPTextureView currentUserTextureView;

    private ViewGroup acceptDeclineView;
    private TextView acceptCallText, declineCallText;
    private AnimatorSet acceptCallBtnAnimation;

    private BlobView btnAcceptCallBlob;
    private View btnDeclineCall, btnAcceptCall;
    boolean isAcceptDeclineShowed = true;
    boolean isRetryMode = false;

    View bottomShadow;
    View topShadow;

    private VoIPButtonsLayout buttonsLayout;

    boolean isOutgoing;
    boolean callingUserIsVideo;
    boolean currentUserIsVideo;

    private PrivateVideoPreviewDialog previewDialog;

    private int currentState;
    private int previousState;
    private WindowInsets lastInsets;

    private static VoIPFragment instance;
    private VoIPWindowView windowView;
//    private int statusLayoutAnimateToOffset;

    private AccessibilityManager accessibilityManager;

    private boolean uiVisible = true;
    float uiVisibilityAlpha = 1f;
    private boolean canHideUI;
    private Animator cameraShowingAnimator;
    private boolean emojiLoaded;
    private boolean emojiExpanded;

    private boolean canSwitchToPip;
    private boolean switchingToPip;

    private float enterTransitionProgress;
    private boolean isFinished;
    boolean cameraForceExpanded;
    boolean enterFromPiP;
    private boolean deviceIsLocked;

    long lastContentTapTime;
    int animationIndex = -1;
    VoIPNotificationsLayout notificationsLayout;

    HintView tapToVideoTooltip;

    boolean hideUiRunnableWaiting;
    Runnable hideUIRunnable = () -> {
        hideUiRunnableWaiting = false;
        if (canHideUI && uiVisible && !emojiExpanded) {
            lastContentTapTime = System.currentTimeMillis();
            showUi(false);
            previousState = currentState;
            updateViewState();
        }
    };
    private boolean lockOnScreen;
    private boolean screenWasWakeup;
    private boolean isVideoCall;

    private final int FRAME_RATE = 60;
    private Timer timer = new Timer();
    private boolean isAnimationsPaused = false;

    public static void show(Activity activity, int account) {
        show(activity, false, account);
    }

    public static void show(Activity activity, boolean overlay, int account) {
        if (instance != null && instance.windowView.getParent() == null) {
            if (instance != null) {
                instance.callingUserTextureView.renderer.release();
                instance.currentUserTextureView.renderer.release();
                instance.callingUserMiniTextureRenderer.release();
                instance.destroy();
            }
            instance = null;
        }
        if (instance != null || activity.isFinishing()) {
            return;
        }
        boolean transitionFromPip = VoIPPiPView.getInstance() != null;
        if (VoIPService.getSharedInstance() == null || VoIPService.getSharedInstance().getUser() == null) {
            return;
        }
        VoIPFragment fragment = new VoIPFragment(account);
        fragment.activity = activity;
        instance = fragment;
        VoIPWindowView windowView = new VoIPWindowView(activity, !transitionFromPip) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (fragment.isFinished || fragment.switchingToPip) {
                    return false;
                }
                final int keyCode = event.getKeyCode();
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !fragment.lockOnScreen) {
                    fragment.onBackPressed();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (fragment.currentState == VoIPService.STATE_WAITING_INCOMING) {
                        final VoIPService service = VoIPService.getSharedInstance();
                        if (service != null) {
                            service.stopRinging();
                            return true;
                        }
                    }
                }
                return super.dispatchKeyEvent(event);
            }
        };
        instance.deviceIsLocked = ((KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode();

        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        boolean screenOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = pm.isInteractive();
        } else {
            screenOn = pm.isScreenOn();
        }
        instance.screenWasWakeup = !screenOn;
        windowView.setLockOnScreen(instance.deviceIsLocked);
        fragment.windowView = windowView;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            windowView.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    fragment.setInsets(windowInsets);
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    return WindowInsets.CONSUMED;
                } else {
                    return windowInsets.consumeSystemWindowInsets();
                }
            });
        }

        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = windowView.createWindowLayoutParams();
        if (overlay) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }
        }
        wm.addView(windowView, layoutParams);
        View view = fragment.createView(activity, windowView);
        windowView.addView(view);

        if (transitionFromPip) {
            fragment.enterTransitionProgress = 0f;
            fragment.startTransitionFromPiP();
        } else {
            fragment.enterTransitionProgress = 1f;
        }
    }

    private void onBackPressed() {
        if (isFinished || switchingToPip) {
            return;
        }
        if (previewDialog != null) {
            previewDialog.dismiss(false, false);
            return;
        }
        if (callingUserIsVideo && currentUserIsVideo && cameraForceExpanded) {
            cameraForceExpanded = false;
            currentUserCameraFloatingLayout.setRelativePosition(callingUserMiniFloatingLayout);
            currentUserCameraIsFullscreen = false;
            previousState = currentState;
            updateViewState();
            return;
        }

        if (canSwitchToPip && !lockOnScreen) {
            if (AndroidUtilities.checkInlinePermissions(activity)) {
                switchToPip();
            } else {
                requestInlinePermissions();
            }
        } else {
            windowView.finish();
        }
    }

    public static void clearInstance() {
        if (instance != null) {
            if (VoIPService.getSharedInstance() != null) {
                int h = instance.windowView.getMeasuredHeight();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && instance.lastInsets != null) {
                    h -= instance.lastInsets.getSystemWindowInsetBottom();
                }
                if (instance.canSwitchToPip) {
                    VoIPPiPView.show(instance.activity, instance.currentAccount, instance.windowView.getMeasuredWidth(), h, VoIPPiPView.ANIMATION_ENTER_TYPE_SCALE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && instance.lastInsets != null) {
                        VoIPPiPView.topInset = instance.lastInsets.getSystemWindowInsetTop();
                        VoIPPiPView.bottomInset = instance.lastInsets.getSystemWindowInsetBottom();
                    }
                }
            }
            instance.callingUserTextureView.renderer.release();
            instance.currentUserTextureView.renderer.release();
            instance.callingUserMiniTextureRenderer.release();
            instance.destroy();
        }
        instance = null;
    }

    public static VoIPFragment getInstance() {
        return instance;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setInsets(WindowInsets windowInsets) {
        lastInsets = windowInsets;

        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) topInsetView.getLayoutParams();
        layoutParams.height = lastInsets.getSystemWindowInsetTop();
        ConstraintLayout.LayoutParams layoutParams2 = (ConstraintLayout.LayoutParams) bottomInsetView.getLayoutParams();
        layoutParams2.height = lastInsets.getSystemWindowInsetBottom();

        currentUserCameraFloatingLayout.setInsets(lastInsets);
        callingUserMiniFloatingLayout.setInsets(lastInsets);
        fragmentView.setInsets(lastInsets);
        fragmentView.requestLayout();
        if (previewDialog != null) {
            previewDialog.setBottomPadding(lastInsets.getSystemWindowInsetBottom());
        }
    }

    public VoIPFragment(int account) {
        currentAccount = account;
        currentUser = MessagesController.getInstance(currentAccount).getUser(UserConfig.getInstance(currentAccount).getClientUserId());
        callingUser = VoIPService.getSharedInstance().getUser();
        VoIPService.getSharedInstance().registerStateListener(this);
        isOutgoing = VoIPService.getSharedInstance().isOutgoing();
        previousState = -1;
        currentState = VoIPService.getSharedInstance().getCallState();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.voipServiceCreated);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.closeInCallActivity);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.webRtcMicAmplitudeEvent);
    }

    private void destroy() {
        final VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            service.unregisterStateListener(this);
        }
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.voipServiceCreated);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.closeInCallActivity);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.webRtcMicAmplitudeEvent);

        stopAnimations();
    }

    @Override
    public void onStateChanged(int state) {
        if (currentState != state) {
            previousState = currentState;
            currentState = state;
            if (windowView != null) {

                setBackgroundState(currentState);
                switch (state) {
                    case VoIPService.STATE_ESTABLISHED:
                        if (isOutgoing) {
                            startGreenBGAnimation(callingUserPhoto);
                        } else {
                            acceptCallBtnAnimation.cancel();
                        }
                        break;
                }

                updateViewState();
            }
        }
    }

    @Override
    public void onProximitySensor(boolean screenLocked) {
        setAnimationsPaused(screenLocked);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.voipServiceCreated) {
            if (currentState == VoIPService.STATE_BUSY && VoIPService.getSharedInstance() != null) {
                currentUserTextureView.renderer.release();
                callingUserTextureView.renderer.release();
                callingUserMiniTextureRenderer.release();
                initRenderers();
                VoIPService.getSharedInstance().registerStateListener(this);
            }
        } else if (id == NotificationCenter.emojiLoaded) {
            updateKeyView(true);
        } else if (id == NotificationCenter.closeInCallActivity) {
            windowView.finish();
        } else if (id == NotificationCenter.webRtcMicAmplitudeEvent) {
            if (args != null && args.length > 0) {
                float a = (float) args[0];
                if (a > maxMicAmlitude) {
                    maxMicAmlitude = Math.min(3, a);
                    a = maxMicAmlitude;
                }
                a = a / maxMicAmlitude - 0.01f;
                micAmplitude = Math.max(0, Math.min(a, 1));

                if (micAmplitude > 0f) {
                    animateUserPhotoBlobSize(micAmplitude);
                }
            }
        }
    }

    @Override
    public void onSignalBarsCountChanged(int count) {
        if (statusTextView != null) {
            statusTextView.setSignalBarCount(count);
        }
    }

    @Override
    public void onAudioSettingsChanged() {
        updateButtons(true);
    }

    @Override
    public void onMediaStateUpdated(int audioState, int videoState) {
        previousState = currentState;
        if (videoState == Instance.VIDEO_STATE_ACTIVE && !isVideoCall) {
            isVideoCall = true;
        }
        updateViewState();
    }

    @Override
    public void onCameraSwitch(boolean isFrontFace) {
        previousState = currentState;
        updateViewState();
    }

    @Override
    public void onVideoAvailableChange(boolean isAvailable) {
        previousState = currentState;
        if (isAvailable && !isVideoCall) {
            isVideoCall = true;
        }
        updateViewState();
    }

    @Override
    public void onScreenOnChange(boolean screenOn) {

    }

    public View createView(Context context, ViewGroup parent) {
        accessibilityManager = ContextCompat.getSystemService(context, AccessibilityManager.class);
        fragmentView = (ColoredInsetConstraintLayout) LayoutInflater.from(context).inflate(R.layout.screen_voip, parent, false);

        topInsetView = fragmentView.findViewById(R.id.topInsetView);
        bottomInsetView = fragmentView.findViewById(R.id.bottomInsetView);

        mainBackgroundView = fragmentView.findViewById(R.id.mainBackgroundView);

        pinchZoomLayout = fragmentView.findViewById(R.id.pinch_zoom_view);
        pinchZoomLayout.setCallback(new CallBackgroundViewCallback() {
            @Override
            public void onTap(long time) {
                if (time - lastContentTapTime > 300) {
                    lastContentTapTime = System.currentTimeMillis();
                    if (emojiExpanded) {
                        expandEmoji(false);
                    } else if (canHideUI) {
                        showUi(!uiVisible);
                    }
                }
            }

            @Override
            public VoIPTextureView getFullscreenTextureView() {
                return VoIPFragment.this.getFullscreenTextureView();
            }
        });

        callingUserPhotoBlobView = fragmentView.findViewById(R.id.userPhotoBlobs);
        callingUserPhotoBlobView.init(USER_PHOTO_BLOB_INNER_RADIUS, USER_PHOTO_BLOB_OUTER_RADIUS, USER_PHOTO_BLOB_AMPLITUDE, 0x24ffffff, 0x14ffffff);

        callingUserPhoto = fragmentView.findViewById(R.id.callingUserPhoto);
        callingUserPhoto.setRoundRadius(AndroidUtilities.dp(71));
        callingUserTextureView = fragmentView.findViewById(R.id.callingUserTextureView);
        callingUserTextureView.init(false, true, false, false);
        callingUserTextureView.renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        callingUserTextureView.renderer.setEnableHardwareScaler(true);
        callingUserTextureView.renderer.setRotateTextureWithScreen(true);
        callingUserTextureView.scaleType = VoIPTextureView.SCALE_TYPE_FIT;

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(callingUser);
        callingUserPhoto.setForUserOrChat(callingUser, avatarDrawable);

        currentUserCameraFloatingLayout = fragmentView.findViewById(R.id.currentUserCameraFloatingLayout);
        currentUserCameraFloatingLayout.setDelegate((progress, value) -> currentUserTextureView.setScreenshareMiniProgress(progress, value));
        currentUserCameraFloatingLayout.setRelativePosition(1f, 1f);
        currentUserCameraFloatingLayout.setOnTapListener(view -> {
            if (currentUserIsVideo && callingUserIsVideo && System.currentTimeMillis() - lastContentTapTime > 500) {
                AndroidUtilities.cancelRunOnUIThread(hideUIRunnable);
                hideUiRunnableWaiting = false;
                lastContentTapTime = System.currentTimeMillis();
                callingUserMiniFloatingLayout.setRelativePosition(currentUserCameraFloatingLayout);
                currentUserCameraIsFullscreen = true;
                cameraForceExpanded = true;
                previousState = currentState;
                updateViewState();
            }
        });

        currentUserCameraIsFullscreen = true;
        currentUserTextureView = fragmentView.findViewById(R.id.currentUserTextureView);
        currentUserTextureView.init(true, false);
        currentUserTextureView.renderer.setIsCamera(true);
        currentUserTextureView.renderer.setUseCameraRotation(true);
        currentUserTextureView.renderer.setMirror(true);

        callingUserMiniFloatingLayout = fragmentView.findViewById(R.id.callingUserMiniFloatingLayout);
        callingUserMiniFloatingLayout.alwaysFloating = true;
        callingUserMiniFloatingLayout.setFloatingMode(true, false);
        callingUserMiniTextureRenderer = fragmentView.findViewById(R.id.callingUserMiniTextureRenderer);
        callingUserMiniTextureRenderer.setEnableHardwareScaler(true);
        callingUserMiniTextureRenderer.setIsCamera(false);
        callingUserMiniTextureRenderer.setFpsReduction(30);
        callingUserMiniTextureRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        callingUserMiniFloatingLayout.setOnTapListener(view -> {
            if (cameraForceExpanded && System.currentTimeMillis() - lastContentTapTime > 500) {
                AndroidUtilities.cancelRunOnUIThread(hideUIRunnable);
                hideUiRunnableWaiting = false;
                lastContentTapTime = System.currentTimeMillis();
                currentUserCameraFloatingLayout.setRelativePosition(callingUserMiniFloatingLayout);
                currentUserCameraIsFullscreen = false;
                cameraForceExpanded = false;
                previousState = currentState;
                updateViewState();
            }
        });

        bottomShadow = fragmentView.findViewById(R.id.bottomShadow);
        bottomShadow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.TRANSPARENT, bottomNavigationColor}));

        topShadow = fragmentView.findViewById(R.id.topShadow);
        topShadow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{topNavigationColor, Color.TRANSPARENT}));


        emojiLayout = fragmentView.findViewById(R.id.emojiLayout);
        emojiFrame = fragmentView.findViewById(R.id.emojiFrame);
        emojiBackground = fragmentView.findViewById(R.id.emojiBackground);
        btnHideEmoji = fragmentView.findViewById(R.id.btnHideEmoji);

        OnClickListener emojiClickListener = view -> {
            if (System.currentTimeMillis() - lastContentTapTime < 500) {
                return;
            }
            lastContentTapTime = System.currentTimeMillis();
            if (emojiLoaded) {
                expandEmoji(!emojiExpanded);
            }
        };
        emojiLayout.setOnClickListener(emojiClickListener);
        emojiBackground.setOnClickListener(emojiClickListener);
        btnHideEmoji.setOnClickListener(emojiClickListener);

        emojiRationalTextView = fragmentView.findViewById(R.id.emojiRationalTextView);
        emojiRationalTextView.setText(LocaleController.formatString("CallEmojiKeyTooltip", R.string.CallEmojiKeyTooltip, UserObject.getFirstName(callingUser)));
        emojiEncriptionTextView = fragmentView.findViewById(R.id.emojiEncriptionTextView);

        for (int i = 0; i < EMOJI_COUNT; i++) {
            emojiViews[i] = new ImageView(context);
            emojiViews[i].setScaleType(ImageView.ScaleType.FIT_XY);
            emojiLayout.addView(emojiViews[i], LayoutHelper.createLinear(EMOJI_SIZE, EMOJI_SIZE, i == 0 ? 0 : EMOJI_HOR_PADDING, 0, 0, 0));
        }
        statusLayout = fragmentView.findViewById(R.id.statusLayout);

        callingUserPhotoViewMini = fragmentView.findViewById(R.id.callingUserPhotoViewMini);
        callingUserPhotoViewMini.setImage(ImageLocation.getForUserOrChat(callingUser, ImageLocation.TYPE_SMALL), null, Theme.createCircleDrawable(AndroidUtilities.dp(135), 0xFF000000), callingUser);
        callingUserPhotoViewMini.setRoundRadius(AndroidUtilities.dp(135) / 2);

        callingUserTitle = fragmentView.findViewById(R.id.callingUserTitle);
        CharSequence name = ContactsController.formatName(callingUser.first_name, callingUser.last_name);
        name = Emoji.replaceEmoji(name, callingUserTitle.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20), false);
        callingUserTitle.setText(name);
        callingUserTitle.setShadowLayer(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(.666666667f), 0x4C000000);

        statusTextView = fragmentView.findViewById(R.id.statusTextView);
        buttonsLayout = fragmentView.findViewById(R.id.buttonsLayout);
        for (int i = 0; i < 4; i++) {
            bottomButtons[i] = new VoIPToggleButton(context);
            buttonsLayout.addView(bottomButtons[i]);
        }
        btnAcceptCall = fragmentView.findViewById(R.id.acceptCall);
        btnAcceptCall.setOnClickListener(v -> {
            onClickAcceptCall();
        });
        btnDeclineCall = fragmentView.findViewById(R.id.declineCall);
        btnDeclineCall.setOnClickListener(v -> {
            onClickDeclineCall();
        });
        btnAcceptCallBlob = fragmentView.findViewById(R.id.acceptCallBlob);
        btnAcceptCallBlob.init(ACCEPT_INNNER_RADIUS, ACCEPT_OUTER_RADIUS, AndroidUtilities.dp(3), 0x24ffffff, 0x14ffffff);

        acceptCallText = fragmentView.findViewById(R.id.acceptCallText);
        acceptCallText.setText(LocaleController.getString("AcceptCall", R.string.AcceptCall));

        declineCallText = fragmentView.findViewById(R.id.declineCallText);
        declineCallText.setText(LocaleController.getString("DeclineCall", R.string.DeclineCall));

        acceptDeclineView = fragmentView.findViewById(R.id.acceptDeclineView);

        backIcon = fragmentView.findViewById(R.id.backIcon);
        backIcon.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
        backIcon.setContentDescription(LocaleController.getString("Back", R.string.Back));
        backIcon.setOnClickListener(view -> {
            if (!lockOnScreen) {
                onBackPressed();
            }
        });
        if (windowView.isLockOnScreen()) {
            backIcon.setVisibility(View.GONE);
        }

        speakerPhoneIcon = fragmentView.findViewById(R.id.speakerPhoneIcon);
        speakerPhoneIcon.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(ToggleButton.class.getName());
                info.setCheckable(true);
                VoIPService service = VoIPService.getSharedInstance();
                if (service != null) {
                    info.setChecked(service.isSpeakerphoneOn());
                }
            }
        });
        speakerPhoneIcon.setContentDescription(LocaleController.getString("VoipSpeaker", R.string.VoipSpeaker));
        speakerPhoneIcon.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
        speakerPhoneIcon.setOnClickListener(view -> {
            if (speakerPhoneIcon.getTag() == null) {
                return;
            }
            if (VoIPService.getSharedInstance() != null) {
                VoIPService.getSharedInstance().toggleSpeakerphoneOrShowRouteSheet(activity, false);
            }
        });

        notificationsLayout = fragmentView.findViewById(R.id.notificationsLayout);

        tapToVideoTooltip = fragmentView.findViewById(R.id.tapToVideoTooltip);
        tapToVideoTooltip.init(4, false, null);
        tapToVideoTooltip.setText(LocaleController.getString("TapToTurnCamera", R.string.TapToTurnCamera));
        tapToVideoTooltip.setBottomOffset(AndroidUtilities.dp(4));
        tapToVideoTooltip.setBackgroundColor(ContextCompat.getColor(context, R.color.call_voip_tooltip_bg), Color.WHITE);

        updateViewState();

        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            if (!isVideoCall) {
                isVideoCall = service.privateCall != null && service.privateCall.video;
            }
            initRenderers();
        }

        startAnimations();
        if (!isOutgoing) {
            animateAcceptCallButton();
        }
        return fragmentView;
    }

    private void onClickDeclineCall() {
        if (currentState == VoIPService.STATE_BUSY) {
            windowView.finish();
        } else {
            if (VoIPService.getSharedInstance() != null) {
                VoIPService.getSharedInstance().declineIncomingCall();
            }
        }
    }

    private void onClickAcceptCall() {
        if (currentState == VoIPService.STATE_BUSY) {
            Intent intent = new Intent(activity, VoIPService.class);
            intent.putExtra("user_id", callingUser.id);
            intent.putExtra("is_outgoing", true);
            intent.putExtra("start_incall_activity", false);
            intent.putExtra("video_call", isVideoCall);
            intent.putExtra("can_video_call", isVideoCall);
            intent.putExtra("account", currentAccount);
            try {
                activity.startService(intent);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                if (VoIPService.getSharedInstance() != null) {
                    VoIPService.getSharedInstance().acceptIncomingCall();
                    if (currentUserIsVideo) {
                        VoIPService.getSharedInstance().requestVideoCall(false);
                    }
                }
            }
        }
    }

    private VoIPTextureView getFullscreenTextureView() {
        if (callingUserIsVideo) {
            return callingUserTextureView;
        }
        return currentUserTextureView;
    }

    private void initRenderers() {
        currentUserTextureView.renderer.init(VideoCapturerDevice.getEglBase().getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {
                AndroidUtilities.runOnUIThread(() -> updateViewState());
            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

            }

        });
        callingUserTextureView.renderer.init(VideoCapturerDevice.getEglBase().getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {
                AndroidUtilities.runOnUIThread(() -> updateViewState());
            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

            }

        }, EglBase.CONFIG_PLAIN, new GlRectDrawer());

        callingUserMiniTextureRenderer.init(VideoCapturerDevice.getEglBase().getEglBaseContext(), null);
    }

    public void switchToPip() {
        if (isFinished || !AndroidUtilities.checkInlinePermissions(activity) || instance == null) {
            return;
        }
        isFinished = true;
        if (VoIPService.getSharedInstance() != null) {
            int h = instance.windowView.getMeasuredHeight();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && instance.lastInsets != null) {
                h -= instance.lastInsets.getSystemWindowInsetBottom();
            }
            VoIPPiPView.show(instance.activity, instance.currentAccount, instance.windowView.getMeasuredWidth(), h, VoIPPiPView.ANIMATION_ENTER_TYPE_TRANSITION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && instance.lastInsets != null) {
                VoIPPiPView.topInset = instance.lastInsets.getSystemWindowInsetTop();
                VoIPPiPView.bottomInset = instance.lastInsets.getSystemWindowInsetBottom();
            }
        }
        if (VoIPPiPView.getInstance() == null) {
            return;
        }

        speakerPhoneIcon.animate().alpha(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        backIcon.animate().alpha(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        statusLayout.animate().alpha(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        buttonsLayout.animate().alpha(0).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        callingUserMiniFloatingLayout.animate().alpha(0).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        notificationsLayout.animate().alpha(0).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        mainBackgroundView.setAlpha(0);
        emojiLayout.animate().alpha(0).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        topShadow.animate().alpha(0).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();

        showShadowViews(false);

        VoIPPiPView.switchingToPip = true;
        switchingToPip = true;
        Animator animator = createPiPTransition(false);
        animationIndex = NotificationCenter.getInstance(currentAccount).setAnimationInProgress(animationIndex, null);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                VoIPPiPView.getInstance().windowView.setAlpha(1f);
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).onAnimationFinish(animationIndex);
                    VoIPPiPView.getInstance().onTransitionEnd();
                    currentUserCameraFloatingLayout.setCornerRadius(-1f);
                    callingUserTextureView.renderer.release();
                    currentUserTextureView.renderer.release();
                    callingUserMiniTextureRenderer.release();
                    destroy();
                    windowView.finishImmediate();
                    VoIPPiPView.switchingToPip = false;
                    switchingToPip = false;
                    instance = null;
                }, 200);
            }
        });
        animator.setDuration(350);
        animator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        animator.start();
    }

    public void startTransitionFromPiP() {
        enterFromPiP = true;
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null && service.getVideoState(false) == Instance.VIDEO_STATE_ACTIVE) {
            callingUserTextureView.setStub(VoIPPiPView.getInstance().callingUserTextureView);
            currentUserTextureView.setStub(VoIPPiPView.getInstance().currentUserTextureView);
        }
        windowView.setAlpha(0f);
        updateViewState();
        switchingToPip = true;
        VoIPPiPView.switchingToPip = true;
        VoIPPiPView.prepareForTransition();
        animationIndex = NotificationCenter.getInstance(currentAccount).setAnimationInProgress(animationIndex, null);
        AndroidUtilities.runOnUIThread(() -> {
            windowView.setAlpha(1f);
            Animator animator = createPiPTransition(true);

            backIcon.setAlpha(0f);
            statusLayout.setAlpha(0f);
            buttonsLayout.setAlpha(0f);
            speakerPhoneIcon.setAlpha(0f);
            notificationsLayout.setAlpha(0f);
            mainBackgroundView.setAlpha(0f);
            emojiLayout.setAlpha(0f);

            currentUserCameraFloatingLayout.switchingToPip = true;
            AndroidUtilities.runOnUIThread(() -> {
                VoIPPiPView.switchingToPip = false;
                VoIPPiPView.finish();

                speakerPhoneIcon.animate().setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                backIcon.animate().alpha(1f).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                statusLayout.animate().alpha(1f).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                buttonsLayout.animate().alpha(1f).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                notificationsLayout.animate().alpha(1f).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                mainBackgroundView.animate().alpha(1f).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                emojiLayout.animate().alpha(1f).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT).start();

                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        NotificationCenter.getInstance(currentAccount).onAnimationFinish(animationIndex);
                        currentUserCameraFloatingLayout.setCornerRadius(-1f);
                        switchingToPip = false;
                        currentUserCameraFloatingLayout.switchingToPip = false;
                        previousState = currentState;
                        updateViewState();
                    }
                });
                animator.setDuration(350);
                animator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                animator.start();
            }, 32);
        }, 32);

    }

    public Animator createPiPTransition(boolean enter) {
        currentUserCameraFloatingLayout.animate().cancel();
        float toX = VoIPPiPView.getInstance().windowLayoutParams.x + VoIPPiPView.getInstance().xOffset;
        float toY = VoIPPiPView.getInstance().windowLayoutParams.y + VoIPPiPView.getInstance().yOffset;

        float cameraFromX = currentUserCameraFloatingLayout.getX();
        float cameraFromY = currentUserCameraFloatingLayout.getY();
        float cameraFromScale = currentUserCameraFloatingLayout.getScaleX();
        boolean animateCamera = true;

        float callingUserFromX = 0;
        float callingUserFromY = 0;
        float callingUserFromScale = 1f;
        float callingUserToScale, callingUserToX, callingUserToY;
        float cameraToScale, cameraToX, cameraToY;

        float pipScale = VoIPPiPView.isExpanding() ? 0.4f : 0.25f;
        callingUserToScale = pipScale;
        callingUserToX = toX - (callingUserTextureView.getMeasuredWidth() - callingUserTextureView.getMeasuredWidth() * callingUserToScale) / 2f;
        callingUserToY = toY - (callingUserTextureView.getMeasuredHeight() - callingUserTextureView.getMeasuredHeight() * callingUserToScale) / 2f;
        if (callingUserIsVideo) {
            int currentW = currentUserCameraFloatingLayout.getMeasuredWidth();
            if (currentUserIsVideo && currentW != 0) {
                cameraToScale = (windowView.getMeasuredWidth() / (float) currentW) * pipScale * 0.4f;
                cameraToX = toX - (currentUserCameraFloatingLayout.getMeasuredWidth() - currentUserCameraFloatingLayout.getMeasuredWidth() * cameraToScale) / 2f +
                        VoIPPiPView.getInstance().parentWidth * pipScale - VoIPPiPView.getInstance().parentWidth * pipScale * 0.4f - AndroidUtilities.dp(4);
                cameraToY = toY - (currentUserCameraFloatingLayout.getMeasuredHeight() - currentUserCameraFloatingLayout.getMeasuredHeight() * cameraToScale) / 2f +
                        VoIPPiPView.getInstance().parentHeight * pipScale - VoIPPiPView.getInstance().parentHeight * pipScale * 0.4f - AndroidUtilities.dp(4);
            } else {
                cameraToScale = 0;
                cameraToX = 1f;
                cameraToY = 1f;
                animateCamera = false;
            }
        } else {
            cameraToScale = pipScale;
            cameraToX = toX - (currentUserCameraFloatingLayout.getMeasuredWidth() - currentUserCameraFloatingLayout.getMeasuredWidth() * cameraToScale) / 2f;
            cameraToY = toY - (currentUserCameraFloatingLayout.getMeasuredHeight() - currentUserCameraFloatingLayout.getMeasuredHeight() * cameraToScale) / 2f;
        }

        float cameraCornerRadiusFrom = callingUserIsVideo ? AndroidUtilities.dp(4) : 0;
        float cameraCornerRadiusTo = AndroidUtilities.dp(4) * 1f / cameraToScale;

        float fromCameraAlpha = 1f;
        float toCameraAlpha = 1f;
        if (callingUserIsVideo) {
            fromCameraAlpha = VoIPPiPView.isExpanding() ? 1f : 0f;
        }

        if (enter) {
            if (animateCamera) {
                currentUserCameraFloatingLayout.setScaleX(cameraToScale);
                currentUserCameraFloatingLayout.setScaleY(cameraToScale);
                currentUserCameraFloatingLayout.setTranslationX(cameraToX);
                currentUserCameraFloatingLayout.setTranslationY(cameraToY);
                currentUserCameraFloatingLayout.setCornerRadius(cameraCornerRadiusTo);
                currentUserCameraFloatingLayout.setAlpha(fromCameraAlpha);
            }
            callingUserTextureView.setScaleX(callingUserToScale);
            callingUserTextureView.setScaleY(callingUserToScale);
            callingUserTextureView.setTranslationX(callingUserToX);
            callingUserTextureView.setTranslationY(callingUserToY);
            callingUserTextureView.setRoundCorners(AndroidUtilities.dp(6) * 1f / callingUserToScale);

        }
        ValueAnimator animator = ValueAnimator.ofFloat(enter ? 1f : 0, enter ? 0 : 1f);

        enterTransitionProgress = enter ? 0f : 1f;

        boolean finalAnimateCamera = animateCamera;
        float finalFromCameraAlpha = fromCameraAlpha;
        animator.addUpdateListener(valueAnimator -> {
            float v = (float) valueAnimator.getAnimatedValue();
            enterTransitionProgress = 1f - v;

            if (finalAnimateCamera) {
                float cameraScale = cameraFromScale * (1f - v) + cameraToScale * v;
                currentUserCameraFloatingLayout.setScaleX(cameraScale);
                currentUserCameraFloatingLayout.setScaleY(cameraScale);
                currentUserCameraFloatingLayout.setTranslationX(cameraFromX * (1f - v) + cameraToX * v);
                currentUserCameraFloatingLayout.setTranslationY(cameraFromY * (1f - v) + cameraToY * v);
                currentUserCameraFloatingLayout.setCornerRadius(cameraCornerRadiusFrom * (1f - v) + cameraCornerRadiusTo * v);
                currentUserCameraFloatingLayout.setAlpha(toCameraAlpha * (1f - v) + finalFromCameraAlpha * v);
            }

            float callingUserScale = callingUserFromScale * (1f - v) + callingUserToScale * v;
            callingUserTextureView.setScaleX(callingUserScale);
            callingUserTextureView.setScaleY(callingUserScale);
            float tx = callingUserFromX * (1f - v) + callingUserToX * v;
            float ty = callingUserFromY * (1f - v) + callingUserToY * v;

            callingUserTextureView.setTranslationX(tx);
            callingUserTextureView.setTranslationY(ty);
            callingUserTextureView.setRoundCorners(v * AndroidUtilities.dp(4) * 1 / callingUserScale);
            if (!currentUserCameraFloatingLayout.measuredAsFloatingMode) {
                currentUserTextureView.setScreenshareMiniProgress(v, false);
            }
        });
        return animator;
    }

    private void expandEmoji(boolean expanded) {
        if (!emojiLoaded || emojiExpanded == expanded || !uiVisible) {
            return;
        }
        emojiExpanded = expanded;

        TransitionSet transition = VoIPTransitions.emojiExpandTransition(expanded,
                btnHideEmoji, emojiBackground, emojiRationalTextView,
                emojiEncriptionTextView, emojiViews, callingUserPhotoBlobView,
                callingUserPhoto, emojiLayout, emojiFrame, statusLayout);
        TransitionManager.beginDelayedTransition(fragmentView, transition);

        ConstraintLayout.LayoutParams emojiFrameLP = (ConstraintLayout.LayoutParams) emojiFrame.getLayoutParams();
        int emojiFramePadding = AndroidUtilities.dp(24);
        if (expanded) {
            for (int i = 0; i < emojiViews.length; i++) {
                ImageView emoji = emojiViews[i];
                LinearLayout.LayoutParams emojiLP = (LinearLayout.LayoutParams) emoji.getLayoutParams();
                emojiLP.width = AndroidUtilities.dp(EMOJI_SIZE_BIG);
                emojiLP.height = AndroidUtilities.dp(EMOJI_SIZE_BIG);
                if (i != 0) {
                    emojiLP.leftMargin = AndroidUtilities.dp(EMOJI_BIG_HOR_PADDING);
                }
            }
            emojiFrameLP.topMargin = AndroidUtilities.dp(EMOJI_BIG_MARGIN_TOP);
            emojiFrame.setPadding(emojiFramePadding, emojiFramePadding, emojiFramePadding, emojiFramePadding);
            emojiBackground.setVisibility(View.VISIBLE);
            btnHideEmoji.setVisibility(View.VISIBLE);
            emojiRationalTextView.setVisibility(View.VISIBLE);
            emojiEncriptionTextView.setVisibility(View.VISIBLE);
            callingUserPhotoBlobView.setVisibility(View.GONE);
            callingUserPhoto.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < emojiViews.length; i++) {
                ImageView emoji = emojiViews[i];
                LinearLayout.LayoutParams emojiLP = (LinearLayout.LayoutParams) emoji.getLayoutParams();
                emojiLP.width = AndroidUtilities.dp(EMOJI_SIZE);
                emojiLP.height = AndroidUtilities.dp(EMOJI_SIZE);
                if (i != 0) {
                    emojiLP.leftMargin = AndroidUtilities.dp(EMOJI_HOR_PADDING);
                }
            }
            emojiFrameLP.topMargin = AndroidUtilities.dp(EMOJI_MARGIN_TOP);
            emojiFrame.setPadding(emojiFramePadding, 0, emojiFramePadding, emojiFramePadding);
            emojiBackground.setVisibility(View.GONE);
            btnHideEmoji.setVisibility(View.GONE);
            emojiRationalTextView.setVisibility(View.GONE);
            emojiEncriptionTextView.setVisibility(View.GONE);

            if (!currentUserIsVideo && !callingUserIsVideo) {
                callingUserPhotoBlobView.setVisibility(View.VISIBLE);
                callingUserPhoto.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateViewState() {
        if (isFinished || switchingToPip) {
            return;
        }
        lockOnScreen = false;
        boolean animated = previousState != -1;
        boolean showAcceptDeclineView = false;
        boolean showTimer = false;
        boolean showReconnecting = false;
        boolean showCallingAvatarMini = false;
        VoIPService service = VoIPService.getSharedInstance();

        switch (currentState) {
            case VoIPService.STATE_WAITING_INCOMING:
                showAcceptDeclineView = true;
                lockOnScreen = true;
                setRetryMode(false);
                if (service != null && service.privateCall.video) {
                    if (currentUserIsVideo && callingUser.photo != null) {
                        showCallingAvatarMini = true;
                    } else {
                        showCallingAvatarMini = false;
                    }
                    statusTextView.setText(LocaleController.getString("VoipInVideoCallBranding", R.string.VoipInVideoCallBranding), true, animated);
                } else {
                    statusTextView.setText(LocaleController.getString("VoipInCallBranding", R.string.VoipInCallBranding), true, animated);
                }
                break;
            case VoIPService.STATE_WAIT_INIT:
            case VoIPService.STATE_WAIT_INIT_ACK:
                statusTextView.setText(LocaleController.getString("VoipConnecting", R.string.VoipConnecting), true, animated);
                break;
            case VoIPService.STATE_EXCHANGING_KEYS:
                statusTextView.setText(LocaleController.getString("VoipExchangingKeys", R.string.VoipExchangingKeys), true, animated);
                break;
            case VoIPService.STATE_WAITING:
                statusTextView.setText(LocaleController.getString("VoipWaiting", R.string.VoipWaiting), true, animated);
                break;
            case VoIPService.STATE_RINGING:
                statusTextView.setText(LocaleController.getString("VoipRinging", R.string.VoipRinging), true, animated);
                break;
            case VoIPService.STATE_REQUESTING:
                statusTextView.setText(LocaleController.getString("VoipRequesting", R.string.VoipRequesting), true, animated);
                break;
            case VoIPService.STATE_HANGING_UP:
                break;
            case VoIPService.STATE_BUSY:
                showAcceptDeclineView = true;
                statusTextView.setText(LocaleController.getString("VoipBusy", R.string.VoipBusy), false, animated);
                setRetryMode(true);
                currentUserIsVideo = false;
                callingUserIsVideo = false;
                break;
            case VoIPService.STATE_ESTABLISHED:
                updateKeyView(animated);
                showTimer = true;
                break;
            case VoIPService.STATE_RECONNECTING:
                updateKeyView(animated);
                showTimer = true;
                showReconnecting = true;
                break;
            case VoIPService.STATE_ENDED:
                currentUserTextureView.saveCameraLastBitmap();
                AndroidUtilities.runOnUIThread(() -> windowView.finish(), 200);
                break;
            case VoIPService.STATE_FAILED:
                statusTextView.setText(LocaleController.getString("VoipFailed", R.string.VoipFailed), false, animated);
                processVoIPServiceError();
                break;
        }
        if (previewDialog != null) {
            return;
        }

        if (service != null) {
            callingUserIsVideo = service.getRemoteVideoState() == Instance.VIDEO_STATE_ACTIVE;
            currentUserIsVideo = service.getVideoState(false) == Instance.VIDEO_STATE_ACTIVE || service.getVideoState(false) == Instance.VIDEO_STATE_PAUSED;
            if (currentUserIsVideo && !isVideoCall) {
                isVideoCall = true;
            }
        }

        if (callingUserIsVideo) {
            if (animated) {
                callingUserTextureView.animate().alpha(1f).setDuration(250).start();
            } else {
                callingUserTextureView.animate().cancel();
                callingUserTextureView.setAlpha(1f);
            }
            if (!callingUserTextureView.renderer.isFirstFrameRendered() && !enterFromPiP) {
                callingUserIsVideo = false;
            }
        }

        if (currentUserIsVideo || callingUserIsVideo) {
            showShadowViews(true);
            callingUserPhoto.setVisibility(View.GONE);
            callingUserPhotoBlobView.setVisibility(View.GONE);
        } else {
            showShadowViews(false);
            if (!emojiExpanded) {
                callingUserPhoto.setVisibility(View.VISIBLE);
                callingUserPhotoBlobView.setVisibility(View.VISIBLE);
            }
            if (animated) {
                callingUserTextureView.animate().alpha(0f).setDuration(250).start();
            } else {
                callingUserTextureView.animate().cancel();
                callingUserTextureView.setAlpha(0f);
            }
        }

        if (!currentUserIsVideo || !callingUserIsVideo) {
            cameraForceExpanded = false;
        }

        boolean showCallingUserVideoMini = currentUserIsVideo && cameraForceExpanded;

        showCallingUserAvatarMini(showCallingAvatarMini, animated);
        showAcceptDeclineView(showAcceptDeclineView);
        windowView.setLockOnScreen(lockOnScreen || deviceIsLocked);
        canHideUI = (currentState == VoIPService.STATE_ESTABLISHED) && (currentUserIsVideo || callingUserIsVideo);
        if (!canHideUI && !uiVisible) {
            showUi(true);
        }

        if (uiVisible && canHideUI && !hideUiRunnableWaiting && service != null && !service.isMicMute()) {
            AndroidUtilities.runOnUIThread(hideUIRunnable, 3000);
            hideUiRunnableWaiting = true;
        } else if (service != null && service.isMicMute()) {
            AndroidUtilities.cancelRunOnUIThread(hideUIRunnable);
            hideUiRunnableWaiting = false;
        }

        if (currentState != VoIPService.STATE_HANGING_UP && currentState != VoIPService.STATE_ENDED) {
            updateButtons(animated);
        }

        if (showTimer) {
            statusTextView.showTimer(animated);
        }

        statusTextView.showReconnect(showReconnecting, animated);

        canSwitchToPip = (currentState != VoIPService.STATE_ENDED && currentState != VoIPService.STATE_BUSY) && (currentUserIsVideo || callingUserIsVideo);

        if (service != null) {
            if (currentUserIsVideo) {
                service.sharedUIParams.tapToVideoTooltipWasShowed = true;
            }
            currentUserTextureView.setIsScreencast(service.isScreencast());
            currentUserTextureView.renderer.setMirror(service.isFrontFaceCamera());
            service.setSinks(currentUserIsVideo && !service.isScreencast() ? currentUserTextureView.renderer : null, showCallingUserVideoMini ? callingUserMiniTextureRenderer : callingUserTextureView.renderer);

            if (animated) {
                notificationsLayout.beforeLayoutChanges();
            }
            if ((currentUserIsVideo || callingUserIsVideo) && (currentState == VoIPService.STATE_ESTABLISHED || currentState == VoIPService.STATE_RECONNECTING) && service.getCallDuration() > 500) {
                if (service.getRemoteAudioState() == Instance.AUDIO_STATE_MUTED) {
                    notificationsLayout.addNotification(R.drawable.calls_mute_mini, LocaleController.formatString("VoipUserMicrophoneIsOff", R.string.VoipUserMicrophoneIsOff, UserObject.getFirstName(callingUser)), "muted", animated);
                } else {
                    notificationsLayout.removeNotification("muted");
                }
                if (service.getRemoteVideoState() == Instance.VIDEO_STATE_INACTIVE) {
                    notificationsLayout.addNotification(R.drawable.calls_camera_mini, LocaleController.formatString("VoipUserCameraIsOff", R.string.VoipUserCameraIsOff, UserObject.getFirstName(callingUser)), "video", animated);
                } else {
                    notificationsLayout.removeNotification("video");
                }
            } else {
                if (service.getRemoteAudioState() == Instance.AUDIO_STATE_MUTED) {
                    notificationsLayout.addNotification(R.drawable.calls_mute_mini, LocaleController.formatString("VoipUserMicrophoneIsOff", R.string.VoipUserMicrophoneIsOff, UserObject.getFirstName(callingUser)), "muted", animated);
                } else {
                    notificationsLayout.removeNotification("muted");
                }
                notificationsLayout.removeNotification("video");
            }

            if (notificationsLayout.getChildCount() == 0 && callingUserIsVideo && service.privateCall != null && !service.privateCall.video && !service.sharedUIParams.tapToVideoTooltipWasShowed) {
                service.sharedUIParams.tapToVideoTooltipWasShowed = true;
                tapToVideoTooltip.showForView(bottomButtons[1], true);
            } else if (notificationsLayout.getChildCount() != 0) {
                tapToVideoTooltip.hide();
            }

            if (animated) {
                notificationsLayout.animateLayoutChanges();
            }
        }

        moveFloatingLayouts();

        if (currentUserIsVideo) {
            if (!callingUserIsVideo || cameraForceExpanded) {
                showFloatingLayout(STATE_FULLSCREEN, animated);
            } else {
                showFloatingLayout(STATE_FLOATING, animated);
            }
        } else {
            showFloatingLayout(STATE_GONE, animated);
        }

        if (showCallingUserVideoMini && callingUserMiniFloatingLayout.getTag() == null) {
            callingUserMiniFloatingLayout.setIsActive(true);
            if (callingUserMiniFloatingLayout.getVisibility() != View.VISIBLE) {
                callingUserMiniFloatingLayout.setVisibility(View.VISIBLE);
                callingUserMiniFloatingLayout.setAlpha(0f);
                callingUserMiniFloatingLayout.setScaleX(0.5f);
                callingUserMiniFloatingLayout.setScaleY(0.5f);
            }
            callingUserMiniFloatingLayout.animate().setListener(null).cancel();
            callingUserMiniFloatingLayout.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).setStartDelay(150).start();
            callingUserMiniFloatingLayout.setTag(1);
        } else if (!showCallingUserVideoMini && callingUserMiniFloatingLayout.getTag() != null) {
            callingUserMiniFloatingLayout.setIsActive(false);
            callingUserMiniFloatingLayout.animate().alpha(0).scaleX(0.5f).scaleY(0.5f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (callingUserMiniFloatingLayout.getTag() == null) {
                        callingUserMiniFloatingLayout.setVisibility(View.GONE);
                    }
                }
            }).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            callingUserMiniFloatingLayout.setTag(null);
        }

        currentUserCameraFloatingLayout.restoreRelativePosition();
        callingUserMiniFloatingLayout.restoreRelativePosition();

        updateSpeakerPhoneIcon();
    }

    private void processVoIPServiceError() {
        final VoIPService voipService = VoIPService.getSharedInstance();
        final String lastError = voipService != null ? voipService.getLastError() : Instance.ERROR_UNKNOWN;
        if (!TextUtils.equals(lastError, Instance.ERROR_UNKNOWN)) {
            if (TextUtils.equals(lastError, Instance.ERROR_INCOMPATIBLE)) {
                final String name = ContactsController.formatName(callingUser.first_name, callingUser.last_name);
                final String message = LocaleController.formatString("VoipPeerIncompatible", R.string.VoipPeerIncompatible, name);
                showErrorDialog(AndroidUtilities.replaceTags(message));
            } else if (TextUtils.equals(lastError, Instance.ERROR_PEER_OUTDATED)) {
                if (isVideoCall) {
                    showPeerOutdatedDialog();
                } else {
                    final String name = UserObject.getFirstName(callingUser);
                    final String message = LocaleController.formatString("VoipPeerOutdated", R.string.VoipPeerOutdated, name);
                    showErrorDialog(AndroidUtilities.replaceTags(message));
                }
            } else if (TextUtils.equals(lastError, Instance.ERROR_PRIVACY)) {
                final String name = ContactsController.formatName(callingUser.first_name, callingUser.last_name);
                final String message = LocaleController.formatString("CallNotAvailable", R.string.CallNotAvailable, name);
                showErrorDialog(AndroidUtilities.replaceTags(message));
            } else if (TextUtils.equals(lastError, Instance.ERROR_AUDIO_IO)) {
                showErrorDialog("Error initializing audio hardware");
            } else if (TextUtils.equals(lastError, Instance.ERROR_LOCALIZED)) {
                windowView.finish();
            } else if (TextUtils.equals(lastError, Instance.ERROR_CONNECTION_SERVICE)) {
                showErrorDialog(LocaleController.getString("VoipErrorUnknown", R.string.VoipErrorUnknown));
            } else {
                AndroidUtilities.runOnUIThread(() -> windowView.finish(), 1000);
            }
        } else {
            AndroidUtilities.runOnUIThread(() -> windowView.finish(), 1000);
        }
    }

    private void showPeerOutdatedDialog() {
        final String name = UserObject.getFirstName(callingUser);
        final String message = LocaleController.formatString("VoipPeerVideoOutdated", R.string.VoipPeerVideoOutdated, name);
        boolean[] callAgain = new boolean[1];
        AlertDialog dlg = new DarkAlertDialog.Builder(activity)
                .setTitle(LocaleController.getString("VoipFailed", R.string.VoipFailed))
                .setMessage(AndroidUtilities.replaceTags(message))
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> windowView.finish())
                .setPositiveButton(LocaleController.getString("VoipPeerVideoOutdatedMakeVoice", R.string.VoipPeerVideoOutdatedMakeVoice), (dialogInterface, i) -> {
                    callAgain[0] = true;
                    currentState = VoIPService.STATE_BUSY;
                    Intent intent = new Intent(activity, VoIPService.class);
                    intent.putExtra("user_id", callingUser.id);
                    intent.putExtra("is_outgoing", true);
                    intent.putExtra("start_incall_activity", false);
                    intent.putExtra("video_call", false);
                    intent.putExtra("can_video_call", false);
                    intent.putExtra("account", currentAccount);
                    try {
                        activity.startService(intent);
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                })
                .show();
        dlg.setCanceledOnTouchOutside(true);
        dlg.setOnDismissListener(dialog -> {
            if (!callAgain[0]) {
                windowView.finish();
            }
        });
    }

    private void showUi(boolean show) {
        TransitionSet set = VoIPTransitions.getShowUITransition(speakerPhoneIcon, backIcon, statusLayout, buttonsLayout, emojiLayout, notificationsLayout, notificationsLayout);
        set.addTransition(VoIPTransitions.getShowShadowsTransition(fragmentView, bottomShadow, topShadow));
        TransitionManager.beginDelayedTransition(fragmentView, set);

        if (show) {
            tapToVideoTooltip.hide();

            speakerPhoneIcon.setVisibility(View.VISIBLE);
            backIcon.setVisibility(View.VISIBLE);
            statusLayout.setVisibility(View.VISIBLE);
            buttonsLayout.setVisibility(View.VISIBLE);
            emojiLayout.setVisibility(View.VISIBLE);
        } else {
            speakerPhoneIcon.setVisibility(View.GONE);
            backIcon.setVisibility(View.GONE);
            statusLayout.setVisibility(View.GONE);
            buttonsLayout.setVisibility(View.GONE);
            emojiLayout.setVisibility(View.GONE);

            AndroidUtilities.cancelRunOnUIThread(hideUIRunnable);
            hideUiRunnableWaiting = false;
        }
        showShadowViews(show);

        buttonsLayout.setEnabled(show);

        uiVisible = show;
        windowView.requestFullscreen(!show);

        moveFloatingLayouts();
    }

    private void showShadowViews(boolean show) {
        if (show) {
            fragmentView.setTopColor(topNavigationColor);
            fragmentView.setBottomColor(bottomNavigationColor);
            bottomShadow.setVisibility(View.VISIBLE);
            topShadow.setVisibility(View.VISIBLE);
        } else {
            fragmentView.setTopColor(Color.TRANSPARENT);
            fragmentView.setBottomColor(Color.TRANSPARENT);
            bottomShadow.setVisibility(View.GONE);
            topShadow.setVisibility(View.GONE);
        }
    }

    private void moveFloatingLayouts() {
        currentUserCameraFloatingLayout.saveRelativePosition();
        callingUserMiniFloatingLayout.saveRelativePosition();
        callingUserMiniFloatingLayout.setBottomOffset(notificationsLayout.getChildsHight(), true);
        currentUserCameraFloatingLayout.setBottomOffset(notificationsLayout.getChildsHight(), true);
        currentUserCameraFloatingLayout.setUiVisible(uiVisible);
        callingUserMiniFloatingLayout.setUiVisible(uiVisible);
        currentUserCameraFloatingLayout.restoreRelativePosition();
        callingUserMiniFloatingLayout.restoreRelativePosition();
    }

    private void showFloatingLayout(int state, boolean animated) {
        if (currentUserCameraFloatingLayout.getTag() == null || (int) currentUserCameraFloatingLayout.getTag() != STATE_FLOATING) {
            currentUserCameraFloatingLayout.setUiVisible(uiVisible);
        }
        if (!animated && cameraShowingAnimator != null) {
            cameraShowingAnimator.removeAllListeners();
            cameraShowingAnimator.cancel();
        }
        if (state == STATE_GONE) {
            if (animated) {
                if (currentUserCameraFloatingLayout.getTag() != null && (int) currentUserCameraFloatingLayout.getTag() != STATE_GONE) {
                    if (cameraShowingAnimator != null) {
                        cameraShowingAnimator.removeAllListeners();
                        cameraShowingAnimator.cancel();
                    }
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(currentUserCameraFloatingLayout, View.ALPHA, currentUserCameraFloatingLayout.getAlpha(), 0)
                    );
                    if (currentUserCameraFloatingLayout.getTag() != null && (int) currentUserCameraFloatingLayout.getTag() == STATE_FLOATING) {
                        animatorSet.playTogether(
                                ObjectAnimator.ofFloat(currentUserCameraFloatingLayout, View.SCALE_X, currentUserCameraFloatingLayout.getScaleX(), 0.7f),
                                ObjectAnimator.ofFloat(currentUserCameraFloatingLayout, View.SCALE_Y, currentUserCameraFloatingLayout.getScaleX(), 0.7f)
                        );
                    }
                    cameraShowingAnimator = animatorSet;
                    cameraShowingAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            currentUserCameraFloatingLayout.setTranslationX(0);
                            currentUserCameraFloatingLayout.setTranslationY(0);
                            currentUserCameraFloatingLayout.setScaleY(1f);
                            currentUserCameraFloatingLayout.setScaleX(1f);
                            currentUserCameraFloatingLayout.setVisibility(View.GONE);
                        }
                    });
                    cameraShowingAnimator.setDuration(250).setInterpolator(CubicBezierInterpolator.DEFAULT);
                    cameraShowingAnimator.setStartDelay(50);
                    cameraShowingAnimator.start();
                }
            } else {
                currentUserCameraFloatingLayout.setVisibility(View.GONE);
            }
        } else {
            boolean switchToFloatAnimated = animated;
            if (currentUserCameraFloatingLayout.getTag() == null || (int) currentUserCameraFloatingLayout.getTag() == STATE_GONE) {
                switchToFloatAnimated = false;
            }
            if (animated) {
                if (currentUserCameraFloatingLayout.getTag() != null && (int) currentUserCameraFloatingLayout.getTag() == STATE_GONE) {
                    if (currentUserCameraFloatingLayout.getVisibility() == View.GONE) {
                        currentUserCameraFloatingLayout.setAlpha(0f);
                        currentUserCameraFloatingLayout.setScaleX(0.7f);
                        currentUserCameraFloatingLayout.setScaleY(0.7f);
                        currentUserCameraFloatingLayout.setVisibility(View.VISIBLE);
                    }
                    if (cameraShowingAnimator != null) {
                        cameraShowingAnimator.removeAllListeners();
                        cameraShowingAnimator.cancel();
                    }
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(currentUserCameraFloatingLayout, View.ALPHA, 0.0f, 1f),
                            ObjectAnimator.ofFloat(currentUserCameraFloatingLayout, View.SCALE_X, 0.7f, 1f),
                            ObjectAnimator.ofFloat(currentUserCameraFloatingLayout, View.SCALE_Y, 0.7f, 1f)
                    );
                    cameraShowingAnimator = animatorSet;
                    cameraShowingAnimator.setDuration(150).start();
                }
            } else {
                currentUserCameraFloatingLayout.setVisibility(View.VISIBLE);
            }
            if ((currentUserCameraFloatingLayout.getTag() == null || (int) currentUserCameraFloatingLayout.getTag() != STATE_FLOATING) && currentUserCameraFloatingLayout.relativePositionToSetX < 0) {
                currentUserCameraFloatingLayout.setRelativePosition(1f, 1f);
                currentUserCameraIsFullscreen = true;
            }
            currentUserCameraFloatingLayout.setFloatingMode(state == STATE_FLOATING, switchToFloatAnimated);
            currentUserCameraIsFullscreen = state != STATE_FLOATING;
        }
        currentUserCameraFloatingLayout.setTag(state);
    }

    private void showCallingUserAvatarMini(boolean show, boolean animated) {
        if (animated) {
            if (show && callingUserPhotoViewMini.getTag() == null) {
                callingUserPhotoViewMini.animate().setListener(null).cancel();
                callingUserPhotoViewMini.setVisibility(View.VISIBLE);
                callingUserPhotoViewMini.setAlpha(0);
                callingUserPhotoViewMini.setTranslationY(-AndroidUtilities.dp(135));
                callingUserPhotoViewMini.animate().alpha(1f).translationY(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            } else if (!show && callingUserPhotoViewMini.getTag() != null) {
                callingUserPhotoViewMini.animate().setListener(null).cancel();
                callingUserPhotoViewMini.animate().alpha(0).translationY(-AndroidUtilities.dp(135)).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                callingUserPhotoViewMini.setVisibility(View.GONE);
                            }
                        }).start();
            }
        } else {
            callingUserPhotoViewMini.animate().setListener(null).cancel();
            callingUserPhotoViewMini.setTranslationY(0);
            callingUserPhotoViewMini.setAlpha(1f);
            callingUserPhotoViewMini.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        callingUserPhotoViewMini.setTag(show ? 1 : null);
    }

    private void updateKeyView(boolean animated) {
        if (emojiLoaded) {
            return;
        }
        VoIPService service = VoIPService.getSharedInstance();
        if (service == null) {
            return;
        }
        byte[] auth_key = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(service.getEncryptionKey());
            buf.write(service.getGA());
            auth_key = buf.toByteArray();
        } catch (Exception checkedExceptionsAreBad) {
            FileLog.e(checkedExceptionsAreBad, false);
        }
        if (auth_key == null) {
            return;
        }
        byte[] sha256 = Utilities.computeSHA256(auth_key, 0, auth_key.length);
        String[] emoji = EncryptionKeyEmojifier.emojifyForCall(sha256);
        for (int i = 0; i < EMOJI_COUNT; i++) {
            Emoji.preloadEmoji(emoji[i]);
            Emoji.EmojiDrawable drawable = Emoji.getEmojiDrawable(emoji[i]);
            if (drawable != null) {
                drawable.setBounds(0, 0, AndroidUtilities.dp(EMOJI_SIZE), AndroidUtilities.dp(EMOJI_SIZE));
                drawable.preload();
                emojiViews[i].setImageDrawable(drawable);
                emojiViews[i].setContentDescription(emoji[i]);
                emojiViews[i].setVisibility(View.GONE);
            }
            emojiDrawables[i] = drawable;
        }
        checkEmojiLoaded(animated);
    }

    private void checkEmojiLoaded(boolean animated) {
        int count = 0;

        for (int i = 0; i < EMOJI_COUNT; i++) {
            if (emojiDrawables[i] != null && emojiDrawables[i].isLoaded()) {
                count++;
            }
        }

        if (count == EMOJI_COUNT) {
            emojiLoaded = true;
            for (int i = 0; i < EMOJI_COUNT; i++) {
                if (emojiViews[i].getVisibility() != View.VISIBLE) {
                    emojiViews[i].setVisibility(View.VISIBLE);
                    if (animated) {
                        emojiViews[i].setScaleY(0f);
                        emojiViews[i].setScaleX(0f);
                        emojiViews[i].animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(350)
                                .setInterpolator(new OvershootInterpolator()).start();
                    }
                }
            }
        }
    }

    private void setRetryMode(boolean isRetryMode) {
        this.isRetryMode = isRetryMode;
        if (isRetryMode) {
            acceptCallText.setText(LocaleController.getString("RetryCall", R.string.RetryCall));
        } else {
            acceptCallText.setText(LocaleController.getString("AcceptCall", R.string.AcceptCall));
        }
    }

    private void showAcceptDeclineView(boolean show) {
        if (isAcceptDeclineShowed == show)
            return;
        isAcceptDeclineShowed = show;

        TransitionSet resultSet = VoIPTransitions.acceptDeclineCallTransition(show, acceptCallText, declineCallText, btnAcceptCallBlob, btnAcceptCall, btnDeclineCall);
        TransitionManager.beginDelayedTransition(acceptDeclineView, resultSet);

        if (show) {
            if (!isRetryMode) {
                btnAcceptCallBlob.setVisibility(View.VISIBLE);
            }
            btnAcceptCall.setVisibility(View.VISIBLE);
            acceptCallText.setVisibility(View.VISIBLE);
            btnDeclineCall.setVisibility(View.VISIBLE);
            declineCallText.setVisibility(View.VISIBLE);
        } else {
            if(!isOutgoing && (currentState == VoIPService.STATE_ESTABLISHED || currentState == VoIPService.STATE_EXCHANGING_KEYS)) {
                startGreenBGAnimation(btnAcceptCall);
            }
            btnAcceptCallBlob.setVisibility(View.GONE);
            btnAcceptCall.setVisibility(View.GONE);
            acceptCallText.setVisibility(View.GONE);
            btnDeclineCall.setVisibility(View.GONE);
            declineCallText.setVisibility(View.GONE);
        }
        acceptDeclineView.setEnabled(show);
    }

    private void updateButtons(boolean animated) {
        VoIPService service = VoIPService.getSharedInstance();
        if (service == null) {
            return;
        }

        Transition transition = VoIPTransitions.getButtonsShowTransition(bottomButtons);
        TransitionManager.beginDelayedTransition(buttonsLayout, transition);

        if (currentState == VoIPService.STATE_WAITING_INCOMING || currentState == VoIPService.STATE_BUSY) {
            if (service.privateCall != null && service.privateCall.video && currentState == VoIPService.STATE_WAITING_INCOMING) {
                if (!service.isScreencast() && (currentUserIsVideo || callingUserIsVideo)) {
                    setFrontalCameraAction(bottomButtons[0], service, animated);
                    if (uiVisible) {
                        speakerPhoneIcon.animate().alpha(1f).start();
                    }
                } else {
                    setSpeakerPhoneAction(bottomButtons[0], service, animated);
                    speakerPhoneIcon.animate().alpha(0).start();
                }
                setVideoAction(bottomButtons[1], service, animated);
                setMicrohoneAction(bottomButtons[2], service, animated);
            } else {
                bottomButtons[0].setVisibility(View.GONE);
                bottomButtons[1].setVisibility(View.GONE);
                bottomButtons[2].setVisibility(View.GONE);
            }
            bottomButtons[3].setVisibility(View.GONE);
        } else {
            if (instance == null) {
                return;
            }
            if (!service.isScreencast() && (currentUserIsVideo || callingUserIsVideo)) {
                setFrontalCameraAction(bottomButtons[0], service, animated);
                if (uiVisible) {
                    speakerPhoneIcon.setTag(1);
                    speakerPhoneIcon.animate().alpha(1f).start();
                }
            } else {
                setSpeakerPhoneAction(bottomButtons[0], service, animated);
                speakerPhoneIcon.setTag(null);
                speakerPhoneIcon.animate().alpha(0f).start();
            }
            setVideoAction(bottomButtons[1], service, animated);
            setMicrohoneAction(bottomButtons[2], service, animated);

            bottomButtons[3].setData(R.drawable.calls_decline, Color.WHITE, 0xFFF01D2C, LocaleController.getString("VoipEndCall", R.string.VoipEndCall), false, animated);
            bottomButtons[3].setOnClickListener(view -> {
                if (VoIPService.getSharedInstance() != null) {
                    VoIPService.getSharedInstance().hangUp();
                }
            });
        }

        int animationDelay = 0;
        for (int i = 0; i < 4; i++) {
            if (bottomButtons[i].getVisibility() == View.VISIBLE) {
                bottomButtons[i].animationDelay = animationDelay;
                animationDelay += 16;
            }
        }
        updateSpeakerPhoneIcon();
    }

    private void setMicrohoneAction(VoIPToggleButton bottomButton, VoIPService service, boolean animated) {
        if (service.isMicMute()) {
            bottomButton.setData(R.drawable.calls_unmute, BTN_OFF_COLOR, Color.WHITE, LocaleController.getString("VoipUnmute", R.string.VoipUnmute), true, animated);
        } else {
            bottomButton.setData(R.drawable.calls_unmute, Color.WHITE, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), LocaleController.getString("VoipMute", R.string.VoipMute), false, animated);
        }
        currentUserCameraFloatingLayout.setMuted(service.isMicMute(), animated);
        bottomButton.setOnClickListener(view -> {
            final VoIPService serviceInstance = VoIPService.getSharedInstance();
            if (serviceInstance != null) {
                final boolean micMute = !serviceInstance.isMicMute();
                if (accessibilityManager.isTouchExplorationEnabled()) {
                    final String text;
                    if (micMute) {
                        text = LocaleController.getString("AccDescrVoipMicOff", R.string.AccDescrVoipMicOff);
                    } else {
                        text = LocaleController.getString("AccDescrVoipMicOn", R.string.AccDescrVoipMicOn);
                    }
                    view.announceForAccessibility(text);
                }
                serviceInstance.setMicMute(micMute, false, true);
                previousState = currentState;
                updateViewState();
            }
        });
    }

    private void setVideoAction(VoIPToggleButton bottomButton, VoIPService service, boolean animated) {
        boolean isVideoAvailable;
        if (currentUserIsVideo || callingUserIsVideo) {
            isVideoAvailable = true;
        } else {
            isVideoAvailable = service.isVideoAvailable();
        }
        if (isVideoAvailable) {
            if (currentUserIsVideo) {
                bottomButton.setData(service.isScreencast() ? R.drawable.calls_sharescreen : R.drawable.calls_video, Color.WHITE, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), LocaleController.getString("VoipStopVideo", R.string.VoipStopVideo), false, animated);
            } else {
                bottomButton.setData(R.drawable.calls_video, BTN_OFF_COLOR, Color.WHITE, LocaleController.getString("VoipStartVideo", R.string.VoipStartVideo), true, animated);
            }
            bottomButton.setCrossOffset(-AndroidUtilities.dpf2(3.5f));
            bottomButton.setOnClickListener(view -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, 102);
                } else {
                    if (Build.VERSION.SDK_INT < 21 && service.privateCall != null && !service.privateCall.video && !callingUserIsVideo && !service.sharedUIParams.cameraAlertWasShowed) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(LocaleController.getString("VoipSwitchToVideoCall", R.string.VoipSwitchToVideoCall));
                        builder.setPositiveButton(LocaleController.getString("VoipSwitch", R.string.VoipSwitch), (dialogInterface, i) -> {
                            service.sharedUIParams.cameraAlertWasShowed = true;
                            toggleCameraInput();
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        builder.create().show();
                    } else {
                        toggleCameraInput();
                    }
                }
            });
            bottomButton.setEnabled(true);
        } else {
            bottomButton.setData(R.drawable.calls_video, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.5f)), ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), "Video", false, animated);
            bottomButton.setOnClickListener(null);
            bottomButton.setEnabled(false);
        }
    }

    private void updateSpeakerPhoneIcon() {
        VoIPService service = VoIPService.getSharedInstance();
        if (service == null) {
            return;
        }
        if (service.isBluetoothOn()) {
            speakerPhoneIcon.setImageResource(R.drawable.calls_bluetooth);
        } else if (service.isSpeakerphoneOn()) {
            speakerPhoneIcon.setImageResource(R.drawable.calls_speaker);
        } else {
            if (service.isHeadsetPlugged()) {
                speakerPhoneIcon.setImageResource(R.drawable.calls_menu_headset);
            } else {
                speakerPhoneIcon.setImageResource(R.drawable.calls_menu_phone);
            }
        }
    }

    private void setSpeakerPhoneAction(VoIPToggleButton bottomButton, VoIPService service, boolean animated) {
        if (service.isBluetoothOn()) {
            bottomButton.setData(R.drawable.calls_bluetooth, Color.WHITE, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), LocaleController.getString("VoipAudioRoutingBluetooth", R.string.VoipAudioRoutingBluetooth), false, animated);
            bottomButton.setChecked(false, animated);
        } else if (service.isSpeakerphoneOn()) {
            bottomButton.setData(R.drawable.calls_speaker, BTN_OFF_COLOR, Color.WHITE, LocaleController.getString("VoipSpeaker", R.string.VoipSpeaker), false, animated);
            bottomButton.setChecked(true, animated);
        } else {
            bottomButton.setData(R.drawable.calls_speaker, Color.WHITE, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), LocaleController.getString("VoipSpeaker", R.string.VoipSpeaker), false, animated);
            bottomButton.setChecked(false, animated);
        }
        bottomButton.setCheckableForAccessibility(true);
        bottomButton.setEnabled(true);
        bottomButton.setOnClickListener(view -> {
            if (VoIPService.getSharedInstance() != null) {
                VoIPService.getSharedInstance().toggleSpeakerphoneOrShowRouteSheet(activity, false);
            }
        });
    }

    private void setFrontalCameraAction(VoIPToggleButton bottomButton, VoIPService service, boolean animated) {
        if (!currentUserIsVideo) {
            bottomButton.setData(R.drawable.calls_flip, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.5f)), ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), LocaleController.getString("VoipFlip", R.string.VoipFlip), false, animated);
            bottomButton.setOnClickListener(null);
            bottomButton.setEnabled(false);
        } else {
            bottomButton.setEnabled(true);
            if (!service.isFrontFaceCamera()) {
                bottomButton.setData(R.drawable.calls_flip, Color.TRANSPARENT, Color.WHITE, LocaleController.getString("VoipFlip", R.string.VoipFlip), false, animated);
            } else {
                bottomButton.setData(R.drawable.calls_flip, Color.WHITE, ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.12f)), LocaleController.getString("VoipFlip", R.string.VoipFlip), false, animated);
            }

            bottomButton.setOnClickListener(view -> {
                final VoIPService serviceInstance = VoIPService.getSharedInstance();
                if (serviceInstance != null) {
                    if (accessibilityManager.isTouchExplorationEnabled()) {
                        final String text;
                        if (service.isFrontFaceCamera()) {
                            text = LocaleController.getString("AccDescrVoipCamSwitchedToBack", R.string.AccDescrVoipCamSwitchedToBack);
                        } else {
                            text = LocaleController.getString("AccDescrVoipCamSwitchedToFront", R.string.AccDescrVoipCamSwitchedToFront);
                        }
                        view.announceForAccessibility(text);
                    }
                    serviceInstance.switchCamera();
                }
            });
        }
    }

    public void onScreenCastStart() {
        if (previewDialog == null) {
            return;
        }
        previewDialog.dismiss(true, true);
    }

    private void toggleCameraInput() {
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            if (accessibilityManager.isTouchExplorationEnabled()) {
                final String text;
                if (!currentUserIsVideo) {
                    text = LocaleController.getString("AccDescrVoipCamOn", R.string.AccDescrVoipCamOn);
                } else {
                    text = LocaleController.getString("AccDescrVoipCamOff", R.string.AccDescrVoipCamOff);
                }
                fragmentView.announceForAccessibility(text);
            }
            if (!currentUserIsVideo) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (previewDialog == null) {
                        service.createCaptureDevice(false);
                        if (!service.isFrontFaceCamera()) {
                            service.switchCamera();
                        }
                        windowView.setLockOnScreen(true);
                        previewDialog = new PrivateVideoPreviewDialog(fragmentView.getContext(), false, true) {
                            @Override
                            public void onDismiss(boolean screencast, boolean apply) {
                                previewDialog = null;
                                VoIPService service = VoIPService.getSharedInstance();
                                windowView.setLockOnScreen(false);
                                if (apply) {
                                    currentUserIsVideo = true;
                                    if (service != null && !screencast) {
                                        service.requestVideoCall(false);
                                        service.setVideoState(false, Instance.VIDEO_STATE_ACTIVE);
                                    }
                                } else {
                                    if (service != null) {
                                        service.setVideoState(false, Instance.VIDEO_STATE_INACTIVE);
                                    }
                                }
                                previousState = currentState;
                                updateViewState();
                            }
                        };
                        if (lastInsets != null) {
                            previewDialog.setBottomPadding(lastInsets.getSystemWindowInsetBottom());
                        }
                        fragmentView.addView(previewDialog);
                    }
                    return;
                } else {
                    currentUserIsVideo = true;
                    if (!service.isSpeakerphoneOn()) {
                        VoIPService.getSharedInstance().toggleSpeakerphoneOrShowRouteSheet(activity, false);
                    }
                    service.requestVideoCall(false);
                    service.setVideoState(false, Instance.VIDEO_STATE_ACTIVE);
                }
            } else {
                currentUserTextureView.saveCameraLastBitmap();
                service.setVideoState(false, Instance.VIDEO_STATE_INACTIVE);
                if (Build.VERSION.SDK_INT >= 21) {
                    service.clearCamera();
                }
            }
            previousState = currentState;
            updateViewState();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (instance != null) {
            instance.onRequestPermissionsResultInternal(requestCode, permissions, grantResults);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void onRequestPermissionsResultInternal(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 101) {
            if (VoIPService.getSharedInstance() == null) {
                windowView.finish();
                return;
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                VoIPService.getSharedInstance().acceptIncomingCall();
            } else {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    VoIPService.getSharedInstance().declineIncomingCall();
                    VoIPHelper.permissionDenied(activity, () -> windowView.finish(), requestCode);
                    return;
                }
            }
        }
        if (requestCode == 102) {
            if (VoIPService.getSharedInstance() == null) {
                windowView.finish();
                return;
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleCameraInput();
            }
        }
    }

    public static void onPause() {
        if (instance != null) {
            instance.onPauseInternal();
        }
        if (VoIPPiPView.getInstance() != null) {
            VoIPPiPView.getInstance().onPause();
        }
    }

    public static void onResume() {
        if (instance != null) {
            instance.onResumeInternal();
        }
        if (VoIPPiPView.getInstance() != null) {
            VoIPPiPView.getInstance().onResume();
        }
    }

    public void onPauseInternal() {
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

        boolean screenOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = pm.isInteractive();
        } else {
            screenOn = pm.isScreenOn();
        }

        boolean hasPermissionsToPip = AndroidUtilities.checkInlinePermissions(activity);

        if (canSwitchToPip && hasPermissionsToPip) {
            int h = instance.windowView.getMeasuredHeight();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && instance.lastInsets != null) {
                h -= instance.lastInsets.getSystemWindowInsetBottom();
            }
            VoIPPiPView.show(instance.activity, instance.currentAccount, instance.windowView.getMeasuredWidth(), h, VoIPPiPView.ANIMATION_ENTER_TYPE_SCALE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && instance.lastInsets != null) {
                VoIPPiPView.topInset = instance.lastInsets.getSystemWindowInsetTop();
                VoIPPiPView.bottomInset = instance.lastInsets.getSystemWindowInsetBottom();
            }
        }

        if (currentUserIsVideo && (!hasPermissionsToPip || !screenOn)) {
            VoIPService service = VoIPService.getSharedInstance();
            if (service != null) {
                service.setVideoState(false, Instance.VIDEO_STATE_PAUSED);
            }
        }

        setAnimationsPaused(true);
    }

    public void onResumeInternal() {
        if (VoIPPiPView.getInstance() != null) {
            VoIPPiPView.finish();
        }
        VoIPService service = VoIPService.getSharedInstance();
        if (service != null) {
            if (service.getVideoState(false) == Instance.VIDEO_STATE_PAUSED) {
                service.setVideoState(false, Instance.VIDEO_STATE_ACTIVE);
            }
            updateViewState();
        } else {
            windowView.finish();
        }

        deviceIsLocked = ((KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode();
        setAnimationsPaused(false);
    }

    private void showErrorDialog(CharSequence message) {
        if (activity.isFinishing()) {
            return;
        }
        AlertDialog dlg = new DarkAlertDialog.Builder(activity)
                .setTitle(LocaleController.getString("VoipFailed", R.string.VoipFailed))
                .setMessage(message)
                .setPositiveButton(LocaleController.getString("OK", R.string.OK), null)
                .show();
        dlg.setCanceledOnTouchOutside(true);
        dlg.setOnDismissListener(dialog -> windowView.finish());
    }

    @SuppressLint("InlinedApi")
    private void requestInlinePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertsCreator.createDrawOverlayPermissionDialog(activity, (dialogInterface, i) -> {
                if (windowView != null) {
                    windowView.finish();
                }
            }).show();
        }
    }

    private void startAnimations() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AndroidUtilities.runOnUIThread(() -> tickAnimations());
            }
        }, 0, 1000 / FRAME_RATE);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AndroidUtilities.runOnUIThread(() -> setGradientBackground());
            }
        }, BG_CHANGE_DURATION, BG_CHANGE_DURATION);
    }

    private void stopAnimations() {
        timer.cancel();
        if(acceptCallBtnAnimation != null){
            acceptCallBtnAnimation.cancel();
        }
    }

    private void setAnimationsPaused(boolean paused) {
        isAnimationsPaused = paused;
        mainBackgroundView.setAnimationRunning(!paused);
        if(paused){
            if(acceptCallBtnAnimation != null){
                acceptCallBtnAnimation.pause();
            }
        } else{
            if(acceptCallBtnAnimation != null && acceptCallBtnAnimation.isPaused()){
                acceptCallBtnAnimation.resume();
            }
        }
    }

    private void tickAnimations() {
        if (isAnimationsPaused) {
            return;
        }
        btnAcceptCallBlob.tickAnimation();
        callingUserPhotoBlobView.tickAnimation();
    }

    private void setGradientBackground() {
        if (isAnimationsPaused) {
            return;
        }
        mainBackgroundView.post(() -> {
            mainBackgroundView.switchColors(BG_CHANGE_DURATION);
        });
    }

    private void setBackgroundState(int state) {
        State bgState;
        if (state == VoIPService.STATE_ESTABLISHED) {
            bgState = State.CALL_ESTABLISHED;
        } else if (state == VoIPService.STATE_RECONNECTING) {
            bgState = State.WEAK_SIGNAL;
        } else {
            bgState = State.NOT_ESTABLISHED;
        }
        mainBackgroundView.setState(bgState);
    }

    private void startGreenBGAnimation(View targetView) {
        int[] xy = new int[2];
        targetView.getLocationOnScreen(xy);

        int x = xy[0] + targetView.getWidth() / 2;
        int y = xy[1] + targetView.getHeight() / 2;
        mainBackgroundView.showGreenAnimation(x, y, BG_GREEN_CIRCLE_DURATION, BG_CHANGE_DURATION);
    }

    private void animateUserPhotoBlobSize(float micAmplitude) {
        if (isAnimationsPaused || callingUserPhotoBlobView.getVisibility() != View.VISIBLE || currentState != VoIPService.STATE_ESTABLISHED) {
            return;
        }
        if (userPhotoBlobTransition == null) {
            userPhotoBlobTransition = VoIPTransitions.getUserPhotoBlobSizeTransition(callingUserPhotoBlobView, USER_PHOTO_BLOB_INNER_RADIUS, USER_PHOTO_BLOB_OUTER_RADIUS);
        }
        TransitionManager.beginDelayedTransition(fragmentView, userPhotoBlobTransition);

        if (callingUserPhotoBlobView != null) {
            callingUserPhotoBlobView.setWaveAmplitude(USER_PHOTO_BLOB_AMPLITUDE);
            callingUserPhotoBlobView.setInnerWaveRadius((int) (USER_PHOTO_BLOB_INNER_RADIUS * (1 + 0.3f * micAmplitude)));
            callingUserPhotoBlobView.setOuterWaveRadius((int) (USER_PHOTO_BLOB_OUTER_RADIUS * (1 + 0.5f * micAmplitude)));
        }
    }

    private void animateAcceptCallButton() {
        if (acceptCallBtnAnimation != null) {
            acceptCallBtnAnimation.cancel();
        }
        acceptCallBtnAnimation = new AnimatorSet();

        ValueAnimator animator = ValueAnimator.ofInt(0, AndroidUtilities.dp(7), 0, AndroidUtilities.dp(6), 0);
        animator.setDuration(1000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            btnAcceptCallBlob.setInnerWaveRadius((int) (ACCEPT_INNNER_RADIUS + progress));
            btnAcceptCallBlob.setOuterWaveRadius((int) (ACCEPT_OUTER_RADIUS + progress * 2));
        });
        ValueAnimator rotation = ValueAnimator.ofInt(0, -15, 10, -15, 10, 0);
        rotation.setInterpolator(CubicBezierInterpolator.DEFAULT);
        rotation.setDuration(1000);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            btnAcceptCall.setRotation(progress);
        });
        acceptCallBtnAnimation.playTogether(animator, rotation);
        acceptCallBtnAnimation.start();
        acceptCallBtnAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                btnAcceptCall.setRotation(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                btnAcceptCall.setRotation(0);
            }
        });
    }

}
