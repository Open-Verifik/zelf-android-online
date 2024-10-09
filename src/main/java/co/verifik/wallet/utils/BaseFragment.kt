package co.verifik.wallet.utils

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import co.verifik.wallet.R

open class BaseFragment: Fragment() {
    val dialog: AlertDialog by lazy {
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.loading_dialog)
        dialogBuilder.setView(R.layout.dialog_loading2)
        dialogBuilder.setCancelable(false)
        dialogBuilder.create()
    }

    fun showLoading() {
        dialog.show()
    }

    fun hideLoading() {
        dialog.dismiss()
    }
}