package co.verifik.wallet.ui.fragment.wallet.changeaccount

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import co.verifik.wallet.R
import co.verifik.wallet.adapters.AccountAdapter
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.ui.activity.preprocesswallet.MainActivity
import co.verifik.wallet.ui.activity.wallet.main.WalletActivityViewModel
import co.verifik.wallet.utils.BaseFragment
import com.google.android.material.card.MaterialCardView

class ChangeAccountFragment(
    private val changeSelectedAccount: (QrEntity) -> Unit,
    private val deleteAccount: (QrEntity) -> Unit
) : BaseFragment() {

    private lateinit var textviewReturn: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardViewAddAccount: MaterialCardView
    private var accountAdapter: AccountAdapter? = null

    companion object {
        fun newInstance(
            changeSelectedAccount: (QrEntity) -> Unit,
            deleteAccount: (QrEntity) -> Unit
        ) = ChangeAccountFragment(
            changeSelectedAccount,
            deleteAccount
        )
    }

    private val viewModel: WalletActivityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupComponents()
        setupListeners()
    }

    private fun setupComponents() {
        showLoading()
        textviewReturn = requireView().findViewById(R.id.textview_return)
        recyclerView = requireView().findViewById(R.id.recyclerview_accounts)
        cardViewAddAccount = requireView().findViewById(R.id.cardview_add_account)

        viewModel.getAllTheBalances()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        val qrEntities = viewModel.qrEntities.value ?: listOf()
        val currentQrEntity = viewModel.currentQrEntity.value ?: qrEntities.first()

        viewModel.allBalancesStr.observe(viewLifecycleOwner) { allBalances ->
                val allBalancesStr = allBalances ?: listOf()
                val allBalancesUsd = viewModel.allBalancesUsd.value ?: listOf()
                if(allBalancesStr.isNotEmpty() && allBalancesUsd.isNotEmpty()) {
                    accountAdapter = AccountAdapter(
                        requireContext(),
                        qrEntities.toMutableList(),
                        currentQrEntity,
                        allBalancesStr,
                        allBalancesUsd,
                        {
                            changeSelectedAccount(it)
                        }, {
                            deleteAccount(it)
                        }
                    )
                    recyclerView.adapter = accountAdapter
                }
                hideLoading()
        }

        viewModel.currentNetwork.observe(viewLifecycleOwner) {
            showLoading()
            viewModel.getAllTheBalances()
        }

        textviewReturn.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        cardViewAddAccount.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_change_account, container, false)
    }
}