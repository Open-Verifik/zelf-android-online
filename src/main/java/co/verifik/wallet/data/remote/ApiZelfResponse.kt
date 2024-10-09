package co.verifik.wallet.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiZelfResponse<T>(
    val data: T? = null
)
