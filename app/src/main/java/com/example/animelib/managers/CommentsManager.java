package com.example.animelib.managers;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.adapters.CommentsAdapter;
import com.example.animelib.api.ApiService;
import com.example.animelib.data.entity.TokenEntity;
import com.example.animelib.models.CommentsResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.util.CustomToast;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер для работы с комментариями к эпизодам
 * Обеспечивает загрузку, отображение и управление комментариями
 */
public class CommentsManager {

    private static final String TAG = "CommentsManager";

    // UI компоненты
    private View commentsPanel;
    private ImageButton closeCommentsButton;
    private RecyclerView commentsRecyclerView;
    private View commentsLoadingOverlay;
    private ImageButton commentsButton;
    private View commentsOptionsButton;
    private View menuOverlay;
    private TextView emptyCommentsText;
    
    // Адаптер и состояние
    private CommentsAdapter commentsAdapter;
    private RecyclerView portraitCommentsRecyclerView;
    private CommentsAdapter portraitCommentsAdapter;
    private View portraitCommentsLoadingOverlay;
    private TextView portraitEmptyCommentsText;
    private TextView portraitSortText;

    // Компоненты ввода комментария и ответа (Landscape / Panel)
    private android.widget.EditText commentInputField;
    private ImageButton btnSendComment;
    private View btnInsertSpoiler;
    private View replyBar;
    private TextView tvReplyToText;
    private ImageButton btnCancelReply;

    // Компоненты ввода комментария и ответа (Portrait)
    private android.widget.EditText portraitCommentInputField;
    private ImageButton btnPortraitSendComment;
    private View btnPortraitInsertSpoiler;
    private View portraitReplyBar;
    private TextView tvPortraitReplyToText;
    private ImageButton btnPortraitCancelReply;

    // Состояние ответа
    private Long replyParentCommentId = null;
    private Long replyRootId = null;
    private int replyCommentLevel = 0;
    private String replyUsername = null;

    private boolean isCommentsVisible = false;
    private int commentsPanelWidth = 360; // dp
    private int commentsCurrentPage = 1;
    private boolean commentsHasNextPage = true;
    private boolean isLoadingComments = false;
    private String commentsSortType = "desc";
    
    // Контекст и сервисы
    private Context context;
    private ApiService apiService;
    private EpisodesListResponse.EpisodeItem currentEpisode;
    
    // Callback интерфейсы
    public interface CommentsVisibilityCallback {
        void onCommentsVisibilityChanged(boolean isVisible);
    }
    
    public interface CommentsDataCallback {
        void onCommentsLoaded(List<CommentsResponse.CommentItem> comments);
        void onCommentsError(String error);
    }
    
    private CommentsVisibilityCallback visibilityCallback;
    private CommentsDataCallback dataCallback;
    
    /**
     * Конструктор CommentsManager
     * @param context Контекст приложения
     * @param apiService Сервис для API запросов
     */
    public CommentsManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Инициализация UI компонентов комментариев
     * @param commentsPanel Панель комментариев
     * @param closeCommentsButton Кнопка закрытия
     * @param commentsRecyclerView RecyclerView для списка комментариев
     * @param commentsLoadingOverlay Индикатор загрузки
     * @param commentsButton Кнопка открытия комментариев
     * @param commentsOptionsButton Кнопка опций сортировки
     * @param menuOverlay Overlay для закрытия по клику вне панели
     * @param emptyCommentsText Текст для отображения когда комментариев нет
     */
    public void initializeViews(View commentsPanel, ImageButton closeCommentsButton,
                               RecyclerView commentsRecyclerView, View commentsLoadingOverlay,
                               ImageButton commentsButton, View commentsOptionsButton,
                               View menuOverlay, TextView emptyCommentsText) {
        this.commentsPanel = commentsPanel;
        this.closeCommentsButton = closeCommentsButton;
        this.commentsRecyclerView = commentsRecyclerView;
        this.commentsLoadingOverlay = commentsLoadingOverlay;
        this.commentsButton = commentsButton;
        this.commentsOptionsButton = commentsOptionsButton;
        this.menuOverlay = menuOverlay;
        this.emptyCommentsText = emptyCommentsText;
        
        setupCommentsViews();
        initializePanelPosition();
    }
    
