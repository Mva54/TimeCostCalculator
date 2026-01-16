package com.example.timecostcalculator;

import android.app.Activity;
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
                "ca-app-pub-3940256099942544/5224354917",
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

    public static boolean isRewardedReady() {
        return rewardedAd != null;
    }

    public static void showRewarded(Activity activity, Runnable onReward) {
        if (rewardedAd == null) return;

        rewardedAd.show(activity, rewardItem -> {
            onReward.run();
        });

        rewardedAd = null;
        loadRewarded(activity);
    }
}

