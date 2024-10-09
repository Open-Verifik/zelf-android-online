package co.verifik.wallet.ui

import android.animation.Animator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import co.verifik.wallet.R
import co.verifik.wallet.data.domain.ErrorDetails
import co.verifik.wallet.utils.getScreenHeight
import co.verifik.wallet.utils.getScreenWidth
import com.airbnb.lottie.LottieAnimationView
import kotlin.math.roundToInt

/**
 * Helper class to show common UI elements such as loading dialogs,
 * snackbars, etc.
 */
class UIHelper {
    companion object {
        // Whether a loading dialog is already showing
        private var isLoadingDialogShowing = false

        // Whether a snackbar is already showing
        private var isSnackbarShowing = false

        /**
         * Whether a confirmation dialog is already showing
         */
        private var isConfirmationDialogShowing = false

        /**
         * Whether an info dialog is already showing
         */
        private var isInfoDialogShowing = false

        /**
         * Whether a time out dialog is already showing
         */
        private var isTimeOutDialogShowing = false

        /**
         * Whether a move gently dialog is already showing
         */
        private var isMoveGentlyDialogShowing = false

        /**
         * Whether a liveness retry dialog is already showing
         */
        private var isLivenessRetryDialogShowing = false

        /**
         * Whether  capture does not match dialog is already showing
         */
        private var isCaptureSessionErrorDialogShowing = false

        /**
         * The lock object to synchronize access to the above variables
         */
        private val lock = Any()

        /**
         * A utility method to convert dp to pixels
         * @param dp The dp value
         * @return The pixel value
         */
        fun dpToPx(
            activity: Activity,
            dp: Float,
        ): Int {
            val displayMetrics: DisplayMetrics = activity.resources.displayMetrics
            return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
        }

        /**
         * Show a loading dialog while a lambda is running
         *
         * @param activity The activity to show the loading dialog in
         * @param textResId The resource ID of the text to show in the loading dialog
         * @param lambda The lambda to run while the loading dialog is showing
         */
        fun showLoadingDialog(
            activity: Activity,
            textResId: Int,
            inThreadLambda: () -> Unit,
            postDismissLambda: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // Ignore if the activity is
                if (activity.isFinishing) {
                    return
                }
                // It is a programming error to call this method while a loading dialog is already showing
                if (isLoadingDialogShowing) {
                    throw Exception("Loading dialog is already showing")
                }
                isLoadingDialogShowing = true
            }
            // Create the loading dialog
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(activity, R.style.TransparentDialogTheme)
            val inflater: LayoutInflater = activity.layoutInflater
            builder.setTitle(null)
            builder.setCancelable(false)
            val busyDialogTextView = inflater.inflate(R.layout.dialog_loading, null)
            builder.setView(busyDialogTextView)
            val busyDialog = builder.create()
            busyDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            busyDialog.setCancelable(false)
            val title: TextView = busyDialogTextView.findViewById(R.id.tvBusyTitle)
            title.setText(textResId)

            busyDialog.setOnDismissListener {
                synchronized(lock) {
                    isLoadingDialogShowing = false
                }
            }

            busyDialog.show()

            // Create a handler
            val handler = Handler(Looper.getMainLooper())

            // Run the lambda in a new thread
            Thread {
                // Run the lambda
                inThreadLambda()

                // Dismiss the progress dialog
                handler.post {
                    busyDialog.dismiss()
                    synchronized(lock) {
                        isLoadingDialogShowing = false
                    }
                    if (postDismissLambda != null) {
                        postDismissLambda()
                    }
                }
            }.start()
        }

