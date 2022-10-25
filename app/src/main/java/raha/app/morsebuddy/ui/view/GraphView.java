package raha.app.morsebuddy.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

public class GraphView extends View {
    private static final int COLOR_BACKGROUND = Color.BLACK;
    private static final int COLOR_WAVEFORM = Color.CYAN;
    private static final int COLOR_BASELINE = Color.RED;
    private static final int Y_MAX = 0x7F;
    private static final int Y_MIN = 0;

    private int baseline;
    private int[] valueArray;
    private Paint graphPaint;
    private Paint basePaint;
    private Path path;

    public GraphView(Context context) {
        this(context, null);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        baseline = 0;
        valueArray = new int[0];
        path = new Path();
        graphPaint = new Paint();
        basePaint = new Paint();
        graphPaint.setColor(COLOR_WAVEFORM);
        graphPaint.setStyle(Paint.Style.FILL);
        graphPaint.setStrokeWidth(0f);
        basePaint.setColor(COLOR_BASELINE);
        basePaint.setStyle(Paint.Style.STROKE);
        basePaint.setStrokeWidth(1f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(COLOR_BACKGROUND);
        float width = getWidth();
        float height = getHeight();
        path.reset();
        drawGraph(valueArray, width, height);
        canvas.drawPath(path, graphPaint);

        // Baseline
        float yIncrement = height / (Y_MAX - Y_MIN);
        float lineHeight = height - yIncrement * baseline;
        canvas.drawLine(0f, lineHeight, width, lineHeight, basePaint);
    }

    private void drawGraph(@NonNull int[] array, float width, float height) {
        float xIncrement = width / array.length;
        float yIncrement = height / (Y_MAX - Y_MIN);
        path.moveTo(0f, height);
        for (int i = 0; i < array.length; i++) {
            int value = array[i];
            float x = xIncrement * i;
            float y = height - yIncrement * value;
            path.lineTo(x, y);
            path.lineTo(x + xIncrement, y);
        }
        path.lineTo(width, height);
        path.close();
    }

    public void render(@NonNull int[] valueArray) {
        this.valueArray = valueArray;
        postInvalidate();
    }

    public void clear() {
        Arrays.fill(this.valueArray, 0);
    }

    public void setBaseLine(int value) {
        this.baseline = value;
        postInvalidate();
    }
}
