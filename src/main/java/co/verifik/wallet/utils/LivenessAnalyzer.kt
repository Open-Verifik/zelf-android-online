package co.verifik.wallet.utils

import android.app.Application
import android.content.Context
import co.verifik.wallet.Constants
import com.sensecrypt.sdk.core.SenseCryptSdkException
import java.util.Optional

/**
 * LivenessAnalyzer is a class that analyzes the liveness of a face using the SenseCrypt SDK
 */
class LivenessAnalyzer {
    /**
     * The face analyzer for liveness checks
     */
    private var faceAnalyzer: Any? = null

    /**
     * The image decoder for liveness checks
     */
    private var imgDecoder: Any? = null

    constructor(application: Application) {
        try {
            // Get the AndroidSupport class and create a blueprint using reflection
            val androidSupportClass = Class.forName("net.idrnd.idliveface.android.AndroidSupport")
            val createBlueprintMethod = androidSupportClass.getMethod("createBlueprint", Context::class.java)
            val blueprint = createBlueprintMethod.invoke(null, application)

            // Get the blueprint class and use it to create the faceAnalyzer and imgDecoder objects
            val createFaceAnalyzerMethod = blueprint::class.java.getMethod("createFaceAnalyzer", String::class.java)
            faceAnalyzer = createFaceAnalyzerMethod.invoke(blueprint, Constants.LIVENESS_PIPELINE)

            val createImageDecoderMethod = blueprint::class.java.getMethod("createImageDecoder")
            imgDecoder = createImageDecoderMethod.invoke(blueprint)
        } catch (e: Exception) {
            throw SenseCryptSdkException.SdkNotInitialized("Failed to init LivenessSDK")
        }
    }

    /**
     * Check if the face is live or not
     * @param imageBytes image byte data
     * @return true if face is live
     */
    fun isLiveFace(imageBytes: ByteArray): Boolean {
        try {
            // Decode the image
            val decodeMethod = imgDecoder?.javaClass?.getMethod("decode", ByteArray::class.java)
            val image = decodeMethod?.invoke(imgDecoder, imageBytes)

            // Create the FaceAnalysisParameters using reflection
            val faceAnalysisParamsClass = Class.forName("net.idrnd.idliveface.FaceAnalysisParameters")
            val faceAnalysisParams = faceAnalysisParamsClass.kotlin.constructors.first().call()

            // Set the parameters dynamically
            val setDomainMethod = faceAnalysisParamsClass.getMethod("setDomain", Class.forName("net.idrnd.idliveface.Domain"))
            val domainEnum = Class.forName("net.idrnd.idliveface.Domain").getField("GENERAL").get(null)
            setDomainMethod.invoke(faceAnalysisParams, domainEnum)

            // set the tolerance dynamically
            val toleranceClass = Class.forName("net.idrnd.idliveface.Tolerance")
            val setToleranceMethod = faceAnalysisParamsClass.getMethod("setTolerance", toleranceClass)
            setToleranceMethod.invoke(faceAnalysisParams, mapTolerance(Constants.LIVENESS_TOLERANCE))

            // Analyze the face
            val analyzeMethod = faceAnalyzer?.javaClass?.getMethod("analyze", image?.javaClass, faceAnalysisParamsClass)
            val result = analyzeMethod?.invoke(faceAnalyzer, image, faceAnalysisParams)

            // Get the genuineProbability
            val genuineProbabilityMethod = result?.javaClass?.getMethod("getGenuineProbability")
            val genuineProbability = genuineProbabilityMethod?.invoke(result) as? Optional<Float>

            val value = genuineProbability?.map { it > Constants.LIVENESS_THRESHOLD }
            print(value)
            return genuineProbability?.map { it > Constants.LIVENESS_THRESHOLD }!!.orElse(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * Maps the custom LivenessTolerance to the net.idrnd.idliveface.Tolerance enum
     */
    private fun mapTolerance(tolerance: LivenessTolerance): Any {
        return when (tolerance) {
            LivenessTolerance.REGULAR -> Class.forName("net.idrnd.idliveface.Tolerance").getField("REGULAR").get(null)
            LivenessTolerance.SOFT -> Class.forName("net.idrnd.idliveface.Tolerance").getField("SOFT").get(null)
            LivenessTolerance.HARDENED -> Class.forName("net.idrnd.idliveface.Tolerance").getField("HARDENED").get(null)
        }
    }
}