        /**
         * Convert a drawable resource to a bitmap
         *
         * @param context The context
         * @param drawable The drawable resource ID
         */
        fun vectorDrawableToBitmap(
            context: Context,
            drawable: Int,
        ): Bitmap {
            val drawable = ContextCompat.getDrawable(context, drawable) as VectorDrawable
            val bitmap =
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888,
                )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, getScreenWidth(), getScreenHeight() + 350)
            drawable.draw(canvas)
            return bitmap
        }

        /**
         * Gets the text for a particular resource ID
         *
         * @param activity The activity
         * @param id The resource ID
         */
        fun getInstructionText(
            activity: Activity,
            id: Any,
        ): String {
            val id = id as Int
            return activity.getString(id)
        }

        /**
         * A utility method that shows a snackbar with a message.
         * @param activity The activity to show the snackbar in
         * @param view The view to which the snackbar should be attached
         * @param textId The message of the snackbar
         * @param colorId The color of the snackbar
         * @param delayMs The delay in milliseconds after which the snackbar should be dismissed.
         * Setting this value means that the no new snackbar will be shown until the
         * current snackbar is dismissed.
         */
        @Synchronized
        fun showSnackBar(
            activity: Activity,
            view: View,
            textId: Int,
            colorId: Int,
            delayMs: Int,
        ) {
            showSnackBar(activity, view, textId, colorId, delayMs, 0f)
        }

        /**
         * A utility method that shows a snackbar with a message.
         * @param activity The activity to show the snackbar in
         * @param view The view to which the snackbar should be attached
         * @param textId The message of the snackbar
         * @param colorId The color of the snackbar
         * @param delayMs The delay in milliseconds after which the snackbar should be dismissed.
         * @param marginDp The bottom margin in dp to be applied to the snackbar.
         * Setting this value means that the no new snackbar will be shown until the
         * current snackbar is dismissed.
         */
        fun showSnackBar(
            activity: Activity,
            view: View,
            textId: Int,
            colorId: Int,
            delayMs: Int,
            marginDp: Float,
        ) {
            synchronized(lock) {
                // Ignore if the activity is finishing
                if (activity.isFinishing) {
                    return
                }
                // For snackbars, we just return if one is already showing
                if (isSnackbarShowing) {
                    return
                }
                isSnackbarShowing = true
            }

            val snackBar = Snackbar.make(view, textId, Snackbar.LENGTH_LONG)
            val snackBarView = snackBar.view

            if (!marginDp.equals(0f)) {
                // Convert the dp value to pixels
                val bottomMarginInPixels = dpToPx(activity, marginDp)
                val params = snackBarView.layoutParams as FrameLayout.LayoutParams
                params.setMargins(
                    params.leftMargin,
                    params.topMargin,
                    params.rightMargin,
                    params.bottomMargin + bottomMarginInPixels,
                )
            }

            ViewCompat.setElevation(snackBarView, 12f)

            // Set background color
            snackBarView.setBackgroundColor(activity.getColor(colorId))

            val params = snackBarView.layoutParams as FrameLayout.LayoutParams

            val tv =
                snackBarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView

            params.setMargins(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin + 52,
            )

            // Replace with android.support.design.R.id.snackbar_text if you are not using androidX
            tv.gravity = Gravity.CENTER
            tv.textAlignment = View.TEXT_ALIGNMENT_GRAVITY

            snackBarView.layoutParams = params

            if (delayMs.equals(0f)) {
                snackBar.addCallback(
                    object : Snackbar.Callback() {
                        override fun onDismissed(
                            transientBottomBar: Snackbar,
                            event: Int,
                        ) {
                            // To avoid a flood of snackbar messages, wait for the snackbar to be dismissed
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                synchronized(lock) {
                                    isSnackbarShowing = false
                                }
                            }, delayMs.toLong())
                        }
                    },
                )
            } else {
                snackBar.addCallback(
                    object : Snackbar.Callback() {
                        override fun onDismissed(
                            transientBottomBar: Snackbar,
                            event: Int,
                        ) {
                            synchronized(lock) {
                                isSnackbarShowing = false
                            }
                        }
                    },
                )
            }
            snackBar.show()
        }

        /**
         * This method is used to request for camera permission.
         */
        fun requestCameraPermission(
            activity: Activity,
            viewCamera: View,
        ) {
            val snackbarMultiplePermissionsListener =
                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                    .with(viewCamera, R.string.permissions_needed)
                    .withOpenSettingsButton(activity.getString(R.string.settings))
                    .withCallback(
                        object : Snackbar.Callback() {
                            override fun onShown(snackbar: Snackbar) {
                                // Event handler for when the given Snackbar is visible
                            }

                            override fun onDismissed(
                                snackbar: Snackbar,
                                event: Int,
                            ) {
                                // Event handler for when the given Snackbar has been dismissed
                            }
                        },
                    )
                    .build()

            Dexter.withContext(activity)
                .withPermissions(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.INTERNET,
                )
                .withListener(snackbarMultiplePermissionsListener)
                .check()
        }

        // Check if camera permission is granted
        fun isCameraPermissionGranted(activity: Activity): Boolean {
            return ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Show a confirmation dialog
         *
         * @param activity The activity to show the confirmation dialog in
         * @param titleText The resource ID of the title of the confirmation dialog
         * @param messageText The resource ID of the message of the confirmation dialog
         * @param positiveButtonText The resource ID of the text of the positive button
         * @param negativeButtonText The resource ID of the text of the negative button
         * @param positiveListener The lambda to run when the positive button is clicked
         * @param negativeListener The lambda to run when the negative button is clicked
         *
         * @throws Exception if a confirmation dialog is already showing
         */
        fun showConfirmationDialog(
            activity: Activity,
            titleText: Int,
            messageText: Int,
            positiveButtonText: Int,
            negativeButtonText: Int,
            positiveListener: (() -> Unit)?,
            negativeListener: (() -> Unit)?,
        ) {
            synchronized(lock) {
                // Ignore if the activity is finishing
                if (activity.isFinishing) {
                    return
                }
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isConfirmationDialogShowing) {
                    throw Exception("Confirmation dialog is already showing")
                }
                isConfirmationDialogShowing = true
            }

            val dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_confirm)
            dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp

            window.attributes.windowAnimations = R.style.animation

            val btnYes: AppCompatButton = dialog.findViewById<AppCompatButton>(R.id.btnYes)
            val btnNo: AppCompatButton = dialog.findViewById<AppCompatButton>(R.id.btnNo)
            btnYes.setText(positiveButtonText)
            btnNo.setText(negativeButtonText)
            val title: TextView = dialog.findViewById<TextView>(R.id.tvTitle)
            val message: TextView = dialog.findViewById<TextView>(R.id.tvMessage)

            title.setText(titleText)
            message.setText(messageText)
            btnYes.setOnClickListener { _ ->
                dialog.dismiss()
                if (positiveListener != null) {
                    positiveListener()
                }
                synchronized(lock) {
                    isConfirmationDialogShowing = false
                }
            }

            btnNo.setOnClickListener { _ ->
                dialog.dismiss()
                if (negativeListener != null) {
                    negativeListener()
                }
                synchronized(lock) {
                    isConfirmationDialogShowing = false
                }
            }

            dialog.show()
        }

        fun showInfoDialog(
            activity: Activity,
            titleResId: Int,
            messageResId: Int,
            isSuccess: Boolean = true,
            onClose: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isInfoDialogShowing) {
                    throw Exception("Info dialog is already showing")
                }
                isInfoDialogShowing = true
            }

            val btnOk: AppCompatButton
            val tvTitle: TextView
            val tvMessage: TextView

            var dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_info)
            dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp

            window.attributes.windowAnimations = R.style.animation

            btnOk = dialog.findViewById<AppCompatButton>(R.id.btnOk)
            tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
            tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)

            tvTitle.setText(titleResId)
            if (isSuccess) {
                tvTitle.setTextColor(activity.getColor(R.color.colorSuccess))
            } else {
                tvTitle.setTextColor(activity.getColor(R.color.colorError))
            }
            tvMessage.setText(messageResId)

            dialog.setOnCancelListener {
                synchronized(lock) {
                    isInfoDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                synchronized(lock) {
                    isInfoDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * Show an unrecoverable error dialog with a message based on the error details
         *
         * @param activity The activity
         * @param unRecoverableError The unrecoverable error details
         * @param onClose The lambda to run when the dialog is closed
         */
        fun processUnrecoverableError(
            activity: Activity,
            unRecoverableError: ErrorDetails,
            onClose: (() -> Unit)? = null,
        ) {
            unRecoverableError?.let {
                // Show unrecoverable error dialog
                activity.runOnUiThread {
                    showInfoDialog(
                        activity,
                        it.title,
                        it.message,
                    ) {
                        if (onClose != null) {
                            onClose()
                        }
                    }
                }
            }
        }

        /**
         * Show a recoverable error dialog with a message based on the error details
         *
         * @param activity The activity
         * @param recoverableError The recoverable error details
         * @param positiveListener The lambda to run when the positive button is clicked
         * @param negativeListener The lambda to run when the negative button is clicked
         */
        fun processRecoverableError(
            activity: Activity,
            recoverableError: ErrorDetails,
            positiveListener: (() -> Unit)?,
            negativeListener: (() -> Unit)?,
        ) {
            activity.runOnUiThread {
                showConfirmationDialog(
                    activity,
                    recoverableError.title,
                    recoverableError.message,
                    R.string.retry,
                    R.string.cancel,
                    {
                        // Retry
                        if (positiveListener != null) {
                            positiveListener()
                        }
                    },
                    {
                        if (negativeListener != null) {
                            negativeListener()
                        }
                    },
                )
            }
        }

        /**
         * Show a face scan retry info dialog
         *
         * @param activity The activity to show the info dialog in
         * @param onClose The lambda to run when the info dialog is closed
         *
         * @throws Exception if an info dialog is already showing
         */
        fun showFaceScanRetryDialog(
            activity: Activity,
            onClose: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isLivenessRetryDialogShowing) {
                    throw Exception("Info dialog is already showing")
                }
                isLivenessRetryDialogShowing = true
            }

            val btnOk: AppCompatButton

            var dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_liveness_retry)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp

            window.attributes.windowAnimations = R.style.animation
            btnOk = dialog.findViewById<AppCompatButton>(R.id.btnRetry)

            dialog.setOnCancelListener {
                synchronized(lock) {
                    isLivenessRetryDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                synchronized(lock) {
                    isLivenessRetryDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * Show a face scan retry info dialog
         *
         * @param activity The activity to show the info dialog in
         * @param onClose The lambda to run when the info dialog is closed
         *
         * @throws Exception if an info dialog is already showing
         */
        fun showCaptureSessionErrorDialog(
            activity: Activity,
            titleResId: Int,
            onClose: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isCaptureSessionErrorDialogShowing) {
                    throw Exception("Info dialog is already showing")
                }
                isCaptureSessionErrorDialogShowing = true
            }

            val btnOk: AppCompatButton
            val titleText: TextView

            var dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_capture_not_match)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp

            window.attributes.windowAnimations = R.style.animation
            btnOk = dialog.findViewById<AppCompatButton>(R.id.btnRetry)
            titleText = dialog.findViewById<TextView>(R.id.tvTitle)
            titleText.setText(titleResId)

            dialog.setOnCancelListener {
                synchronized(lock) {
                    isCaptureSessionErrorDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                synchronized(lock) {
                    isCaptureSessionErrorDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * Determines if any dialog is showing
         */
        fun isDialogShowing(): Boolean {
            synchronized(lock) {
                return isInfoDialogShowing ||
                        isConfirmationDialogShowing ||
                        isLivenessRetryDialogShowing ||
                        isTimeOutDialogShowing ||
                        isMoveGentlyDialogShowing ||
                        isCaptureSessionErrorDialogShowing
            }
        }

        /**
         * Shows a success dialog with an image and a message
         *
         * @param imageResourceId The image resource ID
         * @param activity The activity to show the info dialog in
         * @param titleResId The resource ID of the title of the info dialog
         * @param messageResId The resource ID of the message of the info dialog
         * @param onClose The lambda to run when the info dialog is closed
         */
        fun showSuccessDialog(
            imageResourceId: Int,
            activity: Activity,
            titleResId: Int,
            messageResId: Int,
            onClose: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isInfoDialogShowing) {
                    throw Exception("Info dialog is already showing")
                }
                isInfoDialogShowing = true
            }

            val btnOk: AppCompatButton
            val tvTitle: TextView
            val tvMessage: TextView
            val imageView: ImageView

            var dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_success)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp

            window.attributes.windowAnimations = R.style.animation

            btnOk = dialog.findViewById<AppCompatButton>(R.id.btnOk)
            tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
            tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
            imageView = dialog.findViewById<ImageView>(R.id.ivImage)

            tvTitle.setText(titleResId)
            tvMessage.setText(messageResId)
            imageView.setImageResource(imageResourceId)

            dialog.setOnCancelListener {
                synchronized(lock) {
                    isInfoDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                synchronized(lock) {
                    isInfoDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }
            dialog.show()
        }

        /**
         * Show a time out dialog
         *
         * @param activity The activity to show the info dialog in
         * @param onClose The lambda to run when the info dialog is closed
         */
        fun showTimeOutDialog(
            activity: Activity,
            onClose: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isTimeOutDialogShowing) {
                    throw Exception("Info dialog is already showing")
                }
                isTimeOutDialogShowing = true
            }

            val btnOk: AppCompatButton
            val lottie: LottieAnimationView

            var dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_info_with_animation)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp
            window.attributes.windowAnimations = R.style.animation

            btnOk = dialog.findViewById<AppCompatButton>(R.id.btnRetry)
            lottie = dialog.findViewById(R.id.lottie)

            dialog.setOnCancelListener {
                synchronized(lock) {
                    isTimeOutDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            var isSecondAnimationPlaying = false

            lottie.addAnimatorListener(
                object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isSecondAnimationPlaying) {
                            isSecondAnimationPlaying = true
                            lottie.setAnimation("Obstruction.json")
                            lottie.playAnimation()
                        } else {
                            isSecondAnimationPlaying = false
                            lottie.setAnimation("ActiveFaceCaptureBig.json")
                            lottie.playAnimation()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                    }
                },
            )

            btnOk.setOnClickListener {
                synchronized(lock) {
                    isTimeOutDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * Show a move gently dialog
         *
         * @param activity The activity to show the info dialog in
         * @param onClose The lambda to run when the info dialog is closed
         */
        fun showMoveGentlyDialog(
            activity: Activity,
            onClose: (() -> Unit)? = null,
        ) {
            synchronized(lock) {
                // For confirmation dialogs, showing one when one is already showing is
                // a programming error
                if (isMoveGentlyDialogShowing) {
                    throw Exception("Info dialog is already showing")
                }
                isMoveGentlyDialogShowing = true
            }

            val btnOk: AppCompatButton

            var dialog: Dialog = Dialog(activity)
            dialog.setContentView(R.layout.dialog_extreme_movement)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            dialog.setCancelable(false)

            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch

            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp
            window.attributes.windowAnimations = R.style.animation

            btnOk = dialog.findViewById<AppCompatButton>(R.id.btnRetry)

            dialog.setOnCancelListener {
                synchronized(lock) {
                    isMoveGentlyDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                synchronized(lock) {
                    isMoveGentlyDialogShowing = false
                }
                if (onClose != null) {
                    onClose()
                }
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}
