package co.verifik.wallet.ui.activity.createwallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import co.verifik.wallet.R
import co.verifik.wallet.ui.activity.preprocesswallet.FaceInstructionsActivity
import co.verifik.wallet.ui.activity.preprocesswallet.FaceScanActivity
import co.verifik.wallet.utils.afterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PasswordActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var ivBack: ImageView
    private lateinit var psswEditText: TextInputEditText
    private lateinit var psswLayout: TextInputLayout
    private lateinit var confirmPsswEditText: TextInputEditText
    private lateinit var confirmPsswLayout: TextInputLayout
    private lateinit var checkBoxAcceptForget: CheckBox
    private lateinit var textViewMoreInfo: TextView
    private lateinit var createButton: AppCompatButton
    private lateinit var noPsswButton: AppCompatButton

    private val znsName by lazy {
        intent.getStringExtra(EXTRA_ZNS_NAME)
    }
    private val isTwelve by lazy {
        intent.getBooleanExtra(EXTRA_IS_TWELVE, false)
    }

    private var mnemonicWords: String? = null
    companion object {
        const val EXTRA_MNEMONIC = "co.verifik.wallet.EXTRA_MNEMONIC"
        const val EXTRA_ZNS_NAME = "co.verifik.wallet.EXTRA_ZNS_NAME"
        const val EXTRA_IS_TWELVE = "co.verifik.wallet.EXTRA_IS_TWELVE"
        // Method to create a new Intent for MainActivity
        fun newIntent(
            context: Context,
            znsName: String,
            isTwelve: Boolean
        ): Intent {
            val intent = Intent(context, PasswordActivity::class.java)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_IS_TWELVE, isTwelve)
            return intent
        }

        fun newIntent(
            context: Context,
            znsName: String,
            mnemonicWords: String
        ): Intent {
            val intent = Intent(context, PasswordActivity::class.java)
            intent.putExtra(EXTRA_MNEMONIC, mnemonicWords)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            return intent
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        setupComponents()
        setupListeners()
    }

    private fun setupComponents() {
        titleTextView = findViewById(R.id.textview_navtitle)
        subtitleTextView = findViewById(R.id.textview_navsubtitle)
        ivBack = findViewById(R.id.ivBack)
        psswEditText = findViewById(R.id.edittext_pssw)
        psswLayout = findViewById(R.id.layout_pssw)
        confirmPsswEditText = findViewById(R.id.edittext_confirm_pssw)
        confirmPsswLayout = findViewById(R.id.layout_confirm_pssw)
        checkBoxAcceptForget = findViewById(R.id.checkbox_accept_forget)
        textViewMoreInfo = findViewById(R.id.textview_more_info)
        createButton = findViewById(R.id.button_create)
        noPsswButton = findViewById(R.id.button_no_pssw)

        mnemonicWords = intent.getStringExtra(EXTRA_MNEMONIC)

        val znsNameComplete = "$znsName.zelf"
        titleTextView.text = znsNameComplete
        subtitleTextView.text = getString(R.string.activity_password_subheader1)
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }
        psswEditText.afterTextChanged {
            validateToCreateWithPssw()
        }
        confirmPsswEditText.afterTextChanged {
            validateToCreateWithPssw()
        }
        checkBoxAcceptForget.setOnCheckedChangeListener { _, isChecked ->
            validateToCreateWithPssw()
        }
        textViewMoreInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri
                .parse("https://zelf.world/#faq-zelf")
            startActivity(intent)
        }

        createButton.setOnClickListener {
            val pssw = psswEditText.text.toString().trim()
            val pref = getSharedPreferences("wallet_prefs", MODE_PRIVATE)
            val dontShowInstructions = pref.getBoolean("dontShowInstructions", false)
            val intent = if(mnemonicWords != null && dontShowInstructions) {
                val mnemonic = mnemonicWords ?: ""
                FaceScanActivity.newIntent(this, znsName ?: "", mnemonic, pssw)
            } else if(mnemonicWords != null) {
                val mnemonic = mnemonicWords ?: ""
                FaceInstructionsActivity.newIntent(this, znsName ?: "", pssw, mnemonic)
            } else if(dontShowInstructions){
                FaceScanActivity.newIntent(this, znsName ?: "", pssw, isTwelve)
            } else {
                FaceInstructionsActivity.newIntent(this, znsName ?: "", pssw, isTwelve)
            }

            startActivity(intent)
        }

        noPsswButton.setOnClickListener {
            val pssw = ""
            val pref = getSharedPreferences("wallet_prefs", MODE_PRIVATE)
            val dontShowInstructions = pref.getBoolean("dontShowInstructions", false)
            val intent = if(mnemonicWords != null && dontShowInstructions) {
                val mnemonic = mnemonicWords ?: ""
                FaceScanActivity.newIntent(this, znsName ?: "", mnemonic, pssw)
            } else if(mnemonicWords != null) {
                val mnemonic = mnemonicWords ?: ""
                FaceInstructionsActivity.newIntent(this, znsName ?: "", pssw, mnemonic)
            } else if(dontShowInstructions){
                FaceScanActivity.newIntent(this, znsName ?: "", pssw, isTwelve)
            } else {
                FaceInstructionsActivity.newIntent(this, znsName ?: "", pssw, isTwelve)
            }
            startActivity(intent)
        }
    }

    private fun validateToCreateWithPssw() {
        val pssw = psswEditText.text.toString()
        val confirmPssw = confirmPsswEditText.text.toString()
        val acceptForget = checkBoxAcceptForget.isChecked

        createButton.isEnabled = false
        if(pssw.isEmpty()) {
            psswLayout.error = getString(R.string.activity_password_error_empty_password)
            return
        }
        else if(confirmPssw.isEmpty()) {
            psswLayout.error = null
            confirmPsswLayout.error = getString(R.string.activity_password_error_empty_confirm_password)
            return
        }
        else if(pssw!=confirmPssw) {
            psswLayout.error = null
            confirmPsswLayout.error = getString(R.string.activity_password_error_different_password)
            return
        }
        psswLayout.error = null
        confirmPsswLayout.error = null

        createButton.isEnabled = acceptForget
    }
}