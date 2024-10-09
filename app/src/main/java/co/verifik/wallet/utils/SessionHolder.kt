package co.verifik.wallet.utils

import com.sensecrypt.sdk.core.ActiveFaceCaptureSession
import com.sensecrypt.sdk.core.DecryptedSensePrintData
import com.sensecrypt.sdk.core.PassiveFaceCaptureSession
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintQrMobileRequest
import com.sensecrypt.sdk.core.SensePrintRawVerificationMobileRequest

/**
 * SessionHolder is a class that holds either an ActiveFaceCaptureSession or a
 * PassiveFaceCaptureSession and provides a common interface for interacting
 * with either session type.
 */
class SessionHolder(
    private var activeFaceCaptureSession: ActiveFaceCaptureSession?,
    private var passiveFaceCaptureSession: PassiveFaceCaptureSession?,
) {
    /**
     * Constructor that takes a session and assigns it to the appropriate session type
     *
     * @param session - the session to assign
     *
     * @throws IllegalArgumentException - if the session type is invalid
     */
    constructor(session: Any) : this(null, null) {
        when (session) {
            is ActiveFaceCaptureSession -> {
                activeFaceCaptureSession = session
                passiveFaceCaptureSession = null
            }

            is PassiveFaceCaptureSession -> {
                activeFaceCaptureSession = null
                passiveFaceCaptureSession = session
            }

            else -> {
                throw IllegalArgumentException("Invalid session type")
            }
        }
    }

    /**
     * Create a QR code for the given request
     *
     * @param request The SensePrintQrMobileRequest
     * @return The QR code as a byte array
     * @throws SenseCryptSdkException If an error occurs during the operation
     */
    @Throws(SenseCryptSdkException::class)
    fun createQrCode(request: SensePrintQrMobileRequest): ByteArray {
        val result =
            activeFaceCaptureSession?.createQrCode(request) ?: run {
                passiveFaceCaptureSession?.createQrCode(request)
            }
        return result!!
    }

    /**
     * Verify a senseprint for the given request
     *
     * @param request The SensePrintRawVerificationMobileRequest
     * @return The DecryptedSensePrintData
     * @throws SenseCryptSdkException If an error occurs during the operation
     */
    @Throws(SenseCryptSdkException::class)
    fun verifySenseprint(request: SensePrintRawVerificationMobileRequest): DecryptedSensePrintData {
        val result =
            activeFaceCaptureSession?.verifySenseprint(request) ?: run {
                passiveFaceCaptureSession?.verifySenseprint(request)
            }
        return result!!
    }
}
