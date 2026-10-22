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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceInfoHelper {

    private static final Map<String, String> SOC_MAP = new HashMap<>();

    static {
        // Qualcomm Snapdragon flagship & premium
        SOC_MAP.put("SM8850", "Snapdragon 8 Elite (Gen 4/5)");
        SOC_MAP.put("SM8850-AB", "Snapdragon 8 Elite");
        SOC_MAP.put("SM8750", "Snapdragon 8 Elite (Gen 4)");
        SOC_MAP.put("SM8750-AB", "Snapdragon 8 Elite");
        SOC_MAP.put("SM8650", "Snapdragon 8 Gen 3");
        SOC_MAP.put("SM8550", "Snapdragon 8 Gen 2");
        SOC_MAP.put("SM8475", "Snapdragon 8+ Gen 1");
        SOC_MAP.put("SM8450", "Snapdragon 8 Gen 1");
        SOC_MAP.put("SM8350", "Snapdragon 888 / 888+");
        SOC_MAP.put("SM8250", "Snapdragon 865 / 870");
        SOC_MAP.put("SM8150", "Snapdragon 855 / 855+");

        // Qualcomm Snapdragon 7 series
        SOC_MAP.put("SM7675", "Snapdragon 7+ Gen 3");
        SOC_MAP.put("SM7550", "Snapdragon 7 Gen 3");
        SOC_MAP.put("SM7475", "Snapdragon 7+ Gen 2");
        SOC_MAP.put("SM7435", "Snapdragon 7s Gen 2");
        SOC_MAP.put("SM7325", "Snapdragon 778G / 778G+");
        SOC_MAP.put("SM7225", "Snapdragon 750G");
        SOC_MAP.put("SM7150", "Snapdragon 730 / 732G");
        SOC_MAP.put("SM7125", "Snapdragon 720G");

        // Qualcomm Snapdragon 6 & 4 series
        SOC_MAP.put("SM6450", "Snapdragon 6 Gen 1");
        SOC_MAP.put("SM6375", "Snapdragon 695 5G");
        SOC_MAP.put("SM6225", "Snapdragon 680 4G");
        SOC_MAP.put("SM6115", "Snapdragon 662");
        SOC_MAP.put("SM4450", "Snapdragon 4 Gen 2");
        SOC_MAP.put("SM4375", "Snapdragon 4 Gen 1");
        SOC_MAP.put("SM4350", "Snapdragon 480 5G");

        // Qualcomm Board Platform Codenames
        SOC_MAP.put("SUN", "Snapdragon 8 Elite");
        SOC_MAP.put("PINEAPPLE", "Snapdragon 8 Gen 3");
        SOC_MAP.put("KALAMA", "Snapdragon 8 Gen 2");
        SOC_MAP.put("TARO", "Snapdragon 8 Gen 1");
        SOC_MAP.put("LAHAINA", "Snapdragon 888");
        SOC_MAP.put("KONA", "Snapdragon 865 / 870");
        SOC_MAP.put("LITO", "Snapdragon 765G");
        SOC_MAP.put("CLIFFS", "Snapdragon 7+ Gen 3");
        SOC_MAP.put("CROW", "Snapdragon 7 Gen 3");
        SOC_MAP.put("PARROT", "Snapdragon 6 Gen 1");
        SOC_MAP.put("SKY", "Snapdragon 4 Gen 2");

        // MediaTek Dimensity
        SOC_MAP.put("MT6991", "Dimensity 9400");
        SOC_MAP.put("MT6989", "Dimensity 9300 / 9300+");
        SOC_MAP.put("MT6985", "Dimensity 9200 / 9200+");
        SOC_MAP.put("MT6983", "Dimensity 9000 / 9000+");
        SOC_MAP.put("MT6897", "Dimensity 8300-Ultra");
        SOC_MAP.put("MT6896", "Dimensity 8200-Ultra");
        SOC_MAP.put("MT6895", "Dimensity 8100 / 8200");
        SOC_MAP.put("MT6893", "Dimensity 1200");
        SOC_MAP.put("MT6886", "Dimensity 7200-Ultra");
        SOC_MAP.put("MT6877", "Dimensity 7050 / 1080 / 900");
        SOC_MAP.put("MT6855", "Dimensity 930 / 7020");
        SOC_MAP.put("MT6833", "Dimensity 700 / 6020 / 6080");

        // MediaTek Helio
        SOC_MAP.put("MT6789", "Helio G99 / G99-Ultra");
        SOC_MAP.put("MT6769", "Helio G88 / G85");
        SOC_MAP.put("MT6768", "Helio G80");
        SOC_MAP.put("MT6765", "Helio G36 / G35");
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
            String upper = soc.toUpperCase(Locale.US);
            if (SOC_MAP.containsKey(upper)) {
                return SOC_MAP.get(upper) + " (" + upper + ")";
            }
            return soc;
        }

        String platform = getSystemProperty("ro.board.platform");
        if (platform != null && !platform.isEmpty()) {
            String upper = platform.toUpperCase(Locale.US);
            if (SOC_MAP.containsKey(upper)) {
                return SOC_MAP.get(upper);
            }
        }

        // Check hardware from /proc/cpuinfo
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String hw = parts[1].trim();
                        String upper = hw.toUpperCase(Locale.US);
                        if (SOC_MAP.containsKey(upper)) {
                            return SOC_MAP.get(upper) + " (" + hw + ")";
                        }
                        return hw;
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

    public static double getCpuMaxFreqGhz() {
        double maxGhz = 0.0;
        int cores = getCpuCoreCount();
        for (int i = 0; i < cores; i++) {
            String[] paths = {
                    "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq",
                    "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_max_freq"
            };
            for (String p : paths) {
                File f = new File(p);
                if (f.exists() && f.canRead()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line = br.readLine();
                        if (line != null) {
                            long khz = Long.parseLong(line.trim());
                            double ghz = khz / 1000000.0;
                            if (ghz > maxGhz) {
                                maxGhz = ghz;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        // Fallback: Default reasonable max frequency based on typical core capabilities
        if (maxGhz <= 0.5) {
            maxGhz = 3.20; // Default modern octa-core peak
        }
        return maxGhz;
    }

    public static String getCpuFrequencySummary() {
        int cores = getCpuCoreCount();
        double maxGhz = getCpuMaxFreqGhz();
        return String.format(Locale.US, "%d Nhân • Tối đa %.2f GHz", cores, maxGhz);
    }

    public static String getCpuArchitecture() {
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            String abi = Build.SUPPORTED_ABIS[0];
            if ("arm64-v8a".equalsIgnoreCase(abi)) {
                return "arm64-v8a (ARMv8 / ARMv9 64-bit)";
            }
            return abi + " (64-bit)";
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

    public static class ZramStats {
        public final double totalGb;
        public final double usedGb;
        public final double freeGb;
        public final int percentUsed;
        public final boolean isEnabled;

        public ZramStats(double totalGb, double usedGb, double freeGb, int percentUsed, boolean isEnabled) {
            this.totalGb = totalGb;
            this.usedGb = usedGb;
            this.freeGb = freeGb;
            this.percentUsed = percentUsed;
            this.isEnabled = isEnabled;
        }
    }

    public static ZramStats getZramStats() {
        long swapTotalKb = 0;
        long swapFreeKb = 0;

        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("SwapTotal:")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        swapTotalKb = Long.parseLong(parts[1]);
                    }
                } else if (line.startsWith("SwapFree:")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        swapFreeKb = Long.parseLong(parts[1]);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        if (swapTotalKb > 0) {
            double totalGb = swapTotalKb / (1024.0 * 1024.0);
            double freeGb = swapFreeKb / (1024.0 * 1024.0);
            double usedGb = totalGb - freeGb;
            int percent = (int) Math.round((usedGb / totalGb) * 100);
            return new ZramStats(totalGb, usedGb, freeGb, percent, true);
        }

        return new ZramStats(0.0, 0.0, 0.0, 0, false);
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
        public final double screenInches;
        public final String resolutionStandard;
        public final String aspectRatio;
        public final String displayTechnology;
        public final boolean isHdrSupported;

        public DisplayStats(int width, int height, int dpi, int refreshRate, List<Integer> supportedRates,
                            double screenInches, String resolutionStandard, String aspectRatio,
                            String displayTechnology, boolean isHdrSupported) {
            this.width = width;
            this.height = height;
            this.dpi = dpi;
            this.refreshRate = refreshRate;
            this.supportedRates = supportedRates;
            this.screenInches = screenInches;
            this.resolutionStandard = resolutionStandard;
            this.aspectRatio = aspectRatio;
            this.displayTechnology = displayTechnology;
            this.isHdrSupported = isHdrSupported;
        }
    }

    public static DisplayStats getDisplayStats(Context context) {
        int width = 1080;
        int height = 2400;
        int dpi = 440;
        int rate = 120;
        double screenInches = 6.67;
        String resStandard = "FHD+ (1080 x 2400)";
        String aspect = "20:9";
        String tech = "AMOLED (DCI-P3)";
        boolean hasHdr = true;
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

                    // Calculate screen diagonal in inches
                    if (dm.xdpi > 0 && dm.ydpi > 0) {
                        double x = width / (double) dm.xdpi;
                        double y = height / (double) dm.ydpi;
                        screenInches = Math.hypot(x, y);
                    }

                    // Resolution Standard
                    int longSide = Math.max(width, height);
                    int shortSide = Math.min(width, height);
                    if (shortSide >= 1400) {
                        resStandard = "2K WQHD+ (" + width + " x " + height + ")";
                    } else if (shortSide >= 1200) {
                        resStandard = "1.5K CrystalRes (" + width + " x " + height + ")";
                    } else if (shortSide >= 1000) {
                        resStandard = "FHD+ (" + width + " x " + height + ")";
                    } else {
                        resStandard = "HD+ (" + width + " x " + height + ")";
                    }

                    // Aspect ratio calculation
                    double ratio = (double) longSide / (double) shortSide;
                    if (ratio >= 2.1) {
                        aspect = "20.5:9";
                    } else if (ratio >= 1.95) {
                        aspect = "20:9";
                    } else if (ratio >= 1.85) {
                        aspect = "19.5:9";
                    } else if (ratio >= 1.75) {
                        aspect = "16:9";
                    } else {
                        aspect = String.format(Locale.US, "%.1f:1", ratio);
                    }

                    // HDR capabilities
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Display.HdrCapabilities hdr = display.getHdrCapabilities();
                        if (hdr != null && hdr.getSupportedHdrTypes() != null && hdr.getSupportedHdrTypes().length > 0) {
                            hasHdr = true;
                            tech = "AMOLED / OLED (Hỗ trợ HDR10+, Dolby Vision)";
                        } else {
                            hasHdr = false;
                            tech = "IPS LCD / AMOLED (DCI-P3 Color)";
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return new DisplayStats(width, height, dpi, rate, rates, screenInches, resStandard, aspect, tech, hasHdr);
    }
}
