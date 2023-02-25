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

public class Scale extends Visibility {

    private float scaleTo;
    private boolean pivotCenter;

    public Scale(float scaleTo, boolean animatePivotCenter) {
        this.scaleTo = scaleTo;
        this.pivotCenter = animatePivotCenter;
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
    public Animator onAppear(ViewGroup sceneRoot, View view,
                             TransitionValues startValues, TransitionValues endValues) {
        if (endValues == null) {
            return null;
        }

        float terminatePivotX = view.getPivotX();
        float terminatePivotY = view.getPivotY();

        if (pivotCenter) {
            view.setPivotY(view.getHeight() / 2f);
            view.setPivotX(view.getWidth() / 2f);
        }

        float endScaleX = view.getScaleX();
        float endScaleY = view.getScaleY();

        float startScaleX = scaleTo;
        float startScaleY = scaleTo;

        view.setScaleX(startScaleX);
        view.setScaleY(startScaleY);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, startScaleX, endScaleX),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, startScaleY, endScaleY));

        anim.addListener(new AnimationTerminateListener(view, endScaleX, endScaleY, terminatePivotX, terminatePivotY));
        return anim;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view,
                                TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }

        float terminatePivotX = view.getPivotX();
        float terminatePivotY = view.getPivotY();

        if (pivotCenter) {
            view.setPivotY(view.getHeight());
            view.setPivotX(view.getWidth() / 2f);
        }

        float startScaleX = view.getScaleX();
        float startScaleY = view.getScaleY();

        float endScaleX = scaleTo;
        float endScaleY = scaleTo;

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, startScaleX, endScaleX),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, startScaleY, endScaleY));

        anim.addListener(new AnimationTerminateListener(view, startScaleX, startScaleY, terminatePivotX, terminatePivotY));
        return anim;
    }

    private class AnimationTerminateListener extends AnimatorListenerAdapter {
        private final View view;
        private final float scaleX;
        private final float scaleY;
        private float terminatePivotX, terminatePivotY;

        public AnimationTerminateListener(View view, float scaleX, float scaleY, float terminatePivotX, float terminatePivotY) {
            this.view = view;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.terminatePivotX = terminatePivotX;
            this.terminatePivotY = terminatePivotY;
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
            view.setPivotX(terminatePivotX);
            view.setPivotY(terminatePivotY);
            view.setScaleX(scaleX);
            view.setScaleY(scaleY);
        }
    }
}
