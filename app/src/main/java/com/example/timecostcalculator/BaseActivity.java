package com.example.timecostcalculator;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import utils.SharedPrefManager;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }

    private Context updateBaseContextLocale(Context context) {
        int langIndex = SharedPrefManager.getLanguage(context);

        String langCode;
        switch (langIndex) {
            case 1: langCode = "en"; break;
            case 2: langCode = "ca"; break;
            default: langCode = "es"; break;
        }

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}

