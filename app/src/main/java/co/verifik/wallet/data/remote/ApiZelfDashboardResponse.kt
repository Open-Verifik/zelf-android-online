package co.verifik.wallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiZelfDashboardResponse(
    val address: String? = null,
    val fullname: String? = null,
    val balance: String? = null,
    val account: ZelfAccount? = null,
    val tokenHoldings: ZelfTokenHolding? = null,
    val transactions: List<ZelfTransaction>? = null
)

@JsonClass(generateAdapter = true)
data class ZelfAccount(
    val asset: String? = null,
    val fiatValue: String? = null,
    val price: String? = null
)

@JsonClass(generateAdapter = true)
data class ZelfTokenHolding(
    @Json(name = "tokensContras")
    val tokenContract: ZelfTokenContract? = null,
    val tokens: List<ZelfToken>? = null
)

@JsonClass(generateAdapter = true)
data class ZelfTokenContract(
    val balance: String? = null,
    val tokenTotal: String? = null
)


@JsonClass(generateAdapter = true)
data class ZelfToken(
    val tokenType: String? = null,
    val name: String? = null,
    val symbol: String? = null,
    val amount: String? = null,
    val price: String? = null,
    val type: String? = null,
    val address: String? = null,
    val image: String? = null
)

@JsonClass(generateAdapter = true)
data class ZelfTransaction(
    val hash: String? = null,
    val method: String? = null,
    val block: String? = null,
    val age: String? = null,
    val from: String? = null,
    val traffic: String? = null,
    val to: String? = null,
    val amount: String? = null,
    val txnFee: String? = null
)