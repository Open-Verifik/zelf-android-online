package co.verifik.wallet.ui.activity.wallet.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.QrEntity
import kotlinx.coroutines.launch

class WalletAccountViewModel(
    private val application: Application
): AndroidViewModel(application) {
    private var _currentQrEntity = MutableLiveData<QrEntity>()
    val currentQrEntity get() = _currentQrEntity
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "zelf_database"
    ).build()
    private val qrDao = db.qrDao()

    fun getCurrentQrEntity(qrEntityId: Int) {
        viewModelScope.launch {
            val qrEntity = qrDao.getQrEntity(qrEntityId)
            qrEntity?.let {
                _currentQrEntity.value = it
            }
        }
    }
}