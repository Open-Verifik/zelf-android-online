package co.verifik.wallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_entity")
data class QrEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "id_qr") val idQr: String?,
    @ColumnInfo(name = "eth_address") var ethAddress: String?,
    @ColumnInfo(name = "solana_address") val solanaAddress: String?,
    @ColumnInfo(name = "qr_bytes") val qrBytes: ByteArray?,
    @ColumnInfo(name = "img_bytes") val imgBytes: ByteArray?
)
