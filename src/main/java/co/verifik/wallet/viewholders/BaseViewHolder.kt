package co.verifik.wallet.viewholders

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * BaseViewHolder is a generic class that takes a binding and a data type
 * @param T - data type
 * @param binding - view binding
 */
abstract class BaseViewHolder<T>(val binding: ViewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    var mData: T? = null

    /**
     * bind data to the view
     */
    abstract fun bindData(data: T)
}
