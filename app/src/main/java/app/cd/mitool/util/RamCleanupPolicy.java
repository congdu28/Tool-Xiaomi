package app.cd.mitool.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Parses numeric ps output without inferring running state from installed packages. */
public final class RamCleanupPolicy {
    private RamCleanupPolicy() {}

    public static Map<Integer, Set<Integer>> parseProcesses(String output) {
        Map<Integer, Set<Integer>> result = new HashMap<>();
        if (output == null) return result;
        for (String line : output.split("\n")) {
            String[] fields = line.trim().split("\\s+", 3);
            if (fields.length < 3) continue;
            try {
                int uid = Integer.parseInt(fields[0]);
                int pid = Integer.parseInt(fields[1]);
                if (uid < 0 || pid <= 0) continue;
                result.computeIfAbsent(uid, key -> new HashSet<>()).add(pid);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public static boolean validPackage(String name) {
        return name != null && name.matches("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+");
    }

    public static boolean canStop(int uid, int userId, boolean system, boolean sharedUid, boolean protectedApp) {
        return uid >= 0 && uid / 100000 == userId && uid % 100000 >= 10000
                && !system && !sharedUid && !protectedApp;
    }
}
