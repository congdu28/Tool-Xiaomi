package app.cd.mitool.util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceInfoHelper {

    private static final Map<String, String> SOC_MAP = new HashMap<>();

    static {
        SOC_MAP.put("SM8750", "Snapdragon 8 Elite (Gen 4)");
        SOC_MAP.put("SM8650", "Snapdragon 8 Gen 3");
        SOC_MAP.put("SM8550", "Snapdragon 8 Gen 2");
        SOC_MAP.put("SM8475", "Snapdragon 8+ Gen 1");
        SOC_MAP.put("SM8450", "Snapdragon 8 Gen 1");
        SOC_MAP.put("SM8350", "Snapdragon 888 / 888+");
        SOC_MAP.put("SM8250", "Snapdragon 865 / 870");
        SOC_MAP.put("SM7475", "Snapdragon 7+ Gen 2");
        SOC_MAP.put("SM7550", "Snapdragon 7 Gen 3");
        SOC_MAP.put("SM7675", "Snapdragon 7+ Gen 3");
        SOC_MAP.put("SM7325", "Snapdragon 778G / 778G+");
        SOC_MAP.put("SM7150", "Snapdragon 730 / 732G");
        SOC_MAP.put("SM6375", "Snapdragon 695 5G");
        SOC_MAP.put("MT6989", "Dimensity 9300 / 9300+");
        SOC_MAP.put("MT6985", "Dimensity 9200 / 9200+");
        SOC_MAP.put("MT6983", "Dimensity 9000 / 9000+");
        SOC_MAP.put("MT6895", "Dimensity 8100 / 8200");
        SOC_MAP.put("MT6897", "Dimensity 8300-Ultra");
        SOC_MAP.put("MT6896", "Dimensity 8200-Ultra");
        SOC_MAP.put("MT6877", "Dimensity 900 / 1080 / 7050");
        SOC_MAP.put("MT6789", "Helio G99");
    }

    public static String getSystemProperty(String propName) {
        String line = null;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
        return (line != null && !line.trim().isEmpty()) ? line.trim() : null;
    }

    public static String getDeviceMarketName() {
        String name = getSystemProperty("ro.product.marketname");
        if (name != null && !name.isEmpty()) return name;

        name = getSystemProperty("ro.product.model");
        if (name != null && !name.isEmpty()) return name;

        String brand = Build.MANUFACTURER;
        if (brand != null && !brand.isEmpty()) {
            brand = brand.substring(0, 1).toUpperCase() + brand.substring(1).toLowerCase();
        } else {
            brand = "Xiaomi";
        }
        return brand + " " + Build.MODEL;
    }

    public static String getDeviceCodename() {
        String device = Build.DEVICE;
        return (device != null && !device.isEmpty()) ? device : Build.PRODUCT;
    }

    public static String getOsVersionDisplay() {
        String hyperOs = getSystemProperty("ro.mi.os.version.name");
        if (hyperOs != null && !hyperOs.isEmpty()) {
            return "Xiaomi HyperOS " + hyperOs.replace("OS", "");
        }

        String miui = getSystemProperty("ro.miui.ui.version.name");
        if (miui != null && !miui.isEmpty()) {
            return "Xiaomi MIUI " + miui;
        }

        return "Android " + Build.VERSION.RELEASE;
    }

    public static String getAndroidVersionDisplay() {
        return "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }

    public static String getSecurityPatch() {
        String patch = Build.VERSION.SECURITY_PATCH;
        return (patch != null && !patch.isEmpty()) ? patch : "Không rõ";
    }

    public static String getCpuModel() {
        String soc = getSystemProperty("ro.soc.model");
        if (soc != null && !soc.isEmpty()) {
            soc = soc.toUpperCase(Locale.US);
            if (SOC_MAP.containsKey(soc)) {
                return SOC_MAP.get(soc) + " (" + soc + ")";
            }
            return soc;
        }

        String platform = getSystemProperty("ro.board.platform");
        if (platform != null && !platform.isEmpty()) {
            platform = platform.toUpperCase(Locale.US);
            if (SOC_MAP.containsKey(platform)) {
                return SOC_MAP.get(platform);
            }
        }

        // Fallback to /proc/cpuinfo
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        return parts[1].trim();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return Build.HARDWARE != null ? Build.HARDWARE : "Qualcomm / MediaTek Octa-core";
    }

    public static int getCpuCoreCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String getCpuArchitecture() {
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS[0];
        }
        return "64-bit ARM";
    }

    public static class MemoryStats {
        public final double totalGb;
        public final double usedGb;
        public final double freeGb;
        public final int percentUsed;

        public MemoryStats(double totalGb, double usedGb, double freeGb, int percentUsed) {
            this.totalGb = totalGb;
            this.usedGb = usedGb;
            this.freeGb = freeGb;
            this.percentUsed = percentUsed;
        }
    }

    public static MemoryStats getRamStats(Context context) {
        if (context == null) return new MemoryStats(8.0, 4.0, 4.0, 50);
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(mi);
            double total = mi.totalMem / (1024.0 * 1024.0 * 1024.0);
            double free = mi.availMem / (1024.0 * 1024.0 * 1024.0);
            double used = total - free;
            int percent = (int) Math.round((used / total) * 100);
            return new MemoryStats(total, used, free, percent);
        }
        return new MemoryStats(8.0, 4.0, 4.0, 50);
    }

    public static MemoryStats getInternalStorageStats() {
        try {
            File dataDir = Environment.getDataDirectory();
            StatFs stat = new StatFs(dataDir.getPath());
            long totalBytes = stat.getTotalBytes();
            long freeBytes = stat.getAvailableBytes();
            long usedBytes = totalBytes - freeBytes;

            double total = totalBytes / (1024.0 * 1024.0 * 1024.0);
            double free = freeBytes / (1024.0 * 1024.0 * 1024.0);
            double used = usedBytes / (1024.0 * 1024.0 * 1024.0);
            int percent = (int) Math.round((used / total) * 100);

            return new MemoryStats(total, used, free, percent);
        } catch (Throwable t) {
            return new MemoryStats(128.0, 64.0, 64.0, 50);
        }
    }

    public static class DisplayStats {
        public final int width;
        public final int height;
        public final int dpi;
        public final int refreshRate;
        public final List<Integer> supportedRates;

        public DisplayStats(int width, int height, int dpi, int refreshRate, List<Integer> supportedRates) {
            this.width = width;
            this.height = height;
            this.dpi = dpi;
            this.refreshRate = refreshRate;
            this.supportedRates = supportedRates;
        }
    }

    public static DisplayStats getDisplayStats(Context context) {
        int width = 1080;
        int height = 2400;
        int dpi = 440;
        int rate = 120;
        List<Integer> rates = RefreshRateHelper.getSupportedRefreshRates(context);

        if (context != null) {
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    Display display = wm.getDefaultDisplay();
                    DisplayMetrics dm = new DisplayMetrics();
                    display.getRealMetrics(dm);
                    width = dm.widthPixels;
                    height = dm.heightPixels;
                    dpi = dm.densityDpi;
                    rate = RefreshRateHelper.getCurrentRefreshRate(context);
                }
            } catch (Throwable ignored) {
            }
        }
        return new DisplayStats(width, height, dpi, rate, rates);
    }
}
