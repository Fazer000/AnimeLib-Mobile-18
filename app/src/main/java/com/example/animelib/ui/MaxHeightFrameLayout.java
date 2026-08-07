package com.example.animelib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MaxHeightFrameLayout extends FrameLayout {

    private int maxHeightPx = 0;
    private float maxHeightRatio = 0.45f;
    private boolean fixedHeight = false;

    public MaxHeightFrameLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MaxHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MaxHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        maxHeightPx = (int) (metrics.heightPixels * maxHeightRatio);
    }

    public void setMaxHeightRatio(float ratio) {
        if (ratio > 0 && ratio <= 1.0f) {
            this.maxHeightRatio = ratio;
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            this.maxHeightPx = (int) (metrics.heightPixels * ratio);
            requestLayout();
        }
    }

    public void setMaxHeightPx(int maxHeightPx) {
        this.maxHeightPx = maxHeightPx;
        requestLayout();
    }

    public void setFixedHeight(boolean fixedHeight) {
        this.fixedHeight = fixedHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeightPx > 0) {
            if (fixedHeight) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.EXACTLY);
            } else {
                int hSize = MeasureSpec.getSize(heightMeasureSpec);
                int hMode = MeasureSpec.getMode(heightMeasureSpec);
                if (hMode == MeasureSpec.UNSPECIFIED || hSize > maxHeightPx) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST);
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
