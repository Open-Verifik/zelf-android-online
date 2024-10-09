package co.verifik.wallet.ui.fragment.wallet.main

import android.content.ClipData
import android.content.ClipboardManager
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.CLIPBOARD_SERVICE
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import co.verifik.wallet.Constants
import co.verifik.wallet.R
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.ui.activity.wallet.main.WalletActivityViewModel
import co.verifik.wallet.utils.BaseFragment
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout

class MainWalletFragment(
    private val tapOnReceive: (View) -> Unit,
    private val tapOnSend: (View) -> Unit,
    private val tapOnQR: (View) -> Unit
) : BaseFragment() {

    private lateinit var linearLayoutAddress: LinearLayout
    private lateinit var addressTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var usdBalanceTextView: TextView
    private lateinit var imageButtonReceive: ImageButton
    private lateinit var imageButtonSend: ImageButton
    private lateinit var imageButtonQR: ImageButton
    private lateinit var tabLayout: TabLayout

    companion object {
        fun newInstance(
            tapOnReceive: (View) -> Unit,
            tapOnSend: (View) -> Unit,
            tapOnQR: (View) -> Unit
        ) = MainWalletFragment(
            tapOnReceive,
            tapOnSend,
            tapOnQR
        )
    }

    private val viewModel: WalletActivityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setComponents()
        addObservers()
    }

    private fun setComponents() {
        linearLayoutAddress = requireView().findViewById(R.id.linearlayout_address)
        addressTextView = requireView().findViewById(R.id.textview_address)
        balanceTextView = requireView().findViewById(R.id.textview_balance)
        usdBalanceTextView = requireView().findViewById(R.id.textview_usd_balance)
        imageButtonReceive = requireView().findViewById(R.id.imagebutton_receive)
        imageButtonSend = requireView().findViewById(R.id.imagebutton_send)
        imageButtonQR = requireView().findViewById(R.id.imagebutton_qr_scanner)
        tabLayout = requireView().findViewById(R.id.tablayout)

        childFragmentManager.commit {
            replace(R.id.subfragment_container, TokensFragment.newInstance())
        }
        viewModel.isLoadingMainWallet.value = true
    }

    private fun addObservers() {
        viewModel.isLoadingMainWallet.observe(viewLifecycleOwner) {
            if(it) {
                showLoading()
            } else {
                hideLoading()
            }
        }
        linearLayoutAddress.setOnClickListener {
            val clipboard: ClipboardManager =
                requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ethAddress", viewModel.currentQrEntity.value?.ethAddress)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireActivity(), R.string.activity_show_qr_copied, Toast.LENGTH_SHORT).show()
        }
        viewModel.currentQrEntity.observe(viewLifecycleOwner) {
            addressTextView.text = it.ethAddress
        }
        viewModel.ethBalance.observe(viewLifecycleOwner) {
            balanceTextView.text = it
        }
        viewModel.usdBalance.observe(viewLifecycleOwner) {
            usdBalanceTextView.text = it
        }
        imageButtonReceive.setOnClickListener(tapOnReceive)
        imageButtonSend.setOnClickListener(tapOnSend)
        imageButtonQR.setOnClickListener(tapOnQR)


        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.text.toString()) {
                    getString(R.string.fragment_main_wallet_tokens) -> {
                        viewModel.isLoadingMainWallet.value = true
                        childFragmentManager.commit {
                            replace(R.id.subfragment_container, TokensFragment.newInstance())
                        }
                    }
                    getString(R.string.fragment_main_wallet_nfts) -> {
                        viewModel.isLoadingMainWallet.value = true
                        childFragmentManager.commit {
                            replace(R.id.subfragment_container, NFTsFragment.newInstance())
                        }
                    }
                    getString(R.string.fragment_main_wallet_activity) -> {
                        viewModel.isLoadingMainWallet.value = true
                        childFragmentManager.commit {
                            replace(R.id.subfragment_container, ActivityFragment.newInstance())
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main_wallet, container, false)
    }
}