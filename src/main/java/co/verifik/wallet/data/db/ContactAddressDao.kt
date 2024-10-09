package co.verifik.wallet.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactAddressDao {
    @Query("SELECT * FROM contact_address")
    suspend fun getAll(): List<ContactAddress>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contactAddress: ContactAddress)

    @Delete
    suspend fun delete(contactAddress: ContactAddress)
}