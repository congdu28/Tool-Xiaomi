package app.cd.mitool.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class XiaomiHelper {

    /**
     * Check if device is running Xiaomi MIUI or HyperOS.
     */
    public static boolean isXiaomiDevice() {
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer != null && (manufacturer.equalsIgnoreCase("Xiaomi") || manufacturer.equalsIgnoreCase("Redmi") || manufacturer.equalsIgnoreCase("POCO"))) {
            return true;
        }
        return getSystemProperty("ro.miui.ui.version.name") != null || getSystemProperty("ro.mi.os.version.name") != null;
    }

    private static String getSystemProperty(String propName) {
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

    /**
     * Check if overlay permission (SYSTEM_ALERT_WINDOW) is granted.
     */
    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * Open system Overlay permission screen for this app.
     */
    public static void openOverlaySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        }
    }

    /**
     * Open Xiaomi Autostart management screen.
     */
    public static void openAutostartSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.powercenter.autostart.AutoStartManagementActivity"));
                context.startActivity(intent);
            } catch (Exception e2) {
                openAppDetailsSettings(context);
                Toast.makeText(context, "Vui lòng tìm mục 'Tự khởi chạy' (Autostart) trong cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Open Xiaomi Battery Saver ("Không giới hạn" / No restrictions) screen.
     */
    public static void openBatterySaverSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"));
            intent.putExtra("package_name", context.getPackageName());
            intent.putExtra("package_label", "Mi Tool");
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openAppDetailsSettings(context);
                Toast.makeText(context, "Vui lòng chọn 'Tiết kiệm pin' -> 'Không giới hạn'.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Open Xiaomi Lock Screen / Background pop-up permission screen.
     */
    public static void openLockScreenPermSettings(Context context) {
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            openAppDetailsSettings(context);
            Toast.makeText(context, "Vui lòng bật quyền 'Hiển thị trên màn hình khóa' và 'Cửa sổ bật lên'.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Open Xiaomi hidden battery info testing screen (*#*#4636#*#*).
     */
    public static void openHiddenBatteryInfo(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TestingSettings"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(context, "Bạn có thể bấm *#*#4636#*#* trong bàn phím quay số để xem.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Open Xiaomi CIT Hardware Test screen (*#*#6484#*#*).
     */
    public static boolean openCITHardwareTest(Context context) {
        String[][] components = {
            {"com.miui.cit", "com.miui.cit.CitLauncherActivity"},
            {"com.miui.cit", "com.miui.cit.MainActivity"},
            {"com.qualcomm.qti.qmmi", "com.qualcomm.qti.qmmi.MainActivity"}
        };
        for (String[] comp : components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + Uri.encode("*#*#6484#*#*")));
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dialIntent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Open Xiaomi Battery Info & Cycle Count Secret screen (*#*#6485#*#*).
     */
    public static boolean openBatterySecretInfo(Context context) {
        String[][] components = {
            {"com.miui.cit", "com.miui.cit.charge.ChargeActivity"},
            {"com.miui.cit", "com.miui.cit.battery.BatteryInfoActivity"},
            {"com.android.settings", "com.android.settings.TestingSettings"}
        };
        for (String[] comp : components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        try {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + Uri.encode("*#*#6485#*#*")));
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dialIntent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Open MIUI Deep Clean (No Ads).
     */
    public static boolean openDeepClean(Context context) {
        String[][] components = {
            {"com.miui.cleanmaster", "com.miui.optimizecenter.deepclean.DeepCleanActivity"},
            {"com.miui.cleanmaster", "com.miui.cleanmaster.MainActivity"},
            {"com.miui.securitycenter", "com.miui.securityscan.MainActivity"}
        };
        for (String[] comp : components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        try {
            Intent storageIntent = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
            storageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(storageIntent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Open Xiaomi Game Turbo (Game Space).
     */
    public static boolean openGameTurbo(Context context) {
        String[][] components = {
            {"com.miui.securitycenter", "com.miui.gamebooster.ui.QuickDesktopMainActivity"},
            {"com.miui.securitycenter", "com.miui.gamebooster.MainActivity"},
            {"com.miui.gamebooster", "com.miui.gamebooster.MainActivity"}
        };
        for (String[] comp : components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * Open Xiaomi Authorization & Revocation to disable MSA (MIUI System Ads).
     */
    public static boolean openAuthorizationAndRevocation(Context context) {
        String[][] components = {
            {"com.android.settings", "com.android.settings.Settings$PrivacySettingsActivity"},
            {"com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"}
        };
        for (String[] comp : components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        try {
            Intent fallback = new Intent(Settings.ACTION_PRIVACY_SETTINGS);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Open Ad Services Opt-out Settings.
     */
    public static boolean openAdServicesSettings(Context context) {
        String[][] components = {
            {"com.android.settings", "com.android.settings.Settings$PersonalAdActivity"},
            {"com.google.android.gms", "com.google.android.gms.ads.settings.AdsSettingsActivity"}
        };
        for (String[] comp : components) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
        }
        try {
            Intent fallback = new Intent(Settings.ACTION_SETTINGS);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Read battery cycle count if available via sysfs or BatteryManager API 34+.
     * Returns -1 if not accessible.
     */
    public static int getBatteryCycleCount(Context context) {
        // BatteryManager property 7 is BATTERY_PROPERTY_CYCLE_COUNT on Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                android.os.BatteryManager bm = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (bm != null) {
                    int cycles = bm.getIntProperty(7);
                    if (cycles > 0) return cycles;
                }
            } catch (Exception ignored) {
            }
        }

        // Check common sysfs nodes
        String[] paths = {
            "/sys/class/power_supply/battery/cycle_count",
            "/sys/class/power_supply/bms/battery_cycle",
            "/sys/class/power_supply/bms/cycle_count"
        };
        for (String path : paths) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.canRead()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new java.io.FileReader(file));
                    String line = reader.readLine();
                    if (line != null) {
                        int c = Integer.parseInt(line.trim());
                        if (c > 0) return c;
                    }
                } catch (Exception ignored) {
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
        return -1;
    }

    private static void openAppDetailsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
