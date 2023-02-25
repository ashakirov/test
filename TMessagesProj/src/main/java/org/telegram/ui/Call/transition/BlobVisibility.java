package org.telegram.ui.Call.transition;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.TransitionValues;
import androidx.transition.Visibility;

import org.telegram.ui.Call.BlobView;

public class BlobVisibility extends Visibility {

    private int minRadius;

    public BlobVisibility(int minRadius) {
        this.minRadius = minRadius;
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View viw,
                             TransitionValues startValues, TransitionValues endValues) {
        if (endValues == null || !(viw instanceof BlobView)) {
            return null;
        }

        BlobView view = (BlobView) viw;

        int endInnerRadius = view.getInnerWaveRadius();
        int endOuterRadius = view.getOuterWaveRadius();

        int startInnerRadius = minRadius;
        int startOuterRadius = minRadius;

        view.setInnerWaveRadius(startInnerRadius);
        view.setOuterWaveRadius(startOuterRadius);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofInt(BlobView.PROPERTY_INNER_RADIUS, startInnerRadius, endInnerRadius),
                PropertyValuesHolder.ofInt(BlobView.PROPERTY_OUTER_RADIUS, startOuterRadius, endOuterRadius));

        anim.addListener(new AnimationTerminateListener(view, endInnerRadius, endOuterRadius));
        return anim;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View viw,
                                TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || !(viw instanceof BlobView)) {
            return null;
        }

        BlobView view = (BlobView) viw;

        int startInnerRadius = view.getInnerWaveRadius();
        int startOuterRadius = view.getOuterWaveRadius();

        int endInnerRadius = minRadius;
        int endOuterRadius = minRadius;

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofInt(BlobView.PROPERTY_INNER_RADIUS, startInnerRadius, endInnerRadius),
                PropertyValuesHolder.ofInt(BlobView.PROPERTY_OUTER_RADIUS, startOuterRadius, endOuterRadius));

        anim.addListener(new AnimationTerminateListener(view, startInnerRadius, startOuterRadius));
        return anim;
    }

    private class AnimationTerminateListener extends AnimatorListenerAdapter {
        private final BlobView view;
        private final int innerRadius;
        private final int outerRadius;

        public AnimationTerminateListener(BlobView view, int innerRadius, int outerRadius) {
            this.view = view;
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            restoreParamsAfterAnimation();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            restoreParamsAfterAnimation();
        }

        private void restoreParamsAfterAnimation() {
            view.setInnerWaveRadius(innerRadius);
            view.setOuterWaveRadius(outerRadius);
        }
    }
}
