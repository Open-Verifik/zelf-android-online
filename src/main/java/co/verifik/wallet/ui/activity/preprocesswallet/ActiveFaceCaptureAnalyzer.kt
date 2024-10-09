package co.verifik.wallet.ui.activity.preprocesswallet

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import co.verifik.wallet.utils.ImageUtil
import com.sensecrypt.sdk.core.ActiveFaceCaptureSession
import com.sensecrypt.sdk.core.SenseCryptSdkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This class is used to analyze the pose of the user's face.
 */
class ActiveFaceCaptureAnalyzer(
    /**
     * The listener for the active face capture session
     */
    private val listener: ActiveFaceCaptureSessionListener,
    /**
     * The active face capture session
     */
    private var session: ActiveFaceCaptureSession,
    /**
     * Flag to check if the first frame has been notified
     */
    private var isFirstFrameNotified: Boolean = false,
) : ImageAnalysis.Analyzer {

    private var frameSkipper: Int = 0

    /**
     * Analyzes the image and processes it using the Zelf SDK
     *
     * @param image The image to be analyzed
     */
    override fun analyze(image: ImageProxy) {
        // Applies a one time UI adjustment to the camera preview
        // to align the face circle overlay with the camera preview
        if (!isFirstFrameNotified) {
            isFirstFrameNotified = true
            listener.onFirstFrameCaptured()
        }

        try {
            // Return an intermediate result if available
            val processingStatus = session.getProcessingStatus()

            // If the processing is in progress and the session is not completed
            if (processingStatus.isProcessing && !session.isCompleted()) {
                // Then just use the intermediate result
                val intermediateResult = processingStatus.intermediateResult

                intermediateResult?.let { result ->
                    CoroutineScope(Dispatchers.Main).launch {
                        listener.onProcessingResultAvailable(result)
                    }
                }
                image.close()
                return
            } else if (session.isCompleted()) {
                // If the session is complete, then return
                image.close()
                return
            }
        } catch (e: SenseCryptSdkException) {
            // If there is an error, it would have already been notified
            // to the listener through the process method
            image.close()
            return
        }

        // Use CoroutineScope for launching coroutines
        CoroutineScope(Dispatchers.IO).launch {
            // crop image to 480 * 480
            val centerCropImage = ImageUtil.getCenterCropBitmap(image, true)
            // convert it into byteArray
            val imgBytes = ImageUtil.bitMap2ByteArray(centerCropImage)
            image.close()
            // pass image to sdk
            processFrame(imgBytes)
        }
    }

    /**
     * Processes the frame using the SDK
     *
     * @param imgBytes The image bytes to be processed (As JPEG bytes)
     */
    private suspend fun processFrame(imgBytes: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val result = session.process(imgBytes)
                listener.onProcessingResultAvailable(result)
            } catch (e: SenseCryptSdkException) {
                if (!session.isErrorNotified()) {
                    listener.onProcessingError(
                        e,
                    )
                }
            }
        }
    }
}
