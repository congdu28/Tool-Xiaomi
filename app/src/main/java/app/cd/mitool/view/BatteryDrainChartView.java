package app.cd.mitool.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.cd.mitool.util.BatteryHistoryHelper;

/**
 * Beautiful 24-Hour Battery Drain History Bezier Chart.
 * Designed natively for Xiaomi HyperOS with smooth curves, gradient fill, and interactive tooltips.
 */
public class BatteryDrainChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int primaryLineColor;
    private int fillStartColor;
    private int chargingDotColor;

    private final Path curvePath = new Path();
    private final Path fillPath = new Path();

    private List<BatteryHistoryHelper.BatteryPoint> points = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private float touchX = -1;
    private int selectedIndex = -1;

    public BatteryDrainChartView(Context context) {
        super(context);
        init();
    }

    public BatteryDrainChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryDrainChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void applyThemeColors() {
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (isNight) {
            gridPaint.setColor(Color.parseColor("#22FFFFFF"));
            textPaint.setColor(Color.parseColor("#9E9EA7"));
            primaryLineColor = Color.parseColor("#00E5FF");
            fillStartColor = Color.parseColor("#3800E5FF");
            chargingDotColor = Color.parseColor("#FFD600");
            warnPaint.setColor(Color.parseColor("#FF5252"));
            tooltipBgPaint.setColor(Color.parseColor("#E61E212D"));
        } else {
            gridPaint.setColor(Color.parseColor("#E2E8F0"));
            textPaint.setColor(Color.parseColor("#64748B"));
            primaryLineColor = Color.parseColor("#0284C7");
            fillStartColor = Color.parseColor("#260284C7");
            chargingDotColor = Color.parseColor("#B45309");
            warnPaint.setColor(Color.parseColor("#DC2626"));
            tooltipBgPaint.setColor(Color.parseColor("#F00F172A"));
        }
        linePaint.setColor(primaryLineColor);
        dotPaint.setColor(primaryLineColor);
        tooltipTextPaint.setColor(Color.WHITE);
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(density * 1f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{density * 4f, density * 4f}, 0));

        textPaint.setTextSize(density * 9f);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(density * 2.5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStyle(Paint.Style.FILL);
        warnPaint.setStyle(Paint.Style.FILL);
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint.setTextSize(density * 10f);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        applyThemeColors();
    }

    public void setData(List<BatteryHistoryHelper.BatteryPoint> newPoints) {
        this.points = newPoints != null ? new ArrayList<>(newPoints) : new ArrayList<BatteryHistoryHelper.BatteryPoint>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        applyThemeColors();

        float density = getResources().getDisplayMetrics().density;
        float padLeft = density * 34f;
        float padRight = density * 16f;
        float padTop = density * 16f;
        float padBottom = density * 22f;

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;
        if (chartW <= 0 || chartH <= 0) return;

        // 1. Draw horizontal grid lines & labels (100%, 75%, 50%, 25%, 0%)
        int[] levels = {100, 75, 50, 25, 0};
        for (int lvl : levels) {
            float y = padTop + chartH * (1f - (lvl / 100f));
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
            if (lvl == 100 || lvl == 50 || lvl == 0) {
                canvas.drawText(lvl + "%", density * 6f, y + density * 3f, textPaint);
            }
        }

        if (points == null || points.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (24 * 3600 * 1000L);

        // 2. Map data points to canvas coordinates
        List<Float> xList = new ArrayList<>();
        List<Float> yList = new ArrayList<>();
        List<BatteryHistoryHelper.BatteryPoint> visiblePoints = new ArrayList<>();

        for (BatteryHistoryHelper.BatteryPoint p : points) {
            if (p.timestamp >= windowStart - 3600000L) { // Include nearby margin
                float fraction = (p.timestamp - windowStart) / (float) (24 * 3600 * 1000L);
                fraction = Math.max(0f, Math.min(1f, fraction));
                float x = padLeft + (fraction * chartW);
                float y = padTop + chartH * (1f - (p.level / 100f));

                xList.add(x);
                yList.add(y);
                visiblePoints.add(p);
            }
        }

        if (xList.size() < 2) {
            return;
        }

        // 3. Draw bottom time labels (-20h, -14h, -8h, Bây giờ)
        float yLabel = h - density * 6f;
        canvas.drawText("-24h", padLeft, yLabel, textPaint);
        canvas.drawText("-16h", padLeft + chartW * 0.33f - density * 10f, yLabel, textPaint);
        canvas.drawText("-8h", padLeft + chartW * 0.66f - density * 8f, yLabel, textPaint);
        canvas.drawText("Bây giờ", w - padRight - density * 28f, yLabel, textPaint);

        // 4. Construct smooth cubic Bezier path
        curvePath.reset();
        fillPath.reset();

        float startX = xList.get(0);
        float startY = yList.get(0);
        curvePath.moveTo(startX, startY);
        fillPath.moveTo(startX, padTop + chartH);
        fillPath.lineTo(startX, startY);

        for (int i = 0; i < xList.size() - 1; i++) {
            float x0 = xList.get(i);
            float y0 = yList.get(i);
            float x1 = xList.get(i + 1);
            float y1 = yList.get(i + 1);

            float cx1 = x0 + (x1 - x0) / 2f;
            float cy1 = y0;
            float cx2 = x0 + (x1 - x0) / 2f;
            float cy2 = y1;

            curvePath.cubicTo(cx1, cy1, cx2, cy2, x1, y1);
            fillPath.cubicTo(cx1, cy1, cx2, cy2, x1, y1);
        }

        float lastX = xList.get(xList.size() - 1);
        fillPath.lineTo(lastX, padTop + chartH);
        fillPath.close();

        // 5. Fill gradient under the curve
        fillPaint.setShader(new LinearGradient(
                0, padTop,
                0, padTop + chartH,
                fillStartColor,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);

        // 6. Draw stroke curve
        canvas.drawPath(curvePath, linePaint);

        // 7. Draw dots and highlight steep drain points
        for (int i = 0; i < visiblePoints.size(); i++) {
            BatteryHistoryHelper.BatteryPoint p = visiblePoints.get(i);
            float x = xList.get(i);
            float y = yList.get(i);

            // Check if rapid drop
            boolean isSteepDrop = false;
            if (i > 0) {
                BatteryHistoryHelper.BatteryPoint prev = visiblePoints.get(i - 1);
                int drop = prev.level - p.level;
                long dur = p.timestamp - prev.timestamp;
                if (!prev.isCharging && !p.isCharging && drop >= 5 && dur <= 3 * 3600 * 1000L) {
                    isSteepDrop = true;
                }
            }

            if (isSteepDrop) {
                canvas.drawCircle(x, y, density * 5f, warnPaint);
                canvas.drawCircle(x, y, density * 2.5f, dotPaint);
            } else if (p.isCharging) {
                dotPaint.setColor(chargingDotColor);
                canvas.drawCircle(x, y, density * 3f, dotPaint);
                dotPaint.setColor(primaryLineColor);
            } else if (i == visiblePoints.size() - 1) {
                // Latest current point
                canvas.drawCircle(x, y, density * 4f, dotPaint);
            }
        }

        // 8. Draw interactive touch tooltip if user touches the chart
        if (selectedIndex >= 0 && selectedIndex < visiblePoints.size()) {
            float sx = xList.get(selectedIndex);
            float sy = yList.get(selectedIndex);
            BatteryHistoryHelper.BatteryPoint sp = visiblePoints.get(selectedIndex);

            // Vertical guide line
            canvas.drawLine(sx, padTop, sx, padTop + chartH, linePaint);

            // Tooltip box
            String timeStr = timeFormat.format(new Date(sp.timestamp));
            String status = sp.isCharging ? "Sạc" : "Pin";
            String tooltipText = timeStr + " • " + sp.level + "% (" + status + ")";

            float textW = tooltipTextPaint.measureText(tooltipText);
            float boxW = textW + density * 16f;
            float boxH = density * 22f;

            float boxLeft = Math.max(padLeft, Math.min(w - padRight - boxW, sx - (boxW / 2f)));
            float boxTop = Math.max(0, sy - boxH - density * 8f);

            RectF boxRect = new RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH);
            canvas.drawRoundRect(boxRect, density * 6f, density * 6f, tooltipBgPaint);
            canvas.drawText(tooltipText, boxRect.centerX(), boxRect.centerY() + density * 4f, tooltipTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (points == null || points.isEmpty()) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchX = event.getX();
                updateSelectedIndex();
                invalidate();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        selectedIndex = -1;
                        invalidate();
                    }
                }, 2500);
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateSelectedIndex() {
        if (points == null || points.isEmpty() || touchX < 0) {
            selectedIndex = -1;
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float padLeft = density * 34f;
        float padRight = density * 16f;
        float chartW = getWidth() - padLeft - padRight;
        if (chartW <= 0) return;

        long now = System.currentTimeMillis();
        long windowStart = now - (24 * 3600 * 1000L);

        int bestIndex = -1;
        float minDiff = Float.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            BatteryHistoryHelper.BatteryPoint p = points.get(i);
            float fraction = (p.timestamp - windowStart) / (float) (24 * 3600 * 1000L);
            float x = padLeft + (fraction * chartW);
            float diff = Math.abs(x - touchX);
            if (diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        selectedIndex = bestIndex;
    }
}
