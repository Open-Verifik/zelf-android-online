package co.verifik.wallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EtherscanTransaction(
    val blockNumber: String? = null,
    val timeStamp: String? = null,
    val hash: String? = null,
    val nonce: String? = null,
    val blockHash: String? = null,
    val transactionIndex: String? = null,
    val from: String? = null,
    val to: String? = null,
    var value: String? = null,
    var gas: String? = null,
    var gasPrice: String? = null,
    val isError: String? = null,
    @Json(name = "txreceipt_status")
    var txReceiptStatus: String? = null,
    val input: String? = null,
    val contractAddress: String? = null,
    val cumulativeGasUsed: String? = null,
    var gasUsed: String? = null,
    val confirmations: String? = null,
    val methodId: String? = null,
    val functionName: String? = null,
    val functionSignature: String? = null
)
