package co.verifik.wallet.ui.activity.preprocesswallet

import com.sensecrypt.sdk.core.ActiveFaceCaptureProcessingResult
import com.sensecrypt.sdk.core.SenseCryptSdkException

/**
 * ActiveFaceCaptureSessionListener is an interface that
 * listens to the processing result/error from an Active Face
 * Capture session
 */
interface ActiveFaceCaptureSessionListener {
    /**
     * Notify the listener that the first frame has been captured.
     *
     * This method is called when the first frame is captured
     * and an Activity or Fragment can update the UI accordingly
     */
    fun onFirstFrameCaptured()

    /**
     * Update the processing result.
     *
     * This method is called when a processing result is
     * available and an Activity or Fragment can update the
     * UI accordingly
     *
     * @param result The processing result
     */
    fun onProcessingResultAvailable(result: ActiveFaceCaptureProcessingResult)

    /**
     * Notify the listener that an error occurred during processing.
     *
     * This method is called when an error occurs during the
     * processing and an Activity or Fragment can show an error
     * dialog to the user
     *
     * @param exception The exception that occurred during processing
     */
    fun onProcessingError(exception: SenseCryptSdkException)
}
