package com.example.timecostcalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import utils.SharedPrefManager;

public class ProfileFragment extends Fragment {

    private EditText etSalary;
    private SwitchCompat switchSalaryType;
    private Spinner spCurrency;

    private SwitchCompat switchLimitMode;
    private EditText etLimitGoal;
    private TextView tvLimitInfo;


    public ProfileFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        etSalary = view.findViewById(R.id.etSalary);
        switchSalaryType = view.findViewById(R.id.switchSalaryType);
        spCurrency = view.findViewById(R.id.spCurrency);
        switchLimitMode = view.findViewById(R.id.switchLimitMode);
        etLimitGoal = view.findViewById(R.id.etLimitGoal);
        tvLimitInfo = view.findViewById(R.id.tvLimitInfo);

        // Monedas
        String[] currencies = {"€", "$", "£", "¥"};
        spCurrency.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, currencies));

        // Cargar valores guardados
        loadProfile();

        // Listener del Switch
        switchSalaryType.setOnCheckedChangeListener((buttonView, isChecked) -> saveProfile());

        // Listener del Spinner de moneda
        spCurrency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                saveProfile();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // Guardar sueldo cuando se pierde el foco
        etSalary.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveProfile(); });

        Spinner spLanguage = view.findViewById(R.id.spLanguage);

        // Opciones visibles para el usuario
        String[] languages = {"Español", "English"};
        spLanguage.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, languages));

        // Cargar valor guardado
        spLanguage.setSelection(SharedPrefManager.getLanguage(getContext()));

        // Guardar automáticamente al cambiar
        spLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                int savedLang = SharedPrefManager.getLanguage(getContext());
                if (position != savedLang) {
                    SharedPrefManager.saveLanguage(getContext(), position);
                    ProfileFragment.updateLocale(getContext());
                    getActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        switchLimitMode.setChecked(SharedPrefManager.isSpendingModeEnabled(getContext()));

        // Listener del switch
        switchLimitMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefManager.setSpendingMode(getContext(), isChecked);
        });

        // Guardar objetivo cuando pierda foco
        etLimitGoal.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                double amount = 0;
                try { amount = Double.parseDouble(etLimitGoal.getText().toString()); }
                catch (NumberFormatException ignored) {}
                SharedPrefManager.setMaxSpending(getContext(), amount);
                updateLimitInfoText();
            }
        });

        ImageView btnLimitInfo = view.findViewById(R.id.btnLimitInfo);
        btnLimitInfo.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.info))
                    .setMessage(getString(R.string.info_limit))
                    .setPositiveButton("OK", null)
                    .show();
        });

        return view;
    }

    private void saveProfile() {
        String salary = etSalary.getText().toString();
        boolean isAnnual = switchSalaryType.isChecked(); // true = Anual, false = Horario
        int currency = spCurrency.getSelectedItemPosition();

        SharedPrefManager.saveSalary(getContext(), salary);
        SharedPrefManager.saveSalaryType(getContext(), isAnnual);
        SharedPrefManager.saveCurrency(getContext(), currency);

        SharedPrefManager.recalcProductTimes(getContext(), Double.parseDouble(salary), isAnnual);
    }

    private void loadProfile() {
        etSalary.setText(SharedPrefManager.getSalary(getContext()));
        switchSalaryType.setChecked(SharedPrefManager.getSalaryType(getContext()));
        spCurrency.setSelection(SharedPrefManager.getCurrency(getContext()));
        System.out.println("MAX SPENDING: " + SharedPrefManager.getMaxSpending(getContext()));
        etLimitGoal.setText(String.valueOf(SharedPrefManager.getMaxSpending(getContext())));
        switchLimitMode.setChecked(
                SharedPrefManager.isSpendingModeEnabled(getContext())
        );
        updateLimitInfoText();
    }

    public static void updateLocale(Context context) {
        // Recupera el idioma seleccionado (por ejemplo: 0=Español, 1=Inglés, etc.)
        int langIndex = SharedPrefManager.getLanguage(context);

        // Mapear índice a código de idioma
        String langCode;
        switch (langIndex) {
            case 1: langCode = "en"; break;
            case 2: langCode = "fr"; break;
            case 3: langCode = "de"; break;
            default: langCode = "es"; break; // español por defecto
        }

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private void updateLimitInfoText() {
        String salaryStr = SharedPrefManager.getSalary(getContext());
        if (salaryStr.isEmpty()) {
            tvLimitInfo.setVisibility(View.GONE);
            return;
        }

        double salary = Double.parseDouble(salaryStr);
        boolean isAnnual = SharedPrefManager.getSalaryType(getContext());
        double monthlySalary = isAnnual ? salary / 12 : salary;

        double limit = SharedPrefManager.getMaxSpending(getContext());

        if (monthlySalary <= 0 || limit <= 0) {
            tvLimitInfo.setVisibility(View.GONE);
            return;
        }

        double savings = monthlySalary - limit;
        double percent = (savings / monthlySalary) * 100;
        percent = Math.max(0, percent);

        tvLimitInfo.setText(
                String.format(
                        getString(R.string.info_limit_save),
                        percent
                )
        );

        tvLimitInfo.setVisibility(View.VISIBLE);
    }

}
