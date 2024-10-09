package co.verifik.wallet.ui.activity.wallet.success

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import co.verifik.wallet.Constants
import co.verifik.wallet.R
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.ui.activity.wallet.main.WalletActivity
import kotlinx.coroutines.launch


class SuccessSendCryptoActivity : AppCompatActivity() {

    private lateinit var viewLoading: View
    private lateinit var buttonDone: AppCompatButton
    private lateinit var db: AppDatabase
    private val viewModel: SuccessSendCryptoViewModel by viewModels()
    private var network = ""

    companion object {

        private const val EXTRA_QR_ENTITY_ID = "co.verifik.wallet.EXTRA_QR_ENTITY_ID"
        private const val EXTRA_SEND_TO_ADDRESS = "co.verifik.wallet.EXTRA_SEND_TO_ADDRESS"
        private const val EXTRA_AMOUNT = "co.verifik.wallet.EXTRA_AMOUNT"
        private const val EXTRA_NETWORK = "co.verifik.wallet.EXTRA_NETWORK"
        private const val EXTRA_MNEMONIC = "co.verifik.wallet.EXTRA_MNEMONIC"

        fun newIntent(
            context: Context,
            qrEntityId: Int,
            sendToAddress: String,
            amount: String,
            network: String,
            mnemonic: String
        ): Intent {
            val intent = Intent(context, SuccessSendCryptoActivity::class.java)
            intent.putExtra(EXTRA_QR_ENTITY_ID, qrEntityId)
            intent.putExtra(EXTRA_SEND_TO_ADDRESS, sendToAddress)
            intent.putExtra(EXTRA_AMOUNT, amount)
            intent.putExtra(EXTRA_NETWORK, network)
            intent.putExtra(EXTRA_MNEMONIC, mnemonic)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success_send_crypto)

        setup()
        setupComponents()
        setupListeners()
    }

    private fun setup() {

        network = intent.getStringExtra(EXTRA_NETWORK) ?: "Ethereum"
        val qrEntityId = intent.getIntExtra(EXTRA_QR_ENTITY_ID, 0)
        val sendToAddress = intent.getStringExtra(EXTRA_SEND_TO_ADDRESS)
        val amount = intent.getStringExtra(EXTRA_AMOUNT)
        val mnemonic = intent.getStringExtra(EXTRA_MNEMONIC)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "zelf_database"
        ).build()

        val dao = db.qrDao()

        lifecycleScope.launch {
            val qrEntity = dao.getQrEntity(qrEntityId)
            if (
                qrEntity != null &&
                sendToAddress != null &&
                amount != null &&
                network != null &&
                mnemonic != null
            ) {
                val sendCryptoInfo = SendCryptoInfo(
                    qrEntity.uid,
                    sendToAddress,
                    network,
                    amount
                )
                viewModel.loadItemsForTransaction(mnemonic, sendCryptoInfo)
            }
        }
    }

    private fun setupComponents() {
        viewLoading = findViewById(R.id.view_loading)
        buttonDone = findViewById(R.id.button_done)

        val anim = viewLoading.background as AnimationDrawable
        anim.setEnterFadeDuration(10)
        anim.setExitFadeDuration(300)
        anim.start()
    }

    private fun setupListeners() {

        viewModel.transactionReceipt.observe(this) {
            buttonDone.isEnabled = true
            viewLoading.visibility = View.GONE
        }

        buttonDone.setOnClickListener {
            val networkUrl = when(network) {
                "Ethereum" -> Constants.ETHERSCAN_MAINNET_URL
                "Sepolia" -> Constants.ETHERSCAN_SEPOLIA_URL
                else -> Constants.ETHERSCAN_MAINNET_URL
            }
            val intent = WalletActivity.newIntentFromSendingTransaction(
                this,
                networkUrl,
                viewModel.transactionReceipt.value?.transactionHash ?: ""
            )
            startActivity(intent)
            finish()
        }
    }
}