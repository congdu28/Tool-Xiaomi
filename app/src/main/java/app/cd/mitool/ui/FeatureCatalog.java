package app.cd.mitool.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import app.cd.mitool.R;

/** Binds existing feature actions to one consistent, searchable catalog. */
public final class FeatureCatalog {
    private FeatureCatalog() {}
    public static void bind(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.layout_tool_groups);
        List<View> groups = new ArrayList<>();
        addGroup(activity, container, groups, R.string.group_connections, new int[][] {
                {R.id.card_tool_notif_fix,R.id.tv_tool_notif_fix_sub,R.string.feature_notifications,R.string.feature_notifications_desc,R.drawable.ic_feature_notifications},
                {R.id.card_tool_private_dns,R.id.tv_tool_private_dns_sub,R.string.feature_dns,R.string.feature_dns_desc,R.drawable.ic_feature_dns},
                {R.id.card_tool_volte,R.id.tv_tool_volte_sub,R.string.feature_calls,R.string.feature_calls_desc,R.drawable.ic_feature_calls}});
        addGroup(activity, container, groups, R.string.group_display, new int[][] {
                {R.id.card_tool_hz,R.id.tv_tool_hz_sub,R.string.feature_display,R.string.feature_display_desc,R.drawable.ic_feature_display},
                {R.id.card_tool_clock,R.id.tv_tool_clock_sub,R.string.feature_clock,R.string.feature_clock_desc,R.drawable.ic_feature_clock}});
        addGroup(activity, container, groups, R.string.group_battery, new int[][] {
                {R.id.card_tool_battery_alert,R.id.tv_tool_battery_alert_sub,R.string.feature_alerts,R.string.feature_alerts_desc,R.drawable.ic_feature_battery},
                {R.id.card_tool_ram,R.id.tv_tool_ram_sub,R.string.feature_memory,R.string.feature_memory_desc,R.drawable.ic_feature_memory}});
        addGroup(activity, container, groups, R.string.group_advanced, new int[][] {
                {R.id.card_tool_game_fps,R.id.tv_tool_game_fps_sub,R.string.feature_gaming,R.string.feature_gaming_desc,R.drawable.ic_feature_gaming},
                {R.id.card_tool_debloater,R.id.tv_tool_debloater_sub,R.string.feature_packages,R.string.feature_packages_desc,R.drawable.ic_feature_packages},
                {R.id.card_tool_miui_utils,R.id.tv_tool_miui_utils_sub,R.string.feature_system,R.string.feature_system_desc,R.drawable.ic_feature_system}});
        EditText search = activity.findViewById(R.id.et_tool_search);
        TextView empty = activity.findViewById(R.id.tv_tool_search_empty);
        activity.findViewById(R.id.btn_tool_search_clear).setOnClickListener(v -> search.setText(""));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void afterTextChanged(Editable value) {
                activity.findViewById(R.id.btn_tool_search_clear).setVisibility(value.length() > 0 ? View.VISIBLE : View.GONE);
                int visible = 0;
                for (View group : groups) {
                    LinearLayout rows = (LinearLayout) group;
                    int matches = 0;
                    for (int i = 1; i < rows.getChildCount(); i++) {
                        View row = rows.getChildAt(i);
                        boolean match = FeatureSearch.matches(value.toString(), (String) row.getTag());
                        row.setVisibility(match ? View.VISIBLE : View.GONE);
                        if (match) matches++;
                    }
                    group.setVisibility(matches > 0 ? View.VISIBLE : View.GONE);
                    visible += matches;
                }
                empty.setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {}
        });
    }
    private static void addGroup(Activity activity, LinearLayout root, List<View> groups, int title, int[][] items) {
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        TextView heading = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_feature_heading, group, false);
        heading.setText(title);
        group.addView(heading);
        for (int[] item : items) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_feature, group, false);
            row.setId(item[0]);
            TextView name = row.findViewById(R.id.feature_name);
            TextView description = row.findViewById(R.id.feature_description);
            name.setText(item[2]); description.setText(item[3]); description.setId(item[1]);
            ImageView icon = row.findViewById(R.id.feature_icon);
            icon.setImageResource(item[4]);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.mi_orange)));
            row.setTag(activity.getString(item[2]) + " " + activity.getString(item[3]) + " " + activity.getString(title));
            group.addView(row);
        }
        root.addView(group); groups.add(group);
    }
}
