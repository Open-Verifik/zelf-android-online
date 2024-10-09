package co.verifik.wallet.adapters

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.data.db.QrEntity
import com.google.android.material.card.MaterialCardView

class AccountAdapter(
    private val context: Context,
    var list: MutableList<QrEntity>,
    var currentQrEntity: QrEntity?,
    var allBalancesStr: List<String>,
    var allBalancesUsdStr: List<String>,
    private val onAccountSelected: (QrEntity) -> Unit,
    private val onAccountDeleted: (QrEntity) -> Unit
): RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val accountCardView: MaterialCardView = view.findViewById(R.id.cardview_account)
        val viewLine: View = view.findViewById(R.id.view_line)
        val accountSelectedImageView: ImageView = view.findViewById(R.id.imageview_account_selected)
        val accountNameTextView: TextView = view.findViewById(R.id.textview_account_name)
        val accountAddressTextView: TextView = view.findViewById(R.id.textview_address)
        val accountBalanceTextView: TextView = view.findViewById(R.id.textview_balance)
        val accountBalanceUsdTextView: TextView = view.findViewById(R.id.textview_balance_usd)
        val imageViewMenu: ImageView = view.findViewById(R.id.imageview_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.account_cell_view,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = list[position]
        holder.viewLine.visibility = if (entity.ethAddress == currentQrEntity?.ethAddress) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        val selectedImg = if (entity.ethAddress == currentQrEntity?.ethAddress) {
            R.drawable.check_circle
        } else {
            R.drawable.circle2
        }
        holder.accountNameTextView.text = entity.idQr
        holder.accountAddressTextView.text = entity.ethAddress
        holder.accountSelectedImageView.setImageResource(selectedImg)

        holder.accountBalanceTextView.text = allBalancesStr[position]
        holder.accountBalanceUsdTextView.text = allBalancesUsdStr[position]

        holder.accountCardView.setOnClickListener {
            onAccountSelected(entity)
            handler.postDelayed({
                currentQrEntity = entity
                notifyDataSetChanged()
            }, 200)
        }

        holder.imageViewMenu.setOnClickListener{
            val popup = PopupMenu(context, holder.imageViewMenu)
            popup.inflate(R.menu.wallet_account_menu)
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    when (item.getItemId()) {
                        R.id.see_on_explorer -> {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse("https://etherscan.io/address/${entity.ethAddress}")
                            context.startActivity(intent)
                            return true
                        }
                        R.id.delete -> {
                            if(list.size == 1) {
                                return true
                            }
                            val pos = list.indexOf(entity)
                            list.removeAt(pos)
                            notifyItemRemoved(pos)
                            notifyItemRangeChanged(pos, itemCount)
                            onAccountDeleted(entity)
                            return true
                        }
                        else -> return false
                    }
                }
            })
            popup.show()
        }
    }

}