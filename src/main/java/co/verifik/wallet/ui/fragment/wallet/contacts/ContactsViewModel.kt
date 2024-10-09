package co.verifik.wallet.ui.fragment.wallet.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.ContactAddress
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val application: Application
): AndroidViewModel(application){

    private val _contacts = MutableLiveData<List<ContactAddress>>()
    val contacts get() = _contacts

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "zelf_database"
    ).build()
    private val dao = db.contactAddressDao()

    fun createContact(contactStr: String) {
        val contact = ContactAddress(
            name = "No name",
            address = contactStr,
            network = "ETHEREUM"
        )
        viewModelScope.launch {
            dao.insert(contact)
            fetchContacts()
        }
    }

    fun getContacts() {
        viewModelScope.launch {
            fetchContacts()
        }
    }

    private suspend fun fetchContacts() {
        val contacts = dao.getAll()
        _contacts.value = contacts
    }
}