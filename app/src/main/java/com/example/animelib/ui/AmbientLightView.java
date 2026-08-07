package com.example.animelib.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom View для отображения ambient-подсветки вокруг видео.
 * Высокооптимизированное рисование градиентов с кэшированием шейдеров.
 */
public class AmbientLightView extends View {
    private static final String TAG = "AmbientLightView";
    
    private static final float BLUR_INTENSITY = 1f;
    private static final long COLOR_TRANSITION_DURATION = 400; // Быстрая и плавная анимация
    private static final int LIGHTS_PER_SIDE = 3;
    private static final int LIGHT_OFFSET_DP = -10;
    private static final int LIGHT_RADIUS_DP = 220; // Оптимизированный радиус свечения
    
    private final Paint lightPaint;

    // Массивы текущих цветов
    private final int[] currentLeftColors = new int[LIGHTS_PER_SIDE];
    private final int[] currentTopColors = new int[LIGHTS_PER_SIDE];
    private final int[] currentRightColors = new int[LIGHTS_PER_SIDE];
    private final int[] currentBottomColors = new int[LIGHTS_PER_SIDE];
    
    // Массивы целевых цветов
    private final int[] targetLeftColors = new int[LIGHTS_PER_SIDE];
    private final int[] targetTopColors = new int[LIGHTS_PER_SIDE];
    private final int[] targetRightColors = new int[LIGHTS_PER_SIDE];
    private final int[] targetBottomColors = new int[LIGHTS_PER_SIDE];
    
    // Кэш шейдеров (0 аллокаций в onDraw)
    private final RadialGradient[] leftGradients = new RadialGradient[LIGHTS_PER_SIDE];
    private final RadialGradient[] topGradients = new RadialGradient[LIGHTS_PER_SIDE];
    private final RadialGradient[] rightGradients = new RadialGradient[LIGHTS_PER_SIDE];
    private final RadialGradient[] bottomGradients = new RadialGradient[LIGHTS_PER_SIDE];
    private boolean shadersNeedUpdate = true;
    
    private int lastVideoLeft = -1, lastVideoTop = -1, lastVideoRight = -1, lastVideoBottom = -1;
    
    private final ArgbEvaluator colorEvaluator;
    private ValueAnimator colorAnimator;
    private ValueAnimator intensityAnimator;
    private final int lightOffset;
    private final float lightRadius;
    private float currentIntensity = 1.0f;
    private android.graphics.RectF customVideoBounds = null;

    public void setCustomVideoBounds(float left, float top, float right, float bottom) {
        if (customVideoBounds == null) {
            customVideoBounds = new android.graphics.RectF(left, top, right, bottom);
        } else {
            customVideoBounds.set(left, top, right, bottom);
        }
        shadersNeedUpdate = true;
        postInvalidateOnAnimation();
    }

    public void clearCustomVideoBounds() {
        if (customVideoBounds != null) {
            customVideoBounds = null;
            shadersNeedUpdate = true;
            postInvalidateOnAnimation();
        }
    }

    public AmbientLightView(Context context) {
        this(context, null);
    }

    public AmbientLightView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmbientLightView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lightPaint.setDither(true);
        colorEvaluator = new ArgbEvaluator();
        
        float density = context.getResources().getDisplayMetrics().density;
        lightOffset = (int) (LIGHT_OFFSET_DP * density);
        lightRadius = LIGHT_RADIUS_DP * density;

        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setColors(int[] leftColors, int[] topColors, int[] rightColors, int[] bottomColors) {
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            targetLeftColors[i] = addAlpha(leftColors[i]);
            targetTopColors[i] = addAlpha(topColors[i]);
            targetRightColors[i] = addAlpha(rightColors[i]);
            targetBottomColors[i] = addAlpha(bottomColors[i]);
        }
        
