package co.verifik.wallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.data.local.Contact

interface OnClickOnContactItem {
    fun onClick(contact: Contact)
}

class ContactsAdapter(
    private val context: Context,
    private var contactsList: List<Contact>,
    private val onClickOnContactItem: OnClickOnContactItem
): RecyclerView.Adapter<ContactsAdapter.ViewHolder>(), Filterable {

    private var filteredList = contactsList

    private val searchFilter : Filter = object : Filter() {
        override fun performFiltering(input: CharSequence): FilterResults {
            val filteredList = if (input.isEmpty()) {
                contactsList
            } else {
                contactsList.filter { it.name.lowercase().contains(input) || it.address.lowercase().contains(input)  }
            }
            return FilterResults().apply { values = filteredList }
        }

        override fun publishResults(input: CharSequence, results: FilterResults) {
            filteredList = if(results.values == null)
                emptyList()
            else
                results.values as List<Contact>
            notifyDataSetChanged()
        }
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textViewName: TextView = view.findViewById(R.id.textview_name)
        val textViewAddress: TextView = view.findViewById(R.id.textview_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.contact_cell_view, parent, false)
        val holder = ViewHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            onClickOnContactItem.onClick(filteredList[position])
        }
        return holder
    }

    override fun getItemCount(): Int {
        return filteredList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = filteredList[position]

        holder.textViewName.text = contact.name
        holder.textViewAddress.text = contact.address
    }

    override fun getFilter(): Filter {
        return searchFilter
    }
}