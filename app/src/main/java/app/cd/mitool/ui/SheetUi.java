package app.cd.mitool.ui;

import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import app.cd.mitool.R;

public final class SheetUi {
    private SheetUi() {}
    public static void configure(BottomSheetDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
                behavior.setMaxHeight((int) (dialog.getContext().getResources().getDisplayMetrics().heightPixels * 0.92f));
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
            View close = dialog.findViewById(R.id.sheet_close);
            if (close != null) close.setOnClickListener(v -> dialog.dismiss());
            if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });
    }
}
