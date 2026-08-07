package com.example.animelib.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ResizeModeBottomSheet extends BottomSheetDialog {

    public interface OnResizeModeChangedListener {
        void onResizeModeChanged(int resizeMode);
    }

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    private final OnResizeModeChangedListener listener;
    private OnBackPressedListener onBackPressedListener;
    private int currentResizeMode;

    private ImageView resizeFitCheck;
    private ImageView resizeZoomCheck;
    private ImageView resizeFillCheck;

    public ResizeModeBottomSheet(@NonNull Context context, int currentResizeMode, OnResizeModeChangedListener listener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.currentResizeMode = currentResizeMode;
        this.listener = listener;
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_resize_mode_selection, null);
        setContentView(view);
        com.example.animelib.util.FloatingBottomSheetUtils.setupFloatingStyle(this);

        ImageButton backButton = view.findViewById(R.id.bs_resize_back);
        LinearLayout resizeFitOption = view.findViewById(R.id.resizeFitOption);
        LinearLayout resizeZoomOption = view.findViewById(R.id.resizeZoomOption);
        LinearLayout resizeFillOption = view.findViewById(R.id.resizeFillOption);

        resizeFitCheck = view.findViewById(R.id.resizeFitCheck);
        resizeZoomCheck = view.findViewById(R.id.resizeZoomCheck);
        resizeFillCheck = view.findViewById(R.id.resizeFillCheck);

        updateSelection();

        backButton.setOnClickListener(v -> {
            if (onBackPressedListener != null) {
                dismiss();
                onBackPressedListener.onBackPressed();
            } else {
                dismiss();
            }
        });

        // AspectRatioFrameLayout values:
        // RESIZE_MODE_FIT = 0 ("С полосами по бокам")
        // RESIZE_MODE_ZOOM = 4 ("Ообрезать")
        // RESIZE_MODE_FILL = 3 ("Растянуть")
        resizeFitOption.setOnClickListener(v -> selectMode(0));
        resizeZoomOption.setOnClickListener(v -> selectMode(4));
        resizeFillOption.setOnClickListener(v -> selectMode(3));

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    private void selectMode(int mode) {
        currentResizeMode = mode;
        updateSelection();
        if (listener != null) {
            listener.onResizeModeChanged(mode);
        }
        dismiss();
    }

    private void updateSelection() {
        if (resizeFitCheck != null) resizeFitCheck.setVisibility(currentResizeMode == 0 ? View.VISIBLE : View.GONE);
        if (resizeZoomCheck != null) resizeZoomCheck.setVisibility(currentResizeMode == 4 ? View.VISIBLE : View.GONE);
        if (resizeFillCheck != null) resizeFillCheck.setVisibility(currentResizeMode == 3 ? View.VISIBLE : View.GONE);
    }

    public static String getResizeModeText(int mode) {
        switch (mode) {
            case 4: // RESIZE_MODE_ZOOM
                return "Обрезать";
            case 3: // RESIZE_MODE_FILL
                return "Растянуть";
            case 0: // RESIZE_MODE_FIT
            default:
                return "С полосами по бокам";
        }
    }
}
