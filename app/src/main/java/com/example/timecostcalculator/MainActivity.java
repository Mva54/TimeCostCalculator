package com.example.timecostcalculator;

import static androidx.core.content.ContentProviderCompat.requireContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timecostcalculator.adapter.SavedProductsAdapter;
import com.example.timecostcalculator.model.SavedProduct;
import com.example.timecostcalculator.ui.SavedProductsActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {

    private EditText etSalary;
    private Switch switchSalaryType;
    private Button btnCalculate;
    private TextView tvSavedMoney, tvSavedTime;

    private double savedMoney = 0;
    private double savedHours = 0;

    private List<String> savedProducts = new ArrayList<>();

    private double salary = 0;
    private boolean isMonthly = true;

    public static BillingManager billingManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        ProfileFragment.updateLocale(newBase);
        super.attachBaseContext(newBase);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().clear().apply();

        SharedPreferences prefs = getSharedPreferences("TimeCostPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Limpiar todo el histórico mensual
        editor.remove("monthly_history"); // o editor.putString("monthly_history", "{}");
        editor.remove("history");

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
/*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etSalary = findViewById(R.id.etSalary);
        switchSalaryType = findViewById(R.id.switchSalaryType);
        btnCalculate = findViewById(R.id.btnCalculate);
        tvSavedMoney = findViewById(R.id.tvSavedMoney);
        tvSavedTime = findViewById(R.id.tvSavedTime);

        switchSalaryType.setChecked(!isMonthly);

        switchSalaryType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isMonthly = !isChecked;
            saveData();
        });

        loadData(); // <-- cargar datos guardados

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProductPopup();
            }
        });

        Button btnOpenSavedProducts = findViewById(R.id.btnOpenSavedProducts);

        btnOpenSavedProducts.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedProductsActivity.class);
            startActivity(intent);
        });
    }

    private void showProductPopup() {
        String salaryInput = etSalary.getText().toString();
        if (salaryInput.isEmpty()) {
            Toast.makeText(this, "Introduce tu sueldo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        salary = Double.parseDouble(salaryInput);
        saveData();

        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_product, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);
        final AlertDialog dialog = builder.create();
        dialog.show();

        final EditText etPrice = popupView.findViewById(R.id.etProductPrice);
        final EditText etName = popupView.findViewById(R.id.etProductName);
        final TextView tvTimeNeeded = popupView.findViewById(R.id.tvTimeNeeded);
        Button btnBuy = popupView.findViewById(R.id.btnBuy);
        Button btnNotBuy = popupView.findViewById(R.id.btnNotBuy);
        Button btnSave = popupView.findViewById(R.id.btnSave);
        Button btnDiscard = popupView.findViewById(R.id.btnDiscard);
        Button btnCalculateTime = popupView.findViewById(R.id.btnCalculateTime);

        btnCalculateTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateTime(etPrice, tvTimeNeeded);
            }
        });

        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Producto comprado (simulación)", Toast.LENGTH_SHORT).show();
            }
        });

        btnNotBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double price = parsePrice(etPrice.getText().toString());
                if (price > 0) {
                    savedMoney += price;
                    savedHours += calculateHours(price);
                    updateCounters();
                    saveData(); // <-- guardar cambios
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Introduce un precio válido", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString();
                if (name.isEmpty()) {
                    etName.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Introduce el nombre del producto para guardar", Toast.LENGTH_SHORT).show();
                    return;
                }
                savedProducts.add(name);
                saveData(); // <-- guardar cambios
                Toast.makeText(MainActivity.this, "Producto guardado", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnDiscard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void calculateTime(EditText etPrice, TextView tvTimeNeeded) {
        double price = parsePrice(etPrice.getText().toString());
        if (price <= 0) return;

        double hours = calculateHours(price);
        int days = (int) (hours / 8);
        int remainingHours = (int) (hours % 8);

        tvTimeNeeded.setText("Tiempo necesario: " + days + " días y " + remainingHours + " horas");
    }

    private double calculateHours(double price) {
        if (isMonthly) {
            double monthlyHours = 160; // 40h/semana
            return price / (salary / monthlyHours);
        } else {
            return price / salary;
        }
    }

    private double parsePrice(String input) {
        if (input.isEmpty()) return 0;
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateCounters() {
        tvSavedMoney.setText(String.format("%.2f €", savedMoney));

        if (savedHours < 8) {
            tvSavedTime.setText((int) savedHours + " horas");
        } else {
            int days = (int) (savedHours / 8);
            int hours = (int) (savedHours % 8);
            tvSavedTime.setText(days + " días y " + hours + " h");
        }
    }

    // -------------------
    // SharedPreferences
    // -------------------

    private void saveData() {
        getSharedPreferences("WorkCalcPrefs", MODE_PRIVATE)
                .edit()
                .putFloat("savedMoney", (float) savedMoney)
                .putFloat("savedHours", (float) savedHours)
                .putString("savedProducts", listToString(savedProducts))
                .putFloat("salary", (float) salary)             // <-- guardar sueldo
                .putBoolean("isMonthly", isMonthly)             // <-- guardar tipo de sueldo
                .apply();
    }

    private void loadData() {
        android.content.SharedPreferences prefs = getSharedPreferences("WorkCalcPrefs", MODE_PRIVATE);
        savedMoney = prefs.getFloat("savedMoney", 0);
        savedHours = prefs.getFloat("savedHours", 0);
        savedProducts = stringToList(prefs.getString("savedProducts", ""));

        // Cargar sueldo y tipo
        salary = prefs.getFloat("salary", 0);
        isMonthly = prefs.getBoolean("isMonthly", true);

        // Mostrar sueldo en EditText
        if (salary > 0) {
            etSalary.setText(String.format("%.2f", salary));
        }

        // Seleccionar Switch correcto
        if (isMonthly) {
            switchSalaryType.setChecked(false);
        } else {
            switchSalaryType.setChecked(true);
        }

        updateCounters();
    }

    private String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private List<String> stringToList(String str) {
        List<String> list = new ArrayList<>();
        if (str != null && !str.isEmpty()) {
            String[] items = str.split(",");
            for (String item : items) {
                list.add(item);
            }
        }
        return list;
    }
}
*/