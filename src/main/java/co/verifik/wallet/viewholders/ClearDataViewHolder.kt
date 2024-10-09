package co.verifik.wallet.viewholders

import co.verifik.wallet.databinding.ItemClearDataBinding

/**
 * ClearDataViewHolder is a generic class that takes a binding and a data type
 * bindingView - view binding
 */
class ClearDataViewHolder(private val bindingView: ItemClearDataBinding) : BaseViewHolder<Pair<String, String>>(bindingView) {
    /**
     * bind data to the text view
     * @param data - requested attributes from face verified credentials
     */
    override fun bindData(data: Pair<String, String>) {
        bindingView.tvKey.text = data.first.uppercase().replace("_", " ") + ":"
        bindingView.tvValue.text = data.second.uppercase()
    }
}
