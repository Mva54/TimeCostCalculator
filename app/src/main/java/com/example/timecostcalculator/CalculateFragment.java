package com.example.timecostcalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import utils.SharedPrefManager;

public class CalculateFragment extends Fragment {

    private EditText etProductPrice;
    private TextView tvError;
    private Button btnSubmitPrice;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView imagePreview;

    private static String auxProduct = "";

    public CalculateFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.calculate_fragment, container, false);

        etProductPrice = view.findViewById(R.id.etProductPrice);
        tvError = view.findViewById(R.id.tvError);
        btnSubmitPrice = view.findViewById(R.id.btnSubmitPrice);

        AdView adView = view.findViewById(R.id.adView);
        AdView adView2 = view.findViewById(R.id.adView2);

        if (!SharedPrefManager.isPremium(getContext())) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            adView2.loadAd(adRequest);
        } else {
            adView.setVisibility(View.GONE);
            adView2.setVisibility(View.GONE);
        }

        btnSubmitPrice.setOnClickListener(v -> submitPrice());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                        Uri sourceUri = result.getData().getData();
                        Uri localUri = copyImageToInternalStorage(sourceUri);

                        if (localUri != null && imagePreview != null) {
                            imagePreview.setImageURI(localUri);
                            imagePreview.setTag(localUri.toString());
                        }
                    }
                }
        );




        ImageView btnPriceInfo = view.findViewById(R.id.btnPriceInfo);
        btnPriceInfo.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.info))
                    .setMessage(
                            getString(R.string.info_calculate_price) + "\n\n" +
                            getString(R.string.info_calculate_price_actions) + "\n\n" +
                            getString(R.string.info_calculate_price_buy) + "\n\n" +
                            getString(R.string.info_calculate_price_dont_buy) + "\n\n" +
                            getString(R.string.info_calculate_price_save) + "\n\n" +
                            getString(R.string.info_calculate_price_discard)
                    )
                    .setPositiveButton("OK", null)
                    .show();
        });


        return view;
    }

    private void submitPrice() {
        String priceStr = etProductPrice.getText().toString();
        String salaryStr = SharedPrefManager.getSalary(getContext());
        boolean isAnnual = SharedPrefManager.getSalaryType(getContext());

        if (priceStr.isEmpty()) {
            // Mostrar error
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(getString(R.string.warning));
            return;
        } else {
            // Ocultar error si todo correcto
            tvError.setVisibility(View.GONE);
        }

        if (salaryStr.isEmpty()) {
            salaryStr = "0";
        }

        double price = Double.parseDouble(priceStr);
        double salary = Double.parseDouble(salaryStr);

        double salaryPerHour;
        if (isAnnual) {
            salaryPerHour = salary / 12 / 160; // asumimos 160h/mes
        } else {
            salaryPerHour = salary; // si ya es horario
        }

        // Ahora calculamos tiempo
        double timeNeededHours = price / salaryPerHour;

        int hours = (int) timeNeededHours;
        int minutes = (int) ((timeNeededHours - hours) * 60);

        // Mostrar resultado en popup con botones
        showResultPopup(price, hours, minutes);
    }

    private void showResultPopup(double price, int hours, int minutes) {
        int hoursPerJornada = 8;

        int jornadas = (int) (hours / hoursPerJornada);
        int remainingHours = (int) (hours % hoursPerJornada);

        // Layout principal del popup
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("Worth It?");
        tvTitle.setTextSize(35);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 24, 0, 24);
        tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.black));


        // Resultado
        TextView tvPrevResult = new TextView(getContext());
        tvPrevResult.setText(getString(R.string.work_time));
        tvPrevResult.setTextSize(20);
        //tvPrevResult.setTypeface(null, Typeface.BOLD);
        tvPrevResult.setGravity(Gravity.CENTER);
        tvPrevResult.setPadding(0, 16, 0, 8);

        TextView tvMainResult = new TextView(getContext());
        tvMainResult.setText(hours + " h " + minutes + " min");
        tvMainResult.setTextSize(30);
        tvMainResult.setTypeface(null, Typeface.BOLD);
        tvMainResult.setGravity(Gravity.CENTER);
        tvMainResult.setPadding(0, 16, 0, 8);

        TextView tvSubResult = new TextView(getContext());
        tvSubResult.setText(
                jornadas > 0
                        ? getString(R.string.work_time_with_days, hours, minutes, jornadas, remainingHours)
                        : ""
        );
        tvSubResult.setTextSize(20);
        tvSubResult.setTextColor(Color.GRAY);
        tvSubResult.setGravity(Gravity.CENTER);

        layout.addView(tvTitle);
        layout.addView(tvPrevResult);
        layout.addView(tvMainResult);
        layout.addView(tvSubResult);

        // GridLayout para botones 2x2
        GridLayout buttonsLayout = new GridLayout(getContext());
        buttonsLayout.setColumnCount(2);
        buttonsLayout.setUseDefaultMargins(true);
        buttonsLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);
        buttonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Crear botones
        Button btnBuy = new Button(getContext());
        btnBuy.setText(getString(R.string.buy));
        btnBuy.setLayoutParams(createButtonParams());
        btnBuy.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(getContext(), R.color.primary)
        ));
        btnBuy.setTextColor(Color.WHITE);
        buttonsLayout.addView(btnBuy);

        Button btnNoBuy = new Button(getContext());
        btnNoBuy.setText(getString(R.string.dont_buy));
        btnNoBuy.setLayoutParams(createButtonParams());
        btnNoBuy.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(getContext(), R.color.secondary)
        ));
        btnNoBuy.setTextColor(Color.WHITE);
        buttonsLayout.addView(btnNoBuy);

        Button btnSave = new Button(getContext());
        btnSave.setText(getString(R.string.save));
        btnSave.setLayoutParams(createButtonParams());
        buttonsLayout.addView(btnSave);

        Button btnDiscard = new Button(getContext());
        btnDiscard.setText(getString(R.string.discard));
        btnDiscard.setLayoutParams(createButtonParams());
        btnDiscard.setBackgroundColor(Color.TRANSPARENT);
        btnDiscard.setTextColor(Color.GRAY);
        buttonsLayout.addView(btnDiscard);

        layout.addView(buttonsLayout);

        // Crear el popup
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                //.setTitle("Worth It?")
                .setView(layout)
                .setCancelable(true)
                .create();

        // Eventos de botones
        btnBuy.setOnClickListener(v -> {
            SharedPrefManager.setCurrentSpending(getContext(), price);
            Toast.makeText(
                    getContext(),
                    getString(R.string.product_bought),
                    Toast.LENGTH_LONG
            ).show();
            dialog.dismiss();
        });
        btnNoBuy.setOnClickListener(v -> {
            saveResult(price, hours, minutes);
            Toast.makeText(
                    getContext(),
                    getString(R.string.product_noBought),
                    Toast.LENGTH_LONG
            ).show();
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v -> {
            showSaveProductDialog(price, hours, minutes);
            dialog.dismiss();
        });
        btnDiscard.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(
                    getContext(),
                    getString(R.string.product_discarted),
                    Toast.LENGTH_LONG
            ).show();
        });

        dialog.show();
    }

    private GridLayout.LayoutParams createButtonParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0; // permite usar peso
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // 1f = mitad de ancho
        return params;
    }

    private void showSaveProductDialog(double price, int hours, int minutes) {

        CardView card = new CardView(getContext());
        card.setRadius(24f);
        card.setCardElevation(12f);
        card.setUseCompatPadding(true);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 32, 40, 24);

        card.addView(layout);

        //LinearLayout layout = new LinearLayout(getContext());
        //layout.setOrientation(LinearLayout.VERTICAL);
        //layout.setPadding(32, 16, 32, 0);

        EditText etName = new EditText(getContext());
        etName.setHint(getString(R.string.product_name_hint));
        etName.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(40)
        });
        etName.setSingleLine(true);
        etName.setTextSize(16);
        etName.setHintTextColor(Color.GRAY);
        etName.setPadding(24, 20, 24, 20);
        etName.setBackgroundResource(R.drawable.bg_input_rounded);


        EditText etLink = new EditText(getContext());
        etLink.setHint(getString(R.string.product_link_hint));
        etName.setTextSize(16);
        etName.setHintTextColor(Color.GRAY);
        etName.setPadding(24, 20, 24, 20);
        etName.setBackgroundResource(R.drawable.bg_input_rounded);


        //Button btnImage = new Button(getContext());
        //btnImage.setText(getString(R.string.add_image));

        ImageView imgPreview = new ImageView(getContext());
        imgPreview.setImageResource(R.drawable.ic_product_placeholder);
        imgPreview.setAdjustViewBounds(true);
        imgPreview.setMaxHeight(320);
        imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgPreview.setPadding(0, 16, 0, 16);
        imgPreview.setBackgroundResource(R.drawable.bg_image_placeholder);

        imgPreview.setOnClickListener(v -> {
            imagePreview = imgPreview;
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );
            imagePickerLauncher.launch(intent);
        });

        layout.addView(etName);
        addSpacer(layout, 12);
        layout.addView(etLink);
        addSpacer(layout, 16);
        layout.addView(imgPreview);


        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.save_product_title))
                .setView(card)
                .setPositiveButton(getString(R.string.save), null)
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String link = etLink.getText().toString().trim();

                if (name.isEmpty()) {
                    etName.setError(getString(R.string.product_name_required));
                    etName.requestFocus();
                    return;
                }

                saveResultProduct(
                        name,
                        price,
                        hours,
                        minutes,
                        imgPreview.getTag() != null
                                ? imgPreview.getTag().toString()
                                : "",
                        link
                );

                dialog.dismiss();
            });
        });

        dialog.show();

    }

    private void addSpacer(LinearLayout layout, int dp) {
        View spacer = new View(layout.getContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        layout.getResources().getDisplayMetrics()
                )
        ));
        layout.addView(spacer);
    }


    private void saveResult(double price, int hours, int minutes) {
        SharedPrefManager.addSavedMoney(getContext(), price);
        SharedPrefManager.addSavedTime(getContext(), hours * 60L + minutes);
    }

    private void saveResultProduct(String name, double price, int hours, int minutes, String imageUri,
                                   String link) {
        long totalMinutes = hours * 60L + minutes;

        boolean saved = SharedPrefManager.addSavedProduct(
                getContext(),
                name,
                price,
                totalMinutes,
                imageUri,
                link
        );

        if (!saved) {
            auxProduct = name + "|" + price + "|" + hours + "|" + minutes + "|" + imageUri + "|" + link;
            showPremiumDialog();
            return;
        }

        Toast.makeText(
                getContext(),
                getString(R.string.product_save),
                Toast.LENGTH_LONG
        ).show();
    }

    private void showPremiumDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 24);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.ic_premium); // estrella, corona, etc
        icon.setColorFilter(getResources().getColor(R.color.premium_gold));
        icon.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        layout.addView(icon);

        TextView title = new TextView(getContext());
        title.setText(getString(R.string.limit_reached));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 16, 0, 8);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView description = new TextView(getContext());
        description.setText(getString(R.string.text_premium_short));
        description.setTextSize(14);
        description.setTextColor(Color.DKGRAY);
        description.setGravity(Gravity.CENTER);
        layout.addView(description);

        LinearLayout benefits = new LinearLayout(getContext());
        benefits.setOrientation(LinearLayout.VERTICAL);
        benefits.setPadding(0, 24, 0, 24);

        benefits.addView(createBenefit(getString(R.string.info_premium_history)));
        benefits.addView(createBenefit(getString(R.string.info_premium_products)));
        benefits.addView(createBenefit(getString(R.string.info_premium_ads)));

        layout.addView(benefits);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(layout)
                .setPositiveButton(getString(R.string.update_premium), (d, w) -> {
                    // abrir pantalla de pago
                })
                .setNegativeButton(getString(R.string.watch_ad), (d, w) -> {
                    showRewardedAd();
                })
                .create();

        dialog.show();

    }

    private TextView createBenefit(String text) {
        TextView tv = new TextView(getContext());
        tv.setText("✔ " + text);
        tv.setTextSize(14);
        tv.setTextColor(Color.BLACK);
        tv.setPadding(0, 8, 0, 8);
        return tv;
    }


    public void unlockOneSlot(Context ctx) {
        SharedPrefManager.incrementProdcutSlots(ctx);
        String[] parts = auxProduct.split("\\|");

        saveResultProduct(parts[0], Double.parseDouble(parts[1]), Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                parts.length > 4 ? parts[4] : "",
                parts.length > 5 ? parts[5] : "");

        auxProduct = "";
    }

    private void showRewardedAd() {
        if (AdManager.getRewaredAd() == null) {
            Toast.makeText(getContext(), getString(R.string.no_ad), Toast.LENGTH_SHORT).show();
            AdManager.loadRewarded(getContext()); // intenta recargar
            return;
        }

        AdManager.getRewaredAd().setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                AdManager.setRewardedAd(null);
                AdManager.loadRewarded(getContext()); // precargar siguiente
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                AdManager.setRewardedAd(null);
            }
        });

        AdManager.getRewaredAd().show(requireActivity(), rewardItem -> {
            // 🎁 RECOMPENSA
            unlockOneSlot(requireContext());

            Toast.makeText(
                    getContext(),
                    getString(R.string.extra_slot),
                    Toast.LENGTH_LONG
            ).show();
        });
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





}