        animateToTargetColors();
    }

    private void animateToTargetColors() {
        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }
        
        boolean hasChanges = false;
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            if (currentLeftColors[i] != targetLeftColors[i] || 
                currentTopColors[i] != targetTopColors[i] ||
                currentRightColors[i] != targetRightColors[i] ||
                currentBottomColors[i] != targetBottomColors[i]) {
                hasChanges = true;
                break;
            }
        }
        
        if (!hasChanges) {
            return;
        }
        
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(COLOR_TRANSITION_DURATION);
        colorAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        
        final int[] startLeftColors = currentLeftColors.clone();
        final int[] startTopColors = currentTopColors.clone();
        final int[] startRightColors = currentRightColors.clone();
        final int[] startBottomColors = currentBottomColors.clone();
        
        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                currentLeftColors[i] = (int) colorEvaluator.evaluate(fraction, startLeftColors[i], targetLeftColors[i]);
                currentTopColors[i] = (int) colorEvaluator.evaluate(fraction, startTopColors[i], targetTopColors[i]);
                currentRightColors[i] = (int) colorEvaluator.evaluate(fraction, startRightColors[i], targetRightColors[i]);
                currentBottomColors[i] = (int) colorEvaluator.evaluate(fraction, startBottomColors[i], targetBottomColors[i]);
            }
            
            shadersNeedUpdate = true;
            postInvalidateOnAnimation();
        });
        
        colorAnimator.start();
    }

    private int addAlpha(int color) {
        int alpha = (int) (255 * BLUR_INTENSITY);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void updateShaders(int videoLeft, int videoTop, int videoRight, int videoBottom) {
        boolean boundsChanged = (videoLeft != lastVideoLeft || videoTop != lastVideoTop ||
                videoRight != lastVideoRight || videoBottom != lastVideoBottom);

        if (!shadersNeedUpdate && !boundsChanged) {
            return;
        }

        lastVideoLeft = videoLeft;
        lastVideoTop = videoTop;
        lastVideoRight = videoRight;
        lastVideoBottom = videoBottom;
        shadersNeedUpdate = false;

        int videoHeight = videoBottom - videoTop;
        int videoWidth = videoRight - videoLeft;
        float segHeight = videoHeight / (float) LIGHTS_PER_SIDE;
        float segWidth = videoWidth / (float) LIGHTS_PER_SIDE;

        // Left
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerY = videoTop + (i + 0.5f) * segHeight;
            float centerX = videoLeft - lightOffset;
            int c = currentLeftColors[i];
            int[] colors = new int[]{ c, adjustAlpha(c, 0.85f), adjustAlpha(c, 0.5f), Color.TRANSPARENT };
            float[] pos = new float[]{ 0f, 0.25f, 0.55f, 1f };
            leftGradients[i] = new RadialGradient(centerX, centerY, lightRadius, colors, pos, Shader.TileMode.CLAMP);
        }

        // Top
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerX = videoLeft + (i + 0.5f) * segWidth;
            float centerY = videoTop - lightOffset;
            int c = currentTopColors[i];
            int[] colors = new int[]{ c, adjustAlpha(c, 0.7f), adjustAlpha(c, 0.3f), Color.TRANSPARENT };
            float[] pos = new float[]{ 0f, 0.3f, 0.6f, 1f };
            topGradients[i] = new RadialGradient(centerX, centerY, lightRadius, colors, pos, Shader.TileMode.CLAMP);
        }

        // Right
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerY = videoTop + (i + 0.5f) * segHeight;
            float centerX = videoRight + lightOffset;
            int c = currentRightColors[i];
            int[] colors = new int[]{ c, adjustAlpha(c, 0.85f), adjustAlpha(c, 0.5f), Color.TRANSPARENT };
            float[] pos = new float[]{ 0f, 0.25f, 0.55f, 1f };
            rightGradients[i] = new RadialGradient(centerX, centerY, lightRadius, colors, pos, Shader.TileMode.CLAMP);
        }

        // Bottom
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerX = videoLeft + (i + 0.5f) * segWidth;
            float centerY = videoBottom + lightOffset;
            int c = currentBottomColors[i];
            int[] colors = new int[]{ c, adjustAlpha(c, 0.7f), adjustAlpha(c, 0.3f), Color.TRANSPARENT };
            float[] pos = new float[]{ 0f, 0.3f, 0.6f, 1f };
            bottomGradients[i] = new RadialGradient(centerX, centerY, lightRadius, colors, pos, Shader.TileMode.CLAMP);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        if (getWidth() == 0 || getHeight() == 0 || getVisibility() != VISIBLE) {
            return;
        }
        
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        int videoLeft, videoTop, videoRight, videoBottom;
        
        if (customVideoBounds != null) {
            videoLeft = Math.round(customVideoBounds.left);
            videoTop = Math.round(customVideoBounds.top);
            videoRight = Math.round(customVideoBounds.right);
            videoBottom = Math.round(customVideoBounds.bottom);
        } else {
            float videoAspect = 16f / 9f;
            float screenAspect = (float) screenWidth / screenHeight;
            
            if (screenAspect > videoAspect) {
                int videoWidth = (int) (screenHeight * videoAspect);
                videoLeft = (screenWidth - videoWidth) / 2;
                videoTop = 0;
                videoRight = videoLeft + videoWidth;
                videoBottom = screenHeight;
            } else {
                int videoHeight = (int) (screenWidth / videoAspect);
                videoLeft = 0;
                videoTop = (screenHeight - videoHeight) / 2;
                videoRight = screenWidth;
                videoBottom = videoTop + videoHeight;
            }
        }
        
        updateShaders(videoLeft, videoTop, videoRight, videoBottom);

        int videoHeight = videoBottom - videoTop;
        int videoWidth = videoRight - videoLeft;
        float segHeight = videoHeight / (float) LIGHTS_PER_SIDE;
        float segWidth = videoWidth / (float) LIGHTS_PER_SIDE;

        // Left
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            if (leftGradients[i] != null) {
                float centerY = videoTop + (i + 0.5f) * segHeight;
                float centerX = videoLeft - lightOffset;
                lightPaint.setShader(leftGradients[i]);
                canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
            }
        }

        // Right
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            if (rightGradients[i] != null) {
                float centerY = videoTop + (i + 0.5f) * segHeight;
                float centerX = videoRight + lightOffset;
                lightPaint.setShader(rightGradients[i]);
                canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
            }
        }

        // Top
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            if (topGradients[i] != null) {
                float centerX = videoLeft + (i + 0.5f) * segWidth;
                float centerY = videoTop - lightOffset;
                lightPaint.setShader(topGradients[i]);
                canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
            }
        }

        // Bottom
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            if (bottomGradients[i] != null) {
                float centerX = videoLeft + (i + 0.5f) * segWidth;
                float centerY = videoBottom + lightOffset;
                lightPaint.setShader(bottomGradients[i]);
                canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
            }
        }
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb((int) (alpha * factor * currentIntensity), red, green, blue);
    }
    
    public void dimToIntensity(float targetIntensity) {
        if (intensityAnimator != null && intensityAnimator.isRunning()) {
            intensityAnimator.cancel();
        }
        
        final float startIntensity = currentIntensity;
        
        intensityAnimator = ValueAnimator.ofFloat(startIntensity, targetIntensity);
        intensityAnimator.setDuration(400);
        intensityAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        
        intensityAnimator.addUpdateListener(animation -> {
            currentIntensity = (float) animation.getAnimatedValue();
            shadersNeedUpdate = true;
            postInvalidateOnAnimation();
        });
        
        intensityAnimator.start();
    }
    
    public void suspend() {
        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }
        if (intensityAnimator != null && intensityAnimator.isRunning()) {
            intensityAnimator.cancel();
        }
        // Do not hide the view - keep current rendered glow frozen on screen
    }
    
    public void resume() {
        setVisibility(VISIBLE);
        shadersNeedUpdate = true;
        postInvalidateOnAnimation();
    }

    public void pauseAnimations() {
        suspend();
    }
    
    public void resumeAnimations() {
        resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (colorAnimator != null) {
            colorAnimator.cancel();
            colorAnimator = null;
        }
        if (intensityAnimator != null) {
            intensityAnimator.cancel();
            intensityAnimator = null;
        }
    }
}
