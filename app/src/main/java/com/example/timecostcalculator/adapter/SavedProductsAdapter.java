package com.example.timecostcalculator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timecostcalculator.R;
import com.example.timecostcalculator.model.SavedProduct;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavedProductsAdapter extends RecyclerView.Adapter<SavedProductsAdapter.ViewHolder> {

    private List<SavedProduct> products;

    public SavedProductsAdapter(List<SavedProduct> products) {
        this.products = products;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvDate;

        public ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvProductName);
            tvDetails = view.findViewById(R.id.tvDetails);
            tvDate = view.findViewById(R.id.tvDate);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedProduct product = products.get(position);

        holder.tvName.setText(product.getName());
        holder.tvDetails.setText(
                "⏱️ " + product.getTimeSavedMinutes() + " min | 💸 " +
                        String.format(Locale.getDefault(), "%.2f €", product.getMoneySaved())
        );

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvDate.setText("Guardado el " + sdf.format(new Date(product.getDate())));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }
}
