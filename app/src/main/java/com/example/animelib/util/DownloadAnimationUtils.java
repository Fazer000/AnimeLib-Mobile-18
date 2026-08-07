package com.example.animelib.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;

import com.example.animelib.R;

public class DownloadAnimationUtils {

    public static void startDownloadAnimation(View view) {
        if (view == null) return;

        // Check if animation is already running on this view
        Object existingAnim = view.getTag(R.id.tag_download_animator);
        if (existingAnim instanceof ValueAnimator && ((ValueAnimator) existingAnim).isRunning()) {
            return;
        }

        stopDownloadAnimation(view);

        // Smooth color animation between primary purple and bright white
        int startColor = androidx.core.content.ContextCompat.getColor(view.getContext(), R.color.purple_primary);
        int endColor = 0xFFFFFFFF;

        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        colorAnim.setDuration(800);
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        colorAnim.addUpdateListener(animator -> {
            int animatedColor = (int) animator.getAnimatedValue();
            if (view instanceof ImageView) {
                ((ImageView) view).setColorFilter(animatedColor);
            } else {
                ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(animatedColor));
            }
        });

        view.setTag(R.id.tag_download_animator, colorAnim);
        colorAnim.start();
    }

    public static void stopDownloadAnimation(View view) {
        if (view == null) return;

        Object tag = view.getTag(R.id.tag_download_animator);
        if (tag instanceof Animator) {
            try {
                ((Animator) tag).cancel();
            } catch (Exception ignored) {}
        }
        view.setTag(R.id.tag_download_animator, null);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setTranslationY(0f);
        view.setAlpha(1.0f);

        if (view instanceof ImageView) {
            ((ImageView) view).clearColorFilter();
        } else {
            ViewCompat.setBackgroundTintList(view, null);
        }
    }

    public static void updateDownloadIconAnimation(View view, boolean isDownloading) {
        if (isDownloading) {
            startDownloadAnimation(view);
        } else {
            stopDownloadAnimation(view);
        }
    }
}
