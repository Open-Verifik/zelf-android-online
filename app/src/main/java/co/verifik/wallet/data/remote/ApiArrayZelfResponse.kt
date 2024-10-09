package co.verifik.wallet.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiArrayZelfResponse<T>(
    val data: List<T>? = null
)
