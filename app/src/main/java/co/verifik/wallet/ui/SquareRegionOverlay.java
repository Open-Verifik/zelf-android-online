package co.verifik.wallet.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Shows a transparent overlay with a hole in the middle
 */
public class SquareRegionOverlay extends View {
    private Paint mSemiBlackPaint;
    private Paint squarePaint;

    private final Path mPath = new Path();

    private Integer borderColor = Color.parseColor("#FFFFFFFF");

    private final Float borderStrokeWidth = 10f;

    int width;
    int height;

    int rectWidth;
    int rectHeight;

    RectF lastFace;

    public SquareRegionOverlay(Context context) {
        super(context);
        initPaints();
    }

    public SquareRegionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public SquareRegionOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        Paint mTransparentPaint = new Paint();
        mTransparentPaint.setColor(Color.TRANSPARENT);
        mTransparentPaint.setStrokeWidth(10);

        mSemiBlackPaint = new Paint();
        mSemiBlackPaint.setColor(Color.TRANSPARENT);
        mSemiBlackPaint.setStrokeWidth(10);

        Paint mBlackPaint = new Paint();
        mBlackPaint.setColor(Color.BLACK);

        Resources r = getResources();
        int px = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, borderStrokeWidth,r.getDisplayMetrics()));

        squarePaint = new Paint();
        squarePaint.setStyle(Paint.Style.STROKE);
        squarePaint.setStrokeWidth(px);
    }

    public int getRectWidth() {
        return rectWidth;
    }

    public int getRectHeight() {
        return rectHeight;
    }

    public int getRectLongSide() {
        return Math.max(rectWidth, rectHeight);
    }

    public int getRectShortSide() {
        return Math.min(rectWidth, rectHeight);
    }

    public void setBorderColor(int color) {
        borderColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.parseColor("#44000000"));

        mPath.reset();

        width = getWidth();
        height = getHeight();

        int centerX = (int) (width / 2.0);
        int centerY = (int) (height / 2.0);

        // We want to leave a margin from the screen edge to the closest
        // part of the oval
        float margin = 0.2f;

        // In the case of landscape, we want to leave a larger margin
        // at the top and bottom, as the camera controls are at the bottom
        if(width > height) {
            //margin = 0.15f;
        }

        float left;
        float right;
        float top;
        float bottom;

        // The ratio of the width to the height of the oval
        float ratio = 1.0f;

        if(width  > height) {
            // We want a smaller margin for landscape
            margin = 0.05f;
            // leave a margin at the top and bottom
            float factor = height * margin;
            top = factor;
            bottom = height - factor;
            // Left and right should be more than top and bottom
            // So we use a factor of 0.85
            left = centerX - (bottom - top) / 2f * ratio;
            right = centerX + (bottom - top) / 2f * ratio;
        } else {
            // leave a margin at the left and right
            float factor = width * margin;
            left = factor;
            right = width - factor;
            // Top and bottom should be more than left and right
            // So we use a factor of 0.85
            top = centerY - (right - left) / 2f / ratio;
            bottom = centerY + (right - left) / 2f / ratio;
        }

        // left, top, right, bottom
        mPath.addRoundRect(left, top, right, bottom, 80, 80, Path.Direction.CW);

        rectWidth = (int) (right - left);
        rectHeight = (int) (bottom - top);

        squarePaint.setColor(borderColor);

        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawPath(mPath, mSemiBlackPaint);
        canvas.clipPath(mPath);
        canvas.drawColor(Color.parseColor("#80FFFFFF"));
        canvas.drawRoundRect(left, top, right, bottom, 80, 80, squarePaint);
    }
}



