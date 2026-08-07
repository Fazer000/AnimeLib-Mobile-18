package com.example.animelib.util;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.animelib.R;

public class SkeletonHelper {

    /**
     * Custom shimmer drawable for text skeletons
     */
    public static class SkeletonShimmerDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rectF = new RectF();
        private final float cornerRadius;
        private ValueAnimator animator;
        private float gradientTranslate = 0f;
        private final int baseColor;
        private final int shimmerColor;

        public SkeletonShimmerDrawable(float cornerRadiusDp, boolean isDarkTheme) {
            float density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
            this.cornerRadius = cornerRadiusDp * density;

            if (isDarkTheme) {
                baseColor = 0x22FFFFFF; // ~13% white
                shimmerColor = 0x55FFFFFF; // ~33% white
            } else {
                baseColor = 0x1A000000; // ~10% black
                shimmerColor = 0x3D000000; // ~24% black
            }
            startAnimation();
        }

        private void startAnimation() {
            animator = ValueAnimator.ofFloat(-1.2f, 2.2f);
            animator.setDuration(1200);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation -> {
                gradientTranslate = (float) animation.getAnimatedValue();
                invalidateSelf();
            });
            animator.start();
        }

        public void stopAnimation() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            rectF.set(bounds);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            float width = rectF.width();
            if (width <= 0) return;

            float shimmerWidth = width * 0.9f;
            float left = gradientTranslate * width;

            LinearGradient shader = new LinearGradient(
                    left, 0, left + shimmerWidth, 0,
                    new int[]{baseColor, shimmerColor, baseColor},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );

            paint.setShader(shader);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * Показывает скелетон-анимацию на TextView
     */
    public static void showSkeleton(TextView textView) {
        showSkeleton(textView, 100);
    }

    public static void showSkeleton(TextView textView, int minWidthDp) {
        if (textView == null) return;

        // Если скелетон уже запущен, пропускаем
        if (Boolean.TRUE.equals(textView.getTag(R.id.tag_skeleton_active))) {
            return;
        }

        boolean isDark = CustomToast.isDarkTheme(textView.getContext());

        // Сохраняем исходные цвет текста и фон
        textView.setTag(R.id.tag_skeleton_color, textView.getCurrentTextColor());
        textView.setTag(R.id.tag_skeleton_bg, textView.getBackground());
        textView.setTag(R.id.tag_skeleton_active, true);

        // Устанавливаем минимальную ширину
        float density = textView.getResources().getDisplayMetrics().density;
        int minPx = Math.round(minWidthDp * density);
        if (textView.getWidth() < minPx) {
            textView.setMinimumWidth(minPx);
        }

        // Применяем анимацию мерцающего фона и прозрачность для текста
        SkeletonShimmerDrawable drawable = new SkeletonShimmerDrawable(8f, isDark);
        textView.setBackground(drawable);
        textView.setTextColor(android.graphics.Color.TRANSPARENT);

        // Если текст пустой, устанавливаем пробелы для заполнености скелетона по высоте/ширине
        CharSequence currentText = textView.getText();
        if (currentText == null || currentText.length() == 0) {
            textView.setText("                      ");
        }
    }

    public static void showSkeletons(TextView... textViews) {
        if (textViews == null) return;
        for (TextView tv : textViews) {
            showSkeleton(tv);
        }
    }

    /**
     * Скрывает скелетон и устанавливает финальный текст на TextView
     */
    public static void hideSkeleton(TextView textView, CharSequence actualText) {
        if (textView == null) return;

        if (!Boolean.TRUE.equals(textView.getTag(R.id.tag_skeleton_active))) {
            if (actualText != null) {
                textView.setText(actualText);
            }
            return;
        }

        Drawable bg = textView.getBackground();
        if (bg instanceof SkeletonShimmerDrawable) {
            ((SkeletonShimmerDrawable) bg).stopAnimation();
        }

        // Восстанавливаем фон
        Object origBg = textView.getTag(R.id.tag_skeleton_bg);
        if (origBg instanceof Drawable) {
            textView.setBackground((Drawable) origBg);
        } else {
            textView.setBackground(null);
        }

        // Восстанавливаем цвет текста
        Object origColor = textView.getTag(R.id.tag_skeleton_color);
        if (origColor instanceof Integer) {
            textView.setTextColor((Integer) origColor);
        } else {
            textView.setTextColor(0xFFFFFFFF);
        }

        // Сбрасываем минимальную ширину и активный флаг
        textView.setMinimumWidth(0);
        textView.setTag(R.id.tag_skeleton_active, false);

        // Плавно подставляем текст с короткой анимацией альфа
        textView.setAlpha(0.2f);
        textView.setText(actualText != null ? actualText : "");
        textView.animate()
                .alpha(1.0f)
                .setDuration(220)
                .start();
    }
}
