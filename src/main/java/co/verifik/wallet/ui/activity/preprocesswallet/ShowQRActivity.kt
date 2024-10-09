package co.verifik.wallet.ui.activity.preprocesswallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.R
import co.verifik.wallet.adapters.WordsAdapter
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.createwallet.PasswordActivity
import co.verifik.wallet.ui.activity.wallet.main.WalletActivity
import co.verifik.wallet.utils.CryptoWallet
import co.verifik.wallet.utils.GalleryHelper
import co.verifik.wallet.utils.processBitmapToGetQrBytes
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.sensecrypt.sdk.core.SenseCryptSdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException


class ShowQRActivity : AppCompatActivity() {
    // UI Components
    private lateinit var textViewNavTitle: TextView
    private lateinit var textViewNavSubtitle: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var descTextView: TextView
    private lateinit var textViewEthAddress: TextView
    private lateinit var textViewSolanaAddress: TextView
    private lateinit var cardviewCopyMnemonic: CardView
    private lateinit var cardviewEth: CardView
    private lateinit var cardviewSolana: CardView
    private lateinit var cardviewSaveQr: CardView
    private lateinit var cardviewQr: CardView
    private lateinit var ivBack: AppCompatImageView
    private lateinit var ivQr: AppCompatImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var understoodButton: AppCompatButton
    private lateinit var db: AppDatabase

    private var ethAddress: String = ""
    private var solanaAddress: String = ""
    private var readingOnly: Boolean = false
    private val znsName by lazy {
        intent.getStringExtra(EXTRA_ZNS)
    }

    companion object {
        const val EXTRA_ZNS = "co.verifik.wallet.EXTRA_ZNS"
        const val EXTRA_IMG_BYTES = "co.verifik.wallet.EXTRA_IMG_BYTES"
        const val EXTRA_MNEMONIC = "co.verifik.wallet.EXTRA_MNEMONIC"
        const val EXTRA_READING_ONLY_BYTES = "co.verifik.wallet.EXTRA_READING_ONLY_BYTES"
        const val EXTRA_OPENED = "co.verifik.wallet.EXTRA_OPENED"

        // Method to create a new Intent for ShowQRActivity
        fun newIntent(
            context: Context,
            znsName: String,
            mnemonic: String,
            imageBytes: ByteArray?,
            readingOnly: Boolean = false,
            forOpen: Boolean = false
        ): Intent {
            val intent = Intent(context, ShowQRActivity::class.java)
            intent.putExtra(EXTRA_ZNS, znsName)
            intent.putExtra(EXTRA_IMG_BYTES, imageBytes)
            intent.putExtra(EXTRA_MNEMONIC, mnemonic)
            intent.putExtra(EXTRA_READING_ONLY_BYTES, readingOnly)
            intent.putExtra(EXTRA_OPENED, forOpen)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view
        setContentView(R.layout.activity_show_qr)

        setupComponents()

        setUpListeners()
    }

    private fun setupComponents() {
        // Assign values to the components in this activity
        textViewNavTitle = findViewById(R.id.textview_navtitle)
        textViewNavSubtitle = findViewById(R.id.textview_navsubtitle)
        subtitleTextView = findViewById(R.id.textview_subtitle)
        descTextView = findViewById(R.id.textview_desc)
        textViewEthAddress = findViewById(R.id.textviewEthAddres)
        textViewSolanaAddress = findViewById(R.id.textviewSolanaAddres)
        cardviewCopyMnemonic = findViewById(R.id.cardview_copy_mnemonic)
        cardviewEth = findViewById(R.id.cardview_ethereum)
        cardviewSolana = findViewById(R.id.cardview_solana)
        cardviewSaveQr = findViewById(R.id.cardview_download_qr)
        cardviewQr = findViewById(R.id.cardview_qr)
        ivBack = findViewById(R.id.ivBack)
        ivQr = findViewById(R.id.ivQr)
        recyclerView = findViewById(R.id.recycler_mnemonic)
        understoodButton = findViewById(R.id.understoodButton)

        readingOnly = intent.getBooleanExtra(EXTRA_READING_ONLY_BYTES, false)
        val mnemonic = intent.getStringExtra(EXTRA_MNEMONIC) ?: ""
        val mnemonicArr = mnemonic.split(" ")

        val znsNameComplete = "$znsName.zelf"
        textViewNavTitle.text = znsNameComplete
        textViewNavSubtitle.text = getString(R.string.activity_show_qr_subtitle)

        if (mnemonicArr.isEmpty()) {
            recyclerView.visibility = View.GONE
        }
        val layoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = WordsAdapter(this, mnemonicArr)
        recyclerView.suppressLayout(true)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "zelf_database"
        ).build()

        val imageBytes = intent.getByteArrayExtra(EXTRA_IMG_BYTES)
        if(imageBytes==null) {
            cardviewSaveQr.visibility = View.GONE
            cardviewQr.visibility = View.GONE
        }
        val opened = intent.getBooleanExtra(EXTRA_OPENED, false)
        if(opened) {
            subtitleTextView.text = getString(R.string.activity_show_qr_success_title_open)
            descTextView.text = getString(R.string.activity_show_qr_success_desc_open)
        }
    }

