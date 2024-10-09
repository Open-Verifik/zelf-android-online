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

class NftsAdapter(
    private val context: Context,
    private val nftList: List<ZelfToken>
): RecyclerView.Adapter<NftsAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageViewNetwork: ImageView = view.findViewById(R.id.imageview_network)
        val textViewNetwork: TextView = view.findViewById(R.id.textview_network)
        val textViewCryptoType: TextView = view.findViewById(R.id.textview_crypto_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.nft_cell_view,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return nftList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val nft = nftList[position]

        holder.textViewNetwork.text = nft.name
        val amountRaw = nft.amount

        if(amountRaw?.any { it.isLetter() } == true) {
            holder.textViewCryptoType.text = nft.type
        } else {
            var amountStr = amountRaw
                ?.replace(",", "")
                ?.replace("...","")
            if(amountStr?.isEmpty() == true) {
                amountStr = "0"
            }
            val amount = amountStr?.toBigDecimal()
            val price = nft.price?.replace(",", "")?.toBigDecimal() ?: 0.0.toBigDecimal()
            val tokenStr = DecimalFormat("#.######Ξ").format(amount)
            val usdEquivalent = amount?.multiply(price)
            val usdEquivalentStr = DecimalFormat("$#.## USD").format(usdEquivalent)
            holder.textViewCryptoType.text = nft.type
        }

        val url = nft.image
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(holder.imageViewNetwork)
    }
}