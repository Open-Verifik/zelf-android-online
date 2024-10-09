package co.verifik.wallet

import android.app.Activity
import android.app.Application
import co.verifik.wallet.data.remote.IPFSResponse
import co.verifik.wallet.data.remote.ZelfAuthResponse
import co.verifik.wallet.data.remote.ZelfWalletResponse
import co.verifik.wallet.ui.UIHelper.Companion.showLoadingDialog
import co.verifik.wallet.utils.LivenessAnalyzer
import co.verifik.wallet.utils.SessionHolder
import co.verifik.wallet.utils.network.ApiZelfInterface
import co.verifik.wallet.utils.network.ZelfApiClient
import com.sensecrypt.sdk.core.ActiveFaceCaptureSession
import com.sensecrypt.sdk.core.DecryptedSensePrintData
import com.sensecrypt.sdk.core.LicenseInfo
import com.sensecrypt.sdk.core.PassiveFaceCaptureSession
import com.sensecrypt.sdk.core.QrFormatSchema
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintInfo
import com.sensecrypt.sdk.core.SensePrintQrMobileRequest
import com.sensecrypt.sdk.core.SensePrintRawVerificationMobileRequest
import com.sensecrypt.sdk.core.SensePrintToleranceSchema
import com.sensecrypt.sdk.core.initOnlineSdk
import com.sensecrypt.sdk.core.parseSenseprintBytes
import com.sensecrypt.sdk.core.setIssuersPublicKey
import com.sensecrypt.sdk.core.verifyPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.jvm.Throws

