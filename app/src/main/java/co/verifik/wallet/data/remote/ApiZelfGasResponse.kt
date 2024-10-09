package co.verifik.wallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiZelfGasResponse(
    val low: GasObject? = null,
    val average: GasObject? = null,
    val high: GasObject? = null,
    val featuredActions: List<FeaturedAction>? = null
)

@JsonClass(generateAdapter = true)
data class GasObject(
    val gwei: String? = null,
    val base: String? = null,
    @Json(name = "Priority")
    val priority: String? = null,
    val cost: String? = null,
    val time: String? = null
)

@JsonClass(generateAdapter = true)
data class FeaturedAction(
    val action: String? = null,
    val low: String? = null,
    val average: String? = null,
    val high: String? = null
)