    /**
     * Настройка обработчиков событий для комментариев
     */
    private void setupCommentsViews() {
        if (closeCommentsButton != null) {
            closeCommentsButton.setOnClickListener(v -> hideCommentsPanel());
        }
        
        if (commentsButton != null) {
            commentsButton.setOnClickListener(v -> {
                Log.d("CommentsManager", "Comments button clicked!");
                toggleCommentsPanel();
            });
            Log.d("CommentsManager", "Comments button initialized successfully");
        } else {
            Log.w("CommentsManager", "Comments button is null!");
        }
        
        // Настройка текста для пустого состояния
        if (emptyCommentsText != null) {
            emptyCommentsText.setText("Комментариев пока нет");
            emptyCommentsText.setVisibility(View.GONE);
        }

        if (commentsPanel != null) {
            commentInputField = commentsPanel.findViewById(R.id.commentInputField);
            btnSendComment = commentsPanel.findViewById(R.id.btnSendComment);
            replyBar = commentsPanel.findViewById(R.id.replyBar);
            tvReplyToText = commentsPanel.findViewById(R.id.tvReplyToText);
            btnCancelReply = commentsPanel.findViewById(R.id.btnCancelReply);

            if (commentInputField != null) {
                com.example.animelib.util.CommentFormattingHelper.attachFormattingTextWatcher(commentInputField);
            }

            View btnBold = commentsPanel.findViewById(R.id.btnFormatBold);
            View btnItalic = commentsPanel.findViewById(R.id.btnFormatItalic);
            View btnUnderline = commentsPanel.findViewById(R.id.btnFormatUnderline);
            View btnStrike = commentsPanel.findViewById(R.id.btnFormatStrike);
            View btnSpoiler = commentsPanel.findViewById(R.id.btnFormatSpoiler);
            View btnQuote = commentsPanel.findViewById(R.id.btnFormatQuote);

            if (btnBold != null) btnBold.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(commentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.BOLD));
            if (btnItalic != null) btnItalic.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(commentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.ITALIC));
            if (btnUnderline != null) btnUnderline.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(commentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.UNDERLINE));
            if (btnStrike != null) btnStrike.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(commentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.STRIKE));
            if (btnSpoiler != null) btnSpoiler.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(commentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.SPOILER));
            if (btnQuote != null) btnQuote.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(commentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.QUOTE));

            if (btnSendComment != null) {
                btnSendComment.setOnClickListener(v -> sendCommentFromInput(commentInputField));
            }
            if (btnCancelReply != null) {
                btnCancelReply.setOnClickListener(v -> cancelReplyMode());
            }
            if (commentInputField != null) {
                commentInputField.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                        sendCommentFromInput(commentInputField);
                        return true;
                    }
                    return false;
                });
            }
        }
        
        setupCommentsOptionsButton();
        setupCommentsRecyclerView();
    }
    
    /**
     * Инициализация начальной позиции панели комментариев
     */
    private void initializePanelPosition() {
        if (commentsPanel == null) return;
        
        // Устанавливаем начальную позицию панели (за экраном справа)
        int panelWidth = (int) (commentsPanelWidth * context.getResources().getDisplayMetrics().density);
        commentsPanel.setTranslationX(panelWidth);
        Log.d("CommentsManager", "Initialized comments panel position: " + panelWidth);
    }
    
    /**
     * Обновить текст на кнопке сортировки комментариев
     */
    public void updateSortButtonText() {
        String label = "asc".equals(commentsSortType) ? "Старые" : ("votes_up".equals(commentsSortType) ? "Популярные" : "Новые");
        if (commentsOptionsButton != null) {
            TextView tvSort = commentsOptionsButton.findViewById(R.id.tvSortComments);
            if (tvSort != null) {
                tvSort.setText(label);
            }
        }
        if (portraitSortText != null) {
            portraitSortText.setText(label);
        }
    }

    /**
     * Настройка кнопки опций сортировки комментариев
     */
    private void setupCommentsOptionsButton() {
        if (commentsOptionsButton == null) return;
        updateSortButtonText();
        commentsOptionsButton.setOnClickListener(v -> showSortOptionsDialog());
    }
    
    /**
     * Показать выпадающий список выбора сортировки комментариев (PopupWindow)
     */
    public void showSortOptionsDialog() {
        showSortOptionsDialog(commentsOptionsButton);
    }

    public void showSortOptionsDialog(View anchorView) {
        View target = anchorView != null ? anchorView : commentsOptionsButton;
        if (target == null || context == null) return;
        if (!target.isAttachedToWindow()) return;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) return;
        }
        Log.d("CommentsManager", "Showing sort options popup menu");

        try {
            View popupView = LayoutInflater.from(context).inflate(R.layout.popup_comments_sort, null);

            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    dpToPx(160),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.setElevation(dpToPx(12));
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            TextView itemNew = popupView.findViewById(R.id.item_sort_new);
            TextView itemOld = popupView.findViewById(R.id.item_sort_old);
            TextView itemPopular = popupView.findViewById(R.id.item_sort_popular);

            if (itemNew != null) resetPopupItemStyle(itemNew);
            if (itemOld != null) resetPopupItemStyle(itemOld);
            if (itemPopular != null) resetPopupItemStyle(itemPopular);

            if ("asc".equals(commentsSortType)) {
                if (itemOld != null) setPopupItemSelectedStyle(itemOld);
            } else if ("votes_up".equals(commentsSortType)) {
                if (itemPopular != null) setPopupItemSelectedStyle(itemPopular);
            } else {
                if (itemNew != null) setPopupItemSelectedStyle(itemNew);
            }

            if (itemNew != null) {
                itemNew.setOnClickListener(v -> {
                    try {
                        popupWindow.dismiss();
                    } catch (Exception ignored) {}
                    changeCommentsSort("desc");
                });
            }

            if (itemOld != null) {
                itemOld.setOnClickListener(v -> {
                    try {
                        popupWindow.dismiss();
                    } catch (Exception ignored) {}
                    changeCommentsSort("asc");
                });
            }

            if (itemPopular != null) {
                itemPopular.setOnClickListener(v -> {
                    try {
                        popupWindow.dismiss();
                    } catch (Exception ignored) {}
                    changeCommentsSort("votes_up");
                });
            }

            popupWindow.showAsDropDown(anchorView != null ? anchorView : commentsOptionsButton, 0, dpToPx(6));
        } catch (Exception e) {
            Log.e(TAG, "Error showing sort options popup", e);
        }
    }

    /**
     * Показать выпадающее меню действий с комментарием (Удалить / Пожаловаться)
     */
    public void showCommentActionsPopup(View anchorView, CommentsResponse.CommentItem comment) {
        if (anchorView == null || comment == null || context == null) return;
        if (!anchorView.isAttachedToWindow()) return;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) return;
        }

        try {
            View popupView = LayoutInflater.from(context).inflate(R.layout.popup_comment_actions, null);

            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    dpToPx(160),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.setElevation(dpToPx(12));
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            TextView itemDelete = popupView.findViewById(R.id.item_comment_delete);
            TextView itemReport = popupView.findViewById(R.id.item_comment_report);

            boolean isOwn = isOwnComment(comment);

            if (itemDelete != null) {
                itemDelete.setVisibility(isOwn ? View.VISIBLE : View.GONE);
                itemDelete.setOnClickListener(v -> {
                    try {
                        popupWindow.dismiss();
                    } catch (Exception ignored) {}
                    confirmAndDeleteComment(comment);
                });
            }

            if (itemReport != null) {
                itemReport.setOnClickListener(v -> {
                    try {
                        popupWindow.dismiss();
                    } catch (Exception ignored) {}
                    CustomToast.showSuccess(context, "Жалоба отправлена");
                });
            }

            popupWindow.showAsDropDown(anchorView, 0, dpToPx(4));
        } catch (Exception e) {
            Log.e(TAG, "Error showing comment actions popup", e);
        }
    }

    /**
     * Диалог подтверждения и удаление комментария
     */
    public void confirmAndDeleteComment(CommentsResponse.CommentItem comment) {
        if (comment == null || context == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Удаление комментария")
                .setMessage("Вы уверены, что хотите удалить этот комментарий?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (apiService != null) {
                        apiService.deleteComment(comment.getId(), new ApiService.DeleteCommentCallback() {
                            @Override
                            public void onSuccess() {
                                CustomToast.showSuccess(context, "Комментарий удален");
                                if (commentsAdapter != null) {
                                    commentsAdapter.removeComment(comment.getId());
                                }
                                if (portraitCommentsAdapter != null) {
                                    portraitCommentsAdapter.removeComment(comment.getId());
                                }
                            }

                            @Override
                            public void onError(String errorMsg) {
                                CustomToast.showWarning(context, errorMsg);
                            }
                        });
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Проверка, принадлежит ли комментарий текущему пользователю
     */
    public boolean isOwnComment(CommentsResponse.CommentItem comment) {
        if (comment == null || comment.getUser() == null) return false;
        long commentUserId = comment.getUser().getId();

        try {
            if (apiService != null && apiService.getDatabaseManager() != null) {
                TokenEntity token = apiService.getDatabaseManager().getToken();
                if (token != null && token.getAccessToken() != null && !token.getAccessToken().trim().isEmpty()) {
                    String savedUserId = token.getUserId();
                    if (savedUserId != null && !savedUserId.trim().isEmpty()) {
                        try {
                            if (Long.parseLong(savedUserId.trim()) == commentUserId) {
                                return true;
                            }
                        } catch (Exception ignored) {}
                        if (savedUserId.trim().equals(String.valueOf(commentUserId))) {
                            return true;
                        }
                    }

                    String extractedId = ApiService.extractUserIdFromToken(token.getAccessToken());
                    if (extractedId != null && !extractedId.trim().isEmpty()) {
                        try {
                            if (Long.parseLong(extractedId.trim()) == commentUserId) {
                                return true;
                            }
                        } catch (Exception ignored) {}
                        if (extractedId.trim().equals(String.valueOf(commentUserId))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if own comment", e);
        }

        return false;
    }

    private void resetPopupItemStyle(TextView textView) {
        if (textView == null) return;
        textView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        textView.setTextColor(0xFF94A3B8);
    }

    private void setPopupItemSelectedStyle(TextView textView) {
        if (textView == null) return;
        textView.setBackgroundResource(R.drawable.bg_sort_popup_item_selected);
        textView.setTextColor(0xFFFFFFFF);
    }
    
    /**
     * Вспомогательный метод для преобразования dp в px
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    /**
     * Настройка RecyclerView для комментариев
     */
    private void setupCommentsRecyclerView() {
        if (commentsRecyclerView == null) return;
        
        commentsAdapter = new CommentsAdapter();
        commentsAdapter.setCommentActionListener(new CommentsAdapter.CommentActionListener() {
            @Override
            public void onReplyClicked(CommentsResponse.CommentItem comment) {
                if (portraitCommentsAdapter != null) {
                    portraitCommentsAdapter.setActiveReplyCommentId(comment != null ? comment.getId() : null);
                }
                startReplyMode(comment);
            }

            @Override
            public void onSendInlineReply(CommentsResponse.CommentItem comment, CharSequence rawText, android.widget.EditText inputField) {
                sendInlineReply(comment, rawText, inputField);
            }

            @Override
            public void onVoteClicked(CommentsResponse.CommentItem comment, int targetVote) {
                voteOnComment(comment, targetVote);
            }

            @Override
            public void onMoreActionsClicked(View anchorView, CommentsResponse.CommentItem comment) {
                showCommentActionsPopup(anchorView, comment);
            }
        });
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        commentsRecyclerView.setAdapter(commentsAdapter);
        
        // Добавляем слушатель прокрутки для пагинации
        commentsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager == null) return;
                
                int total = layoutManager.getItemCount();
                int last = layoutManager.findLastVisibleItemPosition();
                
                if (!isLoadingComments && commentsHasNextPage && last >= total - 3) {
                    loadCommentsPage(commentsCurrentPage + 1);
                }
            }
        });
    }
    
    /**
     * Переключение видимости панели комментариев
     */
    public void toggleCommentsPanel() {
        Log.d("CommentsManager", "toggleCommentsPanel called, isCommentsVisible: " + isCommentsVisible);
        if (isCommentsVisible) {
            hideCommentsPanel();
        } else {
            showCommentsPanel();
        }
    }
    
    /**
     * Показать панель комментариев
     */
    public void showCommentsPanel() {
        Log.d("CommentsManager", "showCommentsPanel called, commentsPanel: " + (commentsPanel != null) + ", isCommentsVisible: " + isCommentsVisible);
        if (isCommentsVisible) {
            Log.w("CommentsManager", "Comments panel already visible");
            return;
        }
        
        isCommentsVisible = true;
        
        // Use VideoPlayerActivity's method to open draggable panel
        if (context instanceof com.example.animelib.VideoPlayerActivity) {
            ((com.example.animelib.VideoPlayerActivity) context).openCommentsPanel();
        }
        
        // Загрузить первую страницу если комментарии пустые
        if (!isLoadingComments && (commentsAdapter == null || commentsAdapter.getItemCount() == 0)) {
            commentsCurrentPage = 1;
            commentsHasNextPage = true;
            loadCommentsPage(1);
        }
        
        // Уведомить о изменении видимости
        if (visibilityCallback != null) {
            visibilityCallback.onCommentsVisibilityChanged(true);
        }
    }
    
    /**
     * Скрыть панель комментариев
     */
    public void hideCommentsPanel() {
        if (!isCommentsVisible) return;
        
        isCommentsVisible = false;
        
        // Use VideoPlayerActivity's method to close draggable panel
        if (context instanceof com.example.animelib.VideoPlayerActivity) {
            ((com.example.animelib.VideoPlayerActivity) context).closeCommentsPanel();
        }
        
        // Уведомить о изменении видимости
        if (visibilityCallback != null) {
            visibilityCallback.onCommentsVisibilityChanged(false);
        }
    }
    
    /**
     * Загрузить страницу комментариев
     * @param page Номер страницы
     */
    public void loadCommentsPage(int page) {
        if (currentEpisode == null) return;
        
        isLoadingComments = true;
        if (commentsLoadingOverlay != null) {
            commentsLoadingOverlay.setVisibility(View.VISIBLE);
        }
        if (portraitCommentsLoadingOverlay != null) {
            portraitCommentsLoadingOverlay.setVisibility(View.VISIBLE);
        }
        
        long episodeId = currentEpisode.getId();
        apiService.fetchEpisodeComments(episodeId, commentsSortType, page, 
            new ApiService.EpisodeCommentsCallback() {
            @Override
            public void onCommentsReceived(CommentsResponse response) {
                // Выполняем обновление UI в главном потоке
                safeRunOnUiThread(() -> {
                        if (commentsLoadingOverlay != null) {
                            commentsLoadingOverlay.setVisibility(View.GONE);
                        }
                        if (portraitCommentsLoadingOverlay != null) {
                            portraitCommentsLoadingOverlay.setVisibility(View.GONE);
                        }
                        isLoadingComments = false;
                        
                        if (response != null) {
                            if (commentsAdapter != null) {
                                commentsAdapter.appendResponse(response, page > 1);
                            }
                            if (portraitCommentsAdapter != null) {
                                portraitCommentsAdapter.appendResponse(response, page > 1);
                            }
                            
                            // Показываем/скрываем текст пустого состояния
                            boolean hasComments = (portraitCommentsAdapter != null && portraitCommentsAdapter.getItemCount() > 0)
                                    || (commentsAdapter != null && commentsAdapter.getItemCount() > 0);
                            if (response.getData() != null) {
                                if (response.getData().getRoot() != null && !response.getData().getRoot().isEmpty()) {
                                    hasComments = true;
                                }
                                if (response.getData().getReplies() != null && !response.getData().getReplies().isEmpty()) {
                                    hasComments = true;
                                }
                            }

                            if (emptyCommentsText != null) {
                                emptyCommentsText.setVisibility(hasComments ? View.GONE : View.VISIBLE);
                            }
                            if (portraitEmptyCommentsText != null) {
                                portraitEmptyCommentsText.setVisibility(hasComments ? View.GONE : View.VISIBLE);
                            }
                            
                            // Уведомить о загрузке данных
                            if (dataCallback != null && response.getData() != null) {
                                // Объединяем root и replies комментарии
                                List<CommentsResponse.CommentItem> allComments = new ArrayList<>();
                                if (response.getData().getRoot() != null) {
                                    allComments.addAll(response.getData().getRoot());
                                }
                                if (response.getData().getReplies() != null) {
                                    allComments.addAll(response.getData().getReplies());
                                }
                                dataCallback.onCommentsLoaded(allComments);
                            }
                        }
                        
                        if (response != null && response.getMeta() != null) {
                            commentsHasNextPage = response.getMeta().isHas_next_page();
                            commentsCurrentPage = page;
                        }
                    });
            }
                
            @Override
            public void onError(String error) {
                isLoadingComments = false;
                if (commentsLoadingOverlay != null) {
                    commentsLoadingOverlay.setVisibility(View.GONE);
                }
                if (portraitCommentsLoadingOverlay != null) {
                    portraitCommentsLoadingOverlay.setVisibility(View.GONE);
                }
                
                // Скрываем текст пустого состояния при ошибке
                if (emptyCommentsText != null) {
                    emptyCommentsText.setVisibility(View.GONE);
                }
                if (portraitEmptyCommentsText != null) {
                    portraitEmptyCommentsText.setVisibility(View.GONE);
                }
                
                // Показать Toast в UI потоке
                safeRunOnUiThread(() -> {
                        CustomToast.showWarning(context, error);
                    });
                
                // Уведомить об ошибке
                if (dataCallback != null) {
                    dataCallback.onCommentsError(error);
                }
            }
            });
    }

    /**
     * Сбросить состояние комментариев при смене эпизода
     * @param reloadIfVisible Перезагрузить если панель видна
     */
    public void resetCommentsOnEpisodeChange(boolean reloadIfVisible) {
        commentsCurrentPage = 1;
        commentsHasNextPage = true;
        isLoadingComments = false;
        
        if (commentsAdapter != null) {
            commentsAdapter.clearAll();
        }
        if (portraitCommentsAdapter != null) {
            portraitCommentsAdapter.clearAll();
        }
        
        // Скрываем текст пустого состояния при сбросе
        if (emptyCommentsText != null) {
            emptyCommentsText.setVisibility(View.GONE);
        }
        if (portraitEmptyCommentsText != null) {
            portraitEmptyCommentsText.setVisibility(View.GONE);
        }
        
        if (reloadIfVisible && shouldLoadComments()) {
            loadCommentsPage(1);
        }
    }

    public boolean shouldLoadComments() {
        return isCommentsVisible 
            || portraitCommentsRecyclerView != null 
            || (context != null && context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT);
    }
    
    /**
     * Изменить сортировку комментариев
     * @param sort Новый тип сортировки
     */
    public void changeCommentsSort(String sort) {
        if (sort == null) return;
        
        boolean changed = !sort.equals(commentsSortType);
        commentsSortType = sort;
        updateSortButtonText();
        
        if (changed) {
            resetCommentsOnEpisodeChange(false);
            
            if (shouldLoadComments()) {
                loadCommentsPage(1);
            }
        }
    }
    
    /**
     * Установить текущий эпизод
     * @param episode Текущий эпизод
     */
    public void setCurrentEpisode(EpisodesListResponse.EpisodeItem episode) {
        this.currentEpisode = episode;
        if (shouldLoadComments() && (portraitCommentsAdapter == null || portraitCommentsAdapter.getItemCount() == 0)) {
            loadCommentsPage(1);
        }
    }
    
    /**
     * Получить текущий эпизод
     * @return Текущий эпизод
     */
    public EpisodesListResponse.EpisodeItem getCurrentEpisode() {
        return currentEpisode;
    }
    
    /**
     * Проверить видимость панели комментариев
     * @return true если панель видна
     */
    public boolean isCommentsVisible() {
        return isCommentsVisible;
    }
    
    /**
     * Вызывается когда панель закрывается через драг
     */
    public void onPanelClosedByDrag() {
        Log.d("CommentsManager", "Panel closed by drag, updating isCommentsVisible flag");
        isCommentsVisible = false;
    }
    
    /**
     * Получить текущий тип сортировки
     * @return Тип сортировки
     */
    public String getCommentsSortType() {
        return commentsSortType;
    }
    
    /**
     * Установить callback для изменения видимости
     * @param callback Callback для видимости
     */
    public void setPortraitViews(RecyclerView rv, View loadingOverlay, TextView emptyText, View sortBtn, TextView tvSortText, View rulesBtn) {
        this.portraitCommentsRecyclerView = rv;
        this.portraitCommentsLoadingOverlay = loadingOverlay;
        this.portraitEmptyCommentsText = emptyText;
        this.portraitSortText = tvSortText;

        if (this.portraitCommentsRecyclerView != null) {
            this.portraitCommentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            this.portraitCommentsAdapter = new CommentsAdapter();
            this.portraitCommentsAdapter.setCommentActionListener(new CommentsAdapter.CommentActionListener() {
                @Override
                public void onReplyClicked(CommentsResponse.CommentItem comment) {
                    if (commentsAdapter != null) {
                        commentsAdapter.setActiveReplyCommentId(comment != null ? comment.getId() : null);
                    }
                    startReplyMode(comment);
                }

                @Override
                public void onSendInlineReply(CommentsResponse.CommentItem comment, CharSequence rawText, android.widget.EditText inputField) {
                    sendInlineReply(comment, rawText, inputField);
                }

                @Override
                public void onVoteClicked(CommentsResponse.CommentItem comment, int targetVote) {
                    voteOnComment(comment, targetVote);
                }

                @Override
                public void onMoreActionsClicked(View anchorView, CommentsResponse.CommentItem comment) {
                    showCommentActionsPopup(anchorView, comment);
                }
            });
            this.portraitCommentsRecyclerView.setAdapter(this.portraitCommentsAdapter);
            this.portraitCommentsRecyclerView.setNestedScrollingEnabled(false);
        }

        if (context instanceof android.app.Activity) {
            android.app.Activity act = (android.app.Activity) context;
            portraitCommentInputField = act.findViewById(R.id.portraitCommentInputField);
            btnPortraitSendComment = act.findViewById(R.id.btnPortraitSendComment);
            portraitReplyBar = act.findViewById(R.id.portraitReplyBar);
            tvPortraitReplyToText = act.findViewById(R.id.tvPortraitReplyToText);
            btnPortraitCancelReply = act.findViewById(R.id.btnPortraitCancelReply);

            if (portraitCommentInputField != null) {
                com.example.animelib.util.CommentFormattingHelper.attachFormattingTextWatcher(portraitCommentInputField);
            }

            View btnPBold = act.findViewById(R.id.btnPortraitFormatBold);
            View btnPItalic = act.findViewById(R.id.btnPortraitFormatItalic);
            View btnPUnderline = act.findViewById(R.id.btnPortraitFormatUnderline);
            View btnPStrike = act.findViewById(R.id.btnPortraitFormatStrike);
            View btnPSpoiler = act.findViewById(R.id.btnPortraitFormatSpoiler);
            View btnPQuote = act.findViewById(R.id.btnPortraitFormatQuote);

            if (btnPBold != null) btnPBold.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(portraitCommentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.BOLD));
            if (btnPItalic != null) btnPItalic.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(portraitCommentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.ITALIC));
            if (btnPUnderline != null) btnPUnderline.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(portraitCommentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.UNDERLINE));
            if (btnPStrike != null) btnPStrike.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(portraitCommentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.STRIKE));
            if (btnPSpoiler != null) btnPSpoiler.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(portraitCommentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.SPOILER));
            if (btnPQuote != null) btnPQuote.setOnClickListener(v -> com.example.animelib.util.CommentFormattingHelper.applyFormat(portraitCommentInputField, com.example.animelib.util.CommentFormattingHelper.FormatType.QUOTE));

            if (btnPortraitSendComment != null) {
                btnPortraitSendComment.setOnClickListener(v -> sendCommentFromInput(portraitCommentInputField));
            }
            if (btnPortraitCancelReply != null) {
                btnPortraitCancelReply.setOnClickListener(v -> cancelReplyMode());
            }
            if (portraitCommentInputField != null) {
                portraitCommentInputField.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                        sendCommentFromInput(portraitCommentInputField);
                        return true;
                    }
                    return false;
                });
            }
        }

        if (sortBtn != null) {
            sortBtn.setOnClickListener(v -> showSortOptionsDialog(v));
        }

        if (rulesBtn != null) {
            rulesBtn.setOnClickListener(v -> showRulesDialog());
        }

        updateSortButtonText();
        if (currentEpisode != null) {
            loadCommentsForPortraitIfNeeded();
        }
    }

    public void loadCommentsForPortraitIfNeeded() {
        if (currentEpisode != null && !isLoadingComments && (portraitCommentsAdapter == null || portraitCommentsAdapter.getItemCount() == 0)) {
            commentsCurrentPage = 1;
            commentsHasNextPage = true;
            loadCommentsPage(1);
        }
    }

    public void loadNextCommentsPageIfAvailable() {
        if (!isLoadingComments && commentsHasNextPage && currentEpisode != null) {
            loadCommentsPage(commentsCurrentPage + 1);
        }
    }

    public void startReplyMode(CommentsResponse.CommentItem comment) {
        if (comment == null) return;
        replyParentCommentId = comment.getId();
        replyRootId = comment.getRoot_id() != null ? comment.getRoot_id() : comment.getId();
        replyCommentLevel = comment.getComment_level() + 1;
        replyUsername = (comment.getUser() != null && comment.getUser().getUsername() != null)
                ? comment.getUser().getUsername() : "пользователю";

        String replyText = "Ответ для @" + replyUsername;

        if (replyBar != null && tvReplyToText != null) {
            tvReplyToText.setText(replyText);
            replyBar.setVisibility(View.VISIBLE);
        }
        if (portraitReplyBar != null && tvPortraitReplyToText != null) {
            tvPortraitReplyToText.setText(replyText);
            portraitReplyBar.setVisibility(View.VISIBLE);
        }
    }

    public void cancelReplyMode() {
        replyParentCommentId = null;
        replyRootId = null;
        replyCommentLevel = 0;
        replyUsername = null;

        if (commentsAdapter != null) {
            commentsAdapter.setActiveReplyCommentId(null);
        }
        if (portraitCommentsAdapter != null) {
            portraitCommentsAdapter.setActiveReplyCommentId(null);
        }

        if (replyBar != null) {
            replyBar.setVisibility(View.GONE);
        }
        if (portraitReplyBar != null) {
            portraitReplyBar.setVisibility(View.GONE);
        }
    }

    public void insertSpoilerTag(android.widget.EditText editText) {
        if (editText == null) return;
        com.example.animelib.util.CommentFormattingHelper.applyFormat(editText, com.example.animelib.util.CommentFormattingHelper.FormatType.SPOILER);
    }

    public void sendCommentFromInput(android.widget.EditText inputField) {
        if (inputField == null || currentEpisode == null) return;
        CharSequence rawEditable = inputField.getText();
        String text = rawEditable != null ? com.example.animelib.util.CommentFormattingHelper.toBBCode(rawEditable).trim() : "";
        if (text.isEmpty()) {
            CustomToast.showWarning(context, "Введите текст комментария");
            return;
        }

        long episodeId = currentEpisode.getId();
        
        boolean isReplyActive = (replyBar != null && replyBar.getVisibility() == View.VISIBLE)
                || (portraitReplyBar != null && portraitReplyBar.getVisibility() == View.VISIBLE);

        Long parentId = isReplyActive ? replyParentCommentId : null;
        Long rootId = isReplyActive ? replyRootId : null;
        int level = isReplyActive ? replyCommentLevel : 0;

        CustomToast.showInfo(context, "Отправка комментария...");

        apiService.postComment(episodeId, text, parentId, rootId, level, new ApiService.PostCommentCallback() {
            @Override
            public void onSuccess(CommentsResponse.CommentItem postedComment) {
                safeRunOnUiThread(() -> {
                    CustomToast.showSuccess(context, "Комментарий опубликован!");
                    inputField.setText("");
                    if (commentInputField != null) commentInputField.setText("");
                    if (portraitCommentInputField != null) portraitCommentInputField.setText("");

                    cancelReplyMode();

                    try {
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(inputField.getWindowToken(), 0);
                        }
                    } catch (Exception ignored) {}

                    resetCommentsOnEpisodeChange(false);
                    loadCommentsPage(1);
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    CustomToast.showWarning(context, error);
                });
            }
        });
    }

    public void sendInlineReply(CommentsResponse.CommentItem comment, CharSequence rawText, android.widget.EditText inputField) {
        if (comment == null || currentEpisode == null) return;
        String text = rawText != null ? com.example.animelib.util.CommentFormattingHelper.toBBCode(rawText).trim() : "";
        if (text.isEmpty()) {
            CustomToast.showWarning(context, "Введите текст ответа");
            return;
        }

        long episodeId = currentEpisode.getId();
        Long parentId = comment.getId();
        Long rootId = comment.getRoot_id() != null ? comment.getRoot_id() : comment.getId();
        int level = comment.getComment_level() + 1;

        CustomToast.showInfo(context, "Отправка ответа...");

        apiService.postComment(episodeId, text, parentId, rootId, level, new ApiService.PostCommentCallback() {
            @Override
            public void onSuccess(CommentsResponse.CommentItem postedComment) {
                safeRunOnUiThread(() -> {
                    CustomToast.showSuccess(context, "Ответ опубликован!");
                    if (inputField != null) inputField.setText("");

                    cancelReplyMode();

                    try {
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null && inputField != null) {
                            imm.hideSoftInputFromWindow(inputField.getWindowToken(), 0);
                        }
                    } catch (Exception ignored) {}

                    resetCommentsOnEpisodeChange(false);
                    loadCommentsPage(1);
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    CustomToast.showWarning(context, error);
                });
            }
        });
    }

    public void voteOnComment(CommentsResponse.CommentItem comment, int targetVote) {
        if (comment == null) return;
        apiService.voteComment(comment.getId(), targetVote, new ApiService.VoteCommentCallback() {
            @Override
            public void onSuccess(int newVoteValue, CommentsResponse.Votes updatedVotes) {
                safeRunOnUiThread(() -> {
                    if (updatedVotes != null) {
                        comment.setVotes(updatedVotes);
                    } else {
                        if (comment.getVotes() == null) {
                            comment.setVotes(new CommentsResponse.Votes());
                        }
                        Integer oldVote = null;
                        if (comment.getVotes().getUser() != null) {
                            try {
                                if (comment.getVotes().getUser() instanceof Number) {
                                    oldVote = ((Number) comment.getVotes().getUser()).intValue();
                                } else if (comment.getVotes().getUser() instanceof Boolean) {
                                    oldVote = ((Boolean) comment.getVotes().getUser()) ? 1 : 0;
                                } else {
                                    String str = comment.getVotes().getUser().toString().trim();
                                    if ("true".equalsIgnoreCase(str)) oldVote = 1;
                                    else if ("false".equalsIgnoreCase(str)) oldVote = 0;
                                    else oldVote = Integer.parseInt(str);
                                }
                            } catch (Exception ignored) {}
                        }

                        int up = comment.getVotes().getUp();
                        int down = comment.getVotes().getDown();

                        if (oldVote != null) {
                            if (oldVote == 1) up = Math.max(0, up - 1);
                            if (oldVote == 0) down = Math.max(0, down - 1);
                        }

                        if (newVoteValue == 1) up++;
                        if (newVoteValue == 0) down++;

                        comment.getVotes().setUp(up);
                        comment.getVotes().setDown(down);
                        comment.getVotes().setUser(newVoteValue);
                    }

                    if (commentsAdapter != null) {
                        commentsAdapter.notifyDataSetChanged();
                    }
                    if (portraitCommentsAdapter != null) {
                        portraitCommentsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    CustomToast.showWarning(context, error);
                });
            }
        });
    }

    private void showRulesDialog() {
        try {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Правила комментариев")
                .setMessage("1. Соблюдайте уважение к другим пользователям.\n2. Запрещен спам, оффтоп и реклама.\n3. Спойлеры должны быть скрыты или обозначены.")
                .setPositiveButton("Понятно", null)
                .show();
        } catch (Exception e) {
            CustomToast.showInfo(context, "Правила комментариев");
        }
    }

    public void setVisibilityCallback(CommentsVisibilityCallback callback) {
        this.visibilityCallback = callback;
    }
    
    /**
     * Установить callback для данных комментариев
     * @param callback Callback для данных
     */
    public void setDataCallback(CommentsDataCallback callback) {
        this.dataCallback = callback;
    }
    
    /**
     * Обновить видимость кнопки комментариев
     * @param isVisible Видима ли кнопка
     */
    public void updateCommentsButtonVisibility(boolean isVisible) {
        if (commentsButton != null) {
            commentsButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            Log.d("CommentsManager", "Comments button visibility set to: " + (isVisible ? "VISIBLE" : "GONE"));
        } else {
            Log.w("CommentsManager", "Cannot update comments button visibility - button is null!");
        }
    }
    
    /**
     * Скрыть все UI элементы комментариев (для PiP режима)
     */
    public void hideAllCommentsUI() {
        if (isCommentsVisible) {
            hideCommentsPanel();
        }
        updateCommentsButtonVisibility(false);
    }
    
    /**
     * Показать все UI элементы комментариев (для выхода из PiP режима)
     * @param wasVisibleBeforePiP Была ли панель видна до PiP
     */
    public void showAllCommentsUI(boolean wasVisibleBeforePiP) {
        updateCommentsButtonVisibility(true);
        
        if (wasVisibleBeforePiP && !isCommentsVisible) {
            showCommentsPanel();
        }
    }

    /**
     * Очистить ресурсы менеджера комментариев
     */
    /**
     * Безопасно вызывает код в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(runnable);
            } else {
                runnable.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling UI thread", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e(TAG, "Error in fallback callback", ex);
            }
        }
    }

    public void cleanup() {
        if (commentsAdapter != null) {
            commentsAdapter.clearAll();
        }
        
        // Скрываем текст пустого состояния при очистке
        if (emptyCommentsText != null) {
            emptyCommentsText.setVisibility(View.GONE);
        }
        
        commentsCurrentPage = 1;
        commentsHasNextPage = true;
        isLoadingComments = false;
        isCommentsVisible = false;
        
        visibilityCallback = null;
        dataCallback = null;
    }
    
    /**
     * Завершает drag жест с решением открыть или закрыть панель комментариев
     */
    public void completeDrag(boolean shouldOpen) {
        Log.d(TAG, "Complete comments drag: shouldOpen=" + shouldOpen);
        
        if (shouldOpen) {
            // При drag открытии НЕ вызываем openCommentsPanel() - панель уже открывается через DraggableSidePanel
            // Только обновляем флаг и загружаем данные
            if (isCommentsVisible) {
                Log.w(TAG, "Comments panel already visible, skipping");
                return;
            }
            
            isCommentsVisible = true;
            
            // Загрузить первую страницу если комментарии пустые
            if (!isLoadingComments && (commentsAdapter == null || commentsAdapter.getItemCount() == 0)) {
                commentsCurrentPage = 1;
                commentsHasNextPage = true;
                loadCommentsPage(1);
            }
            
            // Уведомить о изменении видимости
            if (visibilityCallback != null) {
                visibilityCallback.onCommentsVisibilityChanged(true);
            }
        } else {
            hideCommentsPanel();
        }
    }
    
    /**
     * Обновляет состояние после drag (вызывается после завершения анимации DraggableSidePanel)
     */
    public void updateDragState(boolean isOpen) {
        Log.d(TAG, "Update drag state: isOpen=" + isOpen);
        
        if (isOpen) {
            isCommentsVisible = true;
            
            // Загрузить первую страницу если комментарии пустые
            if (!isLoadingComments && (commentsAdapter == null || commentsAdapter.getItemCount() == 0)) {
                commentsCurrentPage = 1;
                commentsHasNextPage = true;
                loadCommentsPage(1);
            }
            
            if (visibilityCallback != null) {
                visibilityCallback.onCommentsVisibilityChanged(true);
            }
        } else {
            isCommentsVisible = false;
            if (visibilityCallback != null) {
                visibilityCallback.onCommentsVisibilityChanged(false);
            }
        }
    }
}
