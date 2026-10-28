package app.cd.mitool.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import app.cd.mitool.R;
import app.cd.mitool.util.RunningAppsHelper;
import app.cd.mitool.util.ShizukuHelper;

public final class RamCleanerUi {
    private RamCleanerUi() {}
    public static void show(Activity activity, Runnable onMemoryChanged) {
        new Panel(activity, onMemoryChanged).show();
    }
    private static final class Panel {
        final Activity activity;
        final Runnable onMemoryChanged;
        final BottomSheetDialog sheet;
        final LinearLayout list;
        final TextView status, result;
        final MaterialButton reload, connect, selectAll, stop;
        final Set<String> selected = new HashSet<>();
        final List<RunningAppsHelper.Entry> entries = new ArrayList<>();
        boolean busy;
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        Panel(Activity activity, Runnable onMemoryChanged) {
            this.activity = activity; this.onMemoryChanged = onMemoryChanged;
            sheet = new BottomSheetDialog(activity);
            View root = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_running_apps, null);
            sheet.setContentView(root); SheetUi.configure(sheet);
            sheet.setOnDismissListener(dialog -> cancelled.set(true));
            list = root.findViewById(R.id.ram_app_list); status = root.findViewById(R.id.ram_status);
            result = root.findViewById(R.id.ram_result); reload = root.findViewById(R.id.ram_reload);
            connect = root.findViewById(R.id.ram_connect); selectAll = root.findViewById(R.id.ram_select_all);
            stop = root.findViewById(R.id.ram_stop_selected);
            reload.setOnClickListener(v -> load());
            connect.setOnClickListener(v -> {
                if (!ShizukuHelper.isShizukuRunning()) {
                    if (ShizukuHelper.isShizukuInstalled(activity)) ShizukuHelper.openShizukuApp(activity);
                    else ShizukuHelper.openShizukuDownload(activity);
                } else if (!ShizukuHelper.hasShizukuPermission()) ShizukuHelper.requestShizukuPermission(activity);
                else load();
            });
            selectAll.setOnClickListener(v -> {
                boolean all = allSelected(); selected.clear();
                if (!all) for (RunningAppsHelper.Entry e : entries) if (e.selectable()) selected.add(e.packageName);
                render();
            });
            stop.setOnClickListener(v -> confirm());
        }
        boolean alive() { return !activity.isFinishing() && !activity.isDestroyed() && sheet.isShowing(); }
        void show() { sheet.show(); load(); }
        void load() {
            if (busy || !alive()) return;
            setBusy(true); status.setText(R.string.ram_loading);
            RunningAppsHelper.load(activity, snapshot -> {
                if (!alive()) return;
                entries.clear(); entries.addAll(snapshot.entries);
                Set<String> allowed = new HashSet<>();
                for (RunningAppsHelper.Entry e : entries) if (e.selectable()) allowed.add(e.packageName);
                selected.retainAll(allowed);
                setBusy(false);
                connect.setVisibility(ShizukuHelper.hasShizukuPermission() ? View.GONE : View.VISIBLE);
                if (snapshot.error != null) status.setText(snapshot.error);
                else if (entries.isEmpty()) status.setText(R.string.ram_empty);
            });
        }
        boolean allSelected() {
            int count = 0;
            for (RunningAppsHelper.Entry e : entries) if (e.selectable()) count++;
            return count > 0 && selected.size() == count;
        }
        void updateSelection() {
            status.setText(activity.getString(R.string.ram_list_status, entries.size(), selected.size()));
            stop.setText(activity.getString(R.string.ram_stop_selected, selected.size()));
            stop.setEnabled(!busy && !selected.isEmpty());
            selectAll.setText(allSelected() ? R.string.ram_deselect_all : R.string.ram_select_all);
            boolean selectable = false;
            for (RunningAppsHelper.Entry e : entries) if (e.selectable()) selectable = true;
            selectAll.setEnabled(!busy && selectable);
        }
        void render() {
            list.removeAllViews();
            int pad = (int) (12 * activity.getResources().getDisplayMetrics().density);
            for (RunningAppsHelper.Entry e : entries) {
                LinearLayout row = new LinearLayout(activity); row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(pad, pad, pad, pad); row.setBackgroundResource(R.drawable.bg_card_rounded);
                LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2); margins.bottomMargin = pad;
                list.addView(row, margins);
                MaterialCheckBox choose = new MaterialCheckBox(activity); choose.setText(e.label);
                choose.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
                choose.setMinHeight(pad * 4); choose.setEnabled(!busy && e.selectable());
                choose.setChecked(selected.contains(e.packageName)); row.addView(choose);
                TextView detail = new TextView(activity);
                detail.setText(activity.getString(R.string.ram_process_count, e.packageName, e.processCount));
                detail.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary)); detail.setTextSize(12);
                row.addView(detail);
                if (e.fixedProtection) {
                    TextView protection = new TextView(activity); protection.setText(R.string.ram_fixed_protection);
                    protection.setTextSize(12); protection.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
                    row.addView(protection);
                } else {
                    MaterialCheckBox keep = new MaterialCheckBox(activity); keep.setText(R.string.ram_keep);
                    keep.setMinHeight(pad * 4); keep.setChecked(e.kept); keep.setEnabled(!busy); row.addView(keep);
                    keep.setOnCheckedChangeListener((button, checked) -> {
                        RunningAppsHelper.setKept(activity, e.packageName, checked); e.kept = checked;
                        if (checked) { selected.remove(e.packageName); choose.setChecked(false); }
                        choose.setEnabled(!busy && e.selectable()); updateSelection();
                    });
                }
                choose.setOnCheckedChangeListener((button, checked) -> {
                    if (checked && e.selectable()) selected.add(e.packageName); else selected.remove(e.packageName);
                    updateSelection();
                });
            }
            updateSelection();
        }
        void setBusy(boolean value) {
            busy = value; reload.setEnabled(!value); connect.setEnabled(!value);
            enableChildren(list, !value); updateSelection();
            if (!value) render();
        }
        void enableChildren(View view, boolean enabled) {
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
                enableChildren(((ViewGroup) view).getChildAt(i), enabled);
        }
        void confirm() {
            if (busy || selected.isEmpty() || !alive()) return;
            Set<String> requested = new HashSet<>(selected);
            List<String> labels = new ArrayList<>();
            for (RunningAppsHelper.Entry e : entries) if (requested.contains(e.packageName)) labels.add(e.label);
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(activity.getString(R.string.ram_confirm_title, requested.size()))
                    .setMessage(activity.getString(R.string.ram_confirm_body, android.text.TextUtils.join(", ", labels)))
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.ram_confirm_stop, (dialog, which) -> run(requested)).show();
        }
        void run(Set<String> requested) {
            if (busy || !alive()) return;
            setBusy(true); result.setText("");
            RunningAppsHelper.stopSelected(activity, requested, cancelled, new RunningAppsHelper.StopCallback() {
                @Override public void onProgress(int done, int total) {
                    if (alive()) status.setText(activity.getString(R.string.ram_progress, done, total));
                }
                @Override public void onComplete(RunningAppsHelper.Result value) {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    onMemoryChanged.run();
                    if (!alive()) return;
                    String text = activity.getString(R.string.ram_result, value.stopped, value.failed, value.skipped);
                    if (value.before.totalMb > 0 && value.after.totalMb > 0)
                        text += "\n\n" + activity.getString(R.string.ram_memory_change, value.before.availMb,
                                value.after.availMb, value.after.availMb - value.before.availMb);
                    else text += "\n\n" + activity.getString(R.string.ram_memory_unknown);
                    if (!value.failures.isEmpty()) text += "\n\n" + activity.getString(R.string.ram_failed_apps,
                            android.text.TextUtils.join(", ", value.failures));
                    result.setText(text); selected.clear(); setBusy(false); load();
                }
            });
        }
    }
}
