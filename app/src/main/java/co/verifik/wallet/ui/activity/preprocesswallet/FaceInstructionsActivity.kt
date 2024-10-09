package co.verifik.wallet.ui.activity.preprocesswallet

import android.content.Context
import android.content.Intent
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import co.verifik.wallet.R
import co.verifik.wallet.ui.activity.createwallet.PasswordActivity

class FaceInstructionsActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var textviewNavtitle: TextView
    private lateinit var textviewNavsubtitle: TextView
    private lateinit var textViewDesc: TextView
    private lateinit var checkBoxAcceptDontShow: CheckBox
    private lateinit var startButton: AppCompatButton
    private lateinit var password: String
    private var dontShowInstructions = false
    private var isTwelve = true

    private val znsName by lazy {
        intent.getStringExtra(EXTRA_ZNS_NAME)
    }

    companion object {
        private const val EXTRA_ZNS_NAME = "co.verifik.wallet.EXTRA_ZNS_NAME"
        private const val EXTRA_PASSWORD = "co.verifik.wallet.EXTRA_PASSWORD"
        private const val EXTRA_MNEMONIC_SIZE_IS_TWELVE = "co.verifik.wallet.EXTRA_MNEMONIC_SIZE"
        private const val EXTRA_MNEMONIC = "co.verifik.wallet.EXTRA_MNEMONIC"
        fun newIntent(
            context: Context,
            znsName: String,
            password: String,
            isTwelve: Boolean
        ): Intent {
            val intent = Intent(context, FaceInstructionsActivity::class.java)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_PASSWORD, password)
            intent.putExtra(EXTRA_MNEMONIC_SIZE_IS_TWELVE, isTwelve)
            return intent
        }

        fun newIntent(
            context: Context,
            znsName: String,
            password: String,
            mnemonic: String
        ): Intent {
            val intent = Intent(context, FaceInstructionsActivity::class.java)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_PASSWORD, password)
            intent.putExtra(EXTRA_MNEMONIC, mnemonic)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_instructions)

        setupComponents()
        setupListeners()
    }

    private fun setupComponents() {
        ivBack = findViewById(R.id.ivBack)
        textviewNavtitle = findViewById(R.id.textview_navtitle)
        textviewNavsubtitle = findViewById(R.id.textview_navsubtitle)
        textViewDesc = findViewById(R.id.textview_desc)
        checkBoxAcceptDontShow = findViewById(R.id.checkbox_accept_dont_show)
        startButton = findViewById(R.id.button_start)

        val znsNameComplete = "$znsName.zelf"
        textviewNavtitle.text = znsNameComplete
        textviewNavsubtitle.text = getString(R.string.activity_face_instructions_subheader)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textViewDesc.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        }

        password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
        isTwelve = intent.getBooleanExtra(EXTRA_MNEMONIC_SIZE_IS_TWELVE, true)
    }

    private fun setupListeners() {

        ivBack.setOnClickListener {
            finish()
        }

        checkBoxAcceptDontShow.setOnCheckedChangeListener { _, isChecked ->
            dontShowInstructions = isChecked
        }

        startButton.setOnClickListener {
            val prefs = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("dontShowInstructions", dontShowInstructions)
            editor.apply()

            val mnemonicWords = intent.getStringExtra(EXTRA_MNEMONIC)

            if(mnemonicWords != null) {
                startActivity(
                    FaceScanActivity.newIntent(
                        this,
                        znsName ?: "",
                        mnemonicWords,
                        password
                    )
                )
            } else {
                startActivity(
                    FaceScanActivity.newIntent(
                        this,
                        znsName ?: "",
                        password,
                        isTwelve
                    )
                )
            }
        }
    }
}