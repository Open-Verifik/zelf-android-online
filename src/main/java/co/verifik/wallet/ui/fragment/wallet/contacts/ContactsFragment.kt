package co.verifik.wallet.ui.fragment.wallet.contacts

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.viewModels
import co.verifik.wallet.R
import co.verifik.wallet.utils.afterTextChanged
import com.google.android.material.textfield.TextInputEditText


/**
 * A simple [Fragment] subclass.
 * Use the [ContactsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ContactsFragment : Fragment() {

    private lateinit var textViewReturn: TextView
    private lateinit var editTextSearch: TextInputEditText
    private lateinit var listViewContacts: ListView
    private lateinit var buttonAddAddress: AppCompatButton


    private val viewModel: ContactsViewModel by viewModels()
    private var listAdapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComponents()
        setupListeners()
    }

    private fun setupComponents() {
        textViewReturn = requireView().findViewById(R.id.textview_return)
        editTextSearch = requireView().findViewById(R.id.edittext_search)
        listViewContacts = requireView().findViewById(R.id.listview_contacts)
        buttonAddAddress = requireView().findViewById(R.id.button_add_address)

        viewModel.getContacts()
    }

    private fun setupListeners() {
        textViewReturn.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            val contactsAddr = contacts.map { it.address }
            listAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                contactsAddr
            )
            listViewContacts.adapter = listAdapter
        }
        editTextSearch.afterTextChanged {
            listAdapter?.filter?.filter(it)
        }
        buttonAddAddress.setOnClickListener {
            showCreateAddressDialog()
        }
    }

    private fun showCreateAddressDialog() {
        context?.let { context ->
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_create_contact)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            dialog.setCancelable(true)
            val window = dialog.window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ) // change mathch
            window.setGravity(Gravity.CENTER)
            val lp = window.attributes
            lp.dimAmount = 0.7f
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.attributes = lp
            dialog.window!!.attributes.windowAnimations = R.style.animation

            val imageViewClose: ImageView = dialog.findViewById(R.id.imageview_close)
            val editTextAddress: TextInputEditText = dialog.findViewById(R.id.edittext_address)
            val buttonAddAddress: AppCompatButton = dialog.findViewById(R.id.button_add_address)
            val buttonCancel: AppCompatButton = dialog.findViewById(R.id.button_cancel)

            imageViewClose.setOnClickListener {
                dialog.dismiss()
            }

            buttonAddAddress.setOnClickListener {
                if (editTextAddress.text?.isNotEmpty() == true) {
                    createContact(editTextAddress.text.toString())
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                }
            }

            buttonCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun createContact(contact: String) {
        viewModel.createContact(contact)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ContactsFragment.
         */
        @JvmStatic
        fun newInstance() = ContactsFragment()
    }
}