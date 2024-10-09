package co.verifik.wallet.ui.fragment.wallet.main

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.adapters.TransactionsAdapter
import co.verifik.wallet.ui.activity.wallet.main.WalletActivityViewModel
import co.verifik.wallet.ui.views.PopUpTransactionDetail

class ActivityFragment : Fragment() {

    private lateinit var cardViewEmptyTransactions: CardView
    private lateinit var recyclerViewActivity: RecyclerView

    companion object {
        fun newInstance() = ActivityFragment()
    }

    private val viewModel: WalletActivityViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setComponents()
        setObservers()
    }

    private fun setComponents() {
        cardViewEmptyTransactions = requireView().findViewById(R.id.cardview_empty_transactions)
        recyclerViewActivity = requireView().findViewById(R.id.recyclerview_activity)

        recyclerViewActivity.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        viewModel.getTransactions()
    }

    private fun setObservers() {
        viewModel.transactions.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                recyclerViewActivity.visibility = View.VISIBLE
                cardViewEmptyTransactions.visibility = View.GONE
                val ethAddress = viewModel.currentQrEntity.value?.ethAddress ?: ""
                val usdBalance = viewModel.equivalentBalance
                recyclerViewActivity.adapter = TransactionsAdapter(
                    requireContext(),
                    ethAddress,
                    usdBalance,
                    it
                ) { transaction ->
                    val popup = PopUpTransactionDetail()
                    popup.showPopupWindow(
                        requireContext(),
                        ethAddress,
                        requireView(),
                        transaction
                    )
                }
            } else {
                recyclerViewActivity.visibility = View.GONE
                cardViewEmptyTransactions.visibility = View.VISIBLE
            }
        }

        viewModel.currentNetwork.observe(viewLifecycleOwner) {
            viewModel.getTransactions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }
}