package co.verifik.wallet.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Base64
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import co.verifik.wallet.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.sensecrypt.sdk.core.ActiveFaceCaptureStateName
import com.sensecrypt.sdk.core.HeadPose
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Convert map to hash map
 *
 * @param data - map to convert
 * @return HashMap<String, String> - converted hash map
 */
fun convertToHashMap(data: Map<String, String>): HashMap<String, String> = HashMap(data)

/**
 * Convert hash map to mutable list
 *
 * @param data - hash map to convert
 * @return MutableList<Pair<String, String>> - converted mutable list
 */
fun convertToMutableList(data: HashMap<String, String>): MutableList<Pair<String, String>> =
    data.map { (key, value) -> key to value }.toMutableList()

/**
 * Get serializable extra from intent
 *
 * @param key - key to get the serializable extra
 * @param mClass - class of the serializable object
 */
fun <T : Serializable?> Intent.getSerializable(
    key: String,
    mClass: Class<T>,
): T =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getSerializableExtra(key, mClass)!!
    } else {
        this.getSerializableExtra(key) as T
    }

/**
 * Convert string to sentence case
 */
fun String.toSentenceCase(): String =
    if (this.isNotEmpty()) {
        this
            .lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    } else {
        this
    }

/**
 * Apply window insets ignoring the top inset
 */
fun View.ignoreTouch() {
    // Apply window insets ignoring the top inset
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = systemBarsInsets.left,
            right = systemBarsInsets.right,
            bottom = systemBarsInsets.bottom,
        )
        // Return the insets unconsumed
        WindowInsetsCompat.CONSUMED
    }
}

fun <K, V> Map<K, V>.toSerializable(): HashMap<String, String> {
    val map = HashMap<String, String>()
    for ((key, value) in this) {
        map[key as String] = value as String
    }
    return map
}

/**
 * Underline the text
 */
