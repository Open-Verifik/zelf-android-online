package co.verifik.wallet.ui.activity.wallet.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import co.verifik.wallet.R
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.openwallet.ReaderDetailActivity
import co.verifik.wallet.utils.GalleryHelper
import com.google.android.material.card.MaterialCardView
import java.io.IOException

class WalletAccountActivity : AppCompatActivity() {

    private lateinit var goBackImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var qrImageView: ImageView
    private lateinit var ethCardView: MaterialCardView
    private lateinit var ethAddressTextView: TextView
    private lateinit var solanaCardView: MaterialCardView
    private lateinit var solanaAddressTextView: TextView
    private lateinit var downloadQrCardView: MaterialCardView
    private lateinit var recoverCardView: MaterialCardView


    private var selectedQrEntityUid: Int = 0
    private val viewModel: WalletAccountViewModel by viewModels()

    companion object {
        const val EXTRA_QR_ENTITY_UID = "co.verifik.wallet.EXTRA_QR_ENTITY_UID"

        fun newIntent(
            context: Context,
            qrEntityUid: Int
        ): Intent {
            val intent = Intent(context, WalletAccountActivity::class.java)
            intent.putExtra(EXTRA_QR_ENTITY_UID, qrEntityUid)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_account)

        setupComponents()
        setupListeners()
        loadInfo()
    }

    private fun setupComponents() {
        goBackImageView = findViewById(R.id.ivBack)
        titleTextView = findViewById(R.id.textview_title)
        qrImageView = findViewById(R.id.imageview_qr)
        ethCardView = findViewById(R.id.cardview_ethereum)
        ethAddressTextView = findViewById(R.id.textviewEthAddres)
        solanaCardView = findViewById(R.id.cardview_solana)
        solanaAddressTextView = findViewById(R.id.textviewSolanaAddres)
        downloadQrCardView = findViewById(R.id.cardview_download_qr)
        recoverCardView = findViewById(R.id.cardview_recover)
    }

    private fun setupListeners() {
        goBackImageView.setOnClickListener {
            finish()
        }
        viewModel.currentQrEntity.observe(this) {
            titleTextView.text = it.idQr
            ethAddressTextView.text = it.ethAddress
            solanaAddressTextView.text = it.solanaAddress

            val imgBytes = it.imgBytes
            imgBytes?.let {
                val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                qrImageView.setImageBitmap(bitmap)
            }
        }
        ethCardView.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ethAddress", ethAddressTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }
        solanaCardView.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ethAddress", solanaAddressTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }
        downloadQrCardView.setOnClickListener {
            try {
                val bitmap = (qrImageView.drawable as BitmapDrawable).bitmap
                GalleryHelper.saveImageToGallery(this@WalletAccountActivity, bitmap)
                showSuccessDialog()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        recoverCardView.setOnClickListener {
            onRecoverAccountClick()
        }
    }

    private fun loadInfo() {
        selectedQrEntityUid = intent.getIntExtra(EXTRA_QR_ENTITY_UID, 0)

        viewModel.getCurrentQrEntity(selectedQrEntityUid)
    }

    private fun showSuccessDialog() {
        UIHelper.showInfoDialog(
            this,
            R.string.success,
            R.string.qr_code_is_saved_to_the_gallery,
        )
    }

    fun onRecoverAccountClick() {
        val hashMap = HashMap<String, String>()
        val znsName = viewModel.currentQrEntity.value?.idQr?.replace(".zelf", "") ?: ""
        hashMap["ETHADDRESS"] = viewModel.currentQrEntity.value?.ethAddress ?: ""
        hashMap["SOLANAADDRESS"] = viewModel.currentQrEntity.value?.solanaAddress ?: ""
        val imgBytes = viewModel.currentQrEntity.value?.imgBytes
        imgBytes?.let {
            //CHANGE BITMAP TO QR
            val intent = ReaderDetailActivity.newIntent(
                this,
                znsName,
                hashMap,
                imgBytes,
                true
            )
            startActivity(intent)
        }
    }
}