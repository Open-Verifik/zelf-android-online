package co.verifik.wallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IPFSResponse(
    val id: String? = null,
    @Json(name = "ipfs_pin_hash")
    val ipfsPinHash: String? = null,
    val size: Long? = null,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "date_pinned")
    val datePinned: String? = null,
    @Json(name = "date_unpinned")
    val dateUnpinned: String? = null,
    val metadata: IPFSMetadata? = null,
    //val regions: List<IPFSRegion>? = null,
    @Json(name = "mime_type")
    val mimeType: String? = null,
    @Json(name = "number_of_files")
    val numberOfFiles: Int? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class IPFSMetadata(
    val name: String? = null,
    val keyvalues: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class IPFSRegion(
    val regionId: String? = null,
    val currentReplicationCount: Int? = null,
    val desiredReplicationCount: Int? = null
)