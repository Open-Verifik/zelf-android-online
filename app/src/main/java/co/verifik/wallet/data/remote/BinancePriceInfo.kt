package co.verifik.wallet.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BinancePriceInfo(
    val symbol: String? = null,
    val price: String? = null
)
