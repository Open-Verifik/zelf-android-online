package co.verifik.wallet.adapters

import android.content.Context
import android.graphics.Bitmap
import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.data.domain.TokenData
import co.verifik.wallet.data.remote.ZelfToken
import com.bumptech.glide.Glide

class TokensAdapter(
    private val context: Context,
    private val tokensList: List<ZelfToken>
): RecyclerView.Adapter<TokensAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageViewNetwork: ImageView = view.findViewById(R.id.imageview_network)
        val textViewNetwork: TextView = view.findViewById(R.id.textview_network)
        val textViewCryptoBalance: TextView = view.findViewById(R.id.textview_crypto_balance)
        val textViewUsdBalance: TextView = view.findViewById(R.id.textview_usd_balance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.token_cell_view,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tokensList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = tokensList[position]

        holder.textViewNetwork.text = token.name
        val amountRaw = token.amount

        if(amountRaw?.any { it.isLetter() } == true) {
            holder.textViewCryptoBalance.text = amountRaw
            holder.textViewUsdBalance.text = "$0.00 USD"
        } else {
            var amountStr = amountRaw
                ?.replace(",", "")
                ?.replace("...","")
            if(amountStr?.isEmpty() == true) {
                amountStr = "0"
            }
            val amount = amountStr?.toBigDecimal()
            val price = token.price?.replace(",", "")?.toBigDecimal() ?: 0.0.toBigDecimal()
            val tokenStr = DecimalFormat("#.######Ξ").format(amount)
            val usdEquivalent = amount?.multiply(price)
            val usdEquivalentStr = DecimalFormat("$#.## USD").format(usdEquivalent)
            holder.textViewCryptoBalance.text = tokenStr
            holder.textViewUsdBalance.text = usdEquivalentStr
        }

        val url = token.image
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(holder.imageViewNetwork)
    }
}