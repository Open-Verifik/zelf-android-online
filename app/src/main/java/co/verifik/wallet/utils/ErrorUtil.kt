package co.verifik.wallet.utils

import co.verifik.wallet.R
import co.verifik.wallet.data.domain.ErrorDetails
import co.verifik.wallet.data.domain.FaceScanErrorDetails
import co.verifik.wallet.data.domain.UIHandledError
import co.verifik.wallet.data.domain.UIHandledRecoverableError
import co.verifik.wallet.data.domain.UIHandledUnrecoverableError
import com.sensecrypt.sdk.core.SenseCryptSdkException

/**
 * ErrorUtil maps the SenseCryptSdkException to a UIHandledError so that it can be shown in
 * a dialog to the user.
 */
class ErrorUtil {
    companion object {
        /**
         * Get the UIHandledError for the given SenseCryptSdkException
         *
         * @param exception The SenseCryptSdkException
         * @return The UIHandledError
         */
        fun getUIHandledError(exception: SenseCryptSdkException): UIHandledError =
            when (exception) {
                is SenseCryptSdkException.NetworkCallFailed ->
                    UIHandledRecoverableError(
                        ErrorDetails(
                            R.string.network_error,
                            R.string.network_error_detail,
                        ),
                    )

                is SenseCryptSdkException.LivenessFailed ->
                    UIHandledRecoverableError(
                        FaceScanErrorDetails(
                            R.string.verification_failed,
                            R.string.liveness_failed,
                            true,
                        ),
                    )

                is SenseCryptSdkException.ServerAuthorizationFailed ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.server_auth_failed,
                            R.string.server_auth_failed,
                        ),
                    )

                is SenseCryptSdkException.LicenseExpired ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.license_expired,
                            R.string.license_expired_detail,
                        ),
                    )

                is SenseCryptSdkException.CannotConnectToHomeServer ->
                    UIHandledRecoverableError(
                        ErrorDetails(
                            R.string.server_unreachable,
                            R.string.server_unreachable_detail,
                        ),
                    )

                is SenseCryptSdkException.UnexpectedException ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.unkown_error,
                            R.string.unknown_error_detail,
                        ),
                    )

                is SenseCryptSdkException.NumberOfAvailableInstancesExceeded ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.instance_exceeded,
                            R.string.instance_exceeded_detail,
                        ),
                    )

                is SenseCryptSdkException.DecryptionFailed ->
                    UIHandledRecoverableError(
                        FaceScanErrorDetails(
                            R.string.verification_failed,
                            R.string.no_match_detail,
                            true,
                        ),
                    )

                is SenseCryptSdkException.FeatureNotAvailable ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.feature_not_available_title,
                            R.string.feature_not_available_detail,
                        ),
                    )

                is SenseCryptSdkException.ImageDecodeFailed ->
                    UIHandledUnrecoverableError(
                        FaceScanErrorDetails(
                            R.string.image_decode_failed,
                            R.string.err_parse_detail,
                            true,
                        ),
                    )

                is SenseCryptSdkException.MultipleFacesDetected ->
                    UIHandledRecoverableError(
                        FaceScanErrorDetails(
                            R.string.verification_failed,
                            R.string.err_multiple_faces_detected,
                            true,
                        ),
                    )

                is SenseCryptSdkException.NoFaceDetected ->
                    UIHandledRecoverableError(
                        FaceScanErrorDetails(
                            R.string.verification_failed,
                            R.string.err_no_face_detected,
                            true,
                        ),
                    )

                is SenseCryptSdkException.InvalidSensePrint ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.invalid_print,
                            R.string.invalid_print,
                        ),
                    )

                is SenseCryptSdkException.InvalidLicenseFile ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.in_license_error,
                            R.string.invalid_license_error,
                        ),
                    )

                is SenseCryptSdkException.SensePrintGenerationQuotaExceeded ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.generation_exceeded,
                            R.string.limit_exceed_title,
                        ),
                    )

                is SenseCryptSdkException.SensePrintVerificationQuotaExceeded ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.verification_exceeded,
                            R.string.limit_exceed_title,
                        ),
                    )

                is SenseCryptSdkException.SignatureVerificationFailed ->
                    UIHandledUnrecoverableError(
                        FaceScanErrorDetails(
                            R.string.verification_failed,
                            R.string.err_signature_failed,
                            true,
                        ),
                    )

                is SenseCryptSdkException.CaptureSessionTimeOut ->
                    UIHandledRecoverableError(
                        FaceScanErrorDetails(
                            R.string.verification_failed,
                            R.string.no_match_detail,
                            true,
                        ),
                    )

                is SenseCryptSdkException.LicenseNotFound ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.license_not_found,
                            R.string.license_not_found_detail,
                        ),
                    )

                is SenseCryptSdkException.SdkNotInitialized ->
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.initialization_error,
                            R.string.initialization_error_detail,
                        ),
                    )

                else -> {
                    UIHandledUnrecoverableError(
                        ErrorDetails(
                            R.string.unkown_error,
                            R.string.unknown_error_detail,
                        ),
                    )
                }
            }
    }
}
