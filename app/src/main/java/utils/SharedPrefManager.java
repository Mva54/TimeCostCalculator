package utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedPrefManager {

    private static final String KEY_IS_PREMIUM = "is_premium";

    private static final String PREFS_PROFILE = "user_profile";
    private static final String PREFS_NAME = "prefs";

    private static final String KEY_SALARY = "salary";
    private static final String KEY_MONTHLY_HOURS = "monthly_hours";
    private static final String KEY_SALARY_TYPE = "salary_type";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_LANGUAGE = "language";

    private static final String KEY_SAVED_PRODUCTS = "saved_products";
    private static final String KEY_SAVED_MONEY = "saved_money";
    private static final String KEY_SAVED_TIME = "saved_time";

    private static final String KEY_SAVINGS_MODE = "savings_mode";
    private static final String KEY_SAVINGS_GOAL = "savings_goal";
    private static final String KEY_MONTHLY_SAVINGS = "monthly_savings";

    private static final String KEY_SPENDING_MODE = "spending_mode";
    private static final String KEY_MAX_SPENDING = "max_spending";
    private static final String KEY_CURRENT_SPENDING = "current_spending";

    private static final String KEY_AD_UNLOCKED_SLOTS = "ad_unlocked_slots";

    private static Gson gson = new Gson();

    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ------------------- Anuncios --------------------//
    public static int getMaxProductSlots(Context ctx) {
        if (isPremium(ctx)) return Integer.MAX_VALUE;

        return getPrefs(ctx).getInt(KEY_AD_UNLOCKED_SLOTS, 0);
    }

    public static void incrementProdcutSlots(Context ctx) {
        getPrefs(ctx).edit()
                .putInt(KEY_AD_UNLOCKED_SLOTS, getMaxProductSlots(ctx) + 1)
                .apply();
    }

    public static void decrementProdcutSlots(Context ctx) {
        getPrefs(ctx).edit()
                .putInt(KEY_AD_UNLOCKED_SLOTS, getMaxProductSlots(ctx) - 1)
                .apply();
    }

    // ------------------- Ahorro --------------------//
    public static void setSpendingMode(Context ctx, boolean enabled) {
        getPrefs(ctx).edit().putBoolean(KEY_SPENDING_MODE, enabled).apply();
    }

    public static boolean isSpendingModeEnabled(Context ctx) {
        return getPrefs(ctx).getBoolean(KEY_SPENDING_MODE, false);
    }


    public static void setMaxSpending(Context ctx, double amount) {
        getPrefs(ctx).edit()
                .putLong(KEY_MAX_SPENDING, Double.doubleToLongBits(amount))
                .apply();
    }

    public static double getMaxSpending(Context ctx) {
        return Double.longBitsToDouble(
                getPrefs(ctx).getLong(KEY_MAX_SPENDING, Double.doubleToLongBits(0))
        );
    }

    public static void setCurrentSpending(Context ctx, double amount)
    {
        getPrefs(ctx).edit()
                .putLong(KEY_CURRENT_SPENDING, Double.doubleToLongBits(getCurrentSpending(ctx) + amount))
                .apply();
    }

    public static double getCurrentSpending(Context ctx) {
        return Double.longBitsToDouble(
                getPrefs(ctx).getLong(KEY_CURRENT_SPENDING, Double.doubleToLongBits(0))
        );
    }

    public static double getRemainingSpending(Context ctx) {
        return getMaxSpending(ctx) - getCurrentSpending(ctx);
    }

    public static void setSavingsMode(Context ctx, boolean isActive) {
        getPrefs(ctx).edit().putBoolean(KEY_SAVINGS_MODE, isActive).apply();
    }

    public static void saveMonthlySavings(Context ctx, double amount) {
        SharedPreferences prefs = getPrefs(ctx);
        prefs.edit().putLong(KEY_MONTHLY_SAVINGS, Double.doubleToLongBits(amount)).apply();
    }

    public static double getMonthlySavings(Context ctx) {
        SharedPreferences prefs = getPrefs(ctx);
        return Double.longBitsToDouble(prefs.getLong(KEY_MONTHLY_SAVINGS, Double.doubleToLongBits(0)));
    }

    public static double getRemainingMoney(Context ctx) {
        double salary = Double.parseDouble(getSalary(ctx).isEmpty() ? "0" : getSalary(ctx));
        boolean isAnnual = getSalaryType(ctx);
        double salaryPerMonth = isAnnual ? salary / 12 : salary;
        double savings = getMonthlySavings(ctx);
        double moneySpent = getSavedMoney(ctx); // dinero gastado en productos
        double remaining = salaryPerMonth - savings - moneySpent;
        return Math.max(remaining, 0);
    }


    // ------------------- Productos -------------------

    public static List<String> getSavedProducts(Context ctx) {
        String json = getPrefs(ctx).getString(KEY_SAVED_PRODUCTS, "[]");
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static List<String> getVisibleSavedProducts(Context ctx) {
        List<String> all = getSavedProducts(ctx);

        if (isPremium(ctx)) {
            return all;
        }

        int limit = Math.min(3 + getMaxProductSlots(ctx), all.size());
        return new ArrayList<>(all.subList(0, limit));
    }


    private static void saveProductList(Context ctx, List<String> list) {
        String json = gson.toJson(list);
        getPrefs(ctx).edit().putString(KEY_SAVED_PRODUCTS, json).apply();
    }

    public static boolean addSavedProduct(Context ctx,
                                          String name,
                                          double price,
                                          long minutes,
                                          String imageUri,
                                          String link) {

        List<String> list = getSavedProducts(ctx);

        // Límite SOLO para usuarios gratuitos
        if (!isPremium(ctx) && list.size() >= 3 + getMaxProductSlots(ctx)) {
            return false;
        }

        String product = name + "|" + price + "|" + minutes + "|" + imageUri + "|" + link;
        list.add(product);
        saveProductList(ctx, list);
        return true;
    }

    public static void removeProduct(Context ctx, String raw) {
        List<String> list = getSavedProducts(ctx);
        list.remove(raw);
        saveProductList(ctx, list);
    }

    public static void updateProduct(Context ctx, String raw, String newName, Double newPrice, String newImageUri, String newLink) {
        List<String> list = getSavedProducts(ctx);
        int index = list.indexOf(raw);
        if (index == -1) return;

        String[] parts = raw.split("\\|");

        String name = newName != null ? newName : parts[0];
        double price = newPrice != null ? newPrice : Double.parseDouble(parts[1]);
        String image = newImageUri != null ? newImageUri : (parts.length > 3 ? parts[3] : "");
        String link = newLink != null ? newLink : (parts.length > 4 ? parts[4] : "");

        // Recalcular tiempo según salario actual
        double salary = getSalary(ctx).isEmpty() ? 0 : Double.parseDouble(getSalary(ctx));
        boolean isAnnual = getSalaryType(ctx);
        double salaryPerHour = isAnnual ? salary / 12 / 160 : salary;
        long minutes = Math.round((price / salaryPerHour) * 60);

        String updated = name + "|" + price + "|" + minutes + "|" + image + "|" + link;
        list.set(index, updated);
        saveProductList(ctx, list);
    }

    public static void recalcProductTimes(Context ctx, double salary, boolean isAnnual) {
        List<String> list = getSavedProducts(ctx);
        List<String> updatedList = new ArrayList<>();

        double salaryPerHour = isAnnual ? salary / 12 / 160 : salary;

        for (String raw : list) {
            String[] parts = raw.split("\\|");
            if (parts.length < 3) continue;

            String name = parts[0];
            double price = Double.parseDouble(parts[1]);
            String image = parts.length > 3 ? parts[3] : "";
            String link = parts.length > 4 ? parts[4] : "";

            long newMinutes = Math.round((price / salaryPerHour) * 60);
            String updated = name + "|" + price + "|" + newMinutes + "|" + image + "|" + link;
            updatedList.add(updated);
        }

        saveProductList(ctx, updatedList);
    }

    // ------------------- Money y Time -------------------

    public static void setSavedMoney(Context ctx, double newSavedMoney) {
        SharedPreferences prefs = getPrefs(ctx);
        prefs.edit().putLong(KEY_SAVED_MONEY, Double.doubleToLongBits(newSavedMoney)).apply();
    }

    public static void addSavedMoney(Context ctx, double moneyToAdd) {
        SharedPreferences prefs = getPrefs(ctx);
        double currentMoney = Double.longBitsToDouble(prefs.getLong(KEY_SAVED_MONEY, Double.doubleToLongBits(0)));
        currentMoney += moneyToAdd;
        prefs.edit().putLong(KEY_SAVED_MONEY, Double.doubleToLongBits(currentMoney)).apply();
    }

    public static double getSavedMoney(Context ctx) {
        SharedPreferences prefs = getPrefs(ctx);
        return Double.longBitsToDouble(prefs.getLong(KEY_SAVED_MONEY, Double.doubleToLongBits(0)));
    }

    public static void setSavedTime(Context ctx, long newSavedTime) {
        SharedPreferences prefs = getPrefs(ctx);
        prefs.edit().putLong(KEY_SAVED_TIME, newSavedTime).apply();
    }

    public static void addSavedTime(Context ctx, long minutesToAdd) {
        SharedPreferences prefs = getPrefs(ctx);
        long current = prefs.getLong(KEY_SAVED_TIME, 0);
        current += minutesToAdd;
        prefs.edit().putLong(KEY_SAVED_TIME, current).apply();
    }

    public static long getSavedTime(Context ctx) {
        return getPrefs(ctx).getLong(KEY_SAVED_TIME, 0);
    }

    // ------------------- Perfil -------------------

    public static void saveSalary(Context ctx, String value) {
        ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_SALARY, value).apply();
    }

    public static String getSalary(Context ctx) {
        return ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .getString(KEY_SALARY, "");
    }

    public static void saveMonthlyHours(Context ctx, String value) {
        ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_MONTHLY_HOURS, value).apply();
    }

    public static String getMonthlyHours(Context ctx) {
        return ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .getString(KEY_MONTHLY_HOURS, "");
    }

    public static void saveSalaryType(Context ctx, boolean isAnnual) {
        ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SALARY_TYPE, isAnnual).apply();
    }

    public static boolean getSalaryType(Context ctx) {
        return ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .getBoolean(KEY_SALARY_TYPE, true);
    }

    public static void saveCurrency(Context ctx, int pos) {
        ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .edit().putInt(KEY_CURRENCY, pos).apply();
    }

    public static int getCurrency(Context ctx) {
        return ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .getInt(KEY_CURRENCY, 0);
    }

    public static String getCurrencySymbol(Context ctx) {
        int pos = getCurrency(ctx);
        String[] symbols = {"€", "$", "£", "Fr.", "¥"};
        return (pos >= 0 && pos < symbols.length) ? symbols[pos] : "€";
    }

    public static void saveLanguage(Context ctx, int pos) {
        ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .edit().putInt(KEY_LANGUAGE, pos).apply();
    }

    public static int getLanguage(Context ctx) {
        return ctx.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
                .getInt(KEY_LANGUAGE, 0);
    }

    public static String getLanguageCode(Context ctx) {
        int pos = getLanguage(ctx);
        String[] codes = {"es", "en", "fr", "de"};
        return (pos >= 0 && pos < codes.length) ? codes[pos] : "es";
    }

    public static boolean isPremium(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_PREMIUM, false);
    }

    public static void setPremium(Context ctx, boolean value) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_PREMIUM, value)
                .apply();
    }
}

