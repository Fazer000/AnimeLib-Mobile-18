package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.animelib.util.CustomToast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.example.animelib.data.DatabaseManager;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.viewmodel.AppSettingsViewModel;

import androidx.lifecycle.ViewModelProvider;

public class UrlInputActivity extends AppCompatActivity {
    private EditText urlEditText;
    private MaterialButton saveButton;
    private DatabaseManager databaseManager;
    private AppSettingsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_input);

        databaseManager = new DatabaseManager(this);
        viewModel = new ViewModelProvider(this).get(AppSettingsViewModel.class);

        // Load and apply theme
        loadAndApplyTheme();

        initializeViews();
        setupListeners();
    }

    private void loadAndApplyTheme() {
        try {
            int themeMode = databaseManager.loadThemeSetting();
            int sharedPrefTheme = ThemeUtils.getSavedThemePreference(this);
            int finalTheme = (themeMode >= 0 && themeMode <= 2) ? themeMode : sharedPrefTheme;
            ThemeUtils.applyThemeToActivity(this, finalTheme);
            Log.d("UrlInputActivity", "Theme applied on startup: " + finalTheme);
        } catch (Exception e) {
            Log.e("UrlInputActivity", "Failed to load and apply theme", e);
            ThemeUtils.applyThemeToActivity(this, ThemeUtils.getSavedThemePreference(this));
        }
    }

    private void initializeViews() {
        urlEditText = findViewById(R.id.urlEditText);
        saveButton = findViewById(R.id.saveButton);

        urlEditText.setText("https://animelib.org");

        // Кнопка активна по умолчанию, так как в поле уже есть валидный URL
        saveButton.setEnabled(true);
        saveButton.setAlpha(1.0f);
    }

    private void setupListeners() {
        // Слушатель изменений текста
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                boolean isValid = isValidUrl(text);
                saveButton.setEnabled(isValid);

                // Обновляем цвет кнопки
                if (isValid) {
                    saveButton.setAlpha(1.0f);
                } else {
                    saveButton.setAlpha(0.5f);
                }
            }
        });

        // Слушатель нажатия кнопки сохранения
        saveButton.setOnClickListener(v -> saveUrl());
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmedUrl = url.trim();

        // Проверяем базовые требования
        if (trimmedUrl.length() < 3) {
            return false;
        }

        // Проверяем наличие точки (домен)
        if (!trimmedUrl.contains(".")) {
            return false;
        }

        // Проверяем что не содержит пробелы
        if (trimmedUrl.contains(" ")) {
            return false;
        }

        return true;
    }

    private void saveUrl() {
        String url = urlEditText.getText().toString().trim();

        if (!isValidUrl(url)) {
            CustomToast.showWarning(this, "Пожалуйста, введите корректный URL");
            return;
        }

        try {
            // Добавляем протокол если его нет
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            // Сохраняем URL через ViewModel
            viewModel.saveSettings(url);

            Log.d("UrlInputActivity", "URL saved: " + url);

            // Возвращаем результат в MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("site_url", url);
            setResult(RESULT_OK, resultIntent);

            CustomToast.showSuccess(this, "URL сохранен: " + url);

            // Закрываем активность
            finish();

        } catch (Exception e) {
            Log.e("UrlInputActivity", "Failed to save URL", e);
            CustomToast.showWarning(this, "Ошибка при сохранении URL");
        }
    }


    @Override
    public void onBackPressed() {
        // Просто закрываем активность без сохранения
        super.onBackPressed();
    }
}
