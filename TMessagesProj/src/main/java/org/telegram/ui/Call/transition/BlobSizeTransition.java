package org.telegram.ui.Call.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.Transition;
import androidx.transition.TransitionValues;

import org.telegram.ui.Call.BlobView;

public class BlobSizeTransition extends Transition {
    private static String PROPNAME_BLOB_INNER_RADIUS = "android:blobview:innerradius";
    private static String PROPNAME_BLOB_OUTER_RADIUS = "android:blobview:blobouterradius";

    private int minInnerRadius;
    private int minOuterRadius;

    public BlobSizeTransition(int minInnerRadius, int minOuterRadius) {
        this.minInnerRadius = minInnerRadius;
        this.minOuterRadius = minOuterRadius;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues, true);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues, false);
    }

    private void captureValues(TransitionValues transitionValues, boolean startValue) {
        View view = transitionValues.view;
        if (!(view instanceof BlobView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        BlobView bView = (BlobView) view;
        transitionValues.values.put(PROPNAME_BLOB_INNER_RADIUS, bView.getInnerWaveRadius());
        transitionValues.values.put(PROPNAME_BLOB_OUTER_RADIUS, bView.getOuterWaveRadius());
    }

    @Nullable
    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot, @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        Object startInner = startValues.values.get(PROPNAME_BLOB_INNER_RADIUS);
        Object startOuter = startValues.values.get(PROPNAME_BLOB_OUTER_RADIUS);
        Object endInner = endValues.values.get(PROPNAME_BLOB_INNER_RADIUS);
        Object endOuter = endValues.values.get(PROPNAME_BLOB_OUTER_RADIUS);

        if (startInner == null || startOuter == null || endInner == null || endOuter == null) {
            return null;
        }

        int startInnerRadius = (int) startInner;
        int startOuterRadius = (int) startOuter;

        int endInnerRadius = (int) endInner;
        int endOuterRadius = (int) endOuter;

        if (startInnerRadius == endInnerRadius || startOuterRadius == endOuterRadius) {
            return null;
        }
        BlobView view = (BlobView) endValues.view;

        view.setInnerWaveRadius(startInnerRadius);
        view.setOuterWaveRadius(startOuterRadius);

        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofInt(BlobView.PROPERTY_INNER_RADIUS, startInnerRadius, endInnerRadius, minInnerRadius),
                PropertyValuesHolder.ofInt(BlobView.PROPERTY_OUTER_RADIUS, startOuterRadius, endOuterRadius, minOuterRadius));
    }
}