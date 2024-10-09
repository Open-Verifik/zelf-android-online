package co.verifik.wallet.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val status: String? = null,
    val message: String? = null,
    val result: T? = null
)
