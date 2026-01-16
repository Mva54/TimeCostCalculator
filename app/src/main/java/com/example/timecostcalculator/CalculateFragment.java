package com.example.timecostcalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import utils.SharedPrefManager;

public class CalculateFragment extends Fragment {

    private EditText etProductPrice;
    private TextView tvError;
    private Button btnSubmitPrice;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView imagePreview;

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

        btnSubmitPrice.setOnClickListener(v -> submitPrice());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {

                        Uri uri = result.getData().getData();
                        if (uri == null || imagePreview == null) return;

                        imagePreview.setImageURI(uri);
                        imagePreview.setTag(uri.toString()); // 🔑 guardamos URI
                    }
                }
        );



        ImageView btnPriceInfo = view.findViewById(R.id.btnPriceInfo);
        btnPriceInfo.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.info))
                    .setMessage(getString(R.string.info_calculate_price) + "\n\n" +
                            getString(R.string.info_calculate_price_buy) + "\n" +
                            getString(R.string.info_calculate_price_dont_buy) + "\n" +
                            getString(R.string.info_calculate_price_save) + "\n" +
                            getString(R.string.info_calculate_price_discard))
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

        // Resultado
        TextView tvResult = new TextView(getContext());
        if (jornadas > 0) {
            tvResult.setText(
                    getString(
                            R.string.work_time_with_days,
                            hours,
                            minutes,
                            jornadas,
                            remainingHours
                    )
            );
        } else {
            tvResult.setText(
                    getString(
                            R.string.work_time_basic,
                            hours,
                            minutes
                    )
            );
        }

        tvResult.setTextSize(18);
        tvResult.setPadding(0, 0, 0, 24);
        tvResult.setGravity(Gravity.CENTER);
        layout.addView(tvResult);

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
        buttonsLayout.addView(btnBuy);

        Button btnNoBuy = new Button(getContext());
        btnNoBuy.setText(getString(R.string.dont_buy));
        btnNoBuy.setLayoutParams(createButtonParams());
        buttonsLayout.addView(btnNoBuy);

        Button btnSave = new Button(getContext());
        btnSave.setText(getString(R.string.save));
        btnSave.setLayoutParams(createButtonParams());
        buttonsLayout.addView(btnSave);

        Button btnDiscard = new Button(getContext());
        btnDiscard.setText(R.string.discard);
        btnDiscard.setLayoutParams(createButtonParams());
        buttonsLayout.addView(btnDiscard);

        layout.addView(buttonsLayout);

        // Crear el popup
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.result))
                .setView(layout)
                .setCancelable(true)
                .create();

        // Eventos de botones
        btnBuy.setOnClickListener(v -> {
            SharedPrefManager.setCurrentSpending(getContext(), price);
            dialog.dismiss();
        });
        btnNoBuy.setOnClickListener(v -> {
            saveResult(price, hours, minutes);
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v -> {
            showSaveProductDialog(price, hours, minutes);
            dialog.dismiss();
        });
        btnDiscard.setOnClickListener(v -> dialog.dismiss());

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
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 0);

        EditText etName = new EditText(getContext());
        etName.setHint(getString(R.string.product_name_hint));

        EditText etLink = new EditText(getContext());
        etLink.setHint(getString(R.string.product_link_hint));

        Button btnImage = new Button(getContext());
        btnImage.setText(getString(R.string.add_image));

        ImageView imgPreview = new ImageView(getContext());
        imgPreview.setImageResource(R.drawable.ic_product_placeholder);
        imgPreview.setAdjustViewBounds(true);
        imgPreview.setMaxHeight(300);

        btnImage.setOnClickListener(v -> {
            imagePreview = imgPreview;

            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );
            imagePickerLauncher.launch(intent);
        });

        layout.addView(etName);
        layout.addView(etLink);
        layout.addView(btnImage);
        layout.addView(imgPreview);

        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.save_product_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String link = etLink.getText().toString().trim();

                    if (!name.isEmpty()) {
                        saveResultProduct(name, price, hours, minutes, imgPreview.getTag() != null
                                ? imgPreview.getTag().toString()
                                : "", link);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
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

        //saveResult(price, hours, minutes);

        if (!saved) {
            showPremiumDialog();
        }
    }

    private void showPremiumDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Límite alcanzado")
                .setMessage("Solo puedes guardar hasta 3 productos en la versión gratuita.")
                .setPositiveButton("Actualizar a Premium", (d, w) -> {
                    // futuro: abrir pantalla de pago
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String copyImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream =
                    requireContext().getContentResolver().openInputStream(uri);

            File dir = new File(requireContext().getFilesDir(), "products");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "product_" + System.currentTimeMillis() + ".jpg");

            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return file.getAbsolutePath(); // ⬅️ SIN file://
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


}
