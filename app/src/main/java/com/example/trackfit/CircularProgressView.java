package com.example.trackfit;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * A custom view that draws a circular ring progress arc,
 * matching the style shown in the TrackFit design mockup.
 *
 * Attributes (declare in res/values/attrs.xml if you want XML config):
 *   - progress       : float  0..max
 *   - maxProgress    : float  default 100
 *   - trackColor     : color  default #2A2A2A
 *   - progressColor  : color  default #00E5FF
 *   - strokeWidth    : dimen  default 14dp (large ring), 8dp (small ring)
 *   - sweepAngle     : float  default 280 (leaves a gap at the bottom like the mockup)
 */
public class CircularProgressView extends View {

    // Paint objects
    private Paint trackPaint;
    private Paint progressPaint;

    // Config
    private float progress    = 50f;
    private float maxProgress = 100f;
    private int   trackColor  = Color.parseColor("#2A2A2A");
    private int   progressColor = Color.parseColor("#00E5FF");
    private float strokeWidth = 40f;      // px — overridden by XML attr
    private float sweepDegrees = 280f;    // total arc span (leaves gap at bottom)
    private float startAngle  = 130f;     // where arc starts (bottom-left)

    private RectF ovalRect = new RectF();

    // ── Constructors ────────────────────────────────────────────────────────
    public CircularProgressView(Context context) {
        super(context);
        init(context, null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    // ── Init ────────────────────────────────────────────────────────────────
    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressView);
            progress      = a.getFloat(R.styleable.CircularProgressView_cpv_progress, progress);
            maxProgress   = a.getFloat(R.styleable.CircularProgressView_cpv_maxProgress, maxProgress);
            trackColor    = a.getColor(R.styleable.CircularProgressView_cpv_trackColor, trackColor);
            progressColor = a.getColor(R.styleable.CircularProgressView_cpv_progressColor, progressColor);
            strokeWidth   = a.getDimension(R.styleable.CircularProgressView_cpv_strokeWidth,
                    dpToPx(context, 14));
            sweepDegrees  = a.getFloat(R.styleable.CircularProgressView_cpv_sweepDegrees, sweepDegrees);
            startAngle    = a.getFloat(R.styleable.CircularProgressView_cpv_startAngle, startAngle);
            a.recycle();
        } else {
            strokeWidth = dpToPx(context, 14);
        }

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(trackColor);
        trackPaint.setStrokeWidth(strokeWidth);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeWidth(strokeWidth);
    }

    // ── Draw ────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float halfStroke = strokeWidth / 2f;
        float radius = Math.min(cx, cy) - halfStroke - getPaddingLeft();

        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // 1. Draw track (background ring)
        canvas.drawArc(ovalRect, startAngle, sweepDegrees, false, trackPaint);

        // 2. Draw progress arc
        float fraction = (maxProgress > 0) ? Math.min(progress / maxProgress, 1f) : 0f;
        float progressSweep = sweepDegrees * fraction;
        canvas.drawArc(ovalRect, startAngle, progressSweep, false, progressPaint);
    }

    // ── Setters ─────────────────────────────────────────────────────────────
    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public void setMaxProgress(float max) {
        this.maxProgress = max;
        invalidate();
    }

    public void setProgressColor(int color) {
        this.progressColor = color;
        if (progressPaint != null) {
            progressPaint.setColor(color);
            invalidate();
        }
    }

    public void setTrackColor(int color) {
        this.trackColor = color;
        if (trackPaint != null) {
            trackPaint.setColor(color);
            invalidate();
        }
    }

    public void setStrokeWidthDp(float dp) {
        this.strokeWidth = dpToPx(getContext(), dp);
        if (trackPaint != null)    trackPaint.setStrokeWidth(strokeWidth);
        if (progressPaint != null) progressPaint.setStrokeWidth(strokeWidth);
        invalidate();
    }

    // ── Helper ──────────────────────────────────────────────────────────────
    private float dpToPx(Context ctx, float dp) {
        return dp * ctx.getResources().getDisplayMetrics().density;
    }
}
