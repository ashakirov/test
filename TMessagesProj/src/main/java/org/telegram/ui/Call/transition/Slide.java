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

public class Slide extends Visibility {

    private float deltaX;
    private float deltaY;

    public Slide(float deltaY, float deltaX) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
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

        float endTranslationX = view.getTranslationX();
        float endTranslationY = view.getTranslationY();

        float startTranslationX = endTranslationX + deltaX;
        float startTranslationY = endTranslationY + deltaY;

        view.setTranslationX(startTranslationX);
        view.setTranslationY(startTranslationY);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, startTranslationX, endTranslationX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, startTranslationY, endTranslationY));

        anim.addListener(new AnimationTerminateListener(view, endTranslationX, endTranslationY));
        return anim;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view,
                                TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }

        float startTranslationX = view.getTranslationX();
        float startTranslationY = view.getTranslationY();

        float endTranslationX = startTranslationX + deltaX;
        float endTranslationY = startTranslationY + deltaY;

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, startTranslationX, endTranslationX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, startTranslationY, endTranslationY));

        anim.addListener(new AnimationTerminateListener(view, startTranslationX, startTranslationY));
        return anim;
    }

    private class AnimationTerminateListener extends AnimatorListenerAdapter {
        private final View view;
        private final float translationX;
        private final float translationY;

        public AnimationTerminateListener(View view, float translationX, float translationY) {
            this.view = view;
            this.translationX = translationX;
            this.translationY = translationY;
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
            view.setTranslationX(translationX);
            view.setTranslationY(translationY);
        }
    }
}
