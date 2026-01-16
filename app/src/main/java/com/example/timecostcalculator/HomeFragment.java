package com.example.timecostcalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.util.List;

import utils.SharedPrefManager;

public class HomeFragment extends Fragment {

    private TextView tvSavedMoney, tvSavedTime, tvRemainingMoney;
    private LinearLayout productsContainer;
    private SharedPreferences prefs;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView imagePreview;

    private CardView cardRemainingMoney;


    private static final String PREFS_NAME = "TimeCostPrefs";
    private static final String KEY_SAVED_MONEY = "saved_money";
    private static final String KEY_SAVED_TIME = "saved_time";

    public HomeFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        //requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().clear().apply();

        // Inflar el layout
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        // Referencias a los TextViews
        tvSavedMoney = view.findViewById(R.id.tvSavedMoney);
        tvSavedTime = view.findViewById(R.id.tvSavedTime);
        productsContainer = view.findViewById(R.id.productsContainer);
        View premiumCard = view.findViewById(R.id.cardPremiumPromo);
        View btnGoPremium = view.findViewById(R.id.btnGoPremium);
        View tvPremiumOnlyOne = view.findViewById(R.id.tvPremium);

        boolean isPremium = SharedPrefManager.isPremium(requireContext());

        if (!isPremium) {
            premiumCard.setVisibility(View.VISIBLE);
            tvPremiumOnlyOne.setVisibility(View.VISIBLE);

            btnGoPremium.setOnClickListener(v -> {
                MainActivity.billingManager.launchPremiumPurchase();
            });
        }

        tvRemainingMoney = view.findViewById(R.id.tvRemainingMoney);
        cardRemainingMoney = view.findViewById(R.id.cardRemainingMoney);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && imagePreview != null) {
                            imagePreview.setImageURI(uri);
                            imagePreview.setTag(uri.toString());
                        }
                    }
                }
        );


        // Cargar SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        loadData();
        updateRemainingMoneyCard();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedProducts();
    }


    // Método para cargar datos
    private void loadData() {
        double money = SharedPrefManager.getSavedMoney(getContext());//Double.longBitsToDouble(prefs.getLong(KEY_SAVED_MONEY, Double.doubleToLongBits(0)));
        long timeMinutes = SharedPrefManager.getSavedTime(getContext()); //prefs.getLong(KEY_SAVED_TIME, 0);
        String symbol = SharedPrefManager.getCurrencySymbol(getContext());

        tvSavedMoney.setText(String.format("%.2f %s", money, symbol));

        // Convertimos minutos a horas y minutos
        long hours = timeMinutes / 60;
        long minutes = timeMinutes % 60;
        tvSavedTime.setText(hours + "h " + minutes + "m");
    }

    // Método para actualizar datos y guardarlos
    public void updateData(double moneyAdded, long timeAddedMinutes) {
        double currentMoney = Double.longBitsToDouble(prefs.getLong(KEY_SAVED_MONEY, Double.doubleToLongBits(0)));
        long currentTime = prefs.getLong(KEY_SAVED_TIME, 0);

        currentMoney += moneyAdded;
        currentTime += timeAddedMinutes;

        // Guardar en SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_SAVED_MONEY, Double.doubleToLongBits(currentMoney));
        editor.putLong(KEY_SAVED_TIME, currentTime);
        editor.apply();

        // Actualizar UI
        loadData();
    }

    private void loadSavedProducts() {
        productsContainer.removeAllViews();

        List<String> products = SharedPrefManager.getVisibleSavedProducts(requireContext());

        for (String raw : products) {
            String[] parts = raw.split("\\|");
            if (parts.length < 3) continue;

            String name = parts[0];
            double price = Double.parseDouble(parts[1]);
            long minutes = Long.parseLong(parts[2]);
            String imageUri = parts.length > 3 ? parts[3] : "";
            System.out.println("parts.length > 3: " + imageUri);
            String link = parts.length > 4 ? parts[4] : "";

            int h = (int)(minutes / 60);
            int m = (int)(minutes % 60);

            View card = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_product_card, productsContainer, false);

            ((TextView) card.findViewById(R.id.tvProductName)).setText(name);
            ((TextView) card.findViewById(R.id.tvProductPrice)).setText(getString(R.string.product_price) + " " + SharedPrefManager.getCurrencySymbol(requireContext()) + price);
            ((TextView) card.findViewById(R.id.tvProductTime)).setText(getString(R.string.time) + " " + h + "h " + m + "m");

            ImageView img = card.findViewById(R.id.imgProduct);
            if (!imageUri.isEmpty()) {
                System.out.println("IMAGE URI: " + imageUri);
                img.setImageURI(Uri.parse(imageUri));
            } else {
                System.out.println("IMAGE URI:2 " + imageUri);
                img.setImageResource(R.drawable.ic_product_placeholder);
            }

            card.setOnClickListener(v -> {
                if (!link.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                }
            });

            ImageView menuBtn = card.findViewById(R.id.btnMenu);
            menuBtn.setOnClickListener(v -> showProductMenu(v, raw, name, price, minutes));

            productsContainer.addView(card);
        }
    }



    private void showProductMenu(
            View anchor,
            String rawProduct,
            String name,
            double price,
            long minutes
    ) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.product_menu);

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_buy) {
                buyProduct(rawProduct, price);
                return true;
            }
            if (item.getItemId() == R.id.action_edit) {
                editProduct(rawProduct);
                return true;
            }
            if (item.getItemId() == R.id.action_delete) {
                dontBuyProduct(rawProduct, price, minutes);
                return true;
            }
            return false;
        });

        menu.show();
    }

    private void buyProduct(String raw, double price) {
        SharedPrefManager.removeProduct(requireContext(), raw);
        SharedPrefManager.setCurrentSpending(getContext(), price);
        loadSavedProducts();
        updateRemainingMoneyCard();
        checkAndDecrementProductSlots();
    }


    private void dontBuyProduct(String raw, double price, long minutes) {
        SharedPrefManager.addSavedMoney(requireContext(), price);
        SharedPrefManager.addSavedTime(requireContext(), minutes);

        SharedPrefManager.removeProduct(requireContext(), raw);
        checkAndDecrementProductSlots();

        loadSavedProducts();
        loadData();
    }


    private void editProduct(String rawProduct) {
        String[] parts = rawProduct.split("\\|");

        String name = parts[0];
        double price = Double.parseDouble(parts[1]);
        long minutes = Long.parseLong(parts[2]);
        String imageUri = parts.length > 3 ? parts[3] : "";
        String link = parts.length > 4 ? parts[4] : "";

        // Abrir diálogo de edición
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32,16,32,0);

        EditText etName = new EditText(getContext());
        etName.setText(name);
        etName.setHint("Nombre del producto");

        EditText etPrice = new EditText(getContext());
        etPrice.setText(String.valueOf(price));
        etPrice.setHint("Precio (€)");

        EditText etLink = new EditText(getContext());
        etLink.setText(link);
        etLink.setHint("Link (opcional)");

        ImageView imgPreview = new ImageView(getContext());
        imgPreview.setImageResource(R.drawable.ic_product_placeholder);
        if (!imageUri.isEmpty()) {
            imgPreview.setImageURI(Uri.parse(imageUri));
            imgPreview.setTag(imageUri);
        }
        imgPreview.setAdjustViewBounds(true);
        imgPreview.setMaxHeight(300);

        Button btnImage = new Button(getContext());
        btnImage.setText("Cambiar imagen");

        btnImage.setOnClickListener(v -> {
            imagePreview = imgPreview;

            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );
            imagePickerLauncher.launch(intent);
        });


        layout.addView(etName);
        layout.addView(etPrice);
        layout.addView(etLink);
        layout.addView(imgPreview);
        layout.addView(btnImage);

        new AlertDialog.Builder(getContext())
                .setTitle("Editar producto")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    String newName = etName.getText().toString().trim();
                    double newPrice = Double.parseDouble(etPrice.getText().toString().trim());
                    String newLink = etLink.getText().toString().trim();
                    String newImage = imgPreview.getTag() != null ? imgPreview.getTag().toString() : "";

                    // Recalcular tiempo automáticamente según salario
                    double salary = Double.parseDouble(SharedPrefManager.getSalary(getContext()));
                    boolean isAnnual = SharedPrefManager.getSalaryType(getContext());
                    double salaryPerHour = isAnnual ? salary/12/160 : salary;
                    long newMinutes = Math.round((newPrice / salaryPerHour) * 60);

                    SharedPrefManager.updateProduct(
                            requireContext(),
                            rawProduct,
                            newName,
                            newPrice,
                            newImage,
                            newLink
                    );

                    loadSavedProducts();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    void checkAndDecrementProductSlots()
    {
        if (!SharedPrefManager.isPremium(requireContext()) &&
                SharedPrefManager.getMaxProductSlots(requireContext()) > 0)
        {
            SharedPrefManager.decrementProdcutSlots(requireContext());
        }
    }

