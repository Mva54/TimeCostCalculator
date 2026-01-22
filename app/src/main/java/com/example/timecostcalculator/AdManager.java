package com.example.timecostcalculator;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdManager {

    private static RewardedAd rewardedAd;

    public static RewardedAd getRewaredAd() {
        return rewardedAd;
    }

    public static void setRewardedAd(RewardedAd ad) {
        rewardedAd = ad;
    }

    public static void loadRewarded(Context context) {
        if (rewardedAd != null) return;

        AdRequest request = new AdRequest.Builder().build();

        RewardedAd.load(
                context,
                "ca-app-pub-6751486812061921/2205067213",
                request,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                    }
                }
        );
    }
}

