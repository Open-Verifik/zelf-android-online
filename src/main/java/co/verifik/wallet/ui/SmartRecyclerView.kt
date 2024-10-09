package co.verifik.wallet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * SmartRecyclerView is a custom RecyclerView that can show emptyView when adapter is empty.
 */
class SmartRecyclerView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : RecyclerView(context, attrs, defStyleAttr) {
        /**
         * emptyView to show when adapter is empty
         */
        private var mEmptyView: View? = null

        /**
         * AdapterDataObserver to check if adapter is empty
         */
        private val dataObserver =
            object : AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    checkIfEmpty()
                }

                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    checkIfEmpty()
                }

                override fun onItemRangeRemoved(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeRemoved(positionStart, itemCount)
                    checkIfEmpty()
                }
            }

        /**
         * set adapter to SRV
         */
        override fun setAdapter(adapter: Adapter<*>?) {
            val oldAdapter = getAdapter()
            oldAdapter?.unregisterAdapterDataObserver(dataObserver)

            super.setAdapter(adapter)
            adapter?.registerAdapterDataObserver(dataObserver)
            checkIfEmpty()
        }

        /**
         * set emptyView to SRV
         */
        fun setEmptyView(emptyView: View) {
            mEmptyView = emptyView
        }

        /**
         * check if adapter connected to SRV is empty. If so, show emptyView.
         */
        private fun checkIfEmpty() {
            val isEmpty = adapter!!.itemCount == 0

            mEmptyView?.let {
                it.visibility = if (isEmpty) View.VISIBLE else View.INVISIBLE
                visibility = if (isEmpty) View.INVISIBLE else View.VISIBLE
            }
        }
    }
