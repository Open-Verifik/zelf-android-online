package co.verifik.wallet.data.domain

/**
 * UIHandledError is a base class that holds the error details which are handled by the UI
 * by showing an error dialog to the user
 */
open class UIHandledError(
    /**
     * The error details
     */
    val errorDetails: ErrorDetails,
)

/**
 * UIHandledRecoverableError is a class that holds the error details which are recoverable
 * by the user - i.e. the user can retry the action
 */
class UIHandledRecoverableError(
    errorDetails: ErrorDetails,
) : UIHandledError(
        errorDetails,
    )

/**
 * UIHandledUnrecoverableError is a class that holds the error details which are unrecoverable
 * by the user - i.e. the user cannot retry the action
 */
class UIHandledUnrecoverableError(
    errorDetails: ErrorDetails,
) : UIHandledError(
        errorDetails,
    )

/**
 * ErrorDetails is a class that holds the title and message of the error
 */
open class ErrorDetails(
    val title: Int,
    val message: Int,
)

/**
 * FaceScanErrorDetails is a class that holds the title and message of an error that can occur
 * during a face scan and subsequent Zelfprint generation / verification.
 *
 * Additionally it holds a boolean value that indicates whether the face capture session should be reset
 */
class FaceScanErrorDetails(
    title: Int,
    message: Int,
    val shouldResetFaceCaptureSession: Boolean,
) : ErrorDetails(
        title,
        message,
    )
