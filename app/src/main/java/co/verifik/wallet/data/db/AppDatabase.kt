package co.verifik.wallet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QrEntity::class, ContactAddress::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun qrDao(): QrEntityDao
    abstract fun contactAddressDao(): ContactAddressDao
}