package co.verifik.wallet.ui.activity.wallet.send

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.adapters.ContactsAdapter
import co.verifik.wallet.adapters.CryptoNetworkAdapter
import co.verifik.wallet.adapters.OnClickOnContactItem
import co.verifik.wallet.data.local.Contact
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.ui.activity.wallet.account.WalletAccountActivity
import co.verifik.wallet.ui.activity.wallet.confirmsend.ConfirmSendActivity
import co.verifik.wallet.utils.afterTextChanged
import co.verifik.wallet.utils.hideKeyboard
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SendCryptoActivity : AppCompatActivity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var titleTextView: TextView
    private lateinit var backImageView: AppCompatImageView
    private lateinit var relativeLayoutNetwork: RelativeLayout
    private lateinit var textViewCurrentNetwork: TextView
    private lateinit var spinnerCryptoNetwork: Spinner
    private lateinit var relativeLayout: RelativeLayout
    private lateinit var textViewFromInfo: TextView
    private lateinit var linearLayoutFrom: LinearLayout
    private lateinit var addressFromNameTextView: TextView
    private lateinit var addressFromPublicTextView: TextView
    private lateinit var linearLayoutAmount: LinearLayout
    private lateinit var textViewBalance: TextView
    private lateinit var editTextAmount: EditText
    private lateinit var textViewAmount: TextView
    private lateinit var textViewEquivalent: TextView
    private lateinit var imageViewSwap: ImageView
    private lateinit var linearLayoutBalanceError: LinearLayout
    private lateinit var textInputLayoutSendTo: TextInputLayout
    private lateinit var editTextSendTo: TextInputEditText
    private lateinit var tabsLayout: TabLayout
    private lateinit var textviewGasPrice: TextView
    private lateinit var textviewGasPriceUsd: TextView
    private lateinit var sendButton: AppCompatButton
    private lateinit var backButton: AppCompatButton
    private lateinit var recyclerViewContacts: RecyclerView

    private val viewModel: SendCryptoViewModel by viewModels()
    private var listAdapter: ContactsAdapter? = null
    private var preloadNetwork = true

    companion object {
        private const val EXTRA_QR_ENTITY_UID = "co.verifik.wallet.EXTRA_QR_ENTITY_UID"
        private const val EXTRA_CURRENT_NETWORK = "co.verifik.wallet.EXTRA_CURRENT_NETWORK"
        fun newIntent(
            context: Context,
            qrEntityUid: Int,
            currentNetwork: String
        ): Intent {
            val i = Intent(
                context,
                SendCryptoActivity::class.java
            )
            i.putExtra(EXTRA_QR_ENTITY_UID, qrEntityUid)
            i.putExtra(EXTRA_CURRENT_NETWORK, currentNetwork)
            return i
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_crypto)

        setupComponents()
        setupListeners()
        loadInfo()
    }

    private fun setupComponents() {
        mainLayout = findViewById(R.id.main)
        titleTextView = findViewById(R.id.textview_title)
        backImageView = findViewById(R.id.ivBack)
        relativeLayoutNetwork = findViewById(R.id.relativelayout_network)
        textViewCurrentNetwork = findViewById(R.id.textview_current_network)
        spinnerCryptoNetwork = findViewById(R.id.spinner_crypto_network)
        relativeLayout = findViewById(R.id.relativelayout_amount)
        textViewFromInfo = findViewById(R.id.textview_from_info)
        linearLayoutFrom = findViewById(R.id.linearlayout_from)
        addressFromNameTextView = findViewById(R.id.textview_address_from_name)
        addressFromPublicTextView = findViewById(R.id.textview_address_from_public)
        linearLayoutAmount = findViewById(R.id.linearlayout_amount)
        textViewBalance = findViewById(R.id.textview_balance)
        editTextAmount = findViewById(R.id.edittext_amount)
        textViewAmount = findViewById(R.id.textview_amount)
        textViewEquivalent = findViewById(R.id.textview_equivalent)
        imageViewSwap = findViewById(R.id.imageview_swap)
        linearLayoutBalanceError = findViewById(R.id.linearlayout_balance_error)
        textInputLayoutSendTo = findViewById(R.id.textinputlayout_to_address)
        editTextSendTo = findViewById(R.id.edittext_to_address)
        tabsLayout = findViewById(R.id.tablayout)
        textviewGasPrice = findViewById(R.id.textview_gas_price)
        textviewGasPriceUsd = findViewById(R.id.textview_gas_price_usd)
        sendButton = findViewById(R.id.button_send)
        backButton = findViewById(R.id.button_back)
        recyclerViewContacts = findViewById(R.id.recyclerview_contacts)

        titleTextView.text = getString(R.string.activity_send_crypto_title)
        recyclerViewContacts.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun setupListeners() {
        backImageView.setOnClickListener {
            finish()
        }

        mainLayout.setOnClickListener {
            editTextSendTo.clearFocus()
            mainLayout.hideKeyboard()
        }

        viewModel.currentQrEntity.observe(this) {
            addressFromNameTextView.text = it.idQr
            addressFromPublicTextView.text = it.ethAddress
        }

        editTextSendTo.afterTextChanged {
            viewModel.updateSendAddress(this, it)
            showContactsSuggestions(editTextSendTo.hasFocus())
            listAdapter?.filter?.filter(it.lowercase())
        }

        editTextSendTo.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            showContactsSuggestions(hasFocus)
        }
        editTextSendTo.setOnKeyListener { view, i, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN && i == KEYCODE_ENTER) {
                editTextSendTo.clearFocus()
                view?.hideKeyboard()
                return@setOnKeyListener true
            }
            false
        }

        tabsLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.changeContactsSrcAndUpdate((tab?.position ?: 0) == 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewModel.contacts.observe(this) { contacts ->
            listAdapter = ContactsAdapter(
                this,
                contacts,
                object : OnClickOnContactItem {
                    override fun onClick(contact: Contact) {
                        editTextSendTo.setText(contact.address)
                        showContactsSuggestions(false)
                    }
                }
            )
            recyclerViewContacts.adapter = listAdapter
        }

        viewModel.sendToAddressError.observe(this) {
            textInputLayoutSendTo.error = it
        }

        viewModel.rawAmount.observe(this) {
            viewModel.makeCalculationsForAmount()
        }

        relativeLayoutNetwork.setOnClickListener {
            spinnerCryptoNetwork.performClick()
        }
        viewModel.currentNetwork.observe(this) {
            textViewCurrentNetwork.text = it
        }
        relativeLayout.setOnClickListener {
            editTextAmount.requestFocus()
        }
        viewModel.ethBalance.observe(this) {
            textViewBalance.text = it
        }
        viewModel.gasPrice.observe(this) {
            textviewGasPrice.text = it
        }
        viewModel.gasPriceUsd.observe(this) {
            textviewGasPriceUsd.text = it
        }

        viewModel.networks.observe(this) {
            spinnerCryptoNetwork.adapter = CryptoNetworkAdapter(
                this,
                it
            )
        }

        spinnerCryptoNetwork.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long) {
                if(preloadNetwork) {
                    val currentNetwork = viewModel.currentNetwork
                    val pos = viewModel.networks.value?.indexOf(currentNetwork.value) ?: 0
                    spinnerCryptoNetwork.setSelection(pos)
                    preloadNetwork = false
                    return
                }
                val networks = viewModel.networks.value
                val currentNetwork = networks?.get(position) ?: "Ethereum"
                viewModel.changeCurrentNetwork(currentNetwork)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        editTextAmount.doOnTextChanged { text, _, _, _ ->
            updateAmount()
        }

        viewModel.amountStr.observe(this) {
            textViewAmount.text = it
        }
        viewModel.amountEquiv.observe(this) {
            textViewEquivalent.text = it
        }
        viewModel.isEnoughBalance.observe(this) {
            linearLayoutBalanceError.visibility = if(it) View.GONE else View.VISIBLE
        }

        imageViewSwap.setOnClickListener {
            viewModel.updateEditAmountOnCrypto(!(viewModel.editAmountOnCrypto.value ?: false))
            updateAmount()
        }

        viewModel.enableSendButton.observe(this) {
            sendButton.isEnabled = it
        }

        backButton.setOnClickListener {
            finish()
        }
        sendButton.setOnClickListener {
            val qrEntityId = viewModel.currentQrEntity.value?.uid ?: 0
            val sendToAddress = viewModel.sendToAddress.value ?: ""
            val network = viewModel.currentNetwork.value ?: ""
            val amount = viewModel.currentAmount.value ?: ""
            val sendCryptoInfo = SendCryptoInfo(
                qrEntityId,
                sendToAddress,
                network,
                amount
            )
            startActivity(ConfirmSendActivity.newIntent(this, sendCryptoInfo))
        }
    }

    private fun loadInfo() {
        val selectedQrEntityUid = intent.getIntExtra(WalletAccountActivity.EXTRA_QR_ENTITY_UID, 0)
        val currentNetwork = intent.getStringExtra(EXTRA_CURRENT_NETWORK) ?: "Ethereum"
        viewModel.updateCurrentQrEntity(selectedQrEntityUid)

        viewModel.loadInfo(currentNetwork)
        viewModel.updateEditAmountOnCrypto(true)
        viewModel.loadGasOracle()
        viewModel.getContacts()
    }

    private fun updateAmount() {
        val text = editTextAmount.text
        val amount = if(text.isNullOrEmpty()) "0.00" else text.toString()

        viewModel.updateAmount(amount)
    }

    private fun showContactsSuggestions(show: Boolean) {
        val suggestionVisibility = if(show) View.VISIBLE else View.GONE
        val otherViewsVisibility = if(show) View.GONE else View.VISIBLE
        recyclerViewContacts.visibility = suggestionVisibility
        tabsLayout.visibility = suggestionVisibility
        relativeLayoutNetwork.visibility = otherViewsVisibility
        textViewFromInfo.visibility = otherViewsVisibility
        linearLayoutFrom.visibility = otherViewsVisibility
        linearLayoutAmount.visibility = otherViewsVisibility
        linearLayoutBalanceError.visibility = otherViewsVisibility
    }
}