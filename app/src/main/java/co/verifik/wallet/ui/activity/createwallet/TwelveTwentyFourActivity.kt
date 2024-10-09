package co.verifik.wallet.ui.activity.createwallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import co.verifik.wallet.R
import co.verifik.wallet.ui.activity.createwallet.PasswordActivity.Companion
import co.verifik.wallet.ui.activity.preprocesswallet.FaceInstructionsActivity
import co.verifik.wallet.ui.activity.preprocesswallet.FaceScanActivity
import com.google.android.material.card.MaterialCardView

class TwelveTwentyFourActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var textviewNavtitle: TextView
    private lateinit var textviewNavsubtitle: TextView
    private lateinit var checkBoxAcceptForget: CheckBox
    private lateinit var textViewMoreInfo: TextView
    private lateinit var cardView24: MaterialCardView
    private lateinit var cardView12: MaterialCardView
    private lateinit var textview24: TextView
    private lateinit var textview12: TextView
    private lateinit var createButton: AppCompatButton
    private var isTwelve = MutableLiveData(false)

    private val znsName by lazy {
        intent.getStringExtra(EXTRA_ZNS_NAME)
    }

    companion object {
        private const val EXTRA_ZNS_NAME = "co.verifik.wallet.EXTRA_ZNS_NAME"
        fun newIntent(
            context: Context,
            znsName: String
        ): Intent {
            val intent = Intent(context, TwelveTwentyFourActivity::class.java)
            intent.putExtra(EXTRA_ZNS_NAME, znsName)
            return intent
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_twelve_twenty_four)

        setupComponents()
        setupListeners()
    }

    private fun setupComponents() {
        ivBack = findViewById(R.id.ivBack)
        textviewNavtitle = findViewById(R.id.textview_navtitle)
        textviewNavsubtitle = findViewById(R.id.textview_navsubtitle)
        textViewMoreInfo = findViewById(R.id.textview_more_info)
        checkBoxAcceptForget = findViewById(R.id.checkbox_accept_forget)
        cardView24 = findViewById(R.id.cardview_word24)
        cardView12 = findViewById(R.id.cardview_word12)
        textview24 = findViewById(R.id.textview_word24)
        textview12 = findViewById(R.id.textview_word12)
        createButton = findViewById(R.id.button_create)

        val znsNameComplete = "$znsName.zelf"
        textviewNavtitle.text = znsNameComplete
        textviewNavsubtitle.text = getString(R.string.activity_1224_subheader)
    }
    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        checkBoxAcceptForget.setOnCheckedChangeListener { _, isChecked ->
            createButton.isEnabled = isChecked
        }
        textViewMoreInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri
                .parse("https://zelf.world/#faq-zelf")
            startActivity(intent)
        }

        cardView24.setOnClickListener {
            isTwelve.value = false
        }

        cardView12.setOnClickListener {
            isTwelve.value = true
        }

        isTwelve.observe(this) { isTwelve ->
            if (isTwelve) {
                cardView24.setCardBackgroundColor(getColor(R.color.zWordCardBackground))
                cardView24.strokeColor = getColor(R.color.zWordCardText)
                cardView12.setCardBackgroundColor(getColor(R.color.zSelectedWordCardBackground))
                textview24.setTextColor(getColor(R.color.zWordCardText))
                textview12.setTextColor(getColor(R.color.zSelectedWordCardText))
            } else {
                cardView12.setCardBackgroundColor(getColor(R.color.zWordCardBackground))
                cardView12.strokeColor = getColor(R.color.zWordCardText)
                cardView24.setCardBackgroundColor(getColor(R.color.zSelectedWordCardBackground))
                textview12.setTextColor(getColor(R.color.zWordCardText))
                textview24.setTextColor(getColor(R.color.zSelectedWordCardText))
            }
        }

        createButton.setOnClickListener {
            val twelve = isTwelve.value ?: false

            val intent = PasswordActivity.newIntent(this, znsName ?: "", twelve)
            startActivity(intent)
        }
    }
}