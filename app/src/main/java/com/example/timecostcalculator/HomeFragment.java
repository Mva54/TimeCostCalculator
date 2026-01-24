package com.example.timecostcalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.io.InputStream;

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

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflar el layout
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        View root = view.findViewById(R.id.rootLayout); // tu LinearLayout

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    24 + systemBars.left,
                    24 + systemBars.top,
                    24 + systemBars.right,
                    24 + systemBars.bottom
            );
            return insets;
        });

        // Referencias a los TextViews
        tvSavedMoney = view.findViewById(R.id.tvSavedMoney);
        tvSavedTime = view.findViewById(R.id.tvSavedTime);
        productsContainer = view.findViewById(R.id.productsContainer);
        View premiumCard = view.findViewById(R.id.cardPremiumPromo);
        View btnGoPremium = view.findViewById(R.id.btnGoPremium);
        View tvPremiumOnlyOne = view.findViewById(R.id.tvPremium);

        AdView adView = view.findViewById(R.id.adView);

        boolean isPremium = SharedPrefManager.isPremium(requireContext());

        if (!isPremium) {
            premiumCard.setVisibility(View.VISIBLE);
            tvPremiumOnlyOne.setVisibility(View.VISIBLE);

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);

            btnGoPremium.setOnClickListener(v -> {
                MainActivity.billingManager.launchPremiumPurchase();
            });
        } else {
            adView.setVisibility(View.GONE);
        }

        tvRemainingMoney = view.findViewById(R.id.tvRemainingMoney);
        cardRemainingMoney = view.findViewById(R.id.cardRemainingMoney);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri sourceUri = result.getData().getData();
                        Uri localUri = copyImageToInternalStorage(sourceUri);

                        imagePreview.setImageURI(localUri);
                        imagePreview.setTag(localUri.toString());
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
                File file = new File(imageUri.replace("file://", ""));
                if (file.exists()) {
                    img.setImageURI(Uri.fromFile(file));
                } else {
                    img.setImageResource(R.drawable.ic_product_placeholder);
                }
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
        String imageUri = parts.length > 3 ? parts[3] : "";
        String link = parts.length > 4 ? parts[4] : "";

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView imgPreview = new ImageView(getContext());
        imgPreview.setImageResource(R.drawable.ic_product_placeholder);
        imgPreview.setAdjustViewBounds(true);
        imgPreview.setMaxHeight(300);

        if (!imageUri.isEmpty()) {
            imgPreview.setImageURI(Uri.parse(imageUri));
            imgPreview.setTag(imageUri);
        }

        Button btnImage = new Button(getContext());
        btnImage.setText(getString(R.string.change_image));

        btnImage.setOnClickListener(v -> {
            imagePreview = imgPreview;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            );
            imagePickerLauncher.launch(intent);

        });

        layout.addView(imgPreview);
        layout.addView(btnImage);

        layout.addView(createLabel(getString(R.string.product_name_hint)));

        EditText etName = new EditText(getContext());
        etName.setText(name);
        etName.setSingleLine(true);
        etName.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(40) });
        layout.addView(etName);

        layout.addView(createLabel(getString(R.string.price)));

        EditText etPrice = new EditText(getContext());
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPrice.setText(String.valueOf(price));
        layout.addView(etPrice);

        layout.addView(createLabel(getString(R.string.product_link_hint)));

        EditText etLink = new EditText(getContext());
        etLink.setText(link);
        layout.addView(etLink);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.edit_product_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), null)
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {

                String newName = etName.getText().toString().trim();
                String priceText = etPrice.getText().toString().trim();

                if (newName.isEmpty()) {
                    etName.setError(getString(R.string.product_name_required));
                    etName.requestFocus();
                    return;
                }

                if (priceText.isEmpty()) {
                    etPrice.setError(getString(R.string.price_required));
                    etPrice.requestFocus();
                    return;
                }

                double newPrice = Double.parseDouble(priceText);
                String newLink = etLink.getText().toString().trim();
                String newImage = imgPreview.getTag() != null ? imgPreview.getTag().toString() : "";

                SharedPrefManager.updateProduct(
                        requireContext(),
                        rawProduct,
                        newName,
                        newPrice,
                        newImage,
                        newLink
                );

                loadSavedProducts();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(Color.GRAY);
        tv.setPadding(0, 16, 0, 4);
        return tv;
    }

    void checkAndDecrementProductSlots()
    {
        if (!SharedPrefManager.isPremium(requireContext()) &&
                SharedPrefManager.getMaxProductSlots(requireContext()) > 0)
        {
            SharedPrefManager.decrementProdcutSlots(requireContext());
        }
    }

    private Uri copyImageToInternalStorage(Uri sourceUri) {
        try {
            InputStream inputStream = requireContext()
                    .getContentResolver()
                    .openInputStream(sourceUri);

            File file = new File(
                    requireContext().getFilesDir(),
                    "product_" + System.currentTimeMillis() + ".jpg"
            );

            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            inputStream.close();
            outputStream.close();

            return Uri.fromFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


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
