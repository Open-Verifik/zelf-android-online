package co.verifik.wallet.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import co.verifik.wallet.databinding.ItemClearDataBinding
import co.verifik.wallet.viewholders.BaseViewHolder
import co.verifik.wallet.viewholders.ClearDataViewHolder

/**
 * ClearDataAdapter is an adapter class that holds the credential items
 * @param delegate is an instance of CredentialItemDelegate
 */
class ClearDataAdapter : BaseAdapter<BaseViewHolder<Pair<String, String>>, Pair<String, String>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder<Pair<String, String>> {
        val binding =
            ItemClearDataBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        return ClearDataViewHolder(binding)
    }
}
