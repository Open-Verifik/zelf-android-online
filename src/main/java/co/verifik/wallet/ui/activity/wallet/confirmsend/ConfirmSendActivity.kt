package co.verifik.wallet.ui.activity.wallet.confirmsend

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.preprocesswallet.FaceScanActivity
import com.google.android.material.textfield.TextInputEditText
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintInfo
import com.sensecrypt.sdk.core.SensePrintType

class ConfirmSendActivity : AppCompatActivity() {

    private lateinit var backImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var textviewAddressFrom: TextView
    private lateinit var textviewAddressSend: TextView
    private lateinit var textviewAmount: TextView
    private lateinit var textviewUsdAmount: TextView
    private lateinit var textviewGasPrice: TextView
    private lateinit var textviewGasPriceUsd: TextView
    private lateinit var textviewTotal: TextView
    private lateinit var textviewTotalUsd: TextView
    private lateinit var sendButton: AppCompatButton
    private lateinit var backButton: AppCompatButton

    private val viewModel: ConfirmSendViewModel by viewModels()
    private var sendCryptoInfo: SendCryptoInfo? = null
    private var sendToAddress: String = ""
    private var qrPassword: String = ""

    companion object {

        const val SEND_CRYPTO_QR_ID = "co.verifik.wallet.SEND_CRYPTO_QR_ID"
        const val SEND_CRYPTO_SEND_TO_ADDRESS = "co.verifik.wallet.SEND_CRYPTO_SEND_TO_ADDRESS"
        const val SEND_CRYPTO_AMOUNT = "co.verifik.wallet.SEND_CRYPTO_AMOUNT"
        const val SEND_CRYPTO_NETWORK = "co.verifik.wallet.SEND_CRYPTO_NETWORK"

        fun newIntent(
            context: Context,
            sendCryptoInfo: SendCryptoInfo
        ): Intent {
            val intent = Intent(context, ConfirmSendActivity::class.java)
            intent.putExtra(SEND_CRYPTO_QR_ID, sendCryptoInfo.qrEntityId)
            intent.putExtra(SEND_CRYPTO_SEND_TO_ADDRESS, sendCryptoInfo.sendToAddress)
            intent.putExtra(SEND_CRYPTO_AMOUNT, sendCryptoInfo.amount)
            intent.putExtra(SEND_CRYPTO_NETWORK, sendCryptoInfo.network)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_send)

        setupComponents()
        addObservers()
        loadInfo()
    }

    private fun setupComponents() {
        backImageView = findViewById(R.id.ivBack)
        titleTextView = findViewById(R.id.textview_title)
        textviewAddressFrom = findViewById(R.id.textview_address_from)
        textviewAddressSend = findViewById(R.id.textview_address_send)
        textviewAmount = findViewById(R.id.textview_amount)
        textviewUsdAmount = findViewById(R.id.textview_usd_amount)
        textviewGasPrice = findViewById(R.id.textview_gas_price)
        textviewGasPriceUsd = findViewById(R.id.textview_gas_price_usd)
        textviewTotal = findViewById(R.id.textview_total_price)
        textviewTotalUsd = findViewById(R.id.textview_total_usd_price)
        sendButton = findViewById(R.id.button_send)
        backButton = findViewById(R.id.button_back)

        titleTextView.text = getString(R.string.activity_confirm_send_crypto_title)

        sendToAddress = intent.getStringExtra(SEND_CRYPTO_SEND_TO_ADDRESS) ?: ""
        textviewAddressSend.text = sendToAddress
        val amount = intent.getStringExtra(SEND_CRYPTO_AMOUNT) ?: ""
        textviewAmount.text = amount
    }

    private fun addObservers() {

        backImageView.setOnClickListener {
            finish()
        }

        viewModel.currentQrEntity.observe(this) {
            textviewAddressFrom.text = it.ethAddress
        }

        viewModel.amountEquiv.observe(this) {
            textviewUsdAmount.text = it
        }
        viewModel.gasPrice.observe(this) {
            textviewGasPrice.text = it
        }
        viewModel.gasPriceUsd.observe(this) {
            textviewGasPriceUsd.text = it
        }
        viewModel.total.observe(this) {
            textviewTotal.text = it
        }
        viewModel.totalUsd.observe(this) {
            textviewTotalUsd.text = it
        }

        backButton.setOnClickListener {
            finish()
        }

        sendButton.setOnClickListener {
            onConfirmSendClicked()
        }
    }

    private fun loadInfo() {
        val selectedQrEntityUid = intent.getIntExtra(SEND_CRYPTO_QR_ID, 0)
        val amount = intent.getStringExtra(SEND_CRYPTO_AMOUNT) ?: ""
        val network = intent.getStringExtra(SEND_CRYPTO_NETWORK) ?: ""
        viewModel.updateCurrentQrEntity(selectedQrEntityUid)
        viewModel.changeCurrentNetwork(network)

        viewModel.loadInfo(amount)
    }