/*
    private void showEditProductDialog(
            String rawProduct,
            String name,
            String price,
            String minutes,
            String imageUri,
            String link
    ) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 0);

        EditText etName = new EditText(getContext());
        etName.setText(name);

        EditText etPrice = new EditText(getContext());
        etPrice.setText(price);

        EditText etLink = new EditText(getContext());
        etLink.setText(link);

        ImageView img = new ImageView(getContext());
        if (!imageUri.isEmpty()) {
            img.setImageURI(Uri.parse(imageUri));
            img.setTag(imageUri);
        }

        Button btnImage = new Button(getContext());
        btnImage.setText("Cambiar imagen");

        imagePreview = img;
        btnImage.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*")
        );

        layout.addView(etName);
        layout.addView(etPrice);
        layout.addView(etLink);
        layout.addView(img);
        layout.addView(btnImage);

        new AlertDialog.Builder(getContext())
                .setTitle("Editar producto")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    SharedPrefManager.updateProduct(
                            requireContext(),
                            rawProduct,
                            etName.getText().toString(),
                            Double.parseDouble(etPrice.getText().toString()),
                            img.getTag() != null ? img.getTag().toString() : "",
                            etLink.getText().toString()
                    );
                    loadSavedProducts();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    */

    private void updateRemainingMoneyCard() {
        System.out.println("REMAINING MONEY CARD: " + SharedPrefManager.isSpendingModeEnabled(requireContext()));
        if (!SharedPrefManager.isSpendingModeEnabled(requireContext())) {
            cardRemainingMoney.setVisibility(View.GONE);
            return;
        }

        double remaining = SharedPrefManager.getRemainingSpending(requireContext());
        System.out.println("remaining: " + remaining);
        tvRemainingMoney.setText(
                String.format("%.2f %s", remaining,
                        SharedPrefManager.getCurrencySymbol(requireContext()))
        );

        cardRemainingMoney.setVisibility(View.VISIBLE);
    }












}
