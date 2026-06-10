package com.pos.system;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ProductSuggestionHelper {

    public static final class Suggestion {
        public final String barcode;
        public final String name;
        public final String brand;
        public final String unit;

        Suggestion(String barcode, String name, String brand, String unit) {
            this.barcode = barcode != null ? barcode : "";
            this.name    = name    != null ? name    : "";
            this.brand   = brand   != null ? brand   : "";
            this.unit    = unit    != null ? unit    : "";
        }

        @Override public String toString() { return name; }
    }

    private static volatile List<Suggestion> cache;

    private ProductSuggestionHelper() {}

    public static List<Suggestion> load(Context context) {
        if (cache != null) return cache;
        synchronized (ProductSuggestionHelper.class) {
            if (cache != null) return cache;
            List<Suggestion> list = new ArrayList<>();
            try {
                InputStream is = context.getApplicationContext().getAssets().open("products.json");
                byte[] buf = new byte[is.available()];
                //noinspection ResultOfMethodCallIgnored
                is.read(buf);
                is.close();
                JSONArray arr = new JSONArray(new String(buf, StandardCharsets.UTF_8));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new Suggestion(
                        o.optString("barcode"),
                        o.optString("name"),
                        o.optString("brand"),
                        o.optString("unit")
                    ));
                }
            } catch (Exception ignored) {}
            cache = list;
        }
        return cache;
    }

    /** Returns up to 8 suggestions whose name contains {@code query} (case-insensitive). */
    public static List<Suggestion> searchByName(Context context, String query) {
        List<Suggestion> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) return results;
        String q = query.trim().toLowerCase();
        for (Suggestion s : load(context)) {
            if (!s.name.isEmpty() && s.name.toLowerCase().contains(q)) {
                results.add(s);
                if (results.size() >= 8) break;
            }
        }
        return results;
    }

    /** Returns the first product whose barcode exactly matches, or {@code null}. */
    public static Suggestion findByBarcode(Context context, String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return null;
        String b = barcode.trim();
        for (Suggestion s : load(context)) {
            if (s.barcode.equals(b)) return s;
        }
        return null;
    }
}
