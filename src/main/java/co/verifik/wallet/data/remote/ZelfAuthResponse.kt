package co.verifik.wallet.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ZelfAuthResponse(
    val token: String? = null,
    val error: String? = null
)
