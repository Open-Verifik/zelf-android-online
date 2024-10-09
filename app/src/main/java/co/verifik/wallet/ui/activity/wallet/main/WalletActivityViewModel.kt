package co.verifik.wallet.ui.activity.wallet.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import co.verifik.wallet.Constants
import co.verifik.wallet.R
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.ContactAddress
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.data.domain.TokenData
import co.verifik.wallet.data.remote.EtherscanTransaction
import co.verifik.wallet.data.remote.ZelfToken
import co.verifik.wallet.utils.CryptoWallet
import co.verifik.wallet.utils.gweiToEth
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DecimalFormat
import java.util.concurrent.TimeoutException

class WalletActivityViewModel(
    private val application: Application
): AndroidViewModel(application) {
    private val _qrEntities = MutableLiveData<List<QrEntity>>()
    val qrEntities get() = _qrEntities
    private val _currentQrEntity = MutableLiveData<QrEntity>()
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
    private val _isLoadingMainWallet = MutableLiveData<Boolean>()
    val isLoadingMainWallet get() = _isLoadingMainWallet
    private val _isRefreshingMainWallet = MutableLiveData<Boolean>()
    val isRefreshingMainWallet get() = _isRefreshingMainWallet

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

    private val _total = MutableLiveData<String>()
    val total get() = _total
    private val _totalUsd = MutableLiveData<String>()
    val totalUsd get() = _totalUsd

    private val _allBalancesStr = MutableLiveData<List<String>>()
    val allBalancesStr get() = _allBalancesStr
    private val _allBalancesUsd = MutableLiveData<List<String>>()
    val allBalancesUsd get() = _allBalancesUsd

    private val _allTokens = MutableLiveData<List<ZelfToken>>()
    val allTokens get() = _allTokens

    private val _allNfts = MutableLiveData<List<ZelfToken>>()
    val allNfts get() = _allNfts

    private val _transactions = MutableLiveData<List<EtherscanTransaction>>()
    val transactions get() = _transactions

    private val _sendTransaction = MutableLiveData<EtherscanTransaction?>()
    val sendTransaction get() = _sendTransaction


    var equivalentBalance = BigDecimal.ONE
    private var ethBalanceBigDecimal = BigDecimal.ZERO
    private var gasPriceEthBigDecimal = BigDecimal.ZERO
    private var lastUsdUpdate: Long = 0

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "zelf_database"
    ).build()
    private val qrDao = db.qrDao()

    fun loadInfo() {
        viewModelScope.launch {
            loadNetworks()
            loadQrEntitiesAndSetCurrent()
            loadEthBalance(Constants.ETHERSCAN_MAINNET_URL)
        }
    }

    fun reloadInfo() {
        viewModelScope.launch {
            loadQrEntitiesAndSetCurrent()
            loadEthBalance(Constants.ETHERSCAN_MAINNET_URL)
            _isLoadingMainWallet.value = false
        }
    }

    fun changeCurrentQrEntity(qrEntity: QrEntity) {
        _currentQrEntity.value = qrEntity
        reloadBalance()
    }

    fun changeCurrentNetwork(network: String) {
        _isLoadingMainWallet.value = true
        _currentNetwork.value = network
        reloadBalanceAndMakeCalculations()
    }

    fun reloadBalance() {
        _isLoadingMainWallet.value = true
        _isRefreshingMainWallet.value = true
        viewModelScope.launch {
            loadEthBalance(currentNetworkUrl)
            _isRefreshingMainWallet.value = false
        }
    }

    fun reloadBalanceAndMakeCalculations() {
        viewModelScope.launch {
            loadEthBalance(currentNetworkUrl)
            makeCalculationsForAmount()
        }
    }

    fun deleteQrEntity(qrEntity: QrEntity) {
        viewModelScope.launch {
            qrDao.delete(qrEntity.uid)
        }
    }

    private fun makeCalculationsForAmount() {
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
        }
    }

    fun getMaximumAmountEth(): String {
        var maxAmount = ethBalanceBigDecimal.minus(gasPriceEthBigDecimal)
        maxAmount = if(maxAmount < BigDecimal.ZERO) BigDecimal.ZERO else maxAmount
        maxAmount = maxAmount.setScale(6, RoundingMode.DOWN)
        val maxAmountStr = DecimalFormat("#.######").format(maxAmount)
        return maxAmountStr
    }

    fun getAllTheBalances() {
        viewModelScope.launch {
            getEquivalentBalanceUSD()
            val allBalances = fetchAllTheBalances(currentNetworkUrl)
            _allBalancesUsd.value = allBalances.map {
                val usdBalance = it?.multiply(equivalentBalance)
                DecimalFormat("$#.## USD").format(usdBalance)
            }
            _allBalancesStr.value = allBalances.map {
                DecimalFormat("#.######Ξ").format(it)
            }
        }
    }

    fun getAllTokens() {
        viewModelScope.launch {
            getEquivalentBalanceUSD()
            _allTokens.value = fetchAllTheTokens()
            _isLoadingMainWallet.value = false
        }
    }

    fun getAllNfts() {
        viewModelScope.launch {
            _allNfts.value = fetchAllTheNFTs()
            _isLoadingMainWallet.value = false
        }
    }

    fun getTransactions() {
        viewModelScope.launch {
            _transactions.value = fetchTransactions()
            _isLoadingMainWallet.value = false
        }
    }

    fun getTransaction(
        currentNetworkUrl: String,
        transactionHash: String
    ) {
        viewModelScope.launch {
            _sendTransaction.value = fetchTransaction(
                currentNetworkUrl,
                transactionHash
            )
        }
    }

    private fun loadNetworks() {
        val networks = CryptoWallet.getCryptoNetworks(application)
        _networks.value = networks.toMutableList()
        _currentNetwork.value = networks.first()
    }

    private suspend fun loadQrEntitiesAndSetCurrent() {
        val qrEntities = qrDao.getAll()
        _qrEntities.value = qrEntities
        _currentQrEntity.value = qrEntities.first()
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
        _isLoadingMainWallet.value = false
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

    private suspend fun fetchAllTheBalances(baseUrl: String): List<BigDecimal?> {

        try {
            val balances = coroutineScope {
                val ethBalances = qrEntities.value?.map {
                    async {
                        it.ethAddress?.let { eth ->
                            CryptoWallet.getEthCryptoBalance(application, baseUrl, eth)
                        }
                    }
                }
                return@coroutineScope ethBalances?.awaitAll() ?: emptyList()
            }
            return balances
        } catch (e: HttpException) {
            return emptyList()
        } catch (e: SocketTimeoutException) {
            return emptyList()
        } catch (_: UnknownHostException) {
            return emptyList()
        } catch (_: IndexOutOfBoundsException) {
            return emptyList()
        }
    }

    private suspend fun fetchAllTheTokens(): List<ZelfToken> {

        try {
            val tokens = CryptoWallet.getZelfTokens(
                application,
                currentNetworkUrl,
                currentQrEntity.value?.ethAddress ?: ""
            )
            return tokens.filter { it.price != "0" && it.tokenType == "ERC-20" }
        } catch (e: HttpException) {
        } catch (e: SocketTimeoutException) {
        } catch (_: UnknownHostException) {
        }
        return emptyList()
    }

    private suspend fun fetchAllTheNFTs(): List<ZelfToken> {

        try {
            val tokens = CryptoWallet.getZelfTokens(
                application,
                currentNetworkUrl,
                currentQrEntity.value?.ethAddress ?: ""
            )
            return tokens.filter { it.tokenType == "NFT" }
        } catch (e: HttpException) {
        } catch (e: SocketTimeoutException) {
        } catch (_: UnknownHostException) {
        }
        return emptyList()
    }

    private suspend fun fetchTransactions(): List<EtherscanTransaction> {
        try {
            val ethAddress = currentQrEntity.value?.ethAddress ?: ""
            val transactions = CryptoWallet.getTransactions(currentNetworkUrl, ethAddress)
            return transactions
        } catch (e: HttpException) {
        } catch (e: SocketTimeoutException) {
        } catch (_: UnknownHostException) {
        }
        return emptyList()
    }

    private suspend fun fetchTransaction(
        currentNetworkUrl: String,
        transactionHash: String
    ): EtherscanTransaction? {
        try {
            val transaction = CryptoWallet.getTransaction(
                currentNetworkUrl,
                transactionHash
            )
            return transaction
        } catch (e: HttpException) {
            return null
        } catch (e: SocketTimeoutException) {
            return null
        } catch (_: UnknownHostException) {
            return null
        }
    }
}