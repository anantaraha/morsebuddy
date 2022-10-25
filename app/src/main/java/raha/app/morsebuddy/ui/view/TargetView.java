package raha.app.morsebuddy.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import raha.app.morsebuddy.util.Constants;

public class TargetView extends View {
    private static final int COLOR_LINE = Color.WHITE;
    private static final float AIM_TARGET_RATIO = 0.25f;

    private Path mPath;
    private Paint mPaint;

    public TargetView(Context context) {
        this(context, null);
    }

    public TargetView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TargetView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TargetView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(COLOR_LINE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float size = width * Constants.TARGET_SAMPLE_RATIO;
        float aimSize = size * AIM_TARGET_RATIO;

        float centerX = width / 2f;
        float centerY = height / 2f;

        float targetLeft = (width - size) / 2f;
        float targetTop = (height - size) / 2f;
        float targetRight = (width + size) / 2f;
        float targetBottom = (height + size) / 2f;
        float aimLeft = (width - aimSize) / 2f;
        float aimTop = (height - aimSize) / 2f;
        float aimRight = (width + aimSize) / 2f;
        float aimBottom = (height + aimSize) / 2f;


        mPath.reset();
        mPath.moveTo(0f, centerY);
        mPath.lineTo(targetLeft, centerY);
        mPath.moveTo(targetRight, centerY);
        mPath.lineTo(width, centerY);
        mPath.moveTo(centerX, 0);
        mPath.lineTo(centerX, targetTop);
        mPath.moveTo(centerX, targetBottom);
        mPath.lineTo(centerX, height);

        mPath.moveTo(aimLeft, centerY);
        mPath.lineTo(aimRight, centerY);
        mPath.moveTo(centerX, aimTop);
        mPath.lineTo(centerX, aimBottom);

        canvas.drawRect(targetLeft, targetTop, targetRight, targetBottom, mPaint);
        canvas.drawPath(mPath, mPaint);
    }
}
