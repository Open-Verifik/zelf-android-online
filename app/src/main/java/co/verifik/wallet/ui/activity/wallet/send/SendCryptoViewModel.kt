package co.verifik.wallet.ui.activity.wallet.send

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import co.verifik.wallet.Constants
import co.verifik.wallet.R
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.ContactAddress
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.data.local.Contact
import co.verifik.wallet.utils.CryptoWallet
import co.verifik.wallet.utils.gweiToEth
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DecimalFormat

class SendCryptoViewModel(
    private val application: Application
): AndroidViewModel(application) {
    private var _currentQrEntity = MutableLiveData<QrEntity>()
    val currentQrEntity get() = _currentQrEntity
    private val _networks = MutableLiveData<List<String>>()
    val networks get() = _networks
    private val _currentNetwork = MutableLiveData<String>()
    val currentNetwork get() = _currentNetwork
    val currentNetworkUrl get() = when(currentNetwork.value) {
        "Ethereum" -> Constants.ETHERSCAN_MAINNET_URL
        "Sepolia" -> Constants.ETHERSCAN_SEPOLIA_URL
        else -> Constants.ETHERSCAN_MAINNET_URL
    }
    private val _sendToAddress = MutableLiveData<String>()
    val sendToAddress get() = _sendToAddress
    private val _sendToAddressError = MutableLiveData<String?>()
    val sendToAddressError get() = _sendToAddressError

    private val _ethBalance = MutableLiveData<String>()
    val ethBalance get() = _ethBalance
    private val _usdBalance = MutableLiveData<String>()
    val usdBalance get() = _usdBalance
    private val _currentAmount = MutableLiveData<String>()
    val currentAmount get() = _currentAmount
    private val _editAmountOnCrypto = MutableLiveData<Boolean>()
    val editAmountOnCrypto get() = _editAmountOnCrypto
    private val _rawAmount = MutableLiveData<String>()
    val rawAmount get() = _rawAmount
    private val _amountStr = MutableLiveData<String>()
    val amountStr get() = _amountStr
    private val _amountEquiv = MutableLiveData<String>()
    val amountEquiv get() = _amountEquiv
    private val _gasPrice = MutableLiveData<String>()
    val gasPrice get() = _gasPrice
    private val _gasPriceUsd = MutableLiveData<String>()
    val gasPriceUsd get() = _gasPriceUsd
    private val _isEnoughBalance = MutableLiveData<Boolean>()
    val isEnoughBalance get() = _isEnoughBalance
    private val _enableSendButton = MutableLiveData<Boolean>()
    val enableSendButton get() = _enableSendButton

    private val _total = MutableLiveData<String>()
    val total get() = _total
    private val _totalUsd = MutableLiveData<String>()
    val totalUsd get() = _totalUsd

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts get() = _contacts

    var equivalentBalance = BigDecimal.ONE
    private var ethBalanceBigDecimal = BigDecimal.ZERO
    private var gasPriceEthBigDecimal = BigDecimal.ZERO
    private var lastUsdUpdate: Long = 0
    private var isMyAccounts = true

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "zelf_database"
    ).build()
    private val qrDao = db.qrDao()
    private val contactsDao = db.contactAddressDao()

    fun updateCurrentQrEntity(qrEntityId: Int) {
        viewModelScope.launch {
            val qrEntity = qrDao.getQrEntity(qrEntityId)
            qrEntity?.let {
                _currentQrEntity.value = it
            }
        }
    }

    fun loadInfo(overrideNetwork: String? = null) {
        viewModelScope.launch {
            loadNetworks(overrideNetwork)
            loadEthBalance(currentNetworkUrl)
        }
    }

    fun changeCurrentNetwork(network: String) {
//        _isLoadingMainWallet.value = true
        _currentNetwork.value = network
        reloadBalanceAndMakeCalculations()
    }

    fun updateAmount(amount: String) {
        _rawAmount.value = amount
    }

    fun updateEditAmountOnCrypto(value: Boolean) {
        _editAmountOnCrypto.value = value
    }

    fun loadGasOracle() {
        viewModelScope.launch {
            fetchGasOracle()
        }
    }

    fun getContacts() {
        viewModelScope.launch {
            fetchContacts()
        }
    }

    fun reloadBalanceAndMakeCalculations() {
        viewModelScope.launch {
            loadEthBalance(currentNetworkUrl)
            makeCalculationsForAmount()
        }
    }

    fun updateSendAddress(context: Context, address: String) {
        _sendToAddress.value = address

        if(!validateInputsForToAddress()) {
            _sendToAddressError.value = context.getString(R.string.activity_send_crypto_provide_a_valid_address)
        } else {
            _sendToAddressError.value = null
        }

        validateInputsForNextButton()
    }

    fun makeCalculationsForAmount() {
        viewModelScope.launch {
            val amount = rawAmount.value ?: "0.00"
            val amountBigDecimal = BigDecimal(amount)
            val amountDouble = amount.toDouble()
            var cryptBalance = 0.0
            val usdEquivalent = getEquivalentBalanceUSD()
            _amountStr.value = if (editAmountOnCrypto.value == true) {
                "$amount ETH"
            } else {
                "$$amount USD"
            }
            _amountEquiv.value = if (editAmountOnCrypto.value != true) {
                val equivBalance = usdEquivalent.toDouble()
                cryptBalance = amountDouble / equivBalance
                val cryptBalanceStr = DecimalFormat("#.######Ξ").format(cryptBalance)
                cryptBalanceStr
            } else {
                val usdBalance = usdEquivalent.multiply(amountBigDecimal) ?: BigDecimal.ZERO
                val usdBalanceStr = DecimalFormat("$#.## USD").format(usdBalance)
                usdBalanceStr
            }

            //TOTAL CALCULATION
            val totalBigDecimal = amountBigDecimal.plus(gasPriceEthBigDecimal)
            val totalStr = DecimalFormat("#.######Ξ").format(totalBigDecimal)
            _total.value = totalStr
            val totalUsd = totalBigDecimal.multiply(usdEquivalent)
            val totalUsdStr = DecimalFormat("$#.## USD").format(totalUsd)
            _totalUsd.value = totalUsdStr

            _currentAmount.value =
                if (editAmountOnCrypto.value == true) amount else cryptBalance.toString()
            checkIfEnoughBalance()
            validateInputsForNextButton()
        }
    }

    fun changeContactsSrcAndUpdate(isMyAccounts: Boolean) {
        this.isMyAccounts = isMyAccounts
        getContacts()
    }

    private fun loadNetworks(overrideNetwork: String? = null) {
        val networks = CryptoWallet.getCryptoNetworks(application)
        _networks.value = networks.toMutableList()
        _currentNetwork.value = overrideNetwork ?: networks.first()
    }

    private suspend fun loadEthBalance(baseUrl: String) {
        try {
            val ethAddressStr = currentQrEntity.value?.ethAddress ?: ""
            ethBalanceBigDecimal = CryptoWallet.getEthCryptoBalance(application, baseUrl, ethAddressStr)
            val ethBalanceStr = DecimalFormat("#.######Ξ").format(ethBalanceBigDecimal)
            _ethBalance.value = ethBalanceStr
            val usdBalanceBigDecimal = getEquivalentBalanceUSD().multiply(ethBalanceBigDecimal)
            val usdBalanceStr = DecimalFormat("$#.##").format(usdBalanceBigDecimal)
            _usdBalance.value = "$usdBalanceStr USD"
        } catch (e: HttpException) {
            _ethBalance.value = "0.00"
            _usdBalance.value = "$0.00 USD"
        } catch (e: SocketTimeoutException) {
            _ethBalance.value = "0.00"
            _usdBalance.value = "$0.00 USD"
        } catch (_: UnknownHostException) {
            _ethBalance.value = "0.00"
            _usdBalance.value = "$0.00 USD"
        }
//        _isLoadingMainWallet.value = false
    }

    private suspend fun fetchGasOracle() {
        try {
            //GWEI
            val gasLimit = BigDecimal("21000")
            val equivBalance = getEquivalentBalanceUSD()
            val gasOracleResponse = CryptoWallet.getGasOracle(application)
            val baseGasBigDecimal = BigDecimal(gasOracleResponse?.average?.gwei ?: "0")
            val fastGasBigDecimal = BigDecimal(gasOracleResponse?.high?.gwei ?: "0")
            val gasPriceEthGWei = (fastGasBigDecimal.plus(baseGasBigDecimal)).multiply(gasLimit)
            gasPriceEthBigDecimal = gasPriceEthGWei.gweiToEth()
            val gasPriceStr = DecimalFormat("#.######Ξ").format(gasPriceEthBigDecimal)
            _gasPrice.value = gasPriceStr
            val usdGasPrice = gasPriceEthBigDecimal.multiply(equivBalance)
            val gasPriceUsdStr = DecimalFormat("$#.## USD").format(usdGasPrice)
            _gasPriceUsd.value = gasPriceUsdStr
        } catch (e: HttpException) {
            gasPriceEthBigDecimal = 0.0.toBigDecimal()
            _gasPrice.value = "0.00"
            _gasPriceUsd.value = "$0.00 USD"
        } catch (e: SocketTimeoutException) {
            gasPriceEthBigDecimal = 0.0.toBigDecimal()
            _gasPrice.value = "0.00"
            _gasPriceUsd.value = "$0.00 USD"
        } catch (_: UnknownHostException) {
            gasPriceEthBigDecimal = 0.0.toBigDecimal()
            _gasPrice.value = "0.00"
            _gasPriceUsd.value = "$0.00 USD"
        }
    }

    private suspend fun getEquivalentBalanceUSD(): BigDecimal {
        //30 seconds delay
        if(System.currentTimeMillis() - lastUsdUpdate < 30000) {
            lastUsdUpdate = System.currentTimeMillis()
            return equivalentBalance
        }
        try {
            equivalentBalance = CryptoWallet.getCryptoBalanceInUSD(
                BigDecimal("1"),
                "ETHUSDT"
            )
        } catch (_: HttpException) {
        } catch (_: SocketTimeoutException) {
        } catch (_: UnknownHostException) {
        }
        return equivalentBalance
    }

    private suspend fun fetchContacts() {
        if(isMyAccounts) {
            val contacts = qrDao.getAll()
            _contacts.value = contacts.map {
                Contact(it.idQr ?: "", it.ethAddress ?: "")
            }
        } else {
            val contacts = contactsDao.getAll()
            _contacts.value = contacts.map {
                Contact(it.name, it.address)
            }
        }
    }

    private fun validateInputsForToAddress(): Boolean {
        val sendToAddressStr = sendToAddress.value
        return sendToAddressStr?.isNotEmpty() == true
    }

    private fun checkIfEnoughBalance() {
        _isEnoughBalance.value = isEnoughBalance(currentAmount.value ?: "0.00")
    }

    private fun isEnoughBalance(quantity: String): Boolean {
        val quantityBigDecimal = BigDecimal(quantity)
        val totalBigDecimal = quantityBigDecimal.plus(gasPriceEthBigDecimal)
        val enoughBalance = ethBalanceBigDecimal >= totalBigDecimal
        return enoughBalance
    }

    private fun validateInputsForNextButton() {
        val addressAvailable = validateInputsForToAddress()

        val cAmount = currentAmount.value ?: "0.00"
        val amountGreaterThanZero = cAmount.isNotEmpty() && cAmount.toDouble() > 0
        val amountEnough = isEnoughBalance.value ?: false

        val enableSendButton = addressAvailable && amountGreaterThanZero && amountEnough
        _enableSendButton.value = enableSendButton
    }
}