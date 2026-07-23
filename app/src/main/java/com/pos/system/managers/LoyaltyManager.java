package com.pos.system.managers;

import android.content.Context;
import com.pos.system.DBHelper;

/**
 * Tiered loyalty program:
 *   Bronze (default):  1 pt/EGP,  10 pts = 1 EGP discount
 *   Silver (≥5 000 cumulative pts): 1.5x multiplier, 10 pts = 1.2 EGP
 *   Gold   (≥20 000 cumulative pts): 2x multiplier,  10 pts = 1.5 EGP
 */
public class LoyaltyManager {

    // ─── Tier thresholds (cumulative lifetime points earned) ───
    public static final int TIER_SILVER_THRESHOLD = 5_000;
    public static final int TIER_GOLD_THRESHOLD   = 20_000;

    public enum Tier {
        BRONZE("برونزي", "Bronze", 1.0, 0.10),
        SILVER("فضي",   "Silver", 1.5, 0.12),
        GOLD  ("ذهبي",  "Gold",   2.0, 0.15);

        public final String nameAr;
        public final String nameEn;
        public final double pointsMultiplier;
        public final double egpPerPoint;

        Tier(String nameAr, String nameEn, double multiplier, double egpPerPoint) {
            this.nameAr = nameAr;
            this.nameEn = nameEn;
            this.pointsMultiplier = multiplier;
            this.egpPerPoint = egpPerPoint;
        }

        /** Tier name in the currently active app language */
        public String displayName() {
            return LanguageManager.isArabic() ? nameAr : nameEn;
        }
    }

    private static final double BASE_POINTS_PER_EGP = 1.0;
    private static final int    MIN_REDEEM           = 100;

    private final DBHelper dbHelper;

    public LoyaltyManager(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    // ─────────────────────────────────────────────────────────────
    // Tier logic
    // ─────────────────────────────────────────────────────────────

    public Tier getTierForPoints(int lifetimePoints) {
        if (lifetimePoints >= TIER_GOLD_THRESHOLD)   return Tier.GOLD;
        if (lifetimePoints >= TIER_SILVER_THRESHOLD) return Tier.SILVER;
        return Tier.BRONZE;
    }

    public Tier getTier(String customerId) {
        int lifetime = dbHelper.getCustomerLifetimeLoyaltyPoints(customerId);
        return getTierForPoints(lifetime);
    }

    // ─────────────────────────────────────────────────────────────
    // Points calculation
    // ─────────────────────────────────────────────────────────────

    public int calculatePointsForPurchase(double totalAmount, String customerId) {
        Tier tier = getTier(customerId);
        return (int) (totalAmount * BASE_POINTS_PER_EGP * tier.pointsMultiplier);
    }

    public int calculatePointsForPurchase(double totalAmount) {
        return (int) (totalAmount * BASE_POINTS_PER_EGP);
    }

    public double calculateDiscountForPoints(int points, String customerId) {
        Tier tier = getTier(customerId);
        return points * tier.egpPerPoint;
    }

    public double calculateDiscountForPoints(int points) {
        return points * Tier.BRONZE.egpPerPoint;
    }

    // ─────────────────────────────────────────────────────────────
    // Balance / redeem
    // ─────────────────────────────────────────────────────────────

    public boolean canRedeem(String customerId) {
        return getBalance(customerId) >= MIN_REDEEM;
    }

    public int getBalance(String customerId) {
        return dbHelper.getCustomerLoyaltyPoints(customerId);
    }

    public boolean earnPoints(String customerId, String customerName,
                               double purchaseAmount, String invoiceNumber) {
        int points = calculatePointsForPurchase(purchaseAmount, customerId);
        if (points <= 0) return true;
        return dbHelper.addLoyaltyPoints(customerId, customerName, points, "earn", invoiceNumber);
    }

    public boolean redeemPoints(String customerId, String customerName,
                                 int points, String reference) {
        if (getBalance(customerId) < points) return false;
        if (points < MIN_REDEEM) return false;
        return dbHelper.addLoyaltyPoints(customerId, customerName, points, "redeem", reference);
    }

    // ─────────────────────────────────────────────────────────────
    // Progress to next tier
    // ─────────────────────────────────────────────────────────────

    /** How many more lifetime points needed to reach the next tier, or 0 if already Gold. */
    public int pointsToNextTier(String customerId) {
        int lifetime = dbHelper.getCustomerLifetimeLoyaltyPoints(customerId);
        Tier current = getTierForPoints(lifetime);
        if (current == Tier.GOLD)   return 0;
        if (current == Tier.SILVER) return TIER_GOLD_THRESHOLD   - lifetime;
        return TIER_SILVER_THRESHOLD - lifetime;
    }

    public int getMinimumRedeemPoints() { return MIN_REDEEM; }
    public double getEgpPerPoint() { return Tier.BRONZE.egpPerPoint; }
    public double getPointsPerEgp() { return BASE_POINTS_PER_EGP; }

    public void close() {
        if (dbHelper != null) dbHelper.close();
    }
}