    /**
     * Sets up the listeners for the UI components
     */
    private fun setUpListeners() {
        val mnemonic = intent.getStringExtra(EXTRA_MNEMONIC) ?: ""
        val imageBytes = intent.getByteArrayExtra(EXTRA_IMG_BYTES)
        imageBytes?.let {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ivQr.setImageBitmap(bitmap)

            lifecycleScope.launch {

                val qrBytes = processBitmapToGetQrBytes(bitmap)
                qrBytes?.let { qrb ->
                    processQrBytes(qrb)
                }
                textViewEthAddress.text = ethAddress
                textViewSolanaAddress.text = solanaAddress

                if(!readingOnly) {
                    val preferences = getSharedPreferences(
                        "wallet_prefs",
                        MODE_PRIVATE
                    )
                    val editor = preferences.edit()
                    editor.putBoolean("with_wallet", true)
                    editor.apply()

                    val qrEntityDao = db.qrDao()
                    val accountNum = qrEntityDao.getTotalCount() + 1
                    val znsNameComplete = if (znsName != null) "$znsName.zelf"
                        else  getString(R.string.activity_show_qr_account_, accountNum.toString())
                    val qrEntity = QrEntity(
                        idQr = znsNameComplete,
                        ethAddress = ethAddress,
                        solanaAddress = solanaAddress,
                        qrBytes = qrBytes,
                        imgBytes = imageBytes
                    )
                    qrEntityDao.insert(qrEntity)
                }
            }

            cardviewSaveQr.setOnClickListener {
                try {
                    GalleryHelper.saveImageToGallery(this@ShowQRActivity, bitmap)
                    showSuccessDialog()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
        cardviewCopyMnemonic.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("mnemonic", mnemonic)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }
        cardviewEth.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ethAddress", ethAddress)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }
        cardviewSolana.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("solanaAddress", solanaAddress)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }

        ivBack.setOnClickListener {

            if(readingOnly) {
                val intent = WalletActivity.newIntent(this)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            } else {
                logout()
                val intent = Intent(
                    this,
                    MainActivity::class.java
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Whatever you want
                // when back pressed
                if(readingOnly) {
                    val intent = WalletActivity.newIntent(this@ShowQRActivity)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    logout()
                    val intent = Intent(
                        this@ShowQRActivity,
                        MainActivity::class.java
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                }
            }
        })
        understoodButton.setOnClickListener {
            val intent = WalletActivity.newIntent(this)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }

    }

    private fun processQrBytes(spBytes: ByteArray) {
        try {
            val spInfo = CryptUtil.parseSensePrintBytes(spBytes)
            ethAddress = spInfo?.clearTextData?.get("ethAddress") ?: ""
            solanaAddress = spInfo?.clearTextData?.get("solanaAddress") ?: ""
        } catch (_: Exception) { }
    }


    /**
     * Shows a dialog to indicate that the QR code is saved to the gallery
     */
    private fun showSuccessDialog() {
        UIHelper.showInfoDialog(
            this,
            R.string.success,
            R.string.qr_code_is_saved_to_the_gallery,
        )
    }

    private fun logout() {
        val dao = db.qrDao()
        lifecycleScope.launch {
            dao.deleteAll()
            val pref = getSharedPreferences("wallet_prefs", MODE_PRIVATE)
            pref.edit().remove("with_wallet").apply()
        }
    }
}
