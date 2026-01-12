package com.example.timecostcalculator.model;

public class SavedProduct {

    private String name;
    private double price;
    private int timeSavedMinutes;
    private double moneySaved;
    private long date;

    public SavedProduct(String name, double price, int timeSavedMinutes, double moneySaved, long date) {
        this.name = name;
        this.price = price;
        this.timeSavedMinutes = timeSavedMinutes;
        this.moneySaved = moneySaved;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getTimeSavedMinutes() {
        return timeSavedMinutes;
    }

    public double getMoneySaved() {
        return moneySaved;
    }

    public long getDate() {
        return date;
    }
}
