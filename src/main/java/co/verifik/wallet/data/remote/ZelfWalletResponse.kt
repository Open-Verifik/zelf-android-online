package co.verifik.wallet.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ZelfWalletResponse(
    val anonymous: Boolean? = null,
    val image: String? = null,
    val hasPassword: Boolean? = null,
    val _id: String? = null,
//    val publicData: PublicData? = null
    val zelfProof: String? = null,
    val zkProof: String? = null,
    val ethAddress: String? = null,
    val solanaAddress: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val __v: Int? = null,
//    val qrCode: String? = null,
    val metadata: ZelfMetadata? = null
)

@JsonClass(generateAdapter = true)
data class ZelfMetadata(
    val mnemonic: String? = null
)
