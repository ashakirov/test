package org.telegram.ui.Call;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeImageTransform;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionSet;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Call.transition.BlobAmplitudeTransition;
import org.telegram.ui.Call.transition.BlobSizeTransition;
import org.telegram.ui.Call.transition.BlobVisibility;
import org.telegram.ui.Call.transition.InsetColorTransition;
import org.telegram.ui.Call.transition.InsetColorTransition.Type;
import org.telegram.ui.Call.transition.Scale;
import org.telegram.ui.Call.transition.Slide;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.voip.VoIPButtonsLayout;
import org.telegram.ui.Components.voip.VoIPNotificationsLayout;
import org.telegram.ui.Components.voip.VoIPToggleButton;

public class VoIPTransitions {

    private static final int BTN_CALL_DELTA_Y = 110;
    private static final int BTN_CALL_DELTA_X = 40;
    private static final float BTN_CALL_MIN_SCALE = 0.7f;
    private static final int CALM_DOWN_WAVES_TIME = 3000;
    private static final int EMOJI_SLIDE_Y_DELTA = -AndroidUtilities.dp(150);

    @NonNull
    public static TransitionSet emojiExpandTransition(boolean expanded, View btnHideEmoji, View emojiBackground, TextView emojiRationalTextView, TextView emojiEncriptionTextView, ImageView[] emojiViews, BlobView callingUserPhotoBlobView, BackupImageView callingUserPhoto, LinearLayout emojiLayout, ViewGroup emojiFrame, View statusLayout) {
        TransitionSet set = new TransitionSet();
        Fade btnHideEmojiFade = new Fade();
        btnHideEmojiFade.addTarget(btnHideEmoji);
        set.addTransition(btnHideEmojiFade);

        TransitionSet bgSet = new TransitionSet();
        bgSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        bgSet.addTransition(new Slide(EMOJI_SLIDE_Y_DELTA, 0f));
        bgSet.addTransition(new Scale(0.2f, true));
        bgSet.addTransition(new Fade());
        bgSet.setDuration(350);
        bgSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
        bgSet.addTarget(emojiBackground);
        bgSet.addTarget(emojiRationalTextView);
        bgSet.addTarget(emojiEncriptionTextView);
        set.addTransition(bgSet);

        TransitionSet emojiSet = new TransitionSet();
        emojiSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        emojiSet.addTransition(new ChangeImageTransform());
        emojiSet.addTransition(new ChangeBounds());
        for (ImageView emojiView : emojiViews) {
            emojiSet.addTarget(emojiView);
        }
        set.addTransition(emojiSet);

        TransitionSet blobSet = new TransitionSet();
        blobSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        blobSet.addTarget(callingUserPhotoBlobView);
        blobSet.addTransition(new BlobVisibility(AndroidUtilities.dp(70)));
        blobSet.addTransition(new Fade());

        TransitionSet userPhotoSet = new TransitionSet();
        userPhotoSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        userPhotoSet.addTransition(new Scale(0f, true));
        userPhotoSet.addTransition(new Fade());
        userPhotoSet.addTransition(new Slide(AndroidUtilities.dp(150), 0f));
        userPhotoSet.addTarget(callingUserPhoto);
        userPhotoSet.setDuration(300);

        TransitionSet avatarSet = new TransitionSet();
        avatarSet.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
        if (expanded) {
            blobSet.setDuration(50);
            avatarSet.addTransition(blobSet);
            avatarSet.addTransition(userPhotoSet);
        } else {
            blobSet.setInterpolator(new OvershootInterpolator(3f));
            blobSet.setDuration(400);
            avatarSet.addTransition(userPhotoSet);
            avatarSet.addTransition(blobSet);
        }
        set.addTransition(avatarSet);

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.addTarget(emojiLayout);
        changeBounds.addTarget(emojiFrame);
        changeBounds.addTarget(emojiLayout);
        changeBounds.addTarget(statusLayout);
        set.addTransition(changeBounds);
        return set;
    }

