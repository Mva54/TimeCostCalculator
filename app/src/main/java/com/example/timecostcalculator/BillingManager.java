package com.example.timecostcalculator;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

import utils.SharedPrefManager;

public class BillingManager {

    private BillingClient billingClient;
    private final Activity activity;

    public BillingManager(Activity activity) {
        this.activity = activity;
        setupBillingClient();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryExistingPurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Reintentar si quieres
            }
        });
    }

    // AQUÍ SE LANZA LA COMPRA
    public void launchPremiumPurchase() {

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                Collections.singletonList(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId("premium_lifetime")
                                                .setProductType(BillingClient.ProductType.INAPP)
                                                .build()
                                )
                        )
                        .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {

            if (productDetailsList.isEmpty()) return;

            ProductDetails productDetails = productDetailsList.get(0);

            BillingFlowParams flowParams =
                    BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                    Collections.singletonList(
                                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                                    .setProductDetails(productDetails)
                                                    .build()
                                    )
                            )
                            .build();

            billingClient.launchBillingFlow(activity, flowParams);
        });
    }

    private void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains("premium_lifetime")) {
                    SharedPrefManager.setPremium(activity, true);
                }
            }
        }
    }

    private void queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    for (Purchase purchase : purchases) {
                        if (purchase.getProducts().contains("premium_lifetime")) {
                            SharedPrefManager.setPremium(activity, true);
                        }
                    }
                }
        );
    }
}

