package app.cd.mitool.ui;

import java.text.Normalizer;
import java.util.Locale;

public final class FeatureSearch {
    private FeatureSearch() {}
    public static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
    public static boolean matches(String query, String text) {
        String haystack = normalize(text);
        for (String term : normalize(query).split("\\s+")) if (!haystack.contains(term)) return false;
        return true;
    }
}
