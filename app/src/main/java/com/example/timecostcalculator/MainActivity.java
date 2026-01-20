package com.example.timecostcalculator;

import androidx.fragment.app.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.ads.MobileAds;

import utils.SharedPrefManager;

public class MainActivity extends BaseActivity {

    public static BillingManager billingManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().clear().apply();
/*
        SharedPreferences prefs = getSharedPreferences("TimeCostPrefs", MODE_PRIVATE);
        //SharedPreferences.Editor editor = prefs.edit();


        // Limpiar todo el histórico mensual
        editor.remove("monthly_history"); // o editor.putString("monthly_history", "{}");
        editor.remove("history");
        editor.remove("monthly_hours");
        editor.remove("monthly_savings");

        // Obtenemos el JSON actual del histórico
        String json = prefs.getString("monthly_history", "{}");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> history = gson.fromJson(json, type);

        if (history == null) {
            history = new HashMap<>();
        }



        Map<String, Object> otherData3 = new HashMap<>();
        otherData3.put("savedMoney", 120.0);
        otherData3.put("savedTime", 300L); // en minutos
        otherData3.put("monthlySavings", 50.0);
        otherData3.put("currentSpending", 350.0); // 💰 dinero gastado
        otherData3.put("maxSpending", 150.0);

        history.put("2025-03", otherData3);

        Map<String, Object> otherData4 = new HashMap<>();
        otherData4.put("savedMoney", 120.0);
        otherData4.put("savedTime", 300L); // en minutos
        otherData4.put("monthlySavings", 50.0);
        otherData4.put("currentSpending", 500.0); // 💰 dinero gastado
        otherData4.put("maxSpending", 150.0);

        history.put("2025-04", otherData4);

        Map<String, Object> otherDat5 = new HashMap<>();
        otherDat5.put("savedMoney", 120.0);
        otherDat5.put("savedTime", 300L); // en minutos
        otherDat5.put("monthlySavings", 50.0);
        otherDat5.put("currentSpending", 50.0); // 💰 dinero gastado
        otherDat5.put("maxSpending", 150.0);

        history.put("2025-05", otherDat5);

        Map<String, Object> otherData1 = new HashMap<>();
        otherData1.put("savedMoney", 120.0);
        otherData1.put("savedTime", 300L); // en minutos
        otherData1.put("monthlySavings", 50.0);
        otherData1.put("currentSpending", 50.0); // 💰 dinero gastado
        otherData1.put("maxSpending", 150.0);

        history.put("2025-06", otherData1);

        Map<String, Object> otherData2 = new HashMap<>();
        otherData2.put("savedMoney", 120.0);
        otherData2.put("savedTime", 300L); // en minutos
        otherData2.put("monthlySavings", 50.0);
        otherData2.put("currentSpending", 2.0); // 💰 dinero gastado
        otherData2.put("maxSpending", 150.0);

        history.put("2025-07", otherData2);

        Map<String, Object> otherData = new HashMap<>();
        otherData.put("savedMoney", 120.0);
        otherData.put("savedTime", 300L); // en minutos
        otherData.put("monthlySavings", 50.0);
        otherData.put("currentSpending", 100.0); // 💰 dinero gastado
        otherData.put("maxSpending", 150.0);

        history.put("2025-08", otherData);

        // Añadimos un mes de prueba, por ejemplo "2025-01"
        Map<String, Object> monthData = new HashMap<>();
        monthData.put("savedMoney", 120.0);
        monthData.put("savedTime", 300L); // en minutos
        monthData.put("monthlySavings", 50.0);
        monthData.put("currentSpending", 80.0); // 💰 dinero gastado
        monthData.put("maxSpending", 150.0);

        history.put("2025-09", monthData);

        // Añadimos más meses si quieres
        Map<String, Object> oneData = new HashMap<>();
        oneData.put("savedMoney", 200.0);
        oneData.put("savedTime", 400L);
        oneData.put("monthlySavings", 100.0);
        oneData.put("currentSpending", 120.0);
        oneData.put("maxSpending", 150.0);
        history.put("2025-10", oneData);
        editor.apply();

        Map<String, Object> twoData = new HashMap<>();
        twoData.put("savedMoney", 200.0);
        twoData.put("savedTime", 400L);
        twoData.put("monthlySavings", 100.0);
        twoData.put("currentSpending", 120.0);
        twoData.put("maxSpending", 150.0);
        history.put("2025-11", twoData);
        editor.apply();

        Map<String, Object> threeData = new HashMap<>();
        threeData.put("savedMoney", 200.0);
        threeData.put("savedTime", 400L);
        threeData.put("monthlySavings", 100.0);
        threeData.put("currentSpending", 120.0);
        threeData.put("maxSpending", 150.0);
        history.put("2025-12", threeData);

        editor.putString("monthly_history", gson.toJson(history));
        editor.putString("history", gson.toJson(history));
        editor.apply();


        prefs.edit()
                .putString("last_month", "2025-12") // mes anterior
                .apply();
        */
        // Ahora llama al chequeo
        MonthlyManager.checkAndRotateMonth(this);

        // Botones inferiores
        Button btnHome = findViewById(R.id.btnHome);
        Button btnCalculate = findViewById(R.id.btnCalculate);
        Button btnProfile = findViewById(R.id.btnProfile);
        Button btnHistory = findViewById(R.id.btnHistory);

        MobileAds.initialize(this, initializationStatus -> {});

        if (!SharedPrefManager.isPremium(this)) {
            AdManager.loadRewarded(this);
        }

        // Cargar Home por defecto
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Click listeners
        btnHome.setOnClickListener(v -> loadFragment(new HomeFragment()));
        btnCalculate.setOnClickListener(v -> loadFragment(new CalculateFragment()));
        btnProfile.setOnClickListener(v -> loadFragment(new ProfileFragment()));
        btnHistory.setOnClickListener(v -> loadFragment(new HistoryFragment()));

        billingManager = new BillingManager(this);

    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}