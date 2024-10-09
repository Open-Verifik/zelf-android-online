package co.verifik.wallet.adapters

import android.content.Context
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.data.remote.EtherscanTransaction
import co.verifik.wallet.utils.weiToEth
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class TransactionsAdapter(
    private val context: Context,
    private val address: String,
    private val equivUsd: BigDecimal,
    private val transactions: List<EtherscanTransaction>,
    private val onTransactionClick: (EtherscanTransaction) -> Unit
): RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageViewType: ImageView = view.findViewById(R.id.imageview_type)
        val textViewType: TextView = view.findViewById(R.id.textview_type)
        val textViewDate: TextView = view.findViewById(R.id.textview_date)
        val textViewToFrom: TextView = view.findViewById(R.id.textview_to_from)
        val textViewCryptoBalance: TextView = view.findViewById(R.id.textview_crypto_balance)
        val textViewUsdBalance: TextView = view.findViewById(R.id.textview_usd_balance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.activity_cell_view, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val isSendTransaction = transaction.from == address
        val typeImg = if (isSendTransaction)
            R.drawable.ic_arrow_send
        else R.drawable.ic_arrow_download
        val typeStr = if (isSendTransaction)
            context.getString(R.string.fragment_activity_sent)
        else context.getString(R.string.fragment_activity_received)
        val transactionColor = if (isSendTransaction)
            Color.parseColor("#38A62B")
        else Color.parseColor("#BA1A1A")
        val date = Date((transaction.timeStamp?.toLong() ?: System.currentTimeMillis()) * 1000)
        val dateFormat = SimpleDateFormat("MMM dd ·", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        val toFromStr = if (isSendTransaction) {
            val firstFourLastFour = transaction.to?.take(4) + "..." + transaction.to?.takeLast(4)
            context.getString(R.string.fragment_activity_to_, firstFourLastFour)
        }
        else {
            val firstFourLastFour = transaction.from?.take(4) + "..." + transaction.from?.takeLast(4)
            context.getString(R.string.fragment_activity_from_, firstFourLastFour)
        }

        val cryptoBalance = BigDecimal(transaction.value).weiToEth()
        val cryptoBalanceStr = DecimalFormat("#.######Ξ").format(cryptoBalance)
        val usdBalance = cryptoBalance.multiply(equivUsd)
        val usdBalanceStr = DecimalFormat("$#.## USD").format(usdBalance)

        holder.imageViewType.setImageResource(typeImg)
        holder.textViewType.text = typeStr
        holder.textViewDate.text = dateStr
        holder.textViewDate.setTextColor(transactionColor)
        holder.textViewToFrom.text = toFromStr
        holder.textViewCryptoBalance.text = cryptoBalanceStr
        holder.textViewUsdBalance.text = usdBalanceStr

        holder.itemView.setOnClickListener {
            onTransactionClick(transaction)
        }
    }
}