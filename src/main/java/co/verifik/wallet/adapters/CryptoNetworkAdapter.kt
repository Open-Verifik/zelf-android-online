package co.verifik.wallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import co.verifik.wallet.R


class CryptoNetworkAdapter(
    context: Context,
    var networkList: List<String>
): ArrayAdapter<String>(context, 0, networkList) {

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        return initView(position, convertView, parent)
    }

    private fun initView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        // It is used to set our custom view.
        var cView = convertView ?: View(context)
        if (convertView == null) {
            cView = LayoutInflater
                .from(context)
                .inflate(R.layout.network_cell_view, parent, false)
            val networkTextView = cView.findViewById<TextView>(R.id.textview_network)
            networkTextView.text = networkList[position]
        }
        return cView
    }
}