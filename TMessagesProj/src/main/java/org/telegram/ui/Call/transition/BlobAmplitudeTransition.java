package org.telegram.ui.Call.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.Transition;
import androidx.transition.TransitionValues;

import org.telegram.ui.Call.BlobView;

public class BlobAmplitudeTransition extends Transition {
    private static String PROPNAME_WAVE_AMPLITUDE = "android:avatarwaveicon:waveamplitude";

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
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
        transitionValues.values.put(PROPNAME_WAVE_AMPLITUDE, bView.getWaveAmplitude());
    }

    @Nullable
    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot, @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        Object startWave = startValues.values.get(PROPNAME_WAVE_AMPLITUDE);
        Object endWave = endValues.values.get(PROPNAME_WAVE_AMPLITUDE);
        if (startWave == null || endWave == null) {
            return null;
        }

        int startAmplitude = (int) startWave;
        int endAmplitude = (int) endWave;

        if (startAmplitude == endAmplitude) {
            return null;
        }
        BlobView view = (BlobView) endValues.view;
        view.setWaveAmplitude(startAmplitude);

        return ObjectAnimator.ofInt(view, BlobView.PROPERTY_WAVE_AMPLITUDE, startAmplitude, endAmplitude, 0);
    }
}
