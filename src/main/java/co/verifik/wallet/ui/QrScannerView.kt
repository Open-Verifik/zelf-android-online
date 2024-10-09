package co.verifik.wallet.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import java.lang.Integer.max
import java.lang.Math.min

/**
 * QrScannerView is a custom view that displays a qr scanner view
 */
class QrScannerView(context: Context) : View(context) {
    /**
     * Paints to draw qr scanner view
     * @param mBackgroundPaint - paint to draw background
     * @param mBorderPaint - paint to draw border
     * @param mPath - path to draw qr scanner view
     * @param mStrokePath - path to draw center line
     * @param mStrokePaint - paint to draw center line
     * @param mCornerPaint - paint to draw corner line
     * animator - animator to show and hide center line
     * isStrokeVisible - boolean to check if center line is visible
     * cornerOffset - offset to draw corner line
     */
    private val mBackgroundPaint = Paint()
    private val mBorderPaint = Paint()
    private val mPath = Path()
    private val mStrokePath = Path()
    private val mStrokePaint = Paint()
    private val mCornerPaint = Paint()
    private lateinit var animator: android.animation.ValueAnimator
    private var isStrokeVisible = true
    private lateinit var roi: Rect

    private var cornerOffset = 5.0f

    /**
     * init paints
     */
    init {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet) : this(context) {
        initPaints()
    }

    /**
     * init paints
     */
    private fun initPaints() {
        // Draw qr scanner view
        mBackgroundPaint.color = Color.TRANSPARENT
        mBackgroundPaint.strokeWidth = 10.0f

        mBorderPaint.color = Color.WHITE
        mBorderPaint.strokeWidth = 3.0f
        mStrokePaint.style = Paint.Style.STROKE

        // Draw center line of qr scanner view
        mStrokePaint.color = Color.WHITE
        mStrokePaint.style = Paint.Style.STROKE
        mStrokePaint.strokeWidth = 3.0f

        // Draw corner line
        mCornerPaint.color = Color.WHITE
        mCornerPaint.style = Paint.Style.STROKE
        mCornerPaint.strokeWidth = 12.0f

        // Show and hide the center line at every 500 milliseconds
        animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 500
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            isStrokeVisible = value != 0f
            invalidate()
        }
        animator.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    animator.start()
                }
            },
        )
        animator.start()
    }

    /**
     *
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mPath.reset()
        mStrokePath.reset()

        val width = width
        val height = height
        val centerX = width / 2
        val centerY: Float = (height / 2.3).toFloat()
        var marginWidth = 0.1f

        if (width > height) {
            marginWidth = 0.2f
        }

        val smallSide = min(width, height)
        val factor = smallSide * marginWidth
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float

        /**
         * if width is smaller than height, draw a square in the horizontal of the view
         */
        if (width < height) {
            left = factor
            right = width - factor
            val widthOfSquare = right - left
            top = centerY - widthOfSquare / 2
            bottom = centerY + widthOfSquare / 2
            // if width is larger than height, draw a square in the vertical of the view
        } else {
            top = factor
            bottom = height - factor
            val widthOfSquare = bottom - top
            left = centerX - widthOfSquare / 2
            right = centerX + widthOfSquare / 2
        }

        // isStrokeVisible is true, draw center line
        if (isStrokeVisible) {
            canvas.drawLine(left + cornerOffset, (top + bottom) / 2, right - cornerOffset, (top + bottom) / 2, mStrokePaint)
        }

        mPath.addRect(left, top, right, bottom, Path.Direction.CW)
        mPath.fillType = Path.FillType.INVERSE_EVEN_ODD
        canvas.drawPath(mPath, mBackgroundPaint)
        // canvas.drawPath(mPath, mBorderPaint)
        canvas.clipPath(mPath)
        canvas.drawColor(Color.parseColor("#55000000"))

        val cornerLineWidth = width * 0.1f

        // draw corner lines
        canvas.drawLine(left, top, left + cornerLineWidth, top, mCornerPaint)
        canvas.drawLine(left, top - cornerOffset, left, top + cornerLineWidth, mCornerPaint)

        canvas.drawLine(right, top, right - cornerLineWidth, top, mCornerPaint)
        canvas.drawLine(right, top - cornerOffset, right, top + cornerLineWidth, mCornerPaint)

        canvas.drawLine(left, bottom, left + cornerLineWidth, bottom, mCornerPaint)
        canvas.drawLine(left, bottom + cornerOffset, left, bottom - cornerLineWidth, mCornerPaint)

        canvas.drawLine(right, bottom, right - cornerLineWidth, bottom, mCornerPaint)
        canvas.drawLine(right, bottom + cornerOffset, right, bottom - cornerLineWidth, mCornerPaint)

        roi = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    // get the rect of the qr scanner view
    fun getRect(
        frameWidth: Int,
        frameHeight: Int,
    ): Rect {
        val viewLongSide = max(width, height)
        val frameLongSide = max(frameWidth, frameHeight)
        var scale = 1.0f

        if (viewLongSide > frameLongSide) {
            scale = frameLongSide.toFloat() / viewLongSide.toFloat()
        }

        val sideOfSquare = (roi.right - roi.left) * scale
        val centerX = frameWidth / 2
        val centerY = frameHeight / 2
        val left = (centerX - sideOfSquare / 2).toInt()
        val top = (centerY - sideOfSquare / 2).toInt()
        val right = (centerX + sideOfSquare / 2).toInt()
        val bottom = (centerY + sideOfSquare / 2).toInt()
        return Rect(left, top, right, bottom)
    }
}
