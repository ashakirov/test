package org.telegram.ui.Call.transition;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.Transition;
import androidx.transition.TransitionValues;

import org.telegram.ui.Call.ColoredInsetFrameLayout;

public class InsetColorTransition extends Transition {
    public enum Type {
        TOP, BOTTOM
    }

    public static String PROPNAME_TOP_COLOR = "android:coloredinsetframelayout:topcolor";
    public static String PROPNAME_BOTTOM_COLOR = "android:coloredinsetframelayout:bottomcolor";

    private Type type;

    public InsetColorTransition(Type type) {
        this.type = type;
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues, true);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues, false);
    }

    private void captureValues(TransitionValues transitionValues, boolean startValue) {
        View view = transitionValues.view;
        if (!(view instanceof ColoredInsetFrameLayout) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        ColoredInsetFrameLayout view1 = (ColoredInsetFrameLayout) view;
        switch (type) {
            case TOP: {
                int color = view1.getTopColor();
                transitionValues.values.put(PROPNAME_TOP_COLOR, color);
            }
            break;
            case BOTTOM: {
                int color = view1.getBottomColor();
                transitionValues.values.put(PROPNAME_BOTTOM_COLOR, color);
            }
            break;
        }
    }

    @Nullable
    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot, @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        int startValue = 0;
        int endValue = 0;
        Property<ColoredInsetFrameLayout, Integer> property = null;

        switch (type) {
            case TOP: {
                startValue = (int) startValues.values.get(PROPNAME_TOP_COLOR);
                endValue = (int) endValues.values.get(PROPNAME_TOP_COLOR);
                property = ColoredInsetFrameLayout.PROPERTY_TOP_COLOR;
            }
            break;
            case BOTTOM: {
                startValue = (int) startValues.values.get(PROPNAME_BOTTOM_COLOR);
                endValue = (int) endValues.values.get(PROPNAME_BOTTOM_COLOR);
                property = ColoredInsetFrameLayout.PROPERTY_BOTTOM_COLOR;
            }
            break;
        }
        if (startValue == endValue) {
            return null;
        }
        ColoredInsetFrameLayout view = (ColoredInsetFrameLayout) endValues.view;
        property.set(view, startValue);
        return ObjectAnimator.ofObject(view, property, new ArgbEvaluator(), startValue, endValue);
    }
}
