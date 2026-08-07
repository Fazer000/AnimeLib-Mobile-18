package com.example.animelib.util;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * Утилита для обработки HTML комментариев с поддержкой спойлеров, форматирования и цитат
 */
public class CommentHtmlProcessor {
    
    private final Context context;
    private final int primaryTextColor;
    private final int accentTextColor;
    private final int backgroundColor;
    
    public CommentHtmlProcessor(Context context) {
        this.context = context;
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.primaryTextColor, typedValue, true)) {
            this.primaryTextColor = typedValue.resourceId != 0 ? ContextCompat.getColor(context, typedValue.resourceId) : typedValue.data;
        } else {
            this.primaryTextColor = ContextCompat.getColor(context, R.color.primary_text_color);
        }

        if (context.getTheme().resolveAttribute(R.attr.accentTextColor, typedValue, true)) {
            this.accentTextColor = typedValue.resourceId != 0 ? ContextCompat.getColor(context, typedValue.resourceId) : typedValue.data;
        } else {
            this.accentTextColor = ContextCompat.getColor(context, R.color.accent_text_color);
        }

        if (context.getTheme().resolveAttribute(R.attr.primaryColor, typedValue, true)) {
            this.backgroundColor = typedValue.resourceId != 0 ? ContextCompat.getColor(context, typedValue.resourceId) : typedValue.data;
        } else {
            this.backgroundColor = ContextCompat.getColor(context, R.color.bs_bg_color);
        }
    }
    
    /**
     * Обработать HTML комментарий и создать View элементы
     * @param htmlContent HTML содержимое комментария
     * @param container Контейнер для добавления элементов
     */
    public void processCommentHtml(String htmlContent, LinearLayout container) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return;
        }
        
        // Очищаем контейнер
        container.removeAllViews();
        
        try {
            // Агрессивная очистка HTML от лишних пробелов и переносов
            String cleanedHtml = htmlContent
                .replace("\\\"", "\"")
                .replaceAll("\\s+$", "")           // Убираем все пробелы в конце
                .replaceAll("^\\s+", "")           // Убираем все пробелы в начале
                .replaceAll("\\n\\s*\\n+", "\n")   // Убираем множественные переносы строк
                .replaceAll("\\s+\\n", "\n")        // Убираем пробелы перед переносами
                .replaceAll("\\n\\s+", "\n")        // Убираем пробелы после переносов
                .replaceAll("\\s{2,}", " ")         // Заменяем множественные пробелы на один
                .trim();                           // Финальная очистка
            
            // Парсим HTML с помощью Jsoup
            Document doc = Jsoup.parse(cleanedHtml);
            
            // Обрабатываем каждый элемент
            for (Element element : doc.body().children()) {
                processElement(element, container);
            }
            
        } catch (Exception e) {
            Log.e("CommentHtmlProcessor", "Error parsing HTML", e);
            // Если парсинг не удался, показываем как обычный текст
            TextView fallbackView = createTextView();
            Spanned spanned = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY);
            fallbackView.setText(trimTrailingNewlines(spanned));
            container.addView(fallbackView);
        }
    }
    
    /**
     * Обработать HTML элемент
     */
    private void processElement(Element element, LinearLayout container) {
        String tagName = element.tagName().toLowerCase();
        
        switch (tagName) {
            case "p":
                processParagraph(element, container);
                break;
            case "blockquote":
                processBlockquote(element, container);
                break;
            case "pre":
            case "code":
                processCode(element, container);
                break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                processHeading(element, container);
                break;
            case "ul":
            case "ol":
                processList(element, container);
                break;
            case "div":
                if (element.hasClass("spoiler-node")) {
                    processSpoiler(element, container);
                } else {
                    processDiv(element, container);
                }
                break;
            case "span":
                if (element.hasClass("spoiler-node")) {
                    processSpoiler(element, container);
                } else {
                    processSpan(element, container);
                }
                break;
            default:
                TextView textView = createTextView();
                Spanned spanned = processInlineElements(element);
                if (spanned.length() > 0) {
                    textView.setText(trimTrailingNewlines(spanned));
                    container.addView(textView);
                }
                break;
        }
    }
    
    /**
     * Обработать параграф
     */
    private void processParagraph(Element element, LinearLayout container) {
        if (element.text().trim().isEmpty()) {
            return;
        }
        
        Elements spoilerElements = element.select(".spoiler-node");
        if (!spoilerElements.isEmpty()) {
            for (Element spoilerElement : spoilerElements) {
                processSpoiler(spoilerElement, container);
            }
            return;
        }
        
        TextView textView = createTextView();
        Spanned spanned = processInlineElements(element);
        textView.setText(trimTrailingNewlines(spanned));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(2), 0, dpToPx(2));
        textView.setLayoutParams(params);
        
        container.addView(textView);
    }
    
    /**
     * Обработать цитату
     */
    private void processBlockquote(Element element, LinearLayout container) {
        LinearLayout quoteContainer = new LinearLayout(context);
        quoteContainer.setOrientation(LinearLayout.HORIZONTAL);
        quoteContainer.setGravity(Gravity.TOP);
        quoteContainer.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        
        quoteContainer.setBackgroundResource(R.drawable.quote_background);

        ImageView quoteIcon = new ImageView(context);
        quoteIcon.setImageResource(R.drawable.ic_quote_mark);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
        iconParams.setMarginEnd(dpToPx(8));
        iconParams.topMargin = dpToPx(2);
        quoteIcon.setLayoutParams(iconParams);
        quoteContainer.addView(quoteIcon);

        TextView textView = createTextView();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Spanned content = processInlineElements(element);
        builder.append(content);
        
        builder.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(primaryTextColor), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        textView.setText(trimTrailingNewlines(builder));
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        textView.setLayoutParams(textParams);
        quoteContainer.addView(textView);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
        quoteContainer.setLayoutParams(containerParams);
        
        container.addView(quoteContainer);
    }
    
    /**
     * Обработать блок кода
     */
    private void processCode(Element element, LinearLayout container) {
        TextView textView = createTextView();
        textView.setBackgroundResource(R.drawable.code_block_background);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        textView.setTextSize(12);
        textView.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        textView.setText(trimTrailingNewlines(element.text()));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(6), 0, dpToPx(6));
        textView.setLayoutParams(params);
        
        container.addView(textView);
    }

    /**
     * Обработать заголовки h1-h6
     */
    private void processHeading(Element element, LinearLayout container) {
        TextView textView = createTextView();
        String tag = element.tagName().toLowerCase();
        float textSize = 15f;
        if ("h1".equals(tag)) textSize = 17f;
        else if ("h2".equals(tag)) textSize = 16f;
        
        textView.setTextSize(textSize);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        Spanned content = processInlineElements(element);
        textView.setText(trimTrailingNewlines(content));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(6), 0, dpToPx(4));
        textView.setLayoutParams(params);
        
        container.addView(textView);
    }

    /**
     * Обработать списки ul/ol
     */
    private void processList(Element element, LinearLayout container) {
        boolean isOrdered = "ol".equals(element.tagName().toLowerCase());
        Elements items = element.children();
        int index = 1;
        for (Element li : items) {
            TextView textView = createTextView();
            String prefix = isOrdered ? (index++) + ". " : "• ";
            SpannableStringBuilder builder = new SpannableStringBuilder(prefix);
            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple_primary)), 0, prefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(processInlineElements(li));
            
            textView.setText(trimTrailingNewlines(builder));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dpToPx(12), dpToPx(2), 0, dpToPx(2));
            textView.setLayoutParams(params);
            container.addView(textView);
        }
    }
    
    /**
     * Обработать спойлер
     */
    private void processSpoiler(Element element, LinearLayout container) {
        String spoilerText = element.attr("data-spoiler-text");
        String spoilerType = element.attr("data-spoiler-type");
        
        Elements spoilerTextElements = element.select(".spoiler-node__text");
        SpannableStringBuilder contentBuilder = new SpannableStringBuilder();
        if (!spoilerTextElements.isEmpty()) {
            for (Element textElement : spoilerTextElements) {
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(processInlineElements(textElement));
            }
        } else {
            contentBuilder.append(processInlineElements(element));
        }
        
        com.example.animelib.ui.SpoilerView spoilerView = 
            new com.example.animelib.ui.SpoilerView(context);
        
        String title = !spoilerText.isEmpty() ? spoilerText : "Спойлер";

        spoilerView.setSpoilerData(title, trimTrailingNewlines(contentBuilder));
        container.addView(spoilerView);
    }
    
    /**
     * Обработать div
     */
    private void processDiv(Element element, LinearLayout container) {
        TextView textView = createTextView();
        Spanned spanned = processInlineElements(element);
        if (spanned.length() > 0) {
            textView.setText(spanned);
            container.addView(textView);
        }
    }
    
    /**
     * Обработать span
     */
    private void processSpan(Element element, LinearLayout container) {
        TextView textView = createTextView();
        Spanned spanned = processInlineElements(element);
        if (spanned.length() > 0) {
            textView.setText(spanned);
            container.addView(textView);
        }
    }
    
    /**
     * Обработать inline элементы (strong, em, u, strike, code, a и т.д.)
     */
    private Spanned processInlineElements(Element element) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                String text = textNode.text();
                text = text.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
                builder.append(text);
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                String tagName = childElement.tagName().toLowerCase();
                
                int start = builder.length();
                
                switch (tagName) {
                    case "strong":
                    case "b":
                        String boldText = childElement.text().trim();
                        builder.append(boldText);
                        builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "em":
                    case "i":
                        String italicText = childElement.text().trim();
                        builder.append(italicText);
                        builder.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "u":
                        String underlineText = childElement.text().trim();
                        builder.append(underlineText);
                        builder.setSpan(new UnderlineSpan(), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "strike":
                    case "s":
                        String strikeText = childElement.text().trim();
                        builder.append(strikeText);
                        builder.setSpan(new StrikethroughSpan(), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "code":
                        String codeText = childElement.text().trim();
                        builder.append(codeText);
                        builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple_primary)),
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "a":
                        String linkText = childElement.text().trim();
                        if (linkText.isEmpty()) linkText = childElement.attr("href");
                        builder.append(linkText);
                        builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.purple_primary)),
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.setSpan(new UnderlineSpan(),
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "br":
                        builder.append("\n");
                        break;
                    default:
                        Spanned nested = processInlineElements(childElement);
                        builder.append(nested);
                        break;
                }
            }
        }
        
        String result = builder.toString();
        result = result.replaceAll("\\s+$", "").replaceAll("^\\s+", "").trim();
        
        SpannableStringBuilder cleanedBuilder = new SpannableStringBuilder(result);
        
        Object[] spans = builder.getSpans(0, builder.length(), Object.class);
        for (Object span : spans) {
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            int flags = builder.getSpanFlags(span);
            
            if (start < result.length() && end <= result.length()) {
                cleanedBuilder.setSpan(span, start, end, flags);
            }
        }
        
        return cleanedBuilder;
    }
    
    /**
     * Создать TextView с базовыми настройками
     */
    private TextView createTextView() {
        TextView textView = new TextView(context);
        textView.setTextColor(primaryTextColor);
        textView.setTextSize(13);
        textView.setLineSpacing(dpToPx(2), 1.0f);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 0);
        textView.setLayoutParams(params);
        
        return textView;
    }

    private static CharSequence trimTrailingNewlines(CharSequence text) {
        if (text == null) return "";
        int len = text.length();
        while (len > 0 && (text.charAt(len - 1) == '\n' || text.charAt(len - 1) == '\r' || Character.isWhitespace(text.charAt(len - 1)))) {
            len--;
        }
        return text.subSequence(0, len);
    }
    
    /**
     * Преобразовать dp в px
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
