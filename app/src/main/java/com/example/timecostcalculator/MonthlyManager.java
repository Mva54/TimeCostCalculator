package com.example.timecostcalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import utils.MonthStats;
import utils.SharedPrefManager;

public class MonthlyManager {

    private static final String PREFS_NAME = "TimeCostPrefs";
    private static final String KEY_LAST_MONTH = "last_month";
    private static final Gson gson = new Gson();
    private static final String KEY_HISTORY = "history";
    private static final String KEY_HISTORY_SIZE = "history_size";

    public static void setKeyHistorySize(Context ctx, int size)
    {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_HISTORY_SIZE, size)
                .apply();
    }

    public static int getKeyHistorySize(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_HISTORY_SIZE, 0);
    }

    /**
     * Comprueba si ha cambiado el mes y, si es así, guarda el historial y reinicia los contadores.
     */
    public static void checkAndRotateMonth(Context ctx) {
        String lastMonth = getLastMonthKey(ctx);
        String currentMonth = getCurrentMonthKey();

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean popupShown = prefs.getBoolean("popup_shown_" + currentMonth, false);

        if (!currentMonth.equals(lastMonth)) {
            // Guardar estadísticas del mes anterior
            double prevMoney = SharedPrefManager.getSavedMoney(ctx);
            long prevTime = SharedPrefManager.getSavedTime(ctx);
            double prevMonthlySavings = SharedPrefManager.getMonthlySavings(ctx);
            double prevCurrentSpending = SharedPrefManager.getCurrentSpending(ctx);
            double prevMaxSpending = SharedPrefManager.getMaxSpending(ctx);
            var prevProducts = SharedPrefManager.getSavedProducts(ctx);

            saveMonthStats(ctx, lastMonth, prevMoney, prevTime, prevMonthlySavings, prevProducts,
                    prevCurrentSpending, prevMaxSpending);

            // Reiniciar contadores
            SharedPrefManager.addSavedMoney(ctx, -prevMoney);
            SharedPrefManager.addSavedTime(ctx, -prevTime);
            SharedPrefManager.saveMonthlySavings(ctx, 0);
            SharedPrefManager.setCurrentSpending(ctx, 0);

            // Actualizar último mes
            setLastMonthKey(ctx, currentMonth);

            // Mostrar popup solo si no se ha mostrado este mes
            popupShown = false;
            if (!popupShown) {
                showMonthChangePopup(ctx, lastMonth, prevMoney, prevTime, prevMonthlySavings,
                        prevCurrentSpending, prevMaxSpending);

                // Marcar que el popup ya se mostró este mes
                prefs.edit().putBoolean("popup_shown_" + currentMonth, true).apply();
            }
        }
    }


    /** Devuelve la key del mes actual en formato yyyy-MM */
    public static String getCurrentMonthKey() {
        return new SimpleDateFormat("yyyy-MM").format(new Date());
    }

    /** Obtiene el último mes guardado en SharedPreferences */
    public static String getLastMonthKey(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_MONTH, "");
    }

    /** Guarda el último mes */
    private static void setLastMonthKey(Context ctx, String monthKey) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST_MONTH, monthKey)
                .apply();
    }

    /**
     * Guarda las estadísticas de un mes en el historial completo.
     */
    private static void saveMonthStats(Context ctx,
                                       String monthKey,
                                       double savedMoney,
                                       long savedTime,
                                       double monthlySavings,
                                       List<String> products,
                                       double currentSpending,
                                       double maxSpending) {
        if (monthKey.isEmpty()) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, "{}");

        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> history = gson.fromJson(json, type);
        if (history == null) history = new HashMap<>();

        setKeyHistorySize(ctx, history.size());

        Map<String, Object> monthData = new HashMap<>();
        monthData.put("savedMoney", savedMoney);
        monthData.put("savedTime", savedTime);
        monthData.put("monthlySavings", monthlySavings);
        monthData.put("currentSpending", currentSpending);
        monthData.put("maxSpending", maxSpending);

        history.put(monthKey, monthData);

        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }


    /**
     * Muestra un popup con estadísticas del mes anterior.
     */
    private static void showMonthChangePopup(
            Context ctx,
            String month,
            double money,
            long timeMinutes,
            double monthlySavings,
            double prevCurrentSpending,
            double prevMaxSpending
    ) {
        if (month.isEmpty()) return;

        long hours = timeMinutes / 60;
        long minutes = timeMinutes % 60;

        // === CARD ROOT ===
        CardView card = new CardView(ctx);
        card.setRadius(28f);
        card.setCardElevation(16f);
        card.setUseCompatPadding(true);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 40, 48, 32);
        card.addView(root);

        // === TITLE ===
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(R.string.new_month);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);

        TextView tvMonth = new TextView(ctx);
        tvMonth.setText(
                ctx.getString(R.string.summary_of) + " " + month
        );
        tvMonth.setTextSize(14);
        tvMonth.setTextColor(Color.GRAY);
        tvMonth.setGravity(Gravity.CENTER);

        root.addView(tvTitle);
        root.addView(tvMonth);

        // === DIVIDER ===
        View divider = new View(ctx);
        LinearLayout.LayoutParams divParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 32, 0, 24);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(0x22000000);
        root.addView(divider);

        // === STATS ===
        addStatRow(ctx, root, "\uD83D\uDCB0 " + ctx.getString(R.string.saved_money_card2),
                String.format(Locale.getDefault(), "%.2f €", money));

        addStatRow(ctx, root, "⌛ " + ctx.getString(R.string.saved_time_card2),
                hours + "h " + minutes + "m");

        addStatRow(ctx, root, "\uD83E\uDE99 " + ctx.getString(R.string.monthly_saves2),
                String.format(Locale.getDefault(), "%.2f €", monthlySavings));

        addStatRow(ctx, root, "\uD83D\uDCB8 " + ctx.getString(R.string.month_expenses2),
                String.format(Locale.getDefault(), "%.2f €", prevCurrentSpending));

        addStatRow(ctx, root, "\uD83C\uDFAF " + ctx.getString(R.string.remaining_limit),
                String.format(Locale.getDefault(), "%.2f €",
                        prevMaxSpending - prevCurrentSpending));

        // === DIALOG ===
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(card)
                .setCancelable(false)
                .create();

        dialog.show();

        // === CUSTOM BUTTON ===
        addCustomButton(ctx, root, dialog);
    }

    private static void addCustomButton(
            Context ctx,
            LinearLayout root,
            AlertDialog dialog
    ) {
        TextView btn = new TextView(ctx);
        btn.setText(R.string.continue_card);
        btn.setTextSize(16);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, 28, 0, 28);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 32, 0, 0);

        btn.setLayoutParams(params);
        btn.setBackgroundResource(R.drawable.bg_primary_button);

        btn.setOnClickListener(v -> dialog.dismiss());

        root.addView(btn);
    }



    private static void addStatRow(
            Context ctx,
            LinearLayout parent,
            String label,
            String value
    ) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        tvLabel.setPadding(24, 0, 0, 0);
        tvLabel.setLayoutParams(
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tvValue = new TextView(ctx);
        tvValue.setText(value);
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);

        row.addView(tvLabel);
        row.addView(tvValue);

        parent.addView(row);
    }


    public static List<MonthStats> getHistory(Context ctx, int historyStartIndex, int maxHistoryMonths) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, "{}");

        List<MonthStats> result = new ArrayList<>();

        try {
            JSONObject history = new JSONObject(json);
            Iterator<String> keys = history.keys();

            while (keys.hasNext()) {
                String month = keys.next();
                JSONObject stats = history.optJSONObject(month);
                if (stats == null) continue;

                double savedMoney = stats.optDouble("savedMoney", 0);
                long savedTime = stats.optLong("savedTime", 0);
                double maxSpending = stats.optDouble("maxSpending", 0);
                double spent = stats.optDouble("currentSpending", 0);

                result.add(new MonthStats(
                        month,
                        savedMoney,
                        savedTime,
                        maxSpending,
                        spent
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setKeyHistorySize(ctx, result.size());

        // Orden cronológico
        Collections.sort(result, (a, b) -> b.month.compareTo(a.month));

        List<MonthStats> newResultList = new ArrayList<>();

        int end = Math.min(historyStartIndex + maxHistoryMonths, result.size());

        for (int i = historyStartIndex; i < end; i++) {
            newResultList.add(result.get(i));
        }


        return newResultList;
    }



}