    @NonNull
    public static TransitionSet acceptDeclineCallTransition(boolean show, TextView acceptCallText1, TextView declineCallText1, BlobView btnAcceptCallBlob1, View btnAcceptCall1, View btnDeclineCall1) {
        TransitionSet hideNonButtons = new TransitionSet();
        hideNonButtons.setOrdering(TransitionSet.ORDERING_TOGETHER);

        Fade fade = new Fade();
        fade.addTarget(acceptCallText1);
        fade.addTarget(declineCallText1);
        hideNonButtons.addTransition(fade);

        BlobVisibility blobVisibility = new BlobVisibility(0);
        blobVisibility.addTarget(btnAcceptCallBlob1);
        blobVisibility.setDuration(150);
        hideNonButtons.addTransition(blobVisibility);

        TransitionSet buttonsSet = new TransitionSet();
        buttonsSet.addTransition(new Fade());
        buttonsSet.setOrdering(TransitionSet.ORDERING_TOGETHER);

        Slide acceptBtnSlide = new Slide(AndroidUtilities.dp(BTN_CALL_DELTA_Y), -AndroidUtilities.dp(BTN_CALL_DELTA_X));
        acceptBtnSlide.addTarget(btnAcceptCall1);
        buttonsSet.addTransition(acceptBtnSlide);

        Slide declineBtnSlide = new Slide(AndroidUtilities.dp(BTN_CALL_DELTA_Y), AndroidUtilities.dp(BTN_CALL_DELTA_X));
        declineBtnSlide.addTarget(btnDeclineCall1);
        buttonsSet.addTransition(declineBtnSlide);

        TransitionSet resultSet = new TransitionSet();
        resultSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
        if (show) {
            resultSet.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
            resultSet.addTransition(buttonsSet);
            resultSet.addTransition(hideNonButtons);
        } else {
            resultSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
            resultSet.addTransition(hideNonButtons);
            resultSet.addTransition(buttonsSet);
        }
        return resultSet;
    }

    public static Transition getUserPhotoBlobSizeTransition(BlobView blobView, int innerRadius, int outerRadius) {
        TransitionSet set = new TransitionSet();
        BlobSizeTransition blobSizeTransition = new BlobSizeTransition(innerRadius, outerRadius);
        blobSizeTransition.setInterpolator(new AccelerateDecelerateInterpolator());
        blobSizeTransition.setDuration(200);
        set.addTransition(blobSizeTransition);
        set.addTransition(new BlobAmplitudeTransition().setDuration(CALM_DOWN_WAVES_TIME));
        set.addTarget(blobView);
        return set;
    }

    public static Transition getShowShadowsTransition(ColoredInsetConstraintLayout insetView, View bottomShadow, View topShadow) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);

        InsetColorTransition topColorTransition = new InsetColorTransition(Type.TOP);
        topColorTransition.addTarget(insetView);
        set.addTransition(topColorTransition);

        InsetColorTransition bottomColorTransition = new InsetColorTransition(Type.BOTTOM);
        bottomColorTransition.addTarget(insetView);
        set.addTransition(bottomColorTransition);

        Fade fade = new Fade();
        fade.addTarget(bottomShadow);
        fade.addTarget(topShadow);
        set.addTransition(fade);
        set.setInterpolator(CubicBezierInterpolator.DEFAULT);
        return set;
    }

    public static TransitionSet getShowUITransition(ImageView speakerPhoneIcon,
                                                    ImageView backIcon,
                                                    LinearLayout statusLayout,
                                                    VoIPButtonsLayout buttonsLayout,
                                                    LinearLayout emojiLayout,
                                                    VoIPNotificationsLayout notificationsLayout) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);
        Fade fade = new Fade();
        fade.addTarget(speakerPhoneIcon);
        fade.addTarget(backIcon);
        fade.addTarget(statusLayout);
        fade.addTarget(buttonsLayout);
        fade.addTarget(emojiLayout);
        set.addTransition(fade);

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.addTarget(notificationsLayout);
        set.addTransition(changeBounds);
        set.setInterpolator(CubicBezierInterpolator.DEFAULT);

        return set;
    }


    @NonNull
    public static Transition getButtonsShowTransition(VoIPToggleButton[] bottomButtons) {
        TransitionSet set = new TransitionSet();
        set.addTransition(new Slide(-AndroidUtilities.dp(BTN_CALL_DELTA_Y), 0));
        set.addTransition(new Fade());
        set.addTransition(new Scale(BTN_CALL_MIN_SCALE, true));
        set.setInterpolator(CubicBezierInterpolator.DEFAULT);
        for (VoIPToggleButton button : bottomButtons) {
            set.addTarget(button);
        }
        return set;
    }

    public static AnimatorSet createAcceptCallButtonAnimation(BlobView blobView, final View callButton, int innerRadius, int outerRadius) {
        AnimatorSet set = new AnimatorSet();

        ValueAnimator zoomWaves = ValueAnimator.ofInt(0, AndroidUtilities.dp(7), 0, AndroidUtilities.dp(6), 0);
        zoomWaves.setDuration(1000);
        zoomWaves.setRepeatCount(ValueAnimator.INFINITE);
        zoomWaves.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            blobView.setInnerWaveRadius((int) (innerRadius + progress));
            blobView.setOuterWaveRadius((int) (outerRadius + progress * 2));
        });
        ValueAnimator rotation = ValueAnimator.ofInt(0, -15, 10, -15, 10, 0);
        rotation.setInterpolator(CubicBezierInterpolator.DEFAULT);
        rotation.setDuration(1000);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            callButton.setRotation(progress);
        });
        set.playTogether(zoomWaves, rotation);
        set.start();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                callButton.setRotation(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                callButton.setRotation(0);
            }
        });
        return set;
    }
}
