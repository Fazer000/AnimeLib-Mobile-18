package com.example.animelib.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;

/**
 * Компонент для отображения спойлеров в комментариях
 */
public class SpoilerView extends LinearLayout {

    private LinearLayout headerLayout;
    private ImageView eyeIcon;
    private TextView titleView;
    private TextView actionBadge;
    private ImageView chevronIcon;
    private View lineView;
    private TextView contentView;
    private boolean isExpanded = false;
    private String spoilerTitle = "Спойлер";

    public SpoilerView(Context context) {
        super(context);
        init();
    }

    public SpoilerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpoilerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.spoiler_background);

        // Заголовочный контейнер
        headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        headerLayout.setClickable(true);
        headerLayout.setFocusable(true);

        // Иконка глаза
        eyeIcon = new ImageView(getContext());
        eyeIcon.setImageResource(R.drawable.ic_eye_spoiler);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(18), dpToPx(18));
        iconParams.setMarginEnd(dpToPx(10));
        eyeIcon.setLayoutParams(iconParams);
        headerLayout.addView(eyeIcon);

        // Название спойлера
        titleView = new TextView(getContext());
        titleView.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text_color));
        titleView.setTextSize(13);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setText("Спойлер");
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(titleParams);
        headerLayout.addView(titleView);

        // Текстовый бейдж "Показать" / "Скрыть"
        actionBadge = new TextView(getContext());
        actionBadge.setText("Показать");
        actionBadge.setTextSize(11);
        actionBadge.setTypeface(null, Typeface.BOLD);
        actionBadge.setTextColor(ContextCompat.getColor(getContext(), R.color.purple_primary));
        actionBadge.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        actionBadge.setBackgroundResource(R.drawable.chip_selected);

        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        badgeParams.setMarginEnd(dpToPx(6));
        actionBadge.setLayoutParams(badgeParams);
        headerLayout.addView(actionBadge);

        // Стрелка разворачивания
        chevronIcon = new ImageView(getContext());
        chevronIcon.setImageResource(R.drawable.ic_chevron_right);
        chevronIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.accent_text_color));
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
        chevronIcon.setLayoutParams(chevronParams);
        headerLayout.addView(chevronIcon);

        headerLayout.setOnClickListener(v -> toggleExpanded());

        addView(headerLayout);

        // Разделительная линия
        lineView = new View(getContext());
        lineView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bs_divider_color));
        lineView.setVisibility(View.GONE);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1));
        lineView.setLayoutParams(lineParams);
        addView(lineView);

        // Содержимое спойлера
        contentView = new TextView(getContext());
        contentView.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text_color));
        contentView.setTextSize(13);
        contentView.setLineSpacing(dpToPx(2), 1.0f);
        contentView.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        contentView.setVisibility(GONE);

        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        contentView.setLayoutParams(contentParams);
        addView(contentView);

        // Внешние отступы
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(6), 0, dpToPx(6));
        setLayoutParams(params);
    }

    public void setSpoilerData(String title, CharSequence content) {
        if (title != null && !title.trim().isEmpty()) {
            this.spoilerTitle = title.trim();
        } else {
            this.spoilerTitle = "Спойлер";
        }
        titleView.setText(this.spoilerTitle);

        if (content != null) {
            contentView.setText(content);
        }
    }

    public void setSpoilerData(String title, String content) {
        setSpoilerData(title, (CharSequence) content);
    }

    private void toggleExpanded() {
        isExpanded = !isExpanded;

        if (isExpanded) {
            lineView.setVisibility(View.VISIBLE);
            contentView.setVisibility(VISIBLE);
            contentView.setAlpha(0f);
            contentView.animate().alpha(1f).setDuration(200).start();

            actionBadge.setText("Скрыть");
            chevronIcon.animate().rotation(90f).setDuration(200).start();
        } else {
            lineView.setVisibility(View.GONE);
            contentView.animate().alpha(0f).setDuration(150).withEndAction(() -> contentView.setVisibility(GONE)).start();

            actionBadge.setText("Показать");
            chevronIcon.animate().rotation(0f).setDuration(200).start();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}