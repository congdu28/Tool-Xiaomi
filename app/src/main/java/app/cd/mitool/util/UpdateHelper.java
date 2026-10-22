package app.cd.mitool.util;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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

import app.cd.mitool.MainActivity;
import app.cd.mitool.R;

public class UpdateHelper {

    public static final String CHANNEL_ID_UPDATE = "mitool_update_channel";
    public static final int NOTIFICATION_ID_UPDATE = 8802;
    public static final String EXTRA_OPEN_UPDATE_DIALOG = "open_update_dialog";
    public static final String EXTRA_UPDATE_TAG = "update_tag";
    public static final String EXTRA_UPDATE_BODY = "update_body";
    public static final String EXTRA_UPDATE_URL = "update_url";

    private static final String GITHUB_RELEASE_API = "https://api.github.com/repos/congdu28/Tool-Xiaomi/releases/latest";

    public static String getCurrentVersion(Context context) {
        try {
            if (context != null) {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            }
        } catch (Throwable ignored) {
        }
        return "1.0.51";
    }

    public static String getCurrentVersion() {
        return "1.0.51";
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface CheckCallback {
        void onResult(boolean hasUpdate, String latestTag, String changelog, String downloadUrl);
    }

    public interface UpdateCheckListener {
        void onStart();
        void onFinished(boolean hasUpdate, boolean success);
    }

    public static void checkForUpdate(final Context context, final boolean isManualCheck) {
        checkForUpdate(context, isManualCheck, null);
    }

    public static void checkForUpdate(final Context context, final boolean isManualCheck, final UpdateCheckListener listener) {
        if (listener != null) {
            listener.onStart();
        }
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
                        final String currentVersion = getCurrentVersion(context);
                        final boolean isNewer = isVersionNewer(tagName, currentVersion);

                        // Record check timestamp
                        try {
                            if (context != null) {
                                new PreferencesHelper(context).setLastUpdateCheckTime(System.currentTimeMillis());
                            }
                        } catch (Throwable ignored) {
                        }

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onFinished(isNewer, true);
                                }
                                if (isNewer && finalApkUrl != null) {
                                    // Post System Notification in Android Notification Drawer
                                    postUpdateAvailableNotification(context, tagName, body, finalApkUrl);

                                    // If user is currently looking at the app activity, also show bottom sheet
                                    showUpdateBottomSheet(context, tagName, body, finalApkUrl);
                                } else if (isManualCheck) {
                                    showUpToDateBottomSheet(context, currentVersion);
                                }
                            }
                        });
                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onFinished(false, false);
                                }
                                if (isManualCheck) {
                                    showErrorBottomSheet(context, "Mã phản hồi từ máy chủ: " + responseCode);
                                }
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onFinished(false, false);
                            }
                            if (isManualCheck) {
                                showErrorBottomSheet(context, "Không thể kết nối đến máy chủ: " + e.getMessage());
                            }
                        }
                    });
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
            return "Bản cập nhật tối ưu hiệu năng và sửa lỗi giao diện.";
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

    /**
     * Post a prominent Android system notification when a new update is available.
     */
    public static void postUpdateAvailableNotification(Context context, String newVersion, String changelog, String downloadUrl) {
        if (context == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (context instanceof Activity) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8802);
                    }
                    return;
                }
            }

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID_UPDATE,
                        "Cập Nhật Ứng Dụng",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Thông báo khi có phiên bản Mi Tool mới");
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setShowBadge(true);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(channel);
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(EXTRA_OPEN_UPDATE_DIALOG, true);
            intent.putExtra(EXTRA_UPDATE_TAG, newVersion);
            intent.putExtra(EXTRA_UPDATE_BODY, changelog);
            intent.putExtra(EXTRA_UPDATE_URL, downloadUrl);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(context, NOTIFICATION_ID_UPDATE, intent, flags);

            String formattedVer = (newVersion != null && newVersion.startsWith("v")) ? newVersion : ("v" + newVersion);
            String title = "🎉 Có bản cập nhật mới: " + formattedVer;
            String summaryText = formatChangelog(changelog);
            if (summaryText.length() > 220) {
                summaryText = summaryText.substring(0, 217) + "...";
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_UPDATE)
                    .setSmallIcon(R.drawable.ic_app_icon)
                    .setContentTitle(title)
                    .setContentText("Bản mới đã sẵn sàng. Chạm để xem và cài đặt ngay!")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(title)
                            .bigText(summaryText + "\n\n👉 Chạm vào đây để xem chi tiết & cập nhật."))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            nm.notify(NOTIFICATION_ID_UPDATE, builder.build());
        } catch (Throwable ignored) {
        }
    }

    public static void showUpdateBottomSheet(final Context context, final String newVersion, String changelog, final String downloadUrl) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }
        final Activity activity = (Activity) context;

        final BottomSheetDialog bs = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_update, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvTitle = view.findViewById(R.id.bs_update_title);
        TextView tvSubtitle = view.findViewById(R.id.bs_update_subtitle);
        TextView tvCurrentVer = view.findViewById(R.id.bs_update_current_ver);
        TextView tvNewVer = view.findViewById(R.id.bs_update_new_ver);
        TextView tvChangelog = view.findViewById(R.id.bs_update_changelog);
        TextView tvEmoji = view.findViewById(R.id.bs_update_icon_emoji);
        MaterialButton btnAction = view.findViewById(R.id.bs_update_btn_action);
        MaterialButton btnDismiss = view.findViewById(R.id.bs_update_btn_dismiss);

        if (tvTitle != null) tvTitle.setText("🎉 Có Bản Cập Nhật Mới!");
        if (tvSubtitle != null) tvSubtitle.setText("Phiên bản " + newVersion + " đã sẵn sàng tải về");
        if (tvCurrentVer != null) tvCurrentVer.setText("v" + getCurrentVersion(activity));
        if (tvNewVer != null) tvNewVer.setText(newVersion.startsWith("v") ? newVersion : "v" + newVersion);
        if (tvChangelog != null) tvChangelog.setText(formatChangelog(changelog));
        if (tvEmoji != null) tvEmoji.setText("🚀");

        if (btnAction != null) {
            btnAction.setText("⚡ Tải & Cài Đặt Bản Cập Nhật");
            btnAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bs.dismiss();
                    checkInstallPermissionAndDownload(activity, downloadUrl, "Mi-Tool-" + newVersion + ".apk");
                }
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setText("Để sau");
            btnDismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bs.dismiss();
                }
            });
        }

        bs.show();
    }

    public static void showUpToDateBottomSheet(final Context context, final String currentVersion) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }
        final Activity activity = (Activity) context;
        final BottomSheetDialog bs = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_update, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvTitle = view.findViewById(R.id.bs_update_title);
        TextView tvSubtitle = view.findViewById(R.id.bs_update_subtitle);
        TextView tvCurrentVer = view.findViewById(R.id.bs_update_current_ver);
        TextView tvArrow = view.findViewById(R.id.bs_update_arrow);
        View newVerContainer = view.findViewById(R.id.bs_update_new_ver_container);
        TextView tvChangelogHeader = view.findViewById(R.id.bs_update_changelog_header);
        TextView tvChangelog = view.findViewById(R.id.bs_update_changelog);
        TextView tvEmoji = view.findViewById(R.id.bs_update_icon_emoji);
        TextView tvBadge = view.findViewById(R.id.bs_update_tag_badge);
        MaterialButton btnAction = view.findViewById(R.id.bs_update_btn_action);
        MaterialButton btnDismiss = view.findViewById(R.id.bs_update_btn_dismiss);

        if (tvTitle != null) tvTitle.setText("✅ Bạn Đang Dùng Bản Mới Nhất!");
        if (tvSubtitle != null) tvSubtitle.setText("Hệ thống đã được đồng bộ phiên bản mới nhất từ GitHub");
        if (tvCurrentVer != null) tvCurrentVer.setText("v" + currentVersion);
        if (tvArrow != null) tvArrow.setVisibility(View.GONE);
        if (newVerContainer != null) newVerContainer.setVisibility(View.GONE);
        if (tvBadge != null) {
            tvBadge.setText("CHÍNH THỨC");
            tvBadge.setTextColor(ContextCompat.getColor(activity, R.color.neon_cyan));
        }
        if (tvEmoji != null) tvEmoji.setText("🛡️");

        if (tvChangelogHeader != null) tvChangelogHeader.setText("📋 Trạng thái phiên bản:");
        if (tvChangelog != null) {
            tvChangelog.setText("• Ứng dụng Mi Tool đang chạy phiên bản mới nhất (v" + currentVersion + ").\n"
                    + "• Toàn bộ các công cụ tinh chỉnh HyperOS, tối ưu Pin, SOT và Kiểm tra củ cáp đều đang hoạt động tốt nhất.\n"
                    + "• Khi có phiên bản mới trên GitHub, ứng dụng sẽ tự động thông báo cho bạn.");
        }

        if (btnAction != null) {
            btnAction.setText("Tuyệt vời (Đã hiểu)");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(activity, R.color.card_highlight)));
            btnAction.setTextColor(ContextCompat.getColor(activity, R.color.neon_green));
            btnAction.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(activity, R.color.neon_green)));
            btnAction.setStrokeWidth(2);
            btnAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bs.dismiss();
                }
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setVisibility(View.VISIBLE);
            btnDismiss.setText("🔔 Bắn thông báo thử nghiệm");
            btnDismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    postUpdateAvailableNotification(
                            activity,
                            "v" + currentVersion + " (Thử Nghiệm)",
                            "• Đã kiểm tra cơ chế bắn thông báo cập nhật thành công.\n• Khi có bản cập nhật mới trên GitHub, thông báo này sẽ tự động xuất hiện.",
                            GITHUB_RELEASE_API
                    );
                    Toast.makeText(activity, "🔔 Đã bắn thông báo cập nhật lên thanh trạng thái!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        bs.show();
    }

    public static void showErrorBottomSheet(final Context context, final String errorMessage) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }
        final Activity activity = (Activity) context;
        final BottomSheetDialog bs = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_update, null);
        bs.setContentView(view);
        configureBottomSheet(bs);

        TextView tvTitle = view.findViewById(R.id.bs_update_title);
        TextView tvSubtitle = view.findViewById(R.id.bs_update_subtitle);
        View versionCard = view.findViewById(R.id.bs_update_version_card);
        TextView tvChangelogHeader = view.findViewById(R.id.bs_update_changelog_header);
        TextView tvChangelog = view.findViewById(R.id.bs_update_changelog);
        TextView tvEmoji = view.findViewById(R.id.bs_update_icon_emoji);
        TextView tvBadge = view.findViewById(R.id.bs_update_tag_badge);
        MaterialButton btnAction = view.findViewById(R.id.bs_update_btn_action);
        MaterialButton btnDismiss = view.findViewById(R.id.bs_update_btn_dismiss);

        if (tvTitle != null) tvTitle.setText("⚠️ Không Thể Kiểm Tra Cập Nhật");
        if (tvSubtitle != null) tvSubtitle.setText("Lỗi kết nối tới máy chủ GitHub Releases");
        if (versionCard != null) versionCard.setVisibility(View.GONE);
        if (tvBadge != null) {
            tvBadge.setText("LỖI MẠNG");
            tvBadge.setTextColor(ContextCompat.getColor(activity, R.color.neon_red));
        }
        if (tvEmoji != null) tvEmoji.setText("📡");

        if (tvChangelogHeader != null) tvChangelogHeader.setText("Chi tiết:");
        if (tvChangelog != null) {
            tvChangelog.setText("Không thể kết nối đến máy chủ GitHub (" + errorMessage + ").\n\n"
                    + "Vui lòng kiểm tra lại kết nối Wi-Fi hoặc 4G/5G trên thiết bị rồi thử lại.");
        }

        if (btnAction != null) {
            btnAction.setText("🔄 Thử lại ngay");
            btnAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bs.dismiss();
                    checkForUpdate(activity, true);
                }
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setText("Đóng");
            btnDismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bs.dismiss();
                }
            });
        }

        bs.show();
    }

    private static void configureBottomSheet(BottomSheetDialog bs) {
        bs.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(android.R.color.transparent);
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            }
        });
    }

    private static void showUpdateDialog(final Context context, final String newVersion, String changelog, final String downloadUrl) {
        showUpdateBottomSheet(context, newVersion, changelog, downloadUrl);
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
}