fun String.underLine(): SpannableString {
    val spannableString = SpannableString(this)
    spannableString.setSpan(UnderlineSpan(), 0, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannableString
}

/**
 * Get the resource id for the string for an active capture state name
 */
val activeFaceCaptureTextMap =
    mapOf(
        ActiveFaceCaptureStateName.USER_SHOULD_STAY_STILL to R.string.stay_still,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_LEFT to R.string.look_left,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_RIGHT to R.string.look_right,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_UP to R.string.look_up,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_DOWN to R.string.look_down,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_LEFT to R.string.look_top_left,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_RIGHT to R.string.look_top_right,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_LEFT to R.string.look_bottom_left,
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_RIGHT to R.string.look_bottom_right,
        ActiveFaceCaptureStateName.USER_SHOULD_MOVE_CLOSER to R.string.move_closer,
        ActiveFaceCaptureStateName.USER_SHOULD_MOVE_FARTHER to R.string.move_further,
        ActiveFaceCaptureStateName.USER_SHOULD_CENTER_THEIR_FACE to R.string.center_your_face,
        ActiveFaceCaptureStateName.ACTIVE_FACE_CAPTURE_COMPLETE to R.string.we_got_all,
    )

/**
 * Get the resource id for the string for a head pose
 */
val currentHeadPoseInstructionsMap =
    mapOf(
        HeadPose.NORMAL to R.string.empty_string,
        HeadPose.TOO_FAR to R.string.move_closer,
        HeadPose.LOOKING_LEFT to R.string.look_left,
        HeadPose.LOOKING_RIGHT to R.string.look_right,
        HeadPose.LOOKING_UP to R.string.look_down,
        HeadPose.LOOKING_DOWN to R.string.look_up,
        HeadPose.TILTED_LEFT to R.string.look_straight,
        HeadPose.TILTED_RIGHT to R.string.look_straight,
        HeadPose.TOO_CLOSE to R.string.move_further,
        HeadPose.NOT_CENTERED to R.string.center_your_face,
        HeadPose.LOOKING_TOP_LEFT to R.string.look_straight,
        HeadPose.LOOKING_TOP_RIGHT to R.string.look_straight,
        HeadPose.LOOKING_BOTTOM_LEFT to R.string.look_straight,
        HeadPose.LOOKING_BOTTOM_RIGHT to R.string.look_straight,
    )

/**
 * Map of animation resources for each active face capture state
 */
val stateResources =
    mapOf(
        ActiveFaceCaptureStateName.WAITING_FOR_FIRST_CENTERED_FACE to
            Pair(
                "CenterFace.json",
                null,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_LEFT to
            Pair(
                "LoopFullLookLeft.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_LEFT,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_RIGHT to
            Pair(
                "LoopFullLookRight.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TO_THE_RIGHT,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_UP to
            Pair(
                "LoopFullLookUp.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_UP,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_DOWN to
            Pair(
                "LoopFullLookDown.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_DOWN,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_LEFT to
            Pair(
                "LoopFullLookTopLeft.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_LEFT,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_RIGHT to
            Pair(
                "LoopFullLookTopRight.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_TOP_RIGHT,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_LEFT to
            Pair(
                "LoopFullLookBottomLeft.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_LEFT,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_RIGHT to
            Pair(
                "LoopFullLookBottomRight.json",
                ActiveFaceCaptureStateName.USER_SHOULD_LOOK_BOTTOM_RIGHT,
            ),
        ActiveFaceCaptureStateName.USER_SHOULD_MOVE_CLOSER to Pair("LoopFullMoveCloser.json", null),
        ActiveFaceCaptureStateName.USER_SHOULD_MOVE_FARTHER to Pair("LoopFullMoveFarther.json", null),
    )

/**
 * Set of states that should show center face message based on the current head pose
 * (regardless of the pose)
 */
val showCurrentHeadPose =
    setOf(
        ActiveFaceCaptureStateName.WAITING_FOR_FIRST_CENTERED_FACE,
    )

/**
 * Set of states that should show center face message based on the current head pose
 * (if the pose is not normal)
 */
val checkHeadPose =
    setOf(
        ActiveFaceCaptureStateName.USER_SHOULD_STAY_STILL,
    )

/**
 * Set of states where the user is expected to center their face and
 * have their face in a normal pose in the center of the camera, and
 * at an appropriate distance from the camera
 */
val shouldCenter =
    setOf(
        ActiveFaceCaptureStateName.USER_SHOULD_CENTER_THEIR_FACE,
        ActiveFaceCaptureStateName.USER_SHOULD_STAY_STILL,
        ActiveFaceCaptureStateName.WAITING_FOR_FIRST_CENTERED_FACE,
        ActiveFaceCaptureStateName.USER_SHOULD_MOVE_CLOSER,
        ActiveFaceCaptureStateName.USER_SHOULD_MOVE_FARTHER,
        ActiveFaceCaptureStateName.USER_SHOULD_CENTER_THEIR_FACE,
    )

/**
 * Slight adjustment to the margin top for tablets
 *
 * @param context - context
 * @return Int - margin top
 */
fun getDesiredMarginTop(context: Context): Int {
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels

    val marginTop = if (context.isTablet()) screenHeight * 0.25 else screenHeight * 0.14

    return marginTop.toInt()
}

/**
 * Check if the device is a tablet
 *
 * @return Boolean - true if the device is a tablet
 */
fun Context.isTablet(): Boolean = resources.configuration.smallestScreenWidthDp >= 600

/**
 * Get the screen width in pixels
 *
 * @return Int - screen width
 */
fun getScreenWidth(): Int {
    val displayMetrics = Resources.getSystem().displayMetrics
    return displayMetrics.widthPixels
}

/**
 * Get the screen height in pixels
 *
 * @return Int - screen height
 */
fun getScreenHeight(): Int {
    val displayMetrics = Resources.getSystem().displayMetrics
    return displayMetrics.heightPixels
}

/**
 * Process the bitmap to get the QR bytes inside
 * @param bitmap - bitmap to process
 * @param process - callback when done getting the QR bytes
 */
suspend fun processBitmapToGetQrBytes(bitmap: Bitmap): ByteArray? = suspendCoroutine { continuation ->
    val scanner = BarcodeScanning.getClient()

    val r = scanner.process(bitmap, 0)
        .addOnSuccessListener { barcodes ->
            // Task completed successfully
            if (barcodes.isNotEmpty()) {
                val qrResult = barcodes[0]
                continuation.resume(qrResult.rawBytes)
            } else {
                continuation.resume(null)
            }
        }
        .addOnFailureListener {
            // Task failed with an exception
            // ...
            continuation.resume(null)
        }
}