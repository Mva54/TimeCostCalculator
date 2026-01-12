package com.example.timecostcalculator.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timecostcalculator.R;
import com.example.timecostcalculator.adapter.SavedProductsAdapter;
import com.example.timecostcalculator.model.SavedProduct;

import java.util.ArrayList;
import java.util.List;

public class SavedProductsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_products);

        RecyclerView recyclerView = findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<SavedProduct> products = new ArrayList<>();

        // Datos de prueba
        products.add(new SavedProduct("Café", 4.5, 25, 3.40, System.currentTimeMillis()));
        products.add(new SavedProduct("Comida rápida", 8.0, 40, 6.10, System.currentTimeMillis()));

        recyclerView.setAdapter(new SavedProductsAdapter(products));
    }
}
