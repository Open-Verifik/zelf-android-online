package co.verifik.wallet.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import com.sensecrypt.sdk.core.ActiveFaceCaptureStateName
import com.sensecrypt.sdk.core.DirectionalScores

/**
 * Custom view to draw a circle and arc based on the direction of user
 * actions for an Active Face Capture session.
 */
class
CircleArcView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        /**
         * Animator to rotate the arc as the expected user action changes
         */
        private var directionalArcAnimator: ValueAnimator? = null

        /**
         * Paint to draw the circle and arc
         */
        private val paint: Paint = Paint()

        /**
         * Paint to draw the arc (empty color)
         */
        private val arcPaint: Paint = Paint()

        /**
         * Direction of the arc
         */
        private var direction: ActiveFaceCaptureStateName =
            ActiveFaceCaptureStateName.WAITING_FOR_FIRST_CENTERED_FACE

        /**
         * Rect to track the size of this view
         */
        private val rect: RectF = RectF(0f, 0f, 0f, 0f)

        /**
         * Rect to draw the inner circle (shrinks rect by width/10 - the stroke width of the arc)
         */
        private val innerRect = RectF(0f, 0f, 0f, 0f)

        /**
         * To draw the arc and the directional strength indicator
         */
        private val circle = RectF()

        /**
         * Start angle of the arc
         */
        private var startAngle = -22.5f

        /**
         * Directional score of the current head position
         */
        private var directionalScores = -1f

        /**
         * Color of the circle when the user should stay still
         */
        private var stayStillColor = Color.parseColor("#F9AA03")

        /**
         * Color of the circle initially
         */
        private var initialColor = Color.parseColor("#EBEBEB")

        /**
         * Offset value to set the start angle of the arc
         */
        private val offsetValue = -22.5f

        /**
         * Color of the circle currently
         */
        private var circleColor = CircleColorByState.INITIAL

        init {
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
        }

        /**
         * Enum class to define the color of the circle based on the state
         */
        enum class CircleColorByState {
            INITIAL,
            STAY_STILL,
            COMPLETED,
            EXPECTING_USER_ACTION,
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            rect.set(0f, 0f, w.toFloat(), h.toFloat())
            // Set the inner rect
            paint.strokeWidth = rect.width() / 10

            // Set the inner rect as 10% smaller than the outer rect
            innerRect.set(
                rect.left + paint.strokeWidth,
                rect.top + paint.strokeWidth,
                rect.right - paint.strokeWidth,
                rect.bottom - paint.strokeWidth,
            )
        }

        /**
         * Set the direction of the arc based on expected instruction
         */
        fun setDirection(
            direction: ActiveFaceCaptureStateName,
            directionalScore: DirectionalScores?,
        ) {
            val currentStartAngle = startAngle
            val currentDirection = this.direction
            this.direction = direction

            if (directionalArcAnimator != null) {
                directionalArcAnimator!!.cancel()
            }

            circleColor = CircleColorByState.EXPECTING_USER_ACTION

            // Based on direction pick a new start angle
            val targetStartAngle =
                when (direction) {
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_RIGHT -> offsetValue
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_RIGHT -> offsetValue + 45f
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_DOWN -> offsetValue + 45f * 2
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_LEFT -> offsetValue + 45f * 3
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_LEFT -> offsetValue + 45f * 4
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_LEFT -> offsetValue + 45f * 5
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_UP -> offsetValue + 45f * 6
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_RIGHT -> offsetValue + 45f * 7
                    else -> 0f
                }

            // get the directional score of current head position
            directionalScore?.let { directionalScore ->
                when (direction) {
                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_RIGHT -> {
                        directionalScores = directionalScore.right
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_RIGHT -> {
                        directionalScores = directionalScore.bottomRight
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_DOWN -> {
                        directionalScores = directionalScore.bottom
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_LEFT -> {
                        directionalScores = directionalScore.bottomLeft
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_LEFT -> {
                        directionalScores = directionalScore.left
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_LEFT -> {
                        directionalScores = directionalScore.topLeft
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_UP -> {
                        directionalScores = directionalScore.top
                    }

                    ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_RIGHT -> {
                        directionalScores = directionalScore.topRight
                    }

                    else -> {
                        directionalScores = -1f
                    }
                }
            } ?: run {
                directionalScores = -1f
            }

            if (currentDirection == ActiveFaceCaptureStateName.WAITING_FOR_FIRST_CENTERED_FACE) {
                startAngle = targetStartAngle
                return
            }

            // Animate the arc, but only if animator is null
            directionalArcAnimator ?: run {
                directionalArcAnimator = ValueAnimator.ofFloat(currentStartAngle, targetStartAngle)
                directionalArcAnimator!!.addUpdateListener { animation ->
                    startAngle = animation.animatedValue as Float
                    invalidate()
                }
                // When the animation ends, set the animator to null
                directionalArcAnimator!!.doOnEnd {
                    directionalArcAnimator = null
                }

                // Set the duration of the animation, this is very important
                directionalArcAnimator!!.duration = 75
                directionalArcAnimator!!.start()
            }
        }

        /**
         * Update the color of the circle
         *
         * @param color The color to set
         */
        fun updateCircleColor(color: CircleColorByState) {
            circleColor = color
            directionalScores = -1f
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Draw inner circle
            when (circleColor) {
                CircleColorByState.INITIAL -> paint.color = initialColor
                CircleColorByState.STAY_STILL -> paint.color = stayStillColor
                CircleColorByState.EXPECTING_USER_ACTION -> paint.color = Color.parseColor("#AB121C")
                CircleColorByState.COMPLETED -> paint.color = Color.parseColor("#088C3D")
            }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f

            canvas.drawOval(innerRect, paint)

            // If direction is not set, do not draw anything
            if (directionalScores == -1f) {
                return
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = rect.width() / 10
            paint.isAntiAlias = true
            paint.color = Color.parseColor("#AB121C")

            // Set the base directional guide color
            val alpha = 80
            paint.alpha = alpha

            val elementRadius = rect.width() / 2
            val radius = elementRadius - paint.strokeWidth / 2
            val centerX = rect.centerX()
            val centerY = rect.centerY()
            val left = centerX - radius
            val top: Float = centerY - radius
            val right = centerX + radius
            val bottom: Float = centerY + radius

            circle.set(left, top, right, bottom)

            // Draw the arc
            canvas.drawArc(
                circle,
                startAngle,
                45.0f,
                false,
                paint,
            )

            // Draw the arc for the directional score
            val maxStrokeWidth = rect.width() / 10
            val scoreStrokeWidth = maxStrokeWidth * directionalScores
            paint.strokeWidth = scoreStrokeWidth

            // fill the color indicator based on the direction score value
            val elementRadiusForStrength = rect.width() / 2 - maxStrokeWidth
            val radiusForStrength = elementRadiusForStrength + paint.strokeWidth / 2

            val leftForStrength = centerX - radiusForStrength
            val topForStrength: Float = centerY - radiusForStrength
            val rightForStrength = centerX + radiusForStrength
            val bottomForStrength: Float = centerY + radiusForStrength

            circle.set(leftForStrength, topForStrength, rightForStrength, bottomForStrength)

            arcPaint.style = Paint.Style.STROKE
            arcPaint.strokeWidth = scoreStrokeWidth
            arcPaint.isAntiAlias = true
            arcPaint.color = Color.parseColor("#AB121C")
            // Draw the arc
            canvas.drawArc(
                circle,
                startAngle,
                45.0f,
                false,
                arcPaint,
            )
        }
    }
