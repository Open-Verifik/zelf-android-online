package co.verifik.wallet.ui.activity.wallet.confirmsend

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import co.verifik.wallet.Constants
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.ContactAddress
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.utils.CryptoWallet
import co.verifik.wallet.utils.gweiToEth
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DecimalFormat

class ConfirmSendViewModel(
    private val application: Application
): AndroidViewModel(application) {

    private var _currentQrEntity = MutableLiveData<QrEntity>()
    val currentQrEntity get() = _currentQrEntity

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

    private val _contacts = MutableLiveData<List<ContactAddress>>()
    val contacts get() = _contacts

    var equivalentBalance = BigDecimal.ONE
    private var gasPriceEthBigDecimal = BigDecimal.ZERO
    private var lastUsdUpdate: Long = 0

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

    fun changeCurrentNetwork(network: String) {
//        _isLoadingMainWallet.value = true
        _currentNetwork.value = network
    }

    fun loadInfo(amount: String) {
        rawAmount.value = amount
        viewModelScope.launch {
            fetchGasOracle()
            makeCalculationsForAmount()
        }
    }

    private suspend fun makeCalculationsForAmount() {
        val amount = rawAmount.value ?: "0.00"
        val amountBigDecimal = BigDecimal(amount)
        val amountDouble = amount.toDouble()
        var cryptBalance = 0.0
        val usdEquivalent = getEquivalentBalanceUSD()
        _amountStr.value = "$amount ETH"
        _amountEquiv.value = run {
            val usdBalance1 = usdEquivalent.multiply(amountBigDecimal) ?: BigDecimal.ZERO
            val usdBalanceStr = DecimalFormat("$#.## USD").format(usdBalance1)
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

}