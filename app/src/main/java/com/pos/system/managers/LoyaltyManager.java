package com.pos.system.managers;

import android.content.Context;
import com.pos.system.DBHelper;

public class LoyaltyManager {

    private static final double POINTS_PER_EGP = 1.0;   // 1 point per EGP spent
    private static final double EGP_PER_POINT  = 0.10;  // 10 points = 1 EGP discount
    private static final int    MIN_REDEEM      = 100;   // minimum 100 points to redeem

    private final DBHelper dbHelper;

    public LoyaltyManager(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    public int calculatePointsForPurchase(double totalAmount) {
        return (int) (totalAmount * POINTS_PER_EGP);
    }

    public double calculateDiscountForPoints(int points) {
        return points * EGP_PER_POINT;
    }

    public boolean canRedeem(String customerId) {
        return getBalance(customerId) >= MIN_REDEEM;
    }

    public int getBalance(String customerId) {
        return dbHelper.getCustomerLoyaltyPoints(customerId);
    }

    public boolean earnPoints(String customerId, String customerName,
                               double purchaseAmount, String invoiceNumber) {
        int points = calculatePointsForPurchase(purchaseAmount);
        if (points <= 0) return true;
        return dbHelper.addLoyaltyPoints(customerId, customerName, points, "earn", invoiceNumber);
    }

    public boolean redeemPoints(String customerId, String customerName,
                                 int points, String reference) {
        if (getBalance(customerId) < points) return false;
        if (points < MIN_REDEEM) return false;
        return dbHelper.addLoyaltyPoints(customerId, customerName, points, "redeem", reference);
    }

    public int getMinimumRedeemPoints() { return MIN_REDEEM; }
    public double getEgpPerPoint() { return EGP_PER_POINT; }
    public double getPointsPerEgp() { return POINTS_PER_EGP; }

    public void close() {
        if (dbHelper != null) dbHelper.close();
    }
}
