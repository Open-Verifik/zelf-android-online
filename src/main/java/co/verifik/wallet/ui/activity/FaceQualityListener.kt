package co.verifik.wallet.ui.activity

import com.sensecrypt.sdk.core.HeadPose
import com.sensecrypt.sdk.core.SenseCryptSdkException

interface FaceQualityListener {
    fun processQuality(
        pose: HeadPose,
        acceptedImageBytes: ByteArray?,
        exception: SenseCryptSdkException?,
    )
}
