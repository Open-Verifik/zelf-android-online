package co.verifik.wallet.ui.activity.preprocesswallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import com.google.common.util.concurrent.ListenableFuture
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintInfo
import com.sensecrypt.sdk.core.SensePrintType
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.zns.OpenZNSActivity
import co.verifik.wallet.utils.QRUtil
import co.verifik.wallet.utils.convertToHashMap
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class QRScanActivity : AppCompatActivity(), QRBytesListener {

    private lateinit var textViewNavTitle: TextView
    private lateinit var textViewNavSubtitle: TextView

    // The camera provider future
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    // The camera view
    private lateinit var pvCamera: PreviewView

    // The text view to show instructions to the user
    private lateinit var tvInstructions: TextView

    // The button to go back to the previous screen
    private lateinit var ivBack: ImageView

    private lateinit var spinKitView: SpinKitView
    private lateinit var viewMask: View

    // The password entered by the user
    private lateinit var qrPassword: String

    // The camera
    private var camera: Camera? = null

    // The frame view showing the pinch to zoom animation
    private lateinit var flPinchToZoom: FrameLayout

    // Whether the pinch to zoom hint has been shown
    private var isPinchToZoomHintShown = false

    private class QrAnalyzer(
        val listener: QRBytesListener
    ): ImageAnalysis.Analyzer {

        var currentTimestamp: Long = 0

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            currentTimestamp = System.currentTimeMillis()
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                // ...
                scanBarcodes(imageProxy, image, listener)
            }
        }

        private fun scanBarcodes(imageProxy: ImageProxy, image: InputImage, listener: QRBytesListener) {
            // [START set_detector_options]
            val options = BarcodeScannerOptions.Builder()
//                .enableAllPotentialBarcodes()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE
                )
                .build()
            // [END set_detector_options]

            // [START get_detector]
            val scanner = BarcodeScanning.getClient(options)
            // Or, to specify the formats to recognize:
            // val scanner = BarcodeScanning.getClient(options)
            // [END get_detector]

            // [START run_detector]
            val result = scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Task completed successfully
                    // [START_EXCLUDE]
                    // [START get_barcodes]
                    val barcode = barcodes.firstOrNull()
                    if (barcode == null) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    var bmp = imageProxy.toBitmap()
                    val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                    val rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    var left = (barcode.boundingBox?.left ?: 0)
                    var top = (barcode.boundingBox?.top ?: 0)
                    var width = (barcode.boundingBox?.width() ?: 0)
                    var height = (barcode.boundingBox?.height() ?: 0)
                    left -= 10
                    top -= 10
                    width += 20
                    height += 20
                    if (left < 0) {
                        left = 0
                    }
                    if (top < 0) {
                        top = 0
                    }
                    if (left + width > rotatedBitmap.width) {
                        width = rotatedBitmap.width - left
                    }
                    if (top + height > rotatedBitmap.height) {
                        height = rotatedBitmap.height - top
                    }
                    val cropImg = Bitmap.createBitmap(rotatedBitmap, left, top, width, height)
                    val stream = ByteArrayOutputStream()
                    cropImg.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val cropBytes = stream.toByteArray()
                    rotatedBitmap.recycle()
                    cropImg.recycle()

                    barcode.rawBytes?.let { listener.processQrBytes(it) }

                    // [END get_barcodes]
                    // [END_EXCLUDE]
                    imageProxy.close()
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    // ...
                    imageProxy.close()
                }
            // [END run_detector]
        }
    }

    companion object {
        // The tag for logging
        private const val TAG = "QRScanActivity"

        // Whether we are done with the QR scan
        private var isQRScanComplete = false

        // A boolean to check if the camera is facing back
        private var isCameraFacingBack: Boolean = true

        // Method to create a new Intent for QRScanActivity
        fun newIntent(context: Context): Intent {
            val intent = Intent(context, QRScanActivity::class.java)
            return intent
        }

        private const val EXTRA_TYPE = "co.verifik.wallet.EXTRA_TYPE"
        private const val TYPE_CLEAR_DATA = "co.verifik.wallet.TYPE_CLEAR_DATA"

        fun newIntentWithForClearText(context: Context): Intent {
            val intent = Intent(context, QRScanActivity::class.java)
            intent.putExtra(EXTRA_TYPE, TYPE_CLEAR_DATA)
            return intent
        }
    }

    private fun startCamera() {
        if (UIHelper.isCameraPermissionGranted(this)) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener(
                Runnable {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                },
                ContextCompat.getMainExecutor(this),
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTapToFocusAndPinchToZoom() {
        val scaleGestureDetector =
            ScaleGestureDetector(
                this,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val camera = camera ?: return false

                        val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 0F
                        val delta = detector.scaleFactor
                        camera.cameraControl.setZoomRatio(currentZoomRatio * delta)

                        return true
                    }
                },
            )

        pvCamera.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)

            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener true
            }

            val camera = camera ?: return@setOnTouchListener true

            val meteringPointFactory = pvCamera.meteringPointFactory
            val point = meteringPointFactory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            camera.cameraControl.startFocusAndMetering(action)

            return@setOnTouchListener true
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()

        var preview: Preview =
            Preview.Builder()
                .build()

        val cameraSelector: CameraSelector =
            if (isCameraFacingBack) {
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
            } else {
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
            }

        preview.setSurfaceProvider(pvCamera.surfaceProvider)

        val executor = Executors.newSingleThreadExecutor()

        // NOTE: Since processing takes some time, we need to use a separate thread
        // to run the analyzer. Do not use the main thread - i.e. do not use:
        // val executor = ContextCompat.getMainExecutor(this)

        val resolutionSelector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY,
                ).build()

        val imageAnalysis =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // .setBackgroundExecutor(executor)
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setAnalyzer(
                        executor,
                        QrAnalyzer(this)
                    )
                }

        camera =
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
            )

        setupTapToFocusAndPinchToZoom()

        // Show the pinch to zoom hint
        if (!isPinchToZoomHintShown) {
            isPinchToZoomHintShown = true
            showPinchToZoomHint()
        }
    }

    private fun isClearTextRequest(): Boolean {
        return intent.getStringExtra(EXTRA_TYPE) == TYPE_CLEAR_DATA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        // Assign values to the components in this activity
        setupComponents()

        // Assign listeners to the components in this activity
        setUpListeners()

        // Request for camera permission
        UIHelper.requestCameraPermission(this, pvCamera)
    }

    /**
     * This method is used to set up the components in this activity.
     */
    private fun setupComponents() {
        textViewNavTitle = findViewById(R.id.textview_navtitle)
        textViewNavSubtitle = findViewById(R.id.textview_navsubtitle)
        pvCamera = findViewById<PreviewView>(R.id.pvCamera)
        tvInstructions = findViewById<TextView>(R.id.tvInstructions)
        ivBack = findViewById<ImageView>(R.id.ivBack)
        flPinchToZoom = findViewById<FrameLayout>(R.id.flPinchToZoom)
        spinKitView = findViewById<SpinKitView>(R.id.spin_kit)
        viewMask = findViewById<View>(R.id.view_mask)
        flPinchToZoom.alpha = 0f

        textViewNavTitle.text = getString(R.string.activity_qr_scanner_header)
        textViewNavSubtitle.text = getString(R.string.activity_qr_scanner_subheader)
    }

    /**
     * This method is used to show the pinch to zoom hint.
     */
    private fun showPinchToZoomHint() {
        flPinchToZoom.postDelayed({
            flPinchToZoom.animate().alpha(1f).setDuration(1000).start()

            // Hide the pinch to zoom hint after 5 seconds
            flPinchToZoom.postDelayed({
                hidePinchToZoomHint()
            }, 5000)
        }, 500)
    }

    /**
     * This method is used to hide the pinch to zoom hint.
     */
    private fun hidePinchToZoomHint() {
        flPinchToZoom.animate().alpha(0f).setDuration(1000).start()
    }

    /**
     * This method is used to set up the listeners for the components in this activity.
     */
    private fun setUpListeners() {
        ivBack.setOnClickListener { finish() }
    }

    // ask the user to enter password if it is required to scan qr
    private fun showQrPasswordDialog(continueListener: View.OnClickListener) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_qr_password)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        dialog.setCancelable(true)
        val window = dialog.window
        window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ) // change mathch
        window.setGravity(Gravity.CENTER)
        val lp = window.attributes
        lp.dimAmount = 0.7f
        lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes = lp
        dialog.window!!.attributes.windowAnimations = R.style.animation
        val btnYes: AppCompatButton = dialog.findViewById(R.id.btnConfirm)
        val password: TextInputEditText = dialog.findViewById(R.id.edittext_pssw)
        val tvError: TextView = dialog.findViewById(R.id.tvError)
        btnYes.setOnClickListener { v ->
            if (password.length() != 0 && password.text.toString() != getString(R.string.enter_password)) {
                dialog.dismiss()
                qrPassword = password.text.toString()
                continueListener.onClick(v)
            } else {
                tvError.visibility = View.VISIBLE // show error field if password is null
            }
        }

        dialog.setOnCancelListener {
            finish()
        }

        dialog.show()
    }

    /**
     * This method is used to show a dialog to indicate that the password is incorrect.
     */
    private fun showPasswordIncorrectDialog(
        spInfo: SensePrintInfo,
        spBytes: ByteArray
    ) {
        // Show an error message
        runOnUiThread {
            UIHelper.showConfirmationDialog(
                this,
                R.string.verification_failed,
                R.string.password_incorrect,
                R.string.retry,
                R.string.cancel,
                {
                    showPasswordDialog(spInfo, spBytes)
                },
                {
                    finish()
                },
            )
        }
    }

    private fun showPasswordDialog(
        spInfo: SensePrintInfo,
        spBytes: ByteArray
    ) {
        showQrPasswordDialog {
            // Check if password is correct
            try {
                val isPasswordCorrect =
                    CryptUtil.verifyPassword(
                        spBytes,
                        qrPassword,
                    )
                if (isPasswordCorrect) {
                    // navigate to next activity based on qr scan type
                    navigateToNextActivity(spInfo, spBytes, qrPassword)
                } else {
                    // Show an error message
                    showPasswordIncorrectDialog(spInfo, spBytes)
                }
            } catch (e: SenseCryptSdkException) {
                // This can only happen if the license has expired
                runOnUiThread {
                    UIHelper.showInfoDialog(
                        this,
                        R.string.license_expired,
                        R.string.license_expired_detail,
                        false,
                    ) {
                        finish()
                    }
                }
            }
        }
    }

    private fun processQR(
        spInfo: SensePrintInfo?,
        spBytes: ByteArray,
        imgBytes: ByteArray
    ) {
        if (spInfo == null) {
            // If the QR code is not a Zelf QR code, show an error message
            runOnUiThread {
                UIHelper.showSnackBar(
                    this,
                    pvCamera,
                    R.string.invalid_qr,
                    R.color.colorErrorSnackbar,
                    2000,
                    100f,
                )
            }
        } else {
            // If the QR code is a Zelfcrypt QR code, stop scanning
            isQRScanComplete = true
            var isPasswordRequired = spInfo.spType == SensePrintType.WITH_PASSWORD

            if (isClearTextRequest()) {
                isPasswordRequired = false
            }

            if (isPasswordRequired) {
                runOnUiThread {
                    showPasswordDialog(spInfo, spBytes)
                }
            } else {
                // navigate to next activity based on qr scan type
                navigateToNextActivity(spInfo, imgBytes, null)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (UIHelper.isCameraPermissionGranted(this)) {
            cameraProviderFuture.get().unbindAll()
            camera = null
        }
    }

    /**
     * for navigating to next activity based on qr scan type
     */
    private fun navigateToNextActivity(
        spInfo: SensePrintInfo,
        imgBytes: ByteArray,
        password: String?,
    ) {
        if (isClearTextRequest()) {
            if (spInfo.clearTextData.isEmpty()) {
                // There is no clear text data, so we can't show the details
                runOnUiThread {
                    UIHelper.showInfoDialog(
                        this,
                        R.string.no_clear_text_data,
                        R.string.no_clear_text_data_detail,
                        false,
                    ) {
                        finish()
                    }
                }
                return
            } else {
                val map = convertToHashMap(spInfo.clearTextData)
                val ethAddress = map["ethAddress"]
                if (ethAddress != null) {
                    searchIPFS(ethAddress, imgBytes)
                    return
                }
            }
        } else {
            // We are done with the scan open the FaceScanActivity
            startActivity(
                FaceScanActivity.newIntent(
                    this,
                    "znsName",
                    imgBytes,
                    password,
                    spInfo.isLivenessEnabled,
                ),
            )
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        isQRScanComplete = false
        qrPassword = ""
        CryptUtil.initSdkIfNeeded(this) {
            startCamera()
        }
    }

    override fun processQrBytes(spBytes: ByteArray) {
        if (isQRScanComplete) {
            return
        }
        val bmp = QRUtil.createQRCodeFromBytes(spBytes, 512, 512)
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imgBytes = stream.toByteArray()

        try {
            val spInfo = CryptUtil.parseSensePrintBytes(spBytes)
            processQR(spInfo, spBytes, imgBytes)
        } catch (e: SenseCryptSdkException) {
            // This will only happen if the license has expired
            runOnUiThread {
                // To avoid showing multiple dialogs
                isQRScanComplete = true
                UIHelper.showInfoDialog(
                    this,
                    R.string.license_expired,
                    R.string.license_expired_detail,
                    false,
                ) {
                    finish()
                }
            }
        }
    }

    private fun searchIPFS(publicAddress: String, imgBytes: ByteArray? = null) {
        lifecycleScope.launch {
            spinKitView.visibility = View.VISIBLE
            viewMask.visibility = View.VISIBLE

            val ipfsResponse = try {
                CryptUtil.findIPFSByPublicAddress(publicAddress)
            } catch (e: HttpException) {
                null
            }
            spinKitView.visibility = View.GONE
            viewMask.visibility = View.GONE

            if(ipfsResponse == null) {
                UIHelper.showInfoDialog(
                    this@QRScanActivity,
                    R.string.no_zns_found,
                    R.string.no_zns_found_desc,
                    false,
                )
            } else {
                val zns = ipfsResponse.metadata?.name?.replace(".zelf", "") ?: ""
                val znsUrl = ipfsResponse.url ?: ""
                val ethAddress = ipfsResponse.metadata?.keyvalues?.get("ethAddress") ?: ""
                val solanaAddress = ipfsResponse.metadata?.keyvalues?.get("solanaAddress") ?: ""
                val intent = OpenZNSActivity.newIntent(
                    this@QRScanActivity,
                    zns,
                    znsUrl,
                    ethAddress,
                    solanaAddress,
                    imgBytes
                )
                startActivity(intent)
            }
        }
    }
}
