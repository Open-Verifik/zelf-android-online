package co.verifik.wallet.ui.activity.zns

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import co.verifik.wallet.R
import co.verifik.wallet.ui.activity.createwallet.PasswordActivity
import co.verifik.wallet.ui.activity.createwallet.TwelveTwentyFourActivity
import co.verifik.wallet.ui.activity.importwallet.ImportWalletActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateZNSActivity : AppCompatActivity() {

    private lateinit var backImageView: ImageView
    private lateinit var headerTextView: TextView
    private lateinit var textViewZnsName: TextView
    private lateinit var textViewExpireDate: TextView
    private lateinit var checkBoxAcceptTerms: CheckBox
    private lateinit var textviewTerms: TextView
    private lateinit var createWalletButton: AppCompatButton
    private lateinit var importWalletButton: AppCompatButton

    private val znsName by lazy {
        intent.getStringExtra(EXTRA_CREATE_ZNS_NAME)
    }


    companion object {

        private const val EXTRA_CREATE_ZNS_NAME = "EXTRA_CREATE_ZNS_NAME"

        fun newIntent(
            context: Context,
            znsName: String
        ): Intent {
            val intent = Intent(context, CreateZNSActivity::class.java)
            intent.putExtra(EXTRA_CREATE_ZNS_NAME, znsName)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_znsactivity)

        setupComponents()
        setUpListeners()
    }

    private fun setupComponents() {
        // Assign values to the components in this activity
        backImageView = findViewById(R.id.ivBack)
        headerTextView = findViewById(R.id.textview_navtitle)
        textViewZnsName = findViewById(R.id.textview_zname)
        textViewExpireDate =  findViewById(R.id.textview_expire_date)
        checkBoxAcceptTerms = findViewById(R.id.checkbox_accept_terms)
        textviewTerms = findViewById(R.id.textview_terms)
        createWalletButton = findViewById(R.id.button_create_wallet)
        importWalletButton = findViewById(R.id.button_import_wallet)

        val znsNameComplete = "$znsName.zelf"
        headerTextView.text = znsNameComplete
        textViewZnsName.text = znsNameComplete

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, 1)
        val expireDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        textViewExpireDate.text = getString(R.string.activity_create_zns_until_, expireDate)
    }

    private fun setUpListeners() {
        backImageView.setOnClickListener {
            finish()
        }

        checkBoxAcceptTerms.setOnCheckedChangeListener { _, isChecked ->
            createWalletButton.isEnabled = isChecked
            importWalletButton.isEnabled = isChecked
        }

        textviewTerms.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri
                .parse("https://verifik.notion.site/TERMS-OF-USE-eff74c66f9cc40afafbef029482d3e28")
            startActivity(intent)
        }

        createWalletButton.setOnClickListener {
            val intent = TwelveTwentyFourActivity.newIntent(this, znsName ?: "")
            startActivity(intent)
        }

        importWalletButton.setOnClickListener {
            val intent = ImportWalletActivity.newIntent(this, znsName ?: "")
            startActivity(intent)
        }

    }

}