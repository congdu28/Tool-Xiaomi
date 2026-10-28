package app.cd.mitool.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Only display overrides belong here. A refresh-rate action must not enable performance modes. */
public final class RefreshRatePlan {
    private RefreshRatePlan() {}
    public static final String[] KEYS = {
            "secure:peak_refresh_rate", "system:peak_refresh_rate",
            "secure:user_refresh_rate", "system:user_refresh_rate",
            "secure:miui_refresh_rate", "system:miui_refresh_rate",
            "secure:min_refresh_rate", "system:min_refresh_rate"
    };

    public static Map<String, String> changes(int target, Map<String, String> original) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : KEYS) {
            String value = target > 0 ? Integer.toString(target)
                    : original != null ? original.get(key)
                    : key.contains("min_refresh_rate") || key.contains("peak_refresh_rate") ? null : "0";
            result.put(key, value);
        }
        return result;
    }

    public static String shell(Map<String, String> changes) {
        StringBuilder result = new StringBuilder("settings put secure min_refresh_rate 0 && settings put system min_refresh_rate 0");
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            String[] key = entry.getKey().split(":");
            result.append(" && settings ").append(entry.getValue() == null ? "delete " : "put ")
                    .append(key[0]).append(' ').append(key[1]);
            if (entry.getValue() != null) result.append(" '").append(entry.getValue().replace("'", "'\"'\"'")).append("'");
        }
        return result.toString();
    }
}