    private fun onConfirmSendClicked() {
        val sendCryptoInfo = SendCryptoInfo(
            viewModel.currentQrEntity.value?.uid ?: 0,
            sendToAddress,
            viewModel.currentNetwork.value ?: "",
            viewModel.rawAmount.value ?: ""
        )
        this.sendCryptoInfo = sendCryptoInfo
        val spBytes = viewModel.currentQrEntity.value?.qrBytes
        val imgBytes = viewModel.currentQrEntity.value?.imgBytes
        spBytes?.let { spBytes ->
            imgBytes?.let { imgBytes ->
                processQrBytes(spBytes, imgBytes)
            }
        }
    }

    private fun processQR(
        spInfo: SensePrintInfo?,
        imgBytes: ByteArray
    ) {
        if (spInfo == null) {
            // If the QR code is not a Zelf QR code, show an error message
            runOnUiThread {
                UIHelper.showSnackBar(
                    this,
                    titleTextView,
                    R.string.invalid_qr,
                    R.color.colorErrorSnackbar,
                    2000,
                    100f,
                )
            }
        } else {
            val isPasswordRequired = spInfo.spType == SensePrintType.WITH_PASSWORD

            if (isPasswordRequired) {
                runOnUiThread {
                    showPasswordDialog(spInfo, imgBytes)
                }
            } else {
                // navigate to next activity based on qr scan type
                navigateToNextActivity(spInfo, imgBytes, null)
            }
        }
    }

    // ask the user to enter password if it is required to scan qr
    private fun showQrPasswordDialog(continueListener: View.OnClickListener) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_qr_password)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        dialog.setCancelable(true)
        val window = dialog.window
        window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ) // change mathch
        window.setGravity(Gravity.CENTER)
        val lp = window.attributes
        lp.dimAmount = 0.7f
        lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes = lp
        dialog.window!!.attributes.windowAnimations = R.style.animation
        val btnYes: AppCompatButton = dialog.findViewById(R.id.btnConfirm)
        val password: TextInputEditText = dialog.findViewById(R.id.edittext_pssw)
        val tvError: TextView = dialog.findViewById(R.id.tvError)
        btnYes.setOnClickListener { v ->
            if (password.length() != 0 && password.text.toString() != getString(R.string.enter_password)) {
                dialog.dismiss()
                qrPassword = password.text.toString()
                continueListener.onClick(v)
            } else {
                tvError.visibility = View.VISIBLE // show error field if password is null
            }
        }

        dialog.setOnCancelListener {
            finish()
        }

        dialog.show()
    }

    /**
     * This method is used to show a dialog to indicate that the password is incorrect.
     */
    private fun showPasswordIncorrectDialog(
        spInfo: SensePrintInfo,
        spBytes: ByteArray,
    ) {
        // Show an error message
        runOnUiThread {
            UIHelper.showConfirmationDialog(
                this,
                R.string.verification_failed,
                R.string.password_incorrect,
                R.string.retry,
                R.string.cancel,
                {
                    showPasswordDialog(spInfo, spBytes)
                },
                {
                    finish()
                },
            )
        }
    }

    private fun showPasswordDialog(
        spInfo: SensePrintInfo,
        imgBytes: ByteArray,
    ) {
        showQrPasswordDialog {
            // Check if password is correct
            try {
                val isPasswordCorrect =
                    CryptUtil.verifyPassword(
                        imgBytes,
                        qrPassword,
                    )
                if (isPasswordCorrect) {
                    // navigate to next activity based on qr scan type
                    navigateToNextActivity(spInfo, imgBytes, qrPassword)
                } else {
                    // Show an error message
                    showPasswordIncorrectDialog(spInfo, imgBytes)
                }
            } catch (e: SenseCryptSdkException) {
                // This can only happen if the license has expired
                runOnUiThread {
                    UIHelper.showInfoDialog(
                        this,
                        R.string.license_expired,
                        R.string.license_expired_detail,
                        false,
                    ) {
                        finish()
                    }
                }
            }
        }
    }

    private fun navigateToNextActivity(
        spInfo: SensePrintInfo,
        imgBytes: ByteArray,
        password: String?
    ) {
        if (spInfo.clearTextData.isEmpty()) {
            // There is no clear text data, so we can't show the details
            runOnUiThread {
                UIHelper.showInfoDialog(
                    this,
                    R.string.no_clear_text_data,
                    R.string.no_clear_text_data_detail,
                    false,
                ) {
                    finish()
                }
            }
            return
        } else {
            val qrId = viewModel.currentQrEntity.value?.uid ?: 0
            startActivity(
                FaceScanActivity.newIntentForTransaction(
                    this,
                    imgBytes,
                    password,
                    qrId,
                    sendCryptoInfo
                )
            )
            finish()
        }
    }
    private fun processQrBytes(spBytes: ByteArray, imgBytes: ByteArray) {
        try {
            val spInfo = CryptUtil.parseSensePrintBytes(spBytes)
            processQR(spInfo, imgBytes)
        } catch (e: SenseCryptSdkException) {
            // This will only happen if the license has expired
            runOnUiThread {
                UIHelper.showInfoDialog(
                    this,
                    R.string.license_expired,
                    R.string.license_expired_detail,
                    false,
                ) {
                    finish()
                }
            }
        }
    }
}