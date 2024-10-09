package co.verifik.wallet.ui.fragment.wallet.main

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
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
import co.verifik.wallet.adapters.TokensAdapter
import co.verifik.wallet.ui.activity.wallet.main.WalletActivityViewModel

class TokensFragment : Fragment() {

    private lateinit var cardViewEmptyTokens: CardView
    private lateinit var recyclerViewTokens: RecyclerView

    companion object {
        fun newInstance() = TokensFragment()
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
        cardViewEmptyTokens = requireView().findViewById(R.id.cardview_empty_tokens)
        recyclerViewTokens = requireView().findViewById(R.id.recyclerview_tokens)
        recyclerViewTokens.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            false
        )
        viewModel.getAllTokens()
    }

    private fun setObservers() {
        viewModel.allTokens.observe(viewLifecycleOwner) {
            if(it.isNotEmpty()) {
                recyclerViewTokens.visibility = View.VISIBLE
                cardViewEmptyTokens.visibility = View.GONE
                val adapter = TokensAdapter(requireActivity(), it)
                recyclerViewTokens.adapter = adapter
            } else {
                recyclerViewTokens.visibility = View.GONE
                cardViewEmptyTokens.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tokens, container, false)
    }
}