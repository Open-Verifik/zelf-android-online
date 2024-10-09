package co.verifik.wallet.ui.activity.openwallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.preprocesswallet.QRScanActivity
import co.verifik.wallet.ui.activity.zns.OpenZNSActivity
import co.verifik.wallet.utils.convertToHashMap
import co.verifik.wallet.utils.hideKeyboard
import com.github.ybq.android.spinkit.SpinKitView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintInfo
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException


class ReaderWalletActivity : AppCompatActivity() {
    private lateinit var linearLayoutParent: LinearLayout
    private lateinit var ivBack: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var cardViewContainer: CardView
    private lateinit var editTextPublicAddress: EditText
    private lateinit var imageViewSearch: ImageView
    private lateinit var albumButton: Button
    private lateinit var cameraButton: Button
    private lateinit var backButton: Button
    private lateinit var spinKit: SpinKitView
    private lateinit var viewMask: View

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ReaderWalletActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_wallet)
        setupComponents()
        setListeners()
    }

    private fun setupComponents() {
        linearLayoutParent = findViewById(R.id.linearLayout_parent)
        ivBack = findViewById(R.id.ivBack)
        titleTextView = findViewById(R.id.textview_navtitle)
        subtitleTextView = findViewById(R.id.textview_navsubtitle)
        cardViewContainer = findViewById(R.id.cardview_container)
        editTextPublicAddress = findViewById(R.id.edittext_publicaddress)
        imageViewSearch = findViewById(R.id.imageview_search)
        albumButton = findViewById(R.id.button_album)
        cameraButton = findViewById(R.id.button_camera)
        backButton = findViewById(R.id.button_back)
        spinKit = findViewById(R.id.spin_kit)
        viewMask = findViewById(R.id.view_mask)

        titleTextView.text = getString(R.string.activity_reader_wallet_header)
        subtitleTextView.text = getString(R.string.activity_reader_wallet_subheader)
    }

    private fun setListeners() {

        linearLayoutParent.setOnClickListener {
            editTextPublicAddress.clearFocus()
            editTextPublicAddress.hideKeyboard()
        }

        ivBack.setOnClickListener {
            finish()
        }
        // Registers a photo picker activity launcher in single-select mode.
        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {

                val image: InputImage
                try {
                    image = InputImage.fromFilePath(this, uri)

                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val imgBytes = stream.toByteArray()
                    bitmap.recycle()

                    val scanner = BarcodeScanning.getClient()
                    val result = scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            // Task completed successfully
                            if (barcodes.isNotEmpty()) {
                                val qrResult = barcodes[0]
                                qrResult.rawBytes?.let {
                                    val base64String: String = Base64.encodeToString(it, Base64.DEFAULT)
                                    processQrBytes(it, imgBytes)
                                }
                            }
                        }
                        .addOnFailureListener {
                            // Task failed with an exception
                            // ...
                        }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

        imageViewSearch.setOnClickListener {
            val publicAddress = editTextPublicAddress.text.toString()
            searchIPFS(publicAddress)
        }

        editTextPublicAddress.setOnKeyListener { view, i, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN && i == KEYCODE_ENTER) {
                editTextPublicAddress.clearFocus()
                view?.hideKeyboard()
                val publicAddress = editTextPublicAddress.text.toString()
                searchIPFS(publicAddress)
                return@setOnKeyListener true
            }
            false
        }

        albumButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        cameraButton.setOnClickListener {
            startActivity(QRScanActivity.newIntentWithForClearText(this))
        }

        backButton.setOnClickListener {
            finish()
        }
    }
    

    private fun processQR(
        spInfo: SensePrintInfo?,
        imgBytes: ByteArray
    ) {
        if (spInfo == null) {
            // If the QR code is not a ZelfCrypt QR code, show an error message
            runOnUiThread {
                UIHelper.showSnackBar(
                    this,
                    cardViewContainer,
                    R.string.invalid_qr,
                    R.color.colorErrorSnackbar,
                    2000,
                    100f,
                )
            }
        } else {
            navigateToNextActivity(spInfo, imgBytes, null)
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
            val map = convertToHashMap(spInfo.clearTextData)
            val ethAddress = map["ethAddress"]
            if (ethAddress != null) {
                searchIPFS(ethAddress, imgBytes)
                return
            }
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

    private fun searchIPFS(publicAddress: String, imgBytes: ByteArray? = null) {
        lifecycleScope.launch {
            spinKit.visibility = View.VISIBLE
            viewMask.visibility = View.VISIBLE

            val ipfsResponse = try {
                CryptUtil.findIPFSByPublicAddress(publicAddress)
            } catch (e: HttpException) {
                null
            }
            spinKit.visibility = View.GONE
            viewMask.visibility = View.GONE

            if(ipfsResponse == null) {
                UIHelper.showInfoDialog(
                    this@ReaderWalletActivity,
                    R.string.no_zns_found,
                    R.string.no_zns_found_desc,
                    false,
                )
            } else {
                val zns = ipfsResponse.metadata?.name?.replace(".zelf", "") ?: ""
                val znsUrl = ipfsResponse.url ?: ""
                val ethAddress = ipfsResponse.metadata?.keyvalues?.get("ethAddress") ?: ""
                val solanaAddress = ipfsResponse.metadata?.keyvalues?.get("solanaAddress") ?: ""
                val intent = OpenZNSActivity.newIntent(
                    this@ReaderWalletActivity,
                    zns,
                    znsUrl,
                    ethAddress,
                    solanaAddress,
                    imgBytes
                )
                startActivity(intent)
            }
        }
    }
}