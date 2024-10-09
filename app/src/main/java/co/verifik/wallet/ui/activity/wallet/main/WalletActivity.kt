package co.verifik.wallet.ui.activity.wallet.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import co.verifik.wallet.Constants
import co.verifik.wallet.R
import co.verifik.wallet.adapters.CryptoNetworkAdapter
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.QrEntityDao
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.preprocesswallet.MainActivity
import co.verifik.wallet.ui.activity.wallet.account.WalletAccountActivity
import co.verifik.wallet.ui.fragment.wallet.changeaccount.ChangeAccountFragment
import co.verifik.wallet.ui.fragment.wallet.contacts.ContactsFragment
import co.verifik.wallet.ui.fragment.wallet.main.MainWalletFragment
import co.verifik.wallet.ui.activity.wallet.scantosend.ScanToSendActivity
import co.verifik.wallet.ui.activity.wallet.send.SendCryptoActivity
import co.verifik.wallet.ui.views.PopUpTransactionDetail
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class WalletActivity : AppCompatActivity(),
    PopupMenu.OnMenuItemClickListener {

    private lateinit var linearLayoutTitle: LinearLayout
    private lateinit var imageViewMore: ImageView
    private lateinit var imageViewQr: ImageView
    private lateinit var cardViewToolbarNetwork: CardView
    private lateinit var spinnerToolbarNetwork: Spinner
    private lateinit var textviewTitle: TextView
    private lateinit var qrPassword: String
    private lateinit var db: AppDatabase
    private lateinit var dao: QrEntityDao
    private val viewModel: WalletActivityViewModel by viewModels()
    private var sendCryptoInfo: SendCryptoInfo? = null
    private val inTransition = MaterialSharedAxis(
        MaterialSharedAxis.Z,
        /* forward= */ true
    ).apply {
        duration = ANIM_DURATION
    }
    private val outTransition = MaterialSharedAxis(
        MaterialSharedAxis.Z,
        /* forward= */ false
    ).apply {
        duration = ANIM_DURATION
    }
    private var timer: Timer? = null

    companion object {
        fun newIntent(
            context: Context
        ): Intent {
            val intent = Intent(context, WalletActivity::class.java)
            return intent
        }

        fun newIntentFromSendingTransaction(
            context: Context,
            transactionBaseUrl: String,
            transactionId: String
        ): Intent {
            val intent = Intent(context, WalletActivity::class.java)
            intent.putExtra(EXTRA_SUCCESFUL_TRANSACTION, true)
            intent.putExtra(EXTRA_TRANSACTION_BASE_URL, transactionBaseUrl)
            intent.putExtra(EXTRA_TRANSACTION_ID, transactionId)
            return intent
        }

        const val EXTRA_SUCCESFUL_TRANSACTION = "co.verifik.wallet.EXTRA_SUCCESFUL_TRANSACTION"
        const val EXTRA_TRANSACTION_ID = "co.verifik.wallet.EXTRA_TRANSACTION_ID"
        const val EXTRA_TRANSACTION_BASE_URL = "co.verifik.wallet.EXTRA_TRANSACTION_BASE_URL"
        const val ANIM_DURATION = 500L
    }

    private var mainWalletFragment: MainWalletFragment? = null
    private var changeAccountFragment: ChangeAccountFragment? = null
    private var contactsFragment: ContactsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        setupComponents()
        setupListeners()
        loadInfo()
    }

    override fun onResume() {
        super.onResume()
        qrPassword = ""
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                viewModel.reloadBalanceAndMakeCalculations()
                reloadSubfragmentsInfo()
            }
        }, 0, 60000)
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    private fun setupComponents() {
        linearLayoutTitle = findViewById(R.id.linearlayout_title)
        imageViewMore = findViewById(R.id.imageview_more)
        imageViewQr = findViewById(R.id.imageview_qr)
        cardViewToolbarNetwork = findViewById(R.id.cardview_toolbar_crypto_network)
        spinnerToolbarNetwork = findViewById(R.id.spinner_toolbar_crypto_network)
        textviewTitle = findViewById(R.id.textview_title)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "zelf_database"
        ).build()
        dao = db.qrDao()

        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.backStackEntryCount == 0) {
                changeAccountFragment = null
                contactsFragment = null
            }
            if(supportFragmentManager.fragments.lastOrNull() is ChangeAccountFragment) {
                contactsFragment = null
            }
            if(supportFragmentManager.fragments.lastOrNull() is ContactsFragment) {
                changeAccountFragment = null
            }
        }

        mainWalletFragment = MainWalletFragment.newInstance(
            tapOnReceive = {
                startActivity(
                    WalletAccountActivity.newIntent(
                        this,
                        viewModel.currentQrEntity.value?.uid ?: 0
                    )
                )
            },
            tapOnSend = {
                startActivity(
                    SendCryptoActivity.newIntent(
                        this,
                        viewModel.currentQrEntity.value?.uid ?: 0,
                        viewModel.currentNetwork.value ?: "Ethereum"
                    )
                )
            },
            tapOnQR = {
                val intent = ScanToSendActivity.newIntent(this)
                startActivity(intent)
            }
        )
        mainWalletFragment?.apply {
            exitTransition = inTransition
            reenterTransition = inTransition
        }
        mainWalletFragment?.let { wFragment ->
            supportFragmentManager.commit {
                replace(R.id.fragment_container, wFragment)
            }
        }

        if(intent.getBooleanExtra(EXTRA_SUCCESFUL_TRANSACTION, false)) {
            val txBaseUrl = intent.getStringExtra(EXTRA_TRANSACTION_BASE_URL) ?: Constants.ETHERSCAN_MAINNET_URL
            val tx = intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: ""
            viewModel.getTransaction(txBaseUrl, tx)
        }
    }

    private fun setupListeners() {

        viewModel.currentQrEntity.observe(this) {
            textviewTitle.text = it.idQr
        }

        viewModel.networks.observe(this) {
            spinnerToolbarNetwork.adapter = CryptoNetworkAdapter(
                this,
                it
            )
        }

        linearLayoutTitle.setOnClickListener {
            if(changeAccountFragment == null) {
                changeAccountFragment = ChangeAccountFragment.newInstance({
                    viewModel.changeCurrentQrEntity(it)
                }, {
                    viewModel.deleteQrEntity(it)
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        supportFragmentManager.popBackStack()
                        viewModel.reloadInfo()
                    }, 200)
                }).apply {
                    enterTransition = outTransition
                    exitTransition = inTransition
                    returnTransition = outTransition
                }
                changeAccountFragment?.let { fr ->
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, fr)
                        setReorderingAllowed(true)
                        addToBackStack(null)
                    }
                }
            }
        }

        imageViewMore.setOnClickListener {

            val popup = PopupMenu(this, it).apply {
                setOnMenuItemClickListener(this@WalletActivity)
            }
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.main_wallet_menu, popup.menu)
            popup.show()
        }

        imageViewQr.setOnClickListener {
            startActivity(
                WalletAccountActivity.newIntent(
                    this,
                    viewModel.currentQrEntity.value?.uid ?: 0
                )
            )
        }

        cardViewToolbarNetwork.setOnClickListener {
            spinnerToolbarNetwork.performClick()
        }

        spinnerToolbarNetwork.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long) {
                val networks = viewModel.networks.value
                val currentNetwork = networks?.get(position) ?: "Ethereum"
                viewModel.changeCurrentNetwork(currentNetwork)
                reloadSubfragmentsInfo()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        viewModel.sendTransaction.observe(this) {
            if (it != null) {
                PopUpTransactionDetail().showPopupWindow(
                    this,
                    viewModel.currentQrEntity.value?.ethAddress ?: "",
                    textviewTitle,
                    it
                )
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.all_addresses -> {
                if(contactsFragment == null) {
                    contactsFragment = ContactsFragment.newInstance()
                    contactsFragment?.apply {
                        enterTransition = outTransition
                        exitTransition = inTransition
                        returnTransition = outTransition
                    }
                    contactsFragment?.let { fr ->
                        supportFragmentManager.commit {
                            replace(R.id.fragment_container, fr)
                            setReorderingAllowed(true)
                            addToBackStack(null)
                        }
                    }
                }
                true
            }
            R.id.sign_out -> {
                UIHelper.showConfirmationDialog(
                    this,
                    R.string.logout,
                    R.string.logout_desc,
                    R.string.yes,
                    R.string.no,
                    {
                        logout()
                    },
                    {

                    }
                )
                true
            }
            else -> false
        }
    }

    private fun loadInfo() {
        viewModel.loadInfo()
    }

    private fun reloadSubfragmentsInfo() {
        viewModel.getAllTokens()
        viewModel.getTransactions()
    }

    private fun logout() {
        val dao = db.qrDao()
        lifecycleScope.launch {
            dao.deleteAll()
            val pref = getSharedPreferences("wallet_prefs", MODE_PRIVATE)
            pref.edit().remove("with_wallet").apply()
            val intent = Intent(this@WalletActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}