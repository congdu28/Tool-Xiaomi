package app.cd.mitool.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import app.cd.mitool.R;

/** On-demand process inspection and explicit, verified force-stop through Shizuku. */
public final class RunningAppsHelper {
    private RunningAppsHelper() {}
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    public static final String PROCESS_COMMAND = "ps -A -n -o UID,PID,NAME";

    public static final class Entry {
        public final String packageName, label;
        public final int uid, processCount;
        public final boolean fixedProtection;
        public boolean kept;
        Entry(String pkg, String label, int uid, int count, boolean fixed, boolean kept) {
            packageName = pkg; this.label = label; this.uid = uid; processCount = count;
            fixedProtection = fixed; this.kept = kept;
        }
        public boolean selectable() { return !fixedProtection && !kept; }
    }
    public static final class Snapshot {
        public final List<Entry> entries = new ArrayList<>();
        public String error;
    }
    public static final class Result {
        public int stopped, failed, skipped;
        public final List<String> failures = new ArrayList<>();
        public RamBoosterHelper.MemoryStats before, after;
    }
    public interface LoadCallback { void onLoaded(Snapshot snapshot); }
    public interface StopCallback {
        void onProgress(int completed, int total);
        void onComplete(Result result);
    }
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("ram_cleanup_protection", Context.MODE_PRIVATE);
    }
    public static void setKept(Context context, String pkg, boolean kept) {
        preferences(context).edit().putBoolean(pkg, kept).apply();
    }
    private static boolean isKept(Context context, String pkg) {
        boolean recommended = false;
        for (NotificationFixHelper.TargetApp app : NotificationFixHelper.TARGET_TEMPLATES) {
            if (app.packageName.equals(pkg) && app.isRecommended) { recommended = true; break; }
        }
        return preferences(context).getBoolean(pkg, recommended);
    }
    private static Map<Integer, Set<Integer>> processes() {
        ShizukuHelper.ShellResult result = ShizukuHelper.executeCommand(PROCESS_COMMAND);
        if (!result.success || !result.stdout.contains("PID") || !result.stdout.contains("UID")) {
            throw new IllegalStateException("ps unavailable: " + result.stderr);
        }
        return RamCleanupPolicy.parseProcesses(result.stdout);
    }
    private static boolean eligibleNow(Context context, Entry expected) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai = pm.getApplicationInfo(expected.packageName, 0);
        String[] owners = pm.getPackagesForUid(ai.uid);
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String ime = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        android.content.pm.ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        boolean protectedApp = ai.uid != expected.uid || ai.packageName.equals(context.getPackageName())
                || ai.packageName.equals("moe.shizuku.privileged.api") || power == null
                || power.isIgnoringBatteryOptimizations(ai.packageName) || isKept(context, ai.packageName)
                || (ime != null && ime.startsWith(ai.packageName + "/"))
                || (home != null && home.activityInfo != null && ai.packageName.equals(home.activityInfo.packageName));
        return RamCleanupPolicy.canStop(ai.uid, android.os.Process.myUid() / 100000,
                (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0, owners == null || owners.length != 1, protectedApp);
    }
    public static void load(Context context, LoadCallback callback) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            Snapshot snapshot = snapshot(app);
            MAIN.post(() -> callback.onLoaded(snapshot));
        });
    }
    private static Snapshot snapshot(Context context) {
        Snapshot result = new Snapshot();
        if (!ShizukuHelper.hasShizukuPermission()) {
            result.error = context.getString(R.string.ram_permission_needed); return result;
        }
        try {
            Map<Integer, Set<Integer>> running = processes();
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> installed = pm.getInstalledApplications(0);
            Map<Integer, Integer> uidCounts = new HashMap<>();
            for (ApplicationInfo ai : installed) uidCounts.put(ai.uid, uidCounts.getOrDefault(ai.uid, 0) + 1);
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String ime = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            android.content.pm.ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
            String homePkg = home != null && home.activityInfo != null ? home.activityInfo.packageName : "";
            int userId = android.os.Process.myUid() / 100000;
            for (ApplicationInfo ai : installed) {
                if (!running.containsKey(ai.uid) || ai.uid / 100000 != userId || ai.uid % 100000 < 10000
                        || (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                String pkg = ai.packageName;
                boolean protectedApp = pkg.equals(context.getPackageName()) || pkg.equals("moe.shizuku.privileged.api")
                        || pkg.equals(homePkg) || (ime != null && ime.startsWith(pkg + "/"))
                        || (power != null && power.isIgnoringBatteryOptimizations(pkg));
                boolean fixed = !RamCleanupPolicy.canStop(ai.uid, userId, false,
                        uidCounts.getOrDefault(ai.uid, 0) > 1, protectedApp);
                result.entries.add(new Entry(pkg, pm.getApplicationLabel(ai).toString(), ai.uid,
                        running.get(ai.uid).size(), fixed, isKept(context, pkg)));
            }
            result.entries.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        } catch (Exception e) {
            android.util.Log.w("MiToolRam", "Unable to inspect running applications", e);
            result.error = context.getString(R.string.ram_load_failed);
        }
        return result;
    }
    public static void stopSelected(Context context, Set<String> packages, AtomicBoolean cancelled, StopCallback callback) {
        Context app = context.getApplicationContext();
        Set<String> requested = new HashSet<>(packages);
        WORKER.execute(() -> {
            if (cancelled.get()) return;
            Result result = new Result();
            result.before = RamBoosterHelper.getMemoryStats(app);
            Map<String, Entry> candidates = new HashMap<>();
            Snapshot current = snapshot(app);
            for (Entry entry : current.entries) candidates.put(entry.packageName, entry);
            int completed = 0;
            for (String pkg : requested) {
                if (cancelled.get()) { result.skipped += requested.size() - completed; break; }
                Entry entry = candidates.get(pkg);
                if (current.error != null) { result.failed++; result.failures.add(pkg); }
                else if (!RamCleanupPolicy.validPackage(pkg) || entry == null || !entry.selectable()) result.skipped++;
                else {
                    try {
                        // Recheck protection immediately before each operation, including concurrent preference changes.
                        if (!eligibleNow(app, entry) || !processes().containsKey(entry.uid)) { result.skipped++; }
                        else {
                            ShizukuHelper.ShellResult stop = ShizukuHelper.executeCommand("am force-stop --user "
                                    + (android.os.Process.myUid() / 100000) + " " + pkg);
                            boolean absent = !processes().containsKey(entry.uid);
                            if (stop.success && absent) result.stopped++;
                            else { result.failed++; result.failures.add(entry.label); }
                        }
                    } catch (Exception e) { result.failed++; result.failures.add(entry.label); }
                }
                final int progress = ++completed;
                MAIN.post(() -> callback.onProgress(progress, requested.size()));
            }
            result.after = RamBoosterHelper.getMemoryStats(app);
            MAIN.post(() -> callback.onComplete(result));
        });
    }
}
