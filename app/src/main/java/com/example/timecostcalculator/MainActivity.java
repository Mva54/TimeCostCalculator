package com.example.timecostcalculator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

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
