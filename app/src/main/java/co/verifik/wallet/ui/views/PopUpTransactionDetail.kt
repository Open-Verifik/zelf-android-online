package co.verifik.wallet.ui.views
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.CLIPBOARD_SERVICE
import co.verifik.wallet.R
import co.verifik.wallet.data.remote.EtherscanTransaction
import co.verifik.wallet.utils.weiToEth
import java.math.BigDecimal
import java.text.DecimalFormat


class PopUpTransactionDetail {
    //PopupWindow display method
    fun showPopupWindow(
        context: Context,
        address: String,
        view: View,
        etherscanTransaction: EtherscanTransaction
    ) {
        //Create a View object yourself through inflater
        val inflater =
            view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_detail_transaction, null)

        //Specify the length and width through constants
        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.MATCH_PARENT

        //Make Inactive Items Outside Of PopupWindow
        val focusable = true

        //Create a window with our parameters
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        //Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

        val isSendTransaction = etherscanTransaction.from == address

        //Initialize the elements of our window, install the handler
        val textViewType = popupView.findViewById<TextView>(R.id.textview_type)
        val textViewStatus = popupView.findViewById<TextView>(R.id.textview_status)
        val textViewViewOnExplorer = popupView.findViewById<TextView>(R.id.textview_view_on_explorer)
        val textViewCopyTransactionId = popupView.findViewById<TextView>(R.id.textview_copy_transaction_id)
        val textViewAddressFrom = popupView.findViewById<TextView>(R.id.textview_address_from)
        val textViewAddressTo = popupView.findViewById<TextView>(R.id.textview_address_to)
        val textViewNonce = popupView.findViewById<TextView>(R.id.textview_nonce)
        val textViewAmount = popupView.findViewById<TextView>(R.id.textview_amount)
        val textViewGasLimit = popupView.findViewById<TextView>(R.id.textview_gas_limit)
        val textViewGasUsed = popupView.findViewById<TextView>(R.id.textview_gas_used)
        val textViewBaseFee = popupView.findViewById<TextView>(R.id.textview_base_fee)
        val textViewPriorityFee = popupView.findViewById<TextView>(R.id.textview_priority_fee)
        val textViewTotalGasFee = popupView.findViewById<TextView>(R.id.textview_total_gas_fee)
        val textViewTotalMaxFeePerGas =
            popupView.findViewById<TextView>(R.id.textview_total_max_fee_per_gas)
        val textViewTotal = popupView.findViewById<TextView>(R.id.textview_total)

        val typeText = if (isSendTransaction)
            view.context.getString(R.string.fragment_activity_sent)
        else view.context.getString(R.string.fragment_activity_received)

        val confirmed = etherscanTransaction.txReceiptStatus == "1"
        val statusColor = if (confirmed)
            Color.parseColor("#38A62B")
        else Color.parseColor("#BA1A1A")

        val statusText = if (confirmed)
            view.context.getString(R.string.popup_detail_transaction_status_confirmed)
        else view.context.getString(R.string.popup_detail_transaction_status_error)


        val gasLimit = BigDecimal(etherscanTransaction.gas ?: "0")
        val gasUsed = BigDecimal(etherscanTransaction.gasUsed ?: "0")
        val gasPrice = BigDecimal(etherscanTransaction.gasPrice ?: "0")
        val gasPriceTotal = gasUsed.multiply(gasPrice)
        val eth = BigDecimal(etherscanTransaction.value ?: "0")
        val total = eth.plus(gasPriceTotal)
        val ethString = DecimalFormat("#.######Ξ").format(eth.weiToEth())
        val gasLimitString = DecimalFormat("#.##").format(
            gasLimit
        )
        val gasUsedString = DecimalFormat("#.##").format(
            gasUsed.divide(1000000000.toBigDecimal())
        )
        val gasPriceString = DecimalFormat("#.###### Gwei").format(
            gasPrice.divide(1000000000.toBigDecimal())
        )
        val gasPriceTotalString = DecimalFormat("#.######Ξ").format(
            gasPriceTotal.weiToEth()
        )
        val totalString = DecimalFormat("#.######Ξ").format(total.weiToEth())

        textViewType.text = typeText
        textViewStatus.text = statusText
        textViewStatus.setTextColor(statusColor)
        textViewAddressFrom.text = etherscanTransaction.from
        textViewAddressTo.text = etherscanTransaction.to

        textViewNonce.text = etherscanTransaction.nonce
        textViewAmount.text = ethString
        textViewGasLimit.text = gasLimitString
        textViewGasUsed.text = gasUsedString
        textViewBaseFee.text = gasPriceString
        textViewPriorityFee.text = gasPriceString
        textViewTotalGasFee.text = gasPriceTotalString
        textViewTotalMaxFeePerGas.text = gasPriceString
        textViewTotal.text = totalString

        textViewViewOnExplorer.setOnClickListener {
            val url = "https://etherscan.io/tx/${etherscanTransaction.hash}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            view.context.startActivity(intent)
        }

        textViewCopyTransactionId.setOnClickListener {
            val txHash = etherscanTransaction.hash
            val clipboard: ClipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("txhash", txHash)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }

        //Handler for clicking on the inactive zone of the window
        popupView.setOnTouchListener { v, event -> //Close the window when clicked
            popupWindow.dismiss()
            true
        }
    }
}