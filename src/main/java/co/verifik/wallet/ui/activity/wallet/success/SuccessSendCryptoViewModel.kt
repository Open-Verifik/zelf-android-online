package co.verifik.wallet.ui.activity.wallet.success

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import co.verifik.wallet.data.local.SendCryptoInfo
import co.verifik.wallet.utils.CryptoWallet
import org.web3j.protocol.core.methods.response.TransactionReceipt
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executors


class SuccessSendCryptoViewModel: ViewModel() {

    private val _transactionReceipt = MutableLiveData<TransactionReceipt>()
    val transactionReceipt get() = _transactionReceipt

    fun loadItemsForTransaction(mnemonic: String, sendCryptoInfo: SendCryptoInfo) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val transactionReceipt = CryptoWallet.sendEthTransaction(mnemonic, sendCryptoInfo)
                _transactionReceipt.postValue(transactionReceipt)
            } catch (_: HttpException) {
            } catch (_: SocketTimeoutException) {
            } catch (_: UnknownHostException) {
            }
        }
    }
}