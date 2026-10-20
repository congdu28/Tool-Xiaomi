package app.cd.mitool.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import app.cd.mitool.R;
import app.cd.mitool.util.BatteryHelper;

public class ChargingHudView {

    private static View currentHudView = null;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable dismissRunnable = null;

    public static void show(Context context, BatteryHelper.BatteryInfo info) {
        if (!Settings.canDrawOverlays(context)) {
            return;
        }

        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return;

        // Dismiss existing HUD if showing
        dismiss(windowManager);

        LayoutInflater inflater = LayoutInflater.from(context);
        final View hudView = inflater.inflate(R.layout.layout_charging_hud, null);
        currentHudView = hudView;

        TextView tvTitle = hudView.findViewById(R.id.tv_hud_title);
        TextView tvSubtitle = hudView.findViewById(R.id.tv_hud_subtitle);

        String chargeMode = info.plugTypeName != null ? info.plugTypeName : "Đang sạc";
        tvTitle.setText("⚡ " + chargeMode + " • " + info.level + "%");

        String subtitle;
        if (info.isFull) {
            subtitle = "Pin đã được sạc đầy 100%!";
        } else if (info.estimatedMinutesToFull > 0) {
            subtitle = info.formattedEta;
            if (info.powerWatt > 0) {
                subtitle += String.format(java.util.Locale.US, " • %.1fW", info.powerWatt);
            }
        } else {
            subtitle = "Đang tối ưu dòng sạc...";
        }
        tvSubtitle.setText(subtitle);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 80; // Distance from top

        // Tap to dismiss
        hudView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(windowManager);
            }
        });

        // Add view and animate in
        hudView.setAlpha(0f);
        hudView.setTranslationY(-100f);
        windowManager.addView(hudView, params);

        hudView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .start();

        // Auto dismiss after 6 seconds
        dismissRunnable = new Runnable() {
            @Override
            public void run() {
                dismiss(windowManager);
            }
        };
        handler.postDelayed(dismissRunnable, 6000);
    }

    public static void dismiss(final WindowManager windowManager) {
        if (dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
        }

        if (currentHudView != null) {
            final View viewToRemove = currentHudView;
            currentHudView = null;
            viewToRemove.animate()
                    .alpha(0f)
                    .translationY(-100f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void end(Animator animation) {
                            try {
                                windowManager.removeView(viewToRemove);
                            } catch (Exception ignored) {
                            }
                        }
                    })
                    .start();
        }
    }
}
