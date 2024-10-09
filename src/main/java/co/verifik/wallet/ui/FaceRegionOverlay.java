package co.verifik.wallet.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
/**
 * Shows a transparent overlay with a hole in the middle
 */
public class FaceRegionOverlay extends View {
    private Paint mSemiBlackPaint;
    private Paint ovalPaint;

    private final Path mPath = new Path();

    private Integer borderColor = Color.parseColor("#55000000");

    private final Float borderStrokeWidth = 30f;

    int width;
    int height;

    int ovalWidth;
    int ovalHeight;

    RectF lastFace;

    public FaceRegionOverlay(Context context) {
        super(context);
        initPaints();
    }

    public FaceRegionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public FaceRegionOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
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

        ovalPaint = new Paint();
        ovalPaint.setStyle(Paint.Style.STROKE);
        ovalPaint.setStrokeWidth(px);
        ovalPaint.setColor(borderColor);
    }

    public int getOvalWidth() {
        return ovalWidth;
    }

    public int getOvalHeight() {
        return ovalHeight;
    }

    public int getOvalLongSide() {
        return Math.max(ovalWidth, ovalHeight);
    }

    public int getOvalShortSide() {
        return Math.min(ovalWidth, ovalHeight);
    }

    public void setBorderColor(int color) {

        borderColor = color;
        ovalPaint.setColor(borderColor);
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
        float margin = 0.1f;

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
        float ratio = 0.7f;

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
        float proportion = 3f;
        float realMiddle = (bottom-top)/proportion+top;
        float middleTop = (2*(bottom-top)/proportion+top);
        float middleBottom = realMiddle-(bottom-realMiddle);

        // left, top, right, bottom
        //mPath.addOval(left, top, right, bottom, Path.Direction.CW);
        mPath.addArc(left, top, right, middleTop, 180f, 180f);
        mPath.addArc(left, middleBottom, right, bottom, 0f, 180f);
        mPath.close();

        ovalWidth = (int) (right - left);
        ovalHeight = (int) (bottom - top);

        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawPath(mPath, mSemiBlackPaint);
        canvas.clipPath(mPath);
        canvas.drawColor(Color.parseColor("#88000000"));
        //canvas.drawOval(left, top, right, bottom, ovalPaint);
        canvas.drawArc(left, top, right, middleTop, 180f, 180f, false, ovalPaint);
        canvas.drawArc(left, middleBottom, right, bottom, 0f, 180f, false, ovalPaint);
    }
}



