package co.verifik.wallet.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import co.verifik.wallet.R
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.utils.getScreenHeight
import co.verifik.wallet.utils.getScreenWidth

/**
 * Shows a transparent overlay with a hole in the middle
 */
class FaceCircleOverlayView : View {
    /**
     * Paint to draw the semi-transparent black overlay
     */
    private var mSemiBlackPaint: Paint? = null

    /**
     * Path to draw the circle
     */
    private val mPath = Path()

    /**
     * Center x coordinate of the circle
     */
    var centerX = 0

    /**
     * Center y coordinate of the circle
     */
    var centerY = 0

    /**
     * Width of the circle
     */
    var circleWidth = 0f

    /**
     * Height of the circle
     */
    var circleHeight = 0f

    /**
     * Left coordinate of the circle
     */
    var left: Float = 0f

    /**
     * Right coordinate of the circle
     */
    var right: Float = 0f

    /**
     * Top coordinate of the circle
     */
    var top: Float = 0f

    /**
     * Bottom coordinate of the circle
     */
    var bottom: Float = 0f

    /**
     * Diameter of the circle
     */
    var diameter: Float = 0f

    /**
     * Constructor
     *
     * @param context
     */
    constructor(context: Context?) : super(context) {
        initPaints()
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initPaints()
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        initPaints()
    }

    /**
     * Initialize paints
     */
    private fun initPaints() {
        mSemiBlackPaint = Paint()
        mSemiBlackPaint!!.setColor(Color.TRANSPARENT)
        mSemiBlackPaint!!.strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#1A000000"))
        mPath.reset()
        val widthCircle = width
        val heightCircle = height
        centerX = (widthCircle / 2.0).toInt()
        centerY = (heightCircle / 2.5).toInt()
        // We want to leave a margin from the screen edge to the closest
        // part of the oval
        var margin = 0.15f
        // In the case of landscape, we want to leave a larger margin
        // at the top and bottom, as the camera controls are at the bottom

        // The ratio of the width to the height of the oval
        if (width > height) {
            // We want a smaller margin for landscape
            margin = 0.4f
            // leave a margin at the top and bottom
            val factor = height * margin
            top = factor
            bottom = height - factor
            diameter = bottom - top
            // Ensure left and right give us a perfect circle
            left = centerX - diameter / 2f
            right = centerX + diameter / 2f
        } else {
            // leave a margin at the left and right
            val factor = width * margin
            left = factor
            right = width - factor
            // Calculate the diameter of the circle
            val diameter = right - left
            // Ensure top and bottom give us a perfect circle
            top = centerY - diameter / 2f
            bottom = centerY + diameter / 2f
        }

        // left, top, right, bottom
        mPath.addOval(left, top, right, bottom, Path.Direction.CW)
        circleWidth = right - left
        circleHeight = bottom - top
        mPath.fillType = Path.FillType.INVERSE_EVEN_ODD
        canvas.clipPath(mPath)
        // set background white color
        canvas.drawColor(context.getColor(R.color.zBackgroundCamera))
    }
}
