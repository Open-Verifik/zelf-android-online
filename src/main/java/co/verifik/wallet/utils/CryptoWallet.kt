package co.verifik.wallet.utils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import co.verifik.wallet.Constants
import co.verifik.wallet.R
import co.verifik.wallet.data.domain.MnemonicSize
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.data.remote.ApiZelfGasResponse
import co.verifik.wallet.data.remote.EtherscanGasOracleResponse
import co.verifik.wallet.data.remote.EtherscanTransaction
import co.verifik.wallet.data.remote.GasObject
import co.verifik.wallet.data.remote.ZelfToken
import co.verifik.wallet.utils.network.ApiBinanceInterface
import co.verifik.wallet.utils.network.ApiClient
import co.verifik.wallet.utils.network.ApiEthScanInterface
import co.verifik.wallet.utils.network.ApiZelfInterface
import com.paymennt.crypto.bip32.Network
import com.paymennt.crypto.bip32.wallet.AbstractWallet
import com.paymennt.solanaj.wallet.SolanaWallet
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.security.SecureRandom


class CryptoWallet {

    companion object {

        const val ETHSCAN_API_KEY = "11PX8V3RMCW43UKVXHA5MIXTNAF33T1NCV"

        fun getCryptoNetworks(context: Context): List<String> {
            return context
                .resources
                .getStringArray(R.array.activity_send_crypto_networks)
                .toList()
        }

        suspend fun getEthCryptoBalance(
            application: Application,
            baseUrl: String,
            ethAddressStr: String
        ): BigDecimal {
            val sessionToken = getZelfSession(application)
            val tokenStr = "Bearer $sessionToken"
            val zelfClient = ApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val params = mutableMapOf(
                "address" to ethAddressStr
            )
            if(baseUrl == Constants.ETHERSCAN_SEPOLIA_URL) {
                params["env"] = "development"
            }
            val ethBalance = zelfClient.dashboard(
                tokenStr,
                params
            ).data?.balance ?: "0"
            return ethBalance.toBigDecimal()
        }

        suspend fun getCryptoBalanceInUSD(
            balance: BigDecimal,
            symbol: String
        ): BigDecimal {
            val binanceClient = ApiClient().getApiClient().create(ApiBinanceInterface::class.java)
            val binUnitPrice = binanceClient.getTickerPrice(
                Constants.BINANCE_TICKER_PRICE_URL,
                symbol
            )?.price
            val usdBalance = balance.multiply(BigDecimal(binUnitPrice))
            return usdBalance
        }

        suspend fun getGasOracle(application: Application): ApiZelfGasResponse? {
            val sessionToken = getZelfSession(application)
            val tokenStr = "Bearer $sessionToken"
            val zelfClient = ApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val gasOracleResponse = zelfClient.gasPrices(tokenStr).data
            return gasOracleResponse
        }

        suspend fun getTransactions(baseUrl: String, address: String): List<EtherscanTransaction> {
            val ethScanClient = ApiClient().getApiClient().create(ApiEthScanInterface::class.java)
            val params = mapOf(
                "module" to "account",
                "action" to "txlist",
                "address" to address,
                "sort" to "desc",
                "apikey" to ETHSCAN_API_KEY
            )
            val transactions = ethScanClient.getTransactions(
                baseUrl,
                params
            ).result ?: emptyList()
            return transactions
        }

        @OptIn(ExperimentalStdlibApi::class)
        suspend fun getTransaction(baseUrl: String, transactionHash: String): EtherscanTransaction? {
            val ethScanClient = ApiClient().getApiClient().create(ApiEthScanInterface::class.java)
            val params = mapOf(
                "module" to "proxy",
                "action" to "eth_getTransactionByHash",
                "txhash" to transactionHash,
                "sort" to "desc",
                "apikey" to ETHSCAN_API_KEY
            )
            val transaction = ethScanClient.getTransaction(
                baseUrl,
                params
            ).result
            transaction?.txReceiptStatus = "1"
            transaction?.gas = transaction?.gas?.replace("0x","")?.hexToULong().toString()
            transaction?.gasPrice = transaction?.gasPrice?.replace("0x","")?.hexToULong().toString()
            transaction?.value = transaction?.value?.replace("0x","")?.hexToULong().toString()
            return transaction
        }

        suspend fun getZelfTokens(
            application: Application,
            currentNetworkUrl: String,
            ethAddress: String
        ): List<ZelfToken> {
            val sessionToken = getZelfSession(application)
            val tokenStr = "Bearer $sessionToken"
            val params = mutableMapOf(
                "address" to ethAddress
            )
            if(currentNetworkUrl == Constants.ETHERSCAN_SEPOLIA_URL) {
                params["env"] = "development"
            }
            val zelfClient = ApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val tokens = zelfClient.dashboard(
                tokenStr,
                params
            ).data?.tokenHoldings?.tokens ?: emptyList()
            return tokens
        }

        suspend fun getZelfSession(application: Application): String {
            val zelfClient = ApiClient().getApiClient().create(ApiZelfInterface::class.java)
            val pref = application.getSharedPreferences("wallet_prefs", MODE_PRIVATE)
            var sessionToken = pref.getString("session_token", "")
            if(sessionToken.isNullOrEmpty()) {
                val deviceId: String = Settings.Secure.getString(
                    application.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                val sessionResponse = zelfClient.getSession(mapOf("identifier" to deviceId))
                sessionToken = sessionResponse.data?.token ?: ""
                pref.edit().putString("session_token", sessionToken).apply()
            }
            return sessionToken
        }

        fun sendEthTransaction(mnemonic: String, sendCryptoInfo: SendCryptoInfo): TransactionReceipt {
            val masterKeypair =
                Bip32ECKeyPair.generateKeyPair(MnemonicUtils.generateSeed(mnemonic, ""))
            val derivationPath = intArrayOf(
                44 or Bip32ECKeyPair.HARDENED_BIT,
                60 or Bip32ECKeyPair.HARDENED_BIT,
                0 or Bip32ECKeyPair.HARDENED_BIT,
                0,
                0
            )
            val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath)
            val credentials = Credentials.create(derivedKeyPair)

            val infuraSepolia = "https://sepolia.infura.io/v3/fa9b57b8e54c4867aca64cd891114c96"

            val web3 = if (sendCryptoInfo.network == "Ethereum")
                Web3j.build()
            else Web3j.build(HttpService(infuraSepolia))
            val sentToAddress = sendCryptoInfo.sendToAddress
            val amount = sendCryptoInfo.amount
            val transactionReceipt = Transfer.sendFunds(
                web3,
                credentials,
                sentToAddress,
                BigDecimal(amount),
                Convert.Unit.ETHER
            ).send()

            return transactionReceipt
        }
    }

