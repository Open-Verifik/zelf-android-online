package co.verifik.wallet.ui.activity.openwallet

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.adapters.ClearDataAdapter
import co.verifik.wallet.databinding.ActivityReaderDetailBinding
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.preprocesswallet.FaceScanActivity
import co.verifik.wallet.utils.convertToMutableList
import co.verifik.wallet.utils.getSerializable
import co.verifik.wallet.utils.processBitmapToGetQrBytes
import com.google.android.material.textfield.TextInputEditText
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintInfo
import com.sensecrypt.sdk.core.SensePrintType
import kotlinx.coroutines.launch

class ReaderDetailActivity : AppCompatActivity() {
    // set up for UI root binding
    private lateinit var ivBack: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var buttonContinue: Button
    private lateinit var backButton: Button
    private lateinit var binding: ActivityReaderDetailBinding

    private lateinit var imgBytes: ByteArray
    private var qrPassword: String = ""
    private var readingOnly: Boolean = false
    private val znsName by lazy {
        intent.getStringExtra(EXTRA_ZNS_NAME)
    }

    //
    private lateinit var mClearDataAdapter: ClearDataAdapter

    companion object {
        const val EXTRA_ZNS_NAME = "co.verifik.wallet.EXTRA_ZNS_NAME"
        const val EXTRA_CLEAR_DATA = "co.verifik.wallet.EXTRA_CLEAR_DATA"
        const val EXTRA_IMG_BYTES = "co.verifik.wallet.EXTRA_IMG_BYTES"
        const val EXTRA_READING_ONLY_BYTES = "co.verifik.wallet.EXTRA_READING_ONLY_BYTES"

        // Method to create a new Intent for PersonDetailActivity
        fun newIntent(
            context: Context,
            znsName: String,
            clearData: HashMap<String, String>,
            imgBytes: ByteArray?,
            forReadingOnly: Boolean = false
        ): Intent {
            val intent = Intent(context, ReaderDetailActivity::class.java)
            // Put the clear text data in the intent
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_CLEAR_DATA, clearData)
            intent.putExtra(EXTRA_IMG_BYTES, imgBytes)
            intent.putExtra(EXTRA_READING_ONLY_BYTES, forReadingOnly)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupComponents()

        setUpListeners()

        setUpRecyclerView()

        bindData()
    }

    private fun setupComponents() {
        ivBack = findViewById(R.id.ivBack)
        titleTextView = findViewById(R.id.textview_navtitle)
        subtitleTextView = findViewById(R.id.textview_navsubtitle)
        buttonContinue = findViewById(R.id.button_continue)
        backButton = findViewById(R.id.button_back)

        // Get the QR code bytes from the intent
        imgBytes = intent.getByteArrayExtra(EXTRA_IMG_BYTES)!!
        readingOnly = intent.getBooleanExtra(EXTRA_READING_ONLY_BYTES, false)

        titleTextView.text = getString(R.string.activity_reader_detail_wallet_header)
        subtitleTextView.text = getString(R.string.activity_reader_detail_wallet_subheader)
    }

    /**
     * set up the recycler view
     */
    private fun setUpRecyclerView() {
        mClearDataAdapter = ClearDataAdapter()
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvClearData.layoutManager = linearLayoutManager
        binding.rvClearData.adapter = mClearDataAdapter
    }

    /**
     * finish the activity when the back button is pressed
     */
    private fun setUpListeners() {
        ivBack.setOnClickListener {
            finish()
        }
        buttonContinue.setOnClickListener {
            processQrBytes(imgBytes)
        }
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun navigateToNextActivity(
        imgBytes: ByteArray,
        password: String?
    ) {
        // We are done with the scan open the FaceScanActivity
        startActivity(
            FaceScanActivity.newIntent(
                this,
                znsName ?: "",
                imgBytes,
                password,
                true,
                readingOnly,
                true
            ),
        )
        finish()
    }

    /**
     * bind the data to the recycler view
     */
    private fun bindData() {
        // Get the clear data from the intent
        intent.getSerializable(EXTRA_CLEAR_DATA, HashMap::class.java)?.let {
            mClearDataAdapter.setData(convertToMutableList(it as HashMap<String, String>))
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
        imgBytes: ByteArray
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
                    showPasswordDialog(spInfo, imgBytes)
                },
                {
                    finish()
                },
            )
        }
    }

    private fun showPasswordDialog(
        spInfo: SensePrintInfo,
        imgBytes: ByteArray
    ) {
        showQrPasswordDialog {
            val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
            lifecycleScope.launch {
                val spBytes = processBitmapToGetQrBytes(bitmap)
                // Check if password is correct
                try {
                    val isPasswordCorrect = spBytes?.let {
                        CryptUtil.verifyPassword(
                            spBytes,
                            qrPassword,
                        )
                    } ?: false
                    if (isPasswordCorrect) {
                        // navigate to next activity based on qr scan type
                        navigateToNextActivity(imgBytes, qrPassword)
                    } else {
                        // Show an error message
                        showPasswordIncorrectDialog(spInfo, imgBytes)
                    }
                } catch (e: SenseCryptSdkException) {
                    // This can only happen if the license has expired
                    runOnUiThread {
                        UIHelper.showInfoDialog(
                            this@ReaderDetailActivity,
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
    }

    private fun processQR(
        spInfo: SensePrintInfo?,
        imgBytes: ByteArray
    ) {
        if (spInfo == null) {
            // If the QR code is not a Zelfcrypt QR code, show an error message
            runOnUiThread {
                UIHelper.showSnackBar(
                    this,
                    binding.llPersonDetail,
                    R.string.invalid_qr,
                    R.color.colorErrorSnackbar,
                    2000,
                    100f,
                )
            }
        } else {
            var isPasswordRequired = spInfo.spType == SensePrintType.WITH_PASSWORD

            if (isPasswordRequired) {
                runOnUiThread {
                    showPasswordDialog(spInfo, imgBytes)
                }
            } else {
                // navigate to next activity based on qr scan type
                navigateToNextActivity(imgBytes, null)
            }
        }
    }

    private fun processQrBytes(imgBytes: ByteArray) {

        val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)

        lifecycleScope.launch {
            val spBytes = processBitmapToGetQrBytes(bitmap)
            try {
                val spInfo = spBytes?.let { CryptUtil.parseSensePrintBytes(it) }
                processQR(spInfo, imgBytes)
            } catch (e: SenseCryptSdkException) {
                // This will only happen if the license has expired
                runOnUiThread {
                    UIHelper.showInfoDialog(
                        this@ReaderDetailActivity,
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
}
