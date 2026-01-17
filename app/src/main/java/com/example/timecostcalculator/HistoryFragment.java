package com.example.timecostcalculator;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import utils.MonthStats;
import utils.SharedPrefManager;

public class HistoryFragment extends Fragment {

    private LinearLayout historyContainer;

    private BarChart barChart;

    private int historyStartIndex = 0; // índice del primer mes que mostramos
    private int historyWindowSize = 5;
    private static final String PREFS_NAME = "TimeCostPrefs";
    private static final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.history_fragment, container, false);

        historyContainer = view.findViewById(R.id.historyCardsContainer);
        View premiumCard = view.findViewById(R.id.cardPremiumPromo);
        View btnGoPremium = view.findViewById(R.id.btnGoPremium);
        View tvPremiumOnlyOne = view.findViewById(R.id.tvPremium);

        ImageButton btnPrevMonths = view.findViewById(R.id.btnPrevMonths);
        ImageButton btnNextMonths = view.findViewById(R.id.btnNextMonths);

        btnPrevMonths.setOnClickListener(v -> {
            int historySize = MonthlyManager.getKeyHistorySize(requireContext());

            int maxStartIndex = historySize - historyWindowSize;
            if (maxStartIndex < 0) maxStartIndex = 0;

            if (historyStartIndex < maxStartIndex) {
                historyStartIndex += historyWindowSize;

                if (historyStartIndex > maxStartIndex) {
                    historyStartIndex = maxStartIndex;
                }

                loadHistoryChartWithProjection();
            }
        });

        btnNextMonths.setOnClickListener(v -> {
            if (historyStartIndex > 0) {
                historyStartIndex -= historyWindowSize;

                if (historyStartIndex < 0) {
                    historyStartIndex = 0;
                }

                loadHistoryChartWithProjection();
            }
        });


        boolean isPremium = SharedPrefManager.isPremium(requireContext());

        if (!isPremium) {
            premiumCard.setVisibility(View.VISIBLE);
            tvPremiumOnlyOne.setVisibility(View.VISIBLE);
            btnNextMonths.setVisibility(View.GONE);
            btnPrevMonths.setVisibility(View.GONE);

            btnGoPremium.setOnClickListener(v -> {
                // Aquí más adelante:
                // abrir pantalla de compra / premium
                // o mostrar diálogo
            });
        } else {
            if (MonthlyManager.getKeyHistorySize(requireContext()) < 5) {
                btnNextMonths.setVisibility(View.GONE);
                btnPrevMonths.setVisibility(View.GONE);
            }
        }


        barChart = view.findViewById(R.id.barChart);
        setupBarChart();
        loadHistoryChartWithProjection();
        loadMonthlyHistory();

        return view;
    }

    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setPinchZoom(false);

        barChart.setScaleEnabled(false);
        barChart.setExtraBottomOffset(10f);

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setDrawGridLines(false);

        barChart.getAxisRight().setEnabled(false);
    }

    private void loadHistoryChartWithProjection() {
        List<MonthStats> history = MonthlyManager.getHistory(requireContext(), historyStartIndex, SharedPrefManager.isPremium(getContext()) ? historyWindowSize : 1);
        if (history.isEmpty()) return;
        Collections.reverse(history);

        List<BarEntry> spentEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            spentEntries.add(new BarEntry(i, (float) history.get(i).totalSpent));
            labels.add(history.get(i).month);
        }

        // --- AGREGAMOS EL MES ACTUAL COMO PROYECCIÓN ---
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        double currentSpending = SharedPrefManager.getCurrentSpending(requireContext());
        // double projectedSpending = (currentSpending / today) * daysInMonth;

        // Barra del mes actual
        spentEntries.add(new BarEntry(history.size(), (float) currentSpending));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        labels.add(sdf.format(new Date()));

        // Configuramos DataSet y Chart
        BarDataSet spentSet = new BarDataSet(spentEntries, getString(R.string.history_tag));
        spentSet.setColor(Color.parseColor("#f55347"));

        BarData data = new BarData(spentSet);
        data.setBarWidth(0.5f);
        barChart.setData(data);

        // Configuración eje X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setCenterAxisLabels(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();
    }


    private void loadMonthlyHistory() {
        historyContainer.removeAllViews();

        Context ctx = requireContext();
        String json = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("monthly_history", "{}");

        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> history = gson.fromJson(json, type);
        if (history == null) return;

        boolean isPremium = SharedPrefManager.isPremium(requireContext());
        int maxMonths = isPremium ? Integer.MAX_VALUE : 1;

        List<String> months = new ArrayList<>(history.keySet());
        Collections.sort(months, Collections.reverseOrder()); // último mes primero

        for (int i = 0; i < Math.min(months.size(), maxMonths); i++) {
            String monthKey = months.get(i);
            Map<String, Object> monthData = history.get(monthKey);
            if (monthData == null) continue;

            double savedMoney = ((Number) monthData.getOrDefault("savedMoney", 0)).doubleValue();
            long savedTime = ((Number) monthData.getOrDefault("savedTime", 0)).longValue();
            double monthlySavings = ((Number) monthData.getOrDefault("monthlySavings", 0)).doubleValue();
            double currentSpending = ((Number) monthData.getOrDefault("currentSpending", 0)).doubleValue();
            double maxSpending = ((Number) monthData.getOrDefault("maxSpending", 0)).doubleValue();

            addMonthCard(
                    monthKey,
                    savedMoney,
                    savedTime,
                    monthlySavings,
                    currentSpending,
                    maxSpending
            );
        }
    }

    private void addMonthCard(String monthKey,
                              double savedMoney,
                              long savedTime,
                              double monthlySavings,
                              double currentSpending,
                              double maxSpending) {
        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_month_card, historyContainer, false);

        TextView tvMonth = card.findViewById(R.id.tvMonth);
        TextView tvMoney = card.findViewById(R.id.tvMoney);
        TextView tvTime = card.findViewById(R.id.tvTime);
        TextView tvSavings = card.findViewById(R.id.tvSavings);
        TextView tvSpending = card.findViewById(R.id.tvSpending);

        tvMonth.setText(monthKey);
        tvMoney.setText(getString(R.string.saved_money_card) + " " + String.format("%.2f %s", savedMoney,
                SharedPrefManager.getCurrencySymbol(requireContext())));
        long hours = savedTime / 60;
        long minutes = savedTime % 60;
        tvTime.setText(getString(R.string.saved_time_card) + " "  + hours + "h " + minutes + "m");
        tvSavings.setText(getString(R.string.monthly_saves) + " " + String.format("%.2f %s", monthlySavings,
                SharedPrefManager.getCurrencySymbol(requireContext())));
        tvSpending.setText(getString(R.string.month_expenses) + " "  + String.format("%.2f / %.2f %s", currentSpending, maxSpending,
                SharedPrefManager.getCurrencySymbol(requireContext())));

        historyContainer.addView(card);
    }

}