class CryptUtil {
    companion object {
        /**
         * The face analyzer for liveness checks
         */
        private lateinit var livenessAnalyzer: LivenessAnalyzer


        private fun performInit(application: Application): Result<Boolean> {
            try {
                // Initialize the Zelfcrypt SDK
                initOnlineSdk(Constants.API_SERVER_URL, Constants.MOBILE_AUTH_HEADER)

                Constants.ISSUERS_PUBLIC_KEY?.let {
                    setIssuersPublicKey(it)
                }

                // init LivenessAnalyzer only in offline mode
                // livenessAnalyzer = LivenessAnalyzer(application)

                return Result.success(true)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        /**
         * Initialize the SDK
         *
         * @param application The application
         * @param callback The callback to be invoked when the initialization is complete
         */
        fun init(
            application: Application,
            callback: (Result<Boolean>) -> Unit,
        ) {
            GlobalScope.launch(Dispatchers.Main) {
                // Switch to the IO dispatcher for heavy computation
                val result =
                    withContext(Dispatchers.IO) {
                        performInit(application)
                    }

                // Process the result on the UI thread
                callback(result)
            }
        }

        /**
         * Initialize the SDK synchronously
         *
         * @param application The application
         * @return true if the initialization is successful, false otherwise
         */
        private fun initSynchronously(application: Application): Boolean {
            val result = performInit(application)
            return result.isSuccess
        }

        /**
         * Check if the SDK is initialized
         * @return true if the SDK is initialized, false otherwise
         */
        private fun isInitialized(): Boolean =
            com.sensecrypt.sdk.core
                .isInitialized()

        /**
         * Check if the face is live or not
         * @param imageBytes image byte data
         * @return true if face is live or if SDKMode is Online
         */
        fun isLiveFace(imageBytes: ByteArray): Boolean {
            return true
            //FOR OFFLINE MODE
            //return livenessAnalyzer.isLiveFace(imageBytes)
        }

        /**
         * Get the QR code info from the decoded QR code bytes.
         * This doesn't decrypt the SensePrint.
         *
         * @param qrCodeDecodedBytes The QR code image bytes
         *
         * @return The QR code info
         * @throws SenseCryptSdkException
         */
        @Throws(SenseCryptSdkException::class)
        fun parseSensePrintBytes(qrCodeDecodedBytes: ByteArray): SensePrintInfo? =
            parseSenseprintBytes(
                qrCodeDecodedBytes,
                Constants.VERIFIER_AUTH_KEY,
            )

        /**
         * Get the license info
         *
         * @return The license info
         */
        fun getLicenseInfo(): LicenseInfo =
            com.sensecrypt.sdk.core
                .getLicenseInfo()

        /**
         * Get the version of the SDK
         *
         * @return The version of the SDK
         */
        fun getSDKVersion(): String =
            com.sensecrypt.sdk.core
                .getSdkVersion()

        /**
         * Get the device ID
         *
         * @return The generated device ID
         */
        fun getDeviceId(): String? =
            com.sensecrypt.sdk.core
                .getDeviceId()

        /**
         * Get the app thumbprint
         *
         * @return The app thumbprint
         */
        fun getAppThumbprint(): String? =
            com.sensecrypt.sdk.core
                .getAppThumbprint()

        /**
         * Verify the password
         *
         * @param sensePrintBytes The SensePrint bytes from the QR code info
         * @param password The password
         *
         * @return true if the password is correct, false otherwise
         * @throws SenseCryptSdkException
         */
        @Throws(SenseCryptSdkException::class)
        fun verifyPassword(
            sensePrintBytes: ByteArray,
            password: String,
        ): Boolean {
            try {
                return verifyPassword(
                    sensePrintBytes,
                    password,
                    Constants.VERIFIER_AUTH_KEY,
                )
            } catch (e: SenseCryptSdkException) {
                throw e
            }
        }

        /**
         * Generate a SensePrint QR code
         *
         * @param record  record ID
         * @param isLivenessEnabled Whether liveness check is enabled
         * @param metaData The metadata
         * @param clearTextData The unencrypted text data
         * @param password The password
         * @param sessionHolder The session holder containing the active or passive face capture session
         *
         * @return The SensePrint Qr code bytes
         * @throws SenseCryptSdkException
         */
        @Throws(SenseCryptSdkException::class)
        fun generateSensePrint(
            record: String,
            isLivenessEnabled: Boolean,
            metaData: HashMap<String, String>,
            clearTextData: HashMap<String, String>,
            password: String?,
            sessionHolder: SessionHolder,
        ): ByteArray? {
            val requestSchema =
                SensePrintQrMobileRequest(
                    recordId = record,
                    requireLiveFace = isLivenessEnabled,
                    metadata = metaData,
                    cleartextData = clearTextData,
                    password = password,
                    tolerance = SensePrintToleranceSchema.REGULAR,
                    qrFormat = QrFormatSchema.PNG,
                    refFace = null,
                    verifiersAuthKey = Constants.VERIFIER_AUTH_KEY,
                    livenessTolerance = null,
                    checkLiveFaceBeforeCreation = false,
                )

            return try {
                // make the request to the server through the SDK
                sessionHolder.createQrCode(
                    requestSchema,
                )
                // return the qr code bytes or throw an exception if there is an error in the response
            } catch (e: SenseCryptSdkException) {
                throw e
            }
        }


        suspend fun getZelfAuth(): ZelfAuthResponse? {
            val zelfClient = ZelfApiClient().getApiClient().create(ApiZelfInterface::class.java)
            //Temps variables
            val apiKey = "client_07df4a5606862f67c1c17707"
            val email = "carlos@verifik.co"
            val params = mapOf(
                "email" to email
            )
            return zelfClient.auth(
                apiKey,
                params
            )
        }

        /**
         * Find the corresponding IPFS by its zelfName
         *
         * @param zelfName the zelf identifier of the wallet
         *
         * @return The IPFSResponse
         *
         */
        suspend fun findIPFS(
            zelfName: String
        ): IPFSResponse? {
            val zelfClient = ZelfApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val zelfAuthResponse = getZelfAuth()
            if (zelfAuthResponse?.token == null) {
                return null
            }
            val token = "Bearer ${zelfAuthResponse.token}"
            val completeZelfName = "$zelfName.zelf"
            val params = mutableMapOf(
                "key" to "zelfName",
                "value" to completeZelfName
            )
            return zelfClient.searchIPFS(
                token,
                params
            ).data?.first()
        }

        /**
         * Find the corresponding IPFS by its publicAddress
         *
         * @param publicAddress the public eth or Solana address of the wallet
         *
         * @return The IPFSResponse
         *
         */
        suspend fun findIPFSByPublicAddress(
            publicAddress: String
        ): IPFSResponse? {
            val zelfClient = ZelfApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val zelfAuthResponse = getZelfAuth()
            if (zelfAuthResponse?.token == null) {
                return null
            }
            val token = "Bearer ${zelfAuthResponse.token}"
            val params = mutableMapOf(
                "key" to "ethAddress",
                "value" to publicAddress
            )
            try {
                val ipfs = zelfClient.searchIPFS(
                    token,
                    params
                ).data?.first()
                if (ipfs != null) {
                    return ipfs
                }
            } catch (_: HttpException) { }

            val params2 = mutableMapOf(
                "key" to "solanaAddress",
                "value" to publicAddress
            )
            return zelfClient.searchIPFS(
                token,
                params2
            ).data?.first()
        }


        /**
         * Generate a ZelfPrint QR code
         *
         * @param words the mnemonic size
         * @param face base64 img of the face
         * @param password The password
         *
         * @return The ZelfWalletCreatedResponse
         *
         */
        suspend fun generateZelfPrint(
            znsName: String,
            words: Int,
            face: String,
            password: String?
        ): ZelfWalletResponse? {
            val zelfClient = ZelfApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val zelfAuthResponse = getZelfAuth()
            if (zelfAuthResponse?.token == null) {
                return null
            }
            val znsCompleteName = "$znsName.zelf"
            val token = "Bearer ${zelfAuthResponse.token}"
            val params = mutableMapOf(
                "zelfName" to znsCompleteName,
                "wordsCount" to words,
                "faceBase64" to face,
                "seeWallet" to true,
                "removePGP" to true
            )
            if (password != null) {
                params["password"] = password
            }
            return zelfClient.createWallet(
                token,
                params
            ).data
        }

        /**
         * Import a ZelfPrint QR code
         *
         * @param mnemonicPhrase the mnemonic words for the wallet
         * @param face base64 img of the face
         * @param password The password
         *
         * @return The ZelfWalletCreatedResponse
         *
         */
        suspend fun importZelfPrint(
            znsName: String,
            mnemonicPhrase: String,
            face: String,
            password: String?
        ): ZelfWalletResponse? {
            val zelfClient = ZelfApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val zelfAuthResponse = getZelfAuth()
            if (zelfAuthResponse?.token == null) {
                return null
            }
            val znsCompleteName = "$znsName.zelf"
            val token = "Bearer ${zelfAuthResponse.token}"
            val params = mutableMapOf(
                "zelfName" to znsCompleteName,
                "phrase" to mnemonicPhrase,
                "faceBase64" to face,
                "seeWallet" to true,
                "removePGP" to true,
                "previewZelfProof" to true
            )
            if (password != null) {
                params["password"] = password
            }
            return zelfClient.importWallet(
                token,
                params
            ).data
        }

        /**
         * Generate a ZelfPrint QR code
         *
         * @param zelfProof the qr raw data
         * @param face base64 img of the face
         * @param password The password
         *
         * @return The ZelfWalletCreatedResponse
         *
         */
        suspend fun decryptZelfPrint(
            zelfProof: String,
            face: String,
            password: String?
        ): ZelfWalletResponse? {
            val zelfClient = ZelfApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val zelfAuthResponse = getZelfAuth()
            if (zelfAuthResponse?.token == null) {
                return null
            }
            val token = "Bearer ${zelfAuthResponse.token}"
            val params = mutableMapOf(
                "zelfProof" to zelfProof,
                "faceBase64" to face,
                "removePGP" to true
            )
            if (password != null) {
                params["password"] = password
            }
            return zelfClient.decryptWallet(
                token,
                params
            ).data
        }

        /**
         * Decrypt the SensePrint data using the capture session, and the QR code bytes
         * (SensePrint bytes)
         *
         * @param sensePrintBytes The SensePrint bytes (read from the QR code)
         * @param password The password
         * @param sessionHolder The session holder containing the active or passive face capture session
         *
         * @return The parsed (and decrypted) SensePrint data
         * @throws SenseCryptSdkException
         */
        @Throws(SenseCryptSdkException::class)
        fun verifySensePrint(
            sensePrintBytes: ByteArray,
            password: String?,
            sessionHolder: SessionHolder,
        ): DecryptedSensePrintData {
            val requestSchema =
                SensePrintRawVerificationMobileRequest(
                    password = password,
                    senseprint = sensePrintBytes,
                    verifiersAuthKey = Constants.VERIFIER_AUTH_KEY,
                    livenessTolerance = null
                )

            return try {
                sessionHolder.verifySenseprint(requestSchema)
            } catch (e: SenseCryptSdkException) {
                throw e
            }
        }

        /**
         * Gets a new active face capture session
         *
         * @return The active face capture session
         * @throws SenseCryptSdkException
         */
        @Throws(SenseCryptSdkException::class)
        fun getActiveFaceCaptureSession(): ActiveFaceCaptureSession {
            try {
                return ActiveFaceCaptureSession()
            } catch (e: SenseCryptSdkException) {
                throw e
            }
        }

        /**
         * Gets a new passive face capture session
         *
         * @return The passive face capture session
         * @throws SenseCryptSdkException
         */
        @Throws(SenseCryptSdkException::class)
        fun getPassiveFaceCaptureSession(): PassiveFaceCaptureSession {
            try {
                return PassiveFaceCaptureSession()
            } catch (e: SenseCryptSdkException) {
                throw e
            }
        }

        /**
         * A utility method that ensures that the SDK is initialized before
         * running a lambda. It shows a loading dialog while the SDK is being
         * initialized. The initialization is in a separate thread.
         *
         * @param activity The activity to show the loading dialog in
         * @param postInit The lambda to run after the SDK is initialized
         */
        fun initSdkIfNeeded(
            activity: Activity,
            postInit: () -> Unit,
        ) {
            if (isInitialized()) {
                activity.runOnUiThread(postInit)
            } else {
                // Show a loading dialog
                showLoadingDialog(
                    activity,
                    R.string.reloading_sdk,
                    {
                        // Initialize the SDK
                        initSynchronously(application = activity.application)
                    },
                    {
                        activity.runOnUiThread(postInit)
                    },
                )
            }
        }
    }
}