    fun createWallet(context: Context, password: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val path = context.filesDir
            val file = File(path, "vk_wallet")
            file.mkdirs()
            val wallet = WalletUtils.generateBip39Wallet(password, file)
            val words = wallet.mnemonic
            Log.e("Mnemonico",words)
            return words
        } else {
            TODO("VERSION.SDK_INT < O")
            return ""
        }
    }

    fun generateMnemonic(size: MnemonicSize): String {
        val intSize = when(size) {
            MnemonicSize.MNEMONIC12 -> 16
            MnemonicSize.MNEMONIC24 -> 32
        }
        val initialEntropy = ByteArray(intSize)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(initialEntropy)
        return MnemonicUtils.generateMnemonic(initialEntropy)
    }

    // ETH
    fun loadEthAddressFromMnemonic(mnemonic: String): String {
        val masterKeypair =
            Bip32ECKeyPair.generateKeyPair(MnemonicUtils.generateSeed(mnemonic, ""))
        val derivationPath = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath)
        val credentials = Credentials.create(derivedKeyPair)
        return credentials.address
    }
    // Solana
    fun loadSolanaAddressFromMnemonic(mnemonic: String): String {
        // Temp fix until implement Solana Wallet
//        val network = Network.MAINNET
//        val solanaWallet = SolanaWallet(mnemonic, "", network)
//        // get address (account, chain, index), used to receive
//        return solanaWallet.getAddress(0, AbstractWallet.Chain.EXTERNAL, null)
        return "E21909201928"
    }
}