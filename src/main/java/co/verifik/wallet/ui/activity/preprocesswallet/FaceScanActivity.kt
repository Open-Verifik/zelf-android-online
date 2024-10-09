package co.verifik.wallet.ui.activity.preprocesswallet

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.data.domain.FaceScanErrorDetails
import co.verifik.wallet.data.domain.MnemonicSize
import co.verifik.wallet.data.domain.UIHandledRecoverableError
import co.verifik.wallet.data.domain.UIHandledUnrecoverableError
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.data.remote.ZelfWalletResponse
import co.verifik.wallet.databinding.ActivityFaceScanBinding
import com.google.common.util.concurrent.ListenableFuture
import com.sensecrypt.sdk.core.DecryptedSensePrintData
import com.sensecrypt.sdk.core.HeadPose
import com.sensecrypt.sdk.core.SenseCryptSdkException
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.openwallet.ReaderDetailActivity
import co.verifik.wallet.ui.activity.wallet.success.SuccessSendCryptoActivity
import co.verifik.wallet.ui.views.CircleArcView
import co.verifik.wallet.utils.DataHolder
import co.verifik.wallet.utils.ErrorUtil
import co.verifik.wallet.utils.SessionHolder
import co.verifik.wallet.utils.activeFaceCaptureTextMap
import co.verifik.wallet.utils.checkHeadPose
import co.verifik.wallet.utils.currentHeadPoseInstructionsMap
import co.verifik.wallet.utils.getScreenWidth
import co.verifik.wallet.utils.processBitmapToGetQrBytes
import co.verifik.wallet.utils.shouldCenter
import co.verifik.wallet.utils.showCurrentHeadPose
import co.verifik.wallet.utils.stateResources
import com.sensecrypt.sdk.core.ActiveFaceCaptureProcessingResult
import com.sensecrypt.sdk.core.ActiveFaceCaptureSession
import com.sensecrypt.sdk.core.ActiveFaceCaptureStateName
import com.sensecrypt.sdk.core.IndicatorStateName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.Executors

class FaceScanActivity : AppCompatActivity(), ActiveFaceCaptureSessionListener {
    private lateinit var binding: ActivityFaceScanBinding
    /**
     * The camera provider future
     */
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    /**
     * The active face capture session
     */
    private lateinit var session: ActiveFaceCaptureSession
    /**
     * The capture analyzer, which processes frames from the camera
     */
    private lateinit var captureAnalyzer: ActiveFaceCaptureAnalyzer

    // The button to go back to the previous screen
    private lateinit var ivBack: ImageView

    private lateinit var textviewTitle: TextView
    private lateinit var textviewSubtitle: TextView

    /**
     * The number of completed actions
     */
    private var numCompletedActions: UByte = 0u
    private var completedDecrypt: Boolean = false

    // The parsed data to pass to the next activity
    private var parsedData: DecryptedSensePrintData? = null

    // The camera
    private var camera: Camera? = null

    /**
     * The root view of the activity
     */
    private lateinit var rootView: View

    // The frame view showing the pinch to zoom animation
    private lateinit var flPinchToZoom: FrameLayout

    // Whether the pinch to zoom hint has been shown
    private var isPinchToZoomHintShown = false

    private lateinit var pssw: String
    private lateinit var mnemonicSize: MnemonicSize
    private var qrImageBytes: ByteArray? = null
    private var mnemonicWords: String = ""
    private var sendTransaction: Boolean = false
    private var qrEntityId: Int = 0
    private var sendToAddress: String = ""
    private var amount: String = ""
    private var network: String = ""

    private var readingOnly: Boolean = false
    private var open: Boolean = false

    private val znsName by lazy {
        intent.getStringExtra(EXTRA_ZNS_NAME)
    }

    /**
     * The vibrator service
     */
    private lateinit var vibrator: Vibrator

    private var _isLoading = MutableLiveData(false)
    val isLoading get() = _isLoading

    // Static variables
    companion object {
        // A boolean to check if the camera is facing back
        var isCameraFacingBack: Boolean = false

        var livenessFailureCount: Int = 0

        // Method to create a new Intent for FaceScanActivity from OpenQR activities
        fun newIntent(
            context: Context,
            znsName: String,
            imageBytes: ByteArray,
            password: String?,
            isLivenessCheckEnabled: Boolean,
            isReadingOnly: Boolean = false,
            forOpen: Boolean = false
        ): Intent {
            // Always start with the front camera
            isCameraFacingBack = false
            livenessFailureCount = 0

            val intent = Intent(context, FaceScanActivity::class.java)
            intent.putExtra(EXTRA_CHECK_LIVENESS, isLivenessCheckEnabled)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_IMG_BYTES, imageBytes)
            intent.putExtra(EXTRA_PASSWORD, password)
            intent.putExtra(EXTRA_READING_ONLY_BYTES, isReadingOnly)
            intent.putExtra(EXTRA_OPENED, forOpen)
            return intent
        }

