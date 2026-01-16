package com.example.timecostcalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

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
    private static void showMonthChangePopup(Context ctx,
                                             String month,
                                             double money,
                                             long timeMinutes,
                                             double monthlySavings,
                                             double prevCurrentSpending,
                                             double prevMaxSpending) {
        if (month.isEmpty()) return;

        long hours = timeMinutes / 60;
        long minutes = timeMinutes % 60;

        Calendar cal = Calendar.getInstance();
        String monthName = new SimpleDateFormat("MMMM yyyy").format(cal.getTime());

        String message = "Se ha iniciado un nuevo mes: " + monthName +
                "\n\nResumen del mes anterior (" + month + "):" +
                "\nDinero ahorrado: " + String.format("%.2f", money) +
                "\nTiempo ahorrado: " + hours + "h " + minutes + "m" +
                "\nAhorros mensuales: " + String.format("%.2f", monthlySavings) +
                "\nLímite restante: " + (prevMaxSpending - prevCurrentSpending) +
                "\nDinero total gastado: " + prevCurrentSpending;

        new Handler(Looper.getMainLooper()).post(() ->
                new AlertDialog.Builder(ctx)
                        .setTitle("Nuevo mes iniciado")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
        );
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
