package co.verifik.wallet.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QrEntityDao {
    @Query("SELECT * FROM qr_entity")
    suspend fun getAll(): List<QrEntity>

    @Query("SELECT * FROM qr_entity WHERE `uid` = :idQr")
    suspend fun getQrEntity(idQr: Int): QrEntity?

    @Query("SELECT count(*) FROM qr_entity")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(qrEntity: QrEntity)

    @Query("DELETE FROM qr_entity WHERE `uid` = :idQr")
    suspend fun delete(idQr: Int)

    @Query("DELETE FROM qr_entity")
    suspend fun deleteAll()
}