        fun newIntentForTransaction(
            context: Context,
            imageBytes: ByteArray,
            password: String?,
            qrEntityId: Int,
            sendCryptoInfo: SendCryptoInfo?
        ): Intent {
            // Always start with the front camera
            isCameraFacingBack = false
            livenessFailureCount = 0

            val sendToAddress = sendCryptoInfo?.sendToAddress ?: ""
            val amount = sendCryptoInfo?.amount ?: ""
            val network = sendCryptoInfo?.network ?: ""

            val intent = Intent(context, FaceScanActivity::class.java)
            intent.putExtra(EXTRA_CHECK_LIVENESS, true)
            intent.putExtra(EXTRA_IMG_BYTES, imageBytes)
            intent.putExtra(EXTRA_PASSWORD, password)
            intent.putExtra(EXTRA_BOOL_FOR_TRANSACTION, true)
            intent.putExtra(EXTRA_QR_ENTITY_ID, qrEntityId)
            intent.putExtra(EXTRA_SEND_TO_ADDRESS, sendToAddress)
            intent.putExtra(EXTRA_AMOUNT, amount)
            intent.putExtra(EXTRA_NETWORK, network)
            return intent
        }

        fun newIntent(
            context: Context,
            znsName: String,
            password: String,
            isTwelve: Boolean
        ): Intent {
            isCameraFacingBack = false
            livenessFailureCount = 0

            val intent = Intent(context, FaceScanActivity::class.java)
            intent.putExtra(EXTRA_CHECK_LIVENESS, true)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_PASSWORD, password)
            intent.putExtra(EXTRA_MNEMONIC_SIZE_IS_TWELVE, isTwelve)
            return intent
        }

        fun newIntent(
            context: Context,
            znsName: String,
            mnemonic: String,
            password: String,
        ): Intent {
            isCameraFacingBack = false
            livenessFailureCount = 0

            val intent = Intent(context, FaceScanActivity::class.java)
            intent.putExtra(EXTRA_CHECK_LIVENESS, true)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_MNEMONIC, mnemonic)
            intent.putExtra(EXTRA_PASSWORD, password)
            return intent
        }

        // Tag for logging
        private const val TAG = "FaceScanActivity"

        const val EXTRA_ZNS_NAME = "co.verifik.wallet.EXTRA_ZNS_NAME"

        // Intent extra for QR code data
        const val EXTRA_IMG_BYTES = "co.verifik.wallet.EXTRA_IMG_BYTES"

        // Intent extra for password
        const val EXTRA_PASSWORD = "co.verifik.wallet.EXTRA_PASSWORD"

        // Intent extra for checking liveness
        const val EXTRA_CHECK_LIVENESS = "co.verifik.wallet.EXTRA_CHECK_LIVENESS"

        const val EXTRA_MNEMONIC_SIZE_IS_TWELVE = "co.verifik.wallet.EXTRA_MNEMONIC_SIZE"

        const val EXTRA_MNEMONIC = "co.verifik.wallet.EXTRA_MNEMONIC"

        const val EXTRA_BOOL_FOR_TRANSACTION = "co.verifik.wallet.EXTRA_BOOL_FOR_TRANSACTION"
        const val EXTRA_QR_ENTITY_ID = "co.verifik.wallet.EXTRA_QR_ENTITY_ID"
        const val EXTRA_SEND_TO_ADDRESS = "co.verifik.wallet.EXTRA_SEND_TO_ADDRESS"
        const val EXTRA_AMOUNT = "co.verifik.wallet.EXTRA_AMOUNT"
        const val EXTRA_NETWORK = "co.verifik.wallet.EXTRA_NETWORK"

        const val EXTRA_READING_ONLY_BYTES = "co.verifik.wallet.EXTRA_READING_ONLY_BYTES"
        const val EXTRA_OPENED = "co.verifik.wallet.EXTRA_OPENED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Assign values to the components in this activity
        setupComponents()

        // Assign listeners to the components in this activity
        setUpListeners()

        // Request for camera permission
        UIHelper.requestCameraPermission(this, binding.pvCamera)
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

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()

        // The active capture session
        try {
            session = CryptUtil.getActiveFaceCaptureSession()
            resetUI()
        } catch (e: SenseCryptSdkException) {
            // Sdk is not initialized, license expired, etc.
            showErrorInUIThread(e)
            return
        }

        val preview =
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

        preview.setSurfaceProvider(binding.pvCamera.surfaceProvider)

        // NOTE: Since processing takes some time, we need to use a separate thread
        // to run the analyzer. Do not use the main thread - i.e. do not use:
        // val executor = ContextCompat.getMainExecutor(this)
        val executor = Executors.newSingleThreadExecutor()


        val imageAnalysis =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    captureAnalyzer =
                        ActiveFaceCaptureAnalyzer(
                            this,
                            session = session,
                        )
                    it.setAnalyzer(
                        executor,
                        captureAnalyzer,
                    )
                }

        camera =
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
            )
    }

    /**
     * This method is used to vibrate the phone for haptic feedback when the user completes
     * an action.
     */
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // backward compatibility for Android API < 26
            // noinspection deprecation
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /**
     * This method is used to set up the components in this activity.
     */
    private fun setupComponents() {
        ivBack = findViewById(R.id.ivBack)
        textviewTitle = findViewById(R.id.textview_navtitle)
        textviewSubtitle = findViewById(R.id.textview_navsubtitle)

        // set screen brightness to full
        val layout = window.attributes
        layout.screenBrightness = 1f
        window.attributes = layout

        val znsNameComplete = "$znsName.zelf"
        textviewTitle.text = znsNameComplete
//        textviewSubtitle.text = getString(R.string.activity_face_scan_subheader)

        sendTransaction = intent.getBooleanExtra(EXTRA_BOOL_FOR_TRANSACTION, false)
        qrEntityId = intent.getIntExtra(EXTRA_QR_ENTITY_ID, 0)
        sendToAddress = intent.getStringExtra(EXTRA_SEND_TO_ADDRESS) ?: ""
        amount = intent.getStringExtra(EXTRA_AMOUNT) ?: ""
        network = intent.getStringExtra(EXTRA_NETWORK) ?: ""
        pssw = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
        mnemonicWords = intent.getStringExtra(EXTRA_MNEMONIC) ?: ""
        mnemonicSize = if(intent.getBooleanExtra(EXTRA_MNEMONIC_SIZE_IS_TWELVE, false))
            MnemonicSize.MNEMONIC12
        else
            MnemonicSize.MNEMONIC24
        readingOnly = intent.getBooleanExtra(ReaderDetailActivity.EXTRA_READING_ONLY_BYTES, false)
        open = intent.getBooleanExtra(EXTRA_OPENED, false)

        // set vibration
        vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
    }

    /**
     * This method is used to set up the listeners for the components in this activity.
     */
    private fun setUpListeners() {
        ivBack.setOnClickListener { finish() }

        isLoading.observe(this) {
            binding.mask.visibility = if(it) View.VISIBLE else View.INVISIBLE
            binding.spinKit.visibility = if(it) View.VISIBLE else View.INVISIBLE
        }
    }

    /**
     * Update UI based on a processing result
     *
     * @param result the processing result from the capture analyzer
     */
    private fun updateUI(result: ActiveFaceCaptureProcessingResult) {
        val actionLivenessName = result.expectedUserAction
        // Update the instruction based on the action liveness name
        updateInstructions(actionLivenessName, result)
        // Update the circle color based on the action liveness name
        updateCircleColor(actionLivenessName, result)
        updateProgressTicks(result)
        updateAnimations(actionLivenessName, result)
    }

    /**
     * Update instructions based on the Active Face Capture State
     *
     * @param activeFaceCaptureState the state name of the Active Face Capture session
     * @param result the processing result from the capture analyzer
     */
    private fun updateInstructions(
        activeFaceCaptureState: ActiveFaceCaptureStateName,
        result: ActiveFaceCaptureProcessingResult,
    ) {
        val instructionText =
            when {
                // Show the face scan complete text
                activeFaceCaptureState == ActiveFaceCaptureStateName.ACTIVE_FACE_CAPTURE_COMPLETE ->
                    R.string.face_scan_complete

                // When state is WAITING_FOR_FIRST_CENTERED_FACE, tell the user to center their face
                activeFaceCaptureState in showCurrentHeadPose ->
                    currentHeadPoseInstructionsMap[result.currentHeadPose]
                        ?: R.string.center_your_face

                // When state is USER_SHOULD_STAY_STILL, and HeadPose is not NORMAL, show the head pose instruction
                activeFaceCaptureState in checkHeadPose && result.currentHeadPose != HeadPose.NORMAL ->
                    currentHeadPoseInstructionsMap[result.currentHeadPose]
                        ?: R.string.center_your_face

                // Tell the user to center their face
                else -> activeFaceCaptureTextMap[activeFaceCaptureState] ?: R.string.center_your_face
            }
        binding.tvInstructions.text = UIHelper.getInstructionText(this, instructionText)
    }

    /**
     * Update circle color (the circle around the face) based on the Active Face Capture session
     * state
     *
     * @param activeFaceCaptureState the state name of the active face capture
     * @param result the processing result from the capture analyzer
     */
    private fun updateCircleColor(
        activeFaceCaptureState: ActiveFaceCaptureStateName,
        result: ActiveFaceCaptureProcessingResult,
    ) {
        when (activeFaceCaptureState) {
            // Show a green circle when the face scan is complete
            ActiveFaceCaptureStateName.ACTIVE_FACE_CAPTURE_COMPLETE -> {
                binding.circleArcView.updateCircleColor(CircleArcView.CircleColorByState.COMPLETED)
            }

            in shouldCenter -> {
                // Show a gray circle initially
                if (result.currentHeadPose == null) {
                    binding.circleArcView.updateCircleColor(CircleArcView.CircleColorByState.INITIAL)
                } else {
                    // Show an orange circle before a user is asked to look in a specific direction
                    binding.circleArcView.updateCircleColor(CircleArcView.CircleColorByState.STAY_STILL)
                }
            }

            else -> {
                // Show a red circle when the user is asked to look in a specific direction
                val (_, direction) = stateResources[activeFaceCaptureState] ?: Pair(null, null)

                direction?.let {
                    if (activeFaceCaptureState != ActiveFaceCaptureStateName.WAITING_FOR_FIRST_CENTERED_FACE) {
                        when {
                            // Set's the direction of the circle along with directional scores
                            // that indicates to the user how much they are looking in a particular
                            // direction
                            activeFaceCaptureState !in shouldCenter -> {
                                binding.circleArcView.setDirection(
                                    activeFaceCaptureState,
                                    result.directionalScores,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Update progress ticks based on the number of completed actions
     *
     * @param result the processing result from the capture analyzer
     */
    private fun updateProgressTicks(result: ActiveFaceCaptureProcessingResult) {
        if (result.numCompletedActions != numCompletedActions) {
            numCompletedActions = result.numCompletedActions
            // Vibrates the phone
            vibrate()
        }

        val tickDrawable =
            when (result.numCompletedActions) {
                1u.toUByte() -> R.drawable.one_tick
                2u.toUByte() -> R.drawable.two_tick
                3u.toUByte() -> R.drawable.success_tick
                else -> null
            }
        tickDrawable?.let {
            if (binding.ivProgressTick.drawable != AppCompatResources.getDrawable(this, it)) {
                binding.ivProgressTick.visibility = View.VISIBLE
                binding.ivProgressTick.setImageDrawable(AppCompatResources.getDrawable(this, it))
            }
        }
    }

    /**
     * Update animations based on the session state
     *
     * @param activeFaceCaptureState the state name of the active face capture
     * @param result the processing result from the capture analyzer
     */
    private fun updateAnimations(
        activeFaceCaptureState: ActiveFaceCaptureStateName,
        result: ActiveFaceCaptureProcessingResult,
    ) {
        val (animationFile, _) = stateResources[activeFaceCaptureState] ?: Pair(null, null)

        result.currentIndicatorState?.let { currentIndicatorState ->
            when (currentIndicatorState) {
                // Show the required animation when the SDK tells us to show indicator
                IndicatorStateName.START_INDICATOR -> {
                    binding.lottieFaceInstruction.visibility = View.VISIBLE
                    binding.circleOverlayRegion.setBackgroundColor(getColor(R.color.zCircleCamera))
                    binding.lottieFaceInstruction.setAnimation(
                        animationFile ?: "LoopFullMoveCloser.json",
                    )
                    binding.lottieFaceInstruction.playAnimation()
                }

                // Hide the animation when the SDK tells us to hide the indicator
                IndicatorStateName.HIDE_INDICATOR -> {
                    binding.circleOverlayRegion.setBackgroundColor(getColor(R.color.colorTransparent))
                    binding.lottieFaceInstruction.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Move to the next activity when the face capture session is complete
     */
    private fun onFaceCaptureCompleted() {
        // It is possible that the session has not been initialized when
        // this function is called back from an error dialog, therefore
        // we check and restart the session if needed
        if (!::session.isInitialized) {
            resetSession()
            return
        }
        // We have a suitable image. We can launch the next activity
        // If QR code bytes are set, we need to decrypt the ZelfPrint
        if (intent.hasExtra(EXTRA_IMG_BYTES)) {
            val imgBytes = intent.getByteArrayExtra(EXTRA_IMG_BYTES)
            val password = intent.getStringExtra(EXTRA_PASSWORD)
            val isReadingOnly = intent.getBooleanExtra(EXTRA_READING_ONLY_BYTES, false)

            // This exception will be set by the in thread lambda
            // and subsequently used in the post dismiss lambda
            var exception: SenseCryptSdkException? = null
            // verify the zelfprint against the face

            if (imgBytes != null) {
                lifecycleScope.launch {
                    _isLoading.value = true
                    try {
                        val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                        val spBytes = processBitmapToGetQrBytes(bitmap)
                        if (spBytes == null) {
                            _isLoading.value = false
                            return@launch
                        }
                        val spBytesString = Base64.encodeToString(spBytes, Base64.DEFAULT)
                            .replace(Regex("""(\r\n)|\n"""), "")
                            .replace(" ", "")
                        val faceBytes = session.getBestFrame()
                        val faceBase64 = Base64.encodeToString(faceBytes, Base64.DEFAULT)
                            .replace(Regex("""(\r\n)|\n"""), "")
                            .replace(" ", "")
                        val zelfWalletResponse = CryptUtil.decryptZelfPrint(
                            spBytesString,
                            faceBase64,
                            password
                        )
                        val mnemonic = zelfWalletResponse?.metadata?.mnemonic

                        if (mnemonic == null) {
                            _isLoading.value = false
                            return@launch
                        }
                        val successIntent = SuccessSendCryptoActivity.newIntent(
                            this@FaceScanActivity,
                            qrEntityId,
                            sendToAddress,
                            amount,
                            network,
                            mnemonic
                        )
                        val intent = if (sendTransaction) successIntent
                        else ShowQRActivity.newIntent(
                            this@FaceScanActivity,
                            znsName ?: "",
                            mnemonic,
                            imgBytes,
                            isReadingOnly,
                            open
                        )
                        startActivity(intent)
                    } catch (e: IOException) {
                        //TODO: Manage ioexception
                        _isLoading.value = false
                    } catch (e: HttpException) {
                        _isLoading.value = false
                        val errorStr = e.response()?.errorBody()?.string()?.let { JSONObject(it) }?.get("error")
                        val err = when(errorStr) {
                            "ERR_INVALID_IMAGE" -> SenseCryptSdkException.UnexpectedException("ERR_INVALID_IMAGE")
                            "ERR_NO_FACE_DETECTED" -> SenseCryptSdkException.NoFaceDetected("ERR_NO_FACE_DETECTED")
                            "ERR_MULTIPLE_FACES_DETECTED" -> SenseCryptSdkException.MultipleFacesDetected("ERR_MULTIPLE_FACES_DETECTED")
                            "ERR_REF_FACE_NOT_MATCHED" -> SenseCryptSdkException.DecryptionFailed("ERR_REF_FACE_NOT_MATCHED")
                            "ERR_VERIFICATION_FAILED" -> SenseCryptSdkException.DecryptionFailed("ERR_VERIFICATION_FAILED")
                            "ERR_LIVENESS_FAILED" -> SenseCryptSdkException.LivenessFailed("ERR_LIVENESS_FAILED")
                            "ERR_LIVENESS_FACE_ANGLE_TOO_LARGE" -> SenseCryptSdkException.ExtremeHeadPoseDetected("ERR_LIVENESS_FACE_ANGLE_TOO_LARGE")
                            "ERR_LIVENESS_FACE_CLOSE_TO_BORDER"-> SenseCryptSdkException.ExtremeHeadPoseDetected("ERR_LIVENESS_FACE_CLOSE_TO_BORDER")
                            else -> SenseCryptSdkException.UnexpectedException("UNEXPECTED")
                        }
                        showErrorInUIThread(err)
                    }
                }
            }
        } else {
            // Otherwise, we need to launch the PersonInfo activity
            // passing the image bytes
            createWalletAndGenerateQRCode(SessionHolder(session))
        }
    }

    /**
     * Reset the session
     *
     * This initializes the SDK if needed and starts the camera.
     *
     * The SDK can lose its initialized state if the app is in the
     * background for a long time. Therefore, it is always a good
     * idea to call initSdkIfNeeded before starting the camera.
     */
    private fun resetSession() {
        // Initialize the SDK if needed (in a separate thread)
        CryptUtil.initSdkIfNeeded(this) {
            // Executed after the SDK is initialized in the background
            if (!UIHelper.isDialogShowing()) {
                // If there ws a dialog showing, dismissing
                // it would proceed to the next required steps
                // so we don't need to start the camera again
                startCamera()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetSession()
    }

    override fun onPause() {
        super.onPause()
        // Release the camera
        if (UIHelper.isCameraPermissionGranted(this)) {
            cameraProviderFuture.get().unbindAll()
        }
    }

    /**
     * Reset the UI to initial state
     */
    private fun resetUI() {
        binding.circleArcView.updateCircleColor(CircleArcView.CircleColorByState.INITIAL)
        binding.ivProgressTick.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.empty_check))
        binding.tvInstructions.text = getString(R.string.center_your_face)
    }

    /**
     * Show a dialog for a recoverable error
     *
     * @param uiHandledError The error that occurred
     */
    private fun showRecoverableErrorDialog(uiHandledError: UIHandledRecoverableError) {
        UIHelper.processRecoverableError(activity = this, uiHandledError.errorDetails, {
            Handler(Looper.getMainLooper()).post {
                var shouldResetFaceCaptureSession: Boolean = false
                if (uiHandledError.errorDetails is FaceScanErrorDetails) {
                    shouldResetFaceCaptureSession =
                        (uiHandledError.errorDetails as FaceScanErrorDetails).shouldResetFaceCaptureSession
                }
                intent
                    .getByteArrayExtra(EXTRA_IMG_BYTES)
                    ?.let {
                        if (shouldResetFaceCaptureSession) {
                            resetSession()
                        } else {
                            onFaceCaptureCompleted()
                        }
                    } ?: run {
                    resetSession()
                }
            }
        }, {
            //TODO: Check navigate to main activity
            finish()
        })
    }

    /**
     * Show an error dialog to the user. It handles SenseCryptSdkException.LivenessFailed,
     * SenseCryptSdkException.ExtremeHeadPoseDetected, and
     * SenseCryptSdkException.CaptureSessionTimeOut via the UIHelper methods. It also handles
     * unrecoverable errors via the UIHelper.processUnrecoverableError method. For recoverable
     * errors, it shows a dialog using the showRecoverableErrorDialog method.
     *
     * @param error The error that occurred
     */
    private fun showErrorDialog(error: SenseCryptSdkException) {
        // Define a lambda
        val resetSessionCallback: () -> Unit = {
            resetSession()
        }

        when (error) {
            is SenseCryptSdkException.LivenessFailed ->
                UIHelper.showFaceScanRetryDialog(this, resetSessionCallback)

            is SenseCryptSdkException.CaptureSessionFaceMatchFailed ->
                UIHelper.showCaptureSessionErrorDialog(this, R.string.face_capture_error, resetSessionCallback)

            is SenseCryptSdkException.CaptureSessionLostFace ->
                UIHelper.showCaptureSessionErrorDialog(this, R.string.no_face_detected, resetSessionCallback)

            is SenseCryptSdkException.MultipleFacesDetected ->
                UIHelper.showCaptureSessionErrorDialog(this, R.string.multiple_faces_detected, resetSessionCallback)

            is SenseCryptSdkException.ExtremeHeadPoseDetected,
            ->
                UIHelper.showMoveGentlyDialog(this, resetSessionCallback)

            is SenseCryptSdkException.CaptureSessionTimeOut ->
                UIHelper.showTimeOutDialog(this, resetSessionCallback)

            else -> {
                val uiHandledError = ErrorUtil.getUIHandledError(error)
                if (uiHandledError is UIHandledUnrecoverableError) {
                    // Show unrecoverable error dialog
                    UIHelper.processUnrecoverableError(this, uiHandledError.errorDetails) {
                        //TODO: Go to Main Activity
                        finish()
                    }
                } else {
                    val recoverableError = uiHandledError as UIHandledRecoverableError
                    showRecoverableErrorDialog(recoverableError)
                }
            }
        }
    }

    /**
     * This method is used to show an error dialog in the UI thread.
     * It ensures that the error dialog is shown on the main thread.
     *
     * @param error The error that occurred
     */
    private fun showErrorInUIThread(error: SenseCryptSdkException) {
        if (UIHelper.isDialogShowing()) {
            return
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on the main thread, directly show the error
            showErrorDialog(error)
        } else {
            // Not on the main thread, switch to the main thread
            runOnUiThread {
                showErrorDialog(error)
            }
        }
    }

    /**
     * Called when the first frame is captured.
     *
     * This allows the activity to align the camera preview and the circle overlay
     */
    override fun onFirstFrameCaptured() {
        (this as LifecycleOwner).lifecycleScope.launch(Dispatchers.Main) {
            binding.circleArcView.post {
                val circleCenterY = binding.circleOverlayRegion.centerY

                val params = binding.circleArcView.layoutParams as FrameLayout.LayoutParams

                params.height =
                    (binding.circleOverlayRegion.bottom - binding.circleOverlayRegion.top).toInt() + (getScreenWidth() * 0.21).toInt()
                params.width =
                    (binding.circleOverlayRegion.right - binding.circleOverlayRegion.left).toInt() + (getScreenWidth() * 0.21).toInt()
                val margin = circleCenterY - params.height / 2
                val marginX = binding.circleOverlayRegion.centerX - params.width / 2
                params.topMargin = margin
                params.leftMargin = marginX
                binding.circleArcView.layoutParams = params
                binding.circleArcView.visibility = View.VISIBLE
            }

            binding.pvCamera.post {
                val circleCenterY = binding.circleOverlayRegion.centerY

                // Makes the preview view the same size as the circle overlay
                binding.pvCamera.layoutParams =
                    binding.pvCamera.layoutParams.apply {
                        height = binding.circleOverlayRegion.circleHeight.toInt()
                        width = binding.circleOverlayRegion.circleWidth.toInt()
                        val margin = circleCenterY - height / 2
                        (this as FrameLayout.LayoutParams).topMargin = margin
                    }

                // After setting the layout params, make the preview view visible
                binding.pvCamera.visibility = View.VISIBLE
                binding.pvCamera.requestLayout()
            }

            binding.lottieFaceInstruction.post {
                val previewHeight = binding.circleArcView.height

                val circleCenterY = binding.circleOverlayRegion.centerY

                val margin = circleCenterY - previewHeight / 2
                // Set the margin for the preview view

                val params = binding.lottieFaceInstruction.layoutParams as FrameLayout.LayoutParams
                params.topMargin = margin + (margin / 5)
                binding.lottieFaceInstruction.layoutParams = params
            }
        }
    }

    /**
     * Called when a processing result is available for a camera frame
     *
     * @param result the processing result
     */
    override fun onProcessingResultAvailable(result: ActiveFaceCaptureProcessingResult) {
        if (session.isCompleted()) {
            // Some haptic feedback for the user
            vibrate()

            lifecycleScope.launch {
                // Switch to the IO dispatcher for heavy computation
                withContext(Dispatchers.IO) {
                    try {
                        session.finalize()

//                        checkFaceLiveness()
                        onFaceCaptureCompleted()
                    } catch (e: SenseCryptSdkException) {
                        withContext(Dispatchers.Main) {
                            showErrorInUIThread(e)
                        }
                    }
                }
            }
        }
        runOnUiThread {
            updateUI(result)
        }
    }

    /**
     * Called when there is an error while processing a frame
     *
     * @param exception the exception that occurred
     */
    override fun onProcessingError(exception: SenseCryptSdkException) {
        showErrorInUIThread(exception)
    }

    /**
     * Called for an on device liveness check after the face capture session is complete
     */
    private fun checkFaceLiveness() {
        val isLive =
            session.getBestFrame()?.let { frame ->
                // Check if the face is live by passing the best frame from the session to the SDK
                CryptUtil.isLiveFace(frame)
            } ?: false
        if (isLive) {
            onFaceCaptureCompleted()
        } else {
            showErrorInUIThread(SenseCryptSdkException.LivenessFailed("Liveness check failed"))
        }
    }

    private fun createWalletAndGenerateQRCode(session: SessionHolder) {
        createZelfPrint(session)
    }

    /**
     * Create the Zelfprint from the metadata
     */
    private fun createZelfPrint(sessionHolder: SessionHolder) {
        DataHolder.getInstance().clear()
        val password: String?
        if (pssw.isEmpty()) {
            password = null
        } else {
            password = pssw
        }
        lifecycleScope.launch {
            _isLoading.value = true
            val wordsCount = when(mnemonicSize) {
                MnemonicSize.MNEMONIC12 -> 12
                MnemonicSize.MNEMONIC24 -> 24
            }
            val faceBytes = session.getBestFrame()
            val faceBase64 = Base64.encodeToString(faceBytes, Base64.DEFAULT)
                .replace(Regex("""(\r\n)|\n"""), "")
                .replace(" ", "")
            try {
                val zelfWalletResponse: ZelfWalletResponse?
                if(mnemonicWords.isEmpty()) {
                    zelfWalletResponse = CryptUtil.generateZelfPrint(
                        znsName ?: "",
                        wordsCount,
                        faceBase64,
                        password
                    )

                    mnemonicWords = zelfWalletResponse?.metadata?.mnemonic ?: ""
                } else {
                    zelfWalletResponse = CryptUtil.importZelfPrint(
                        znsName ?: "",
                        mnemonicWords,
                        faceBase64,
                        password
                    )
                }
                val qrImgBase64 = zelfWalletResponse?.image
                qrImageBytes = qrImgBase64?.let {
                    val base64Str = it.replace("data:image/png;base64,", "")
                    Base64.decode(base64Str, Base64.DEFAULT)
                }
                _isLoading.value = false
                qrImageBytes?.let {
                    startActivity(
                        ShowQRActivity.newIntent(
                            this@FaceScanActivity,
                            znsName ?: "",
                            mnemonicWords,
                            it
                        )
                    )
                    finish()
                }
            } catch (e: IOException) {
                //TODO: Manage ioexception
                _isLoading.value = false
            } catch (e: HttpException) {
                _isLoading.value = false
                val errorStr = e.response()?.errorBody()?.string()?.let { JSONObject(it) }?.get("error")
                val err = when(errorStr) {
                    "ERR_INVALID_IMAGE" -> SenseCryptSdkException.UnexpectedException("ERR_INVALID_IMAGE")
                    "ERR_NO_FACE_DETECTED" -> SenseCryptSdkException.NoFaceDetected("ERR_NO_FACE_DETECTED")
                    "ERR_MULTIPLE_FACES_DETECTED" -> SenseCryptSdkException.MultipleFacesDetected("ERR_MULTIPLE_FACES_DETECTED")
                    "ERR_REF_FACE_NOT_MATCHED" -> SenseCryptSdkException.DecryptionFailed("ERR_REF_FACE_NOT_MATCHED")
                    "ERR_VERIFICATION_FAILED" -> SenseCryptSdkException.DecryptionFailed("ERR_VERIFICATION_FAILED")
                    "ERR_LIVENESS_FAILED" -> SenseCryptSdkException.LivenessFailed("ERR_LIVENESS_FAILED")
                    "ERR_LIVENESS_FACE_ANGLE_TOO_LARGE" -> SenseCryptSdkException.ExtremeHeadPoseDetected("ERR_LIVENESS_FACE_ANGLE_TOO_LARGE")
                    "ERR_LIVENESS_FACE_CLOSE_TO_BORDER"-> SenseCryptSdkException.ExtremeHeadPoseDetected("ERR_LIVENESS_FACE_CLOSE_TO_BORDER")
                    else -> SenseCryptSdkException.UnexpectedException("UNEXPECTED")
                }
                showGeneralErrorDialog(err, sessionHolder)
            }
        }
    }

    /**
     * Show the dialog for multiple faces detected if the error is returned
     * by the SDK (after calling the server)
     */
    private fun showMultipleFacesDetectedDialog() {
        // We handle this error slightly differently as it is not
        // a verification error unlike what is returned by ErrorUtil
        UIHelper.showCaptureSessionErrorDialog(this, R.string.multiple_faces_detected) {
            finish()
        }
    }

    /**
     * Show general error dialog
     *
     * @param error the error returned by the SDK
     */
    private fun showGeneralErrorDialog(error: SenseCryptSdkException, sessionHolder: SessionHolder) {
        val uiHandledError = ErrorUtil.getUIHandledError(error)
        if (uiHandledError is UIHandledUnrecoverableError) {
            UIHelper.processUnrecoverableError(this, uiHandledError.errorDetails) {
                //TODO: Go to main activity
                finish()
            }
        } else {
            UIHelper.processRecoverableError(
                this,
                uiHandledError.errorDetails,
                {
                    Handler(Looper.getMainLooper()).post {
                        createZelfPrint(sessionHolder)
                    }
                },
                {
                    //TODO: Go to main activity
                    finish()
                },
            )
        }
    }
}
