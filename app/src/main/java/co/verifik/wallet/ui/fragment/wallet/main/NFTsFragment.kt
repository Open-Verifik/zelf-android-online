package co.verifik.wallet.ui.fragment.wallet.main

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.verifik.wallet.R
import co.verifik.wallet.adapters.NftsAdapter
import co.verifik.wallet.ui.activity.wallet.main.WalletActivityViewModel

class NFTsFragment : Fragment() {

    private lateinit var cardViewEmptyNfts: CardView
    private lateinit var recyclerViewNfts: RecyclerView

    companion object {
        fun newInstance() = NFTsFragment()
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
        cardViewEmptyNfts = requireView().findViewById(R.id.cardview_empty_nft)
        recyclerViewNfts = requireView().findViewById(R.id.recyclerview_nft)
        recyclerViewNfts.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            false
        )
        viewModel.getAllNfts()
    }

    private fun setObservers() {
        viewModel.allNfts.observe(viewLifecycleOwner) {
            if(it.isNotEmpty()) {
                recyclerViewNfts.visibility = View.VISIBLE
                cardViewEmptyNfts.visibility = View.GONE
                val adapter = NftsAdapter(requireActivity(), it)
                recyclerViewNfts.adapter = adapter
            } else {
                recyclerViewNfts.visibility = View.GONE
                cardViewEmptyNfts.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_nfts, container, false)
    }
}