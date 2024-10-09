package co.verifik.wallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EtherscanGasOracleResponse(
    @Json(name = "LastBlock")
    val lastBlock: String? = null,
    @Json(name = "SafeGasPrice")
    val safeGasPrice: String? = null,
    @Json(name = "ProposeGasPrice")
    val proposeGasPrice: String? = null,
    @Json(name = "FastGasPrice")
    val fastGasPrice: String? = null,
    val suggestBaseFee: String? = null,
    val gasUsedRatio: String? = null,
)
