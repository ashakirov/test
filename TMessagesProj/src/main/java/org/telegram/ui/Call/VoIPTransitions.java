package org.telegram.ui.Call;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeImageTransform;
import androidx.transition.Fade;
import androidx.transition.TransitionSet;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Call.transition.BlobVisibility;
import org.telegram.ui.Call.transition.Scale;
import org.telegram.ui.Call.transition.Slide;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;

public class VoIPTransitions {
    @NonNull
    public static TransitionSet emojiExpandTransition(boolean expanded, View btnHideEmoji, View emojiBackground, TextView emojiRationalTextView, TextView emojiEncriptionTextView, ImageView[] emojiViews, BlobView callingUserPhotoBlobView, BackupImageView callingUserPhoto, LinearLayout emojiLayout, ViewGroup emojiFrame, View statusLayout) {
        TransitionSet set = new TransitionSet();
        Fade btnHideEmojiFade = new Fade();
        btnHideEmojiFade.addTarget(btnHideEmoji);
        set.addTransition(btnHideEmojiFade);

        TransitionSet bgSet = new TransitionSet();
        bgSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        bgSet.addTransition(new Slide(-AndroidUtilities.dp(150), 0f));
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
}
