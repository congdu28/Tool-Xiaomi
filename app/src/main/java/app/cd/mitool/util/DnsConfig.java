package app.cd.mitool.util;

import java.net.IDN;
import java.util.Locale;

/** Validation shared by the UI and shell paths. Private DNS requires a DoT hostname. */
public final class DnsConfig {
    private DnsConfig() {}

    public static String cleanHostname(String input) {
        if (input == null) return "";
        String host = input.trim().toLowerCase(Locale.ROOT);
        if (host.startsWith("https://")) host = host.substring(8);
        else if (host.startsWith("http://")) host = host.substring(7);
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        try { return IDN.toASCII(host); } catch (IllegalArgumentException e) { return ""; }
    }

    public static boolean isValidHostname(String host) {
        if (host == null || host.length() > 253 || !host.contains(".")
                || host.matches("[0-9.]+")) return false;
        for (String label : host.split("\\.", -1)) {
            if (!label.matches("[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?")) return false;
        }
        return true;
    }
}
