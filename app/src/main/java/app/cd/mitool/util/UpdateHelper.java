package app.cd.mitool.util;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateHelper {

    private static final String GITHUB_RELEASE_API = "https://api.github.com/repos/congdu28/Tool-Xiaomi/releases/latest";
    private static final String CURRENT_VERSION = "1.0.19";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface CheckCallback {
        void onResult(boolean hasUpdate, String latestTag, String changelog, String downloadUrl);
    }

    public static void checkForUpdate(final Context context, final boolean isManualCheck) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(GITHUB_RELEASE_API);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    conn.setRequestProperty("User-Agent", "MiTool-Android");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        final String tagName = json.optString("tag_name", "");
                        final String body = json.optString("body", "Không có thông tin thay đổi.");
                        String apkUrl = null;

                        JSONArray assets = json.optJSONArray("assets");
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                String name = asset.optString("name", "");
                                if (name.contains("Release") && name.endsWith(".apk")) {
                                    apkUrl = asset.optString("browser_download_url", null);
                                    break;
                                }
                            }
                            // Fallback to any apk
                            if (apkUrl == null) {
                                for (int i = 0; i < assets.length(); i++) {
                                    JSONObject asset = assets.getJSONObject(i);
                                    String name = asset.optString("name", "");
                                    if (name.endsWith(".apk")) {
                                        apkUrl = asset.optString("browser_download_url", null);
                                        break;
                                    }
                                }
                            }
                        }

                        final String finalApkUrl = apkUrl;
                        final boolean isNewer = isVersionNewer(tagName, CURRENT_VERSION);

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (isNewer && finalApkUrl != null) {
                                    showUpdateDialog(context, tagName, body, finalApkUrl);
                                } else if (isManualCheck) {
                                    Toast.makeText(context, "Bạn đang dùng phiên bản mới nhất (v" + CURRENT_VERSION + ")", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else if (isManualCheck) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Không thể kiểm tra bản cập nhật lúc này (Lỗi mạng)", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    if (isManualCheck) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Lỗi kết nối khi kiểm tra cập nhật", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    private static boolean isVersionNewer(String remoteTag, String currentVer) {
        if (remoteTag == null || remoteTag.isEmpty()) return false;
        String cleanRemote = remoteTag.replace("v", "").replace("V", "").trim();
        String cleanCurrent = currentVer.replace("v", "").replace("V", "").trim();

        String[] remoteParts = cleanRemote.split("\\.");
        String[] currentParts = cleanCurrent.split("\\.");

        int length = Math.max(remoteParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int r = i < remoteParts.length ? parseSafeInt(remoteParts[i]) : 0;
            int c = i < currentParts.length ? parseSafeInt(currentParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String formatChangelog(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Bản cập nhật tối ưu hiệu năng và sửa lỗi.";
        }
        String cleaned = raw;
        String[] cutPatterns = {
                "### 📦 Tải về",
                "### Tải về",
                "## 📦 Tải về",
                "📦 Tải về:",
                "Tải về:",
                "- **Mi-Tool-"
        };
        for (String pattern : cutPatterns) {
            int idx = cleaned.indexOf(pattern);
            if (idx != -1) {
                cleaned = cleaned.substring(0, idx).trim();
            }
        }
        cleaned = cleaned.replaceAll("^##?\\s+.*\\n+", "").trim();
        return cleaned;
    }

    private static void showUpdateDialog(final Context context, final String newVersion, String changelog, final String downloadUrl) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }

        String formattedChangelog = formatChangelog(changelog);

        new MaterialAlertDialogBuilder(context)
                .setTitle("🎉 Có bản cập nhật mới (" + newVersion + ")")
                .setMessage(formattedChangelog)
                .setPositiveButton("Tải & Cài đặt ngay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkInstallPermissionAndDownload(context, downloadUrl, "Mi-Tool-" + newVersion + ".apk");
                    }
                })
                .setNegativeButton("Để sau", null)
                .show();
    }

    private static void checkInstallPermissionAndDownload(Context context, String downloadUrl, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(context, "Vui lòng cho phép cài đặt ứng dụng từ nguồn này để tiếp tục.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                return;
            }
        }
        startDownloadWithManager(context, downloadUrl, fileName);
    }

    private static void startDownloadWithManager(final Context context, String downloadUrl, final String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Đang tải Mi Tool " + fileName);
            request.setDescription("Đang tải bản cập nhật mới từ GitHub");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);

            final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                final long downloadId = dm.enqueue(request);
                Toast.makeText(context, "Đang tải bản cập nhật... Vui lòng theo dõi thanh thông báo.", Toast.LENGTH_SHORT).show();

                // Register receiver to trigger installer once finished
                BroadcastReceiver onComplete = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, Intent intent) {
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        if (id == downloadId) {
                            try {
                                context.unregisterReceiver(this);
                            } catch (Exception ignored) {
                            }
                            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                            installApk(context, file);
                        }
                    }
                };

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                }
            }
        } catch (Exception e) {
            // Fallback: open browser
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            } catch (Exception ignored) {
            }
        }
    }

    public static void installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(context, "Không tìm thấy file cài đặt đã tải về.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(contentUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(install);
        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở trình cài đặt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
