package co.verifik.wallet.ui.activity.importwallet

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import co.verifik.wallet.ui.activity.createwallet.PasswordActivity
import co.verifik.wallet.R
import co.verifik.wallet.adapters.MnemonicEditTextAdapter
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.data.domain.MnemonicSize
import co.verifik.wallet.ui.activity.wallet.main.WalletActivity
import co.verifik.wallet.utils.SnapToBlock
import co.verifik.wallet.utils.processBitmapToGetQrBytes
import kotlinx.coroutines.launch

class ImportWalletActivity : AppCompatActivity() {

    final val PAGE_SIZE = 4

    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var textviewNavTitle: TextView
    private lateinit var textviewNavSubtitle: TextView
    private lateinit var textviewTitle: TextView
    private lateinit var textviewDesc: TextView
    private lateinit var cardviewInfo: CardView
    private lateinit var relativeLayoutSize: RelativeLayout
    private lateinit var spinnerMnemonicSize: Spinner
    private lateinit var cardViewLeft: CardView
    private lateinit var cardViewRight: CardView
    private lateinit var textViewCurrentPosition: TextView
    private lateinit var recyclerMnemonic: RecyclerView
    private lateinit var ivBack: ImageView
    private lateinit var btnConfirm: Button
    private var isKeyboardShowing = false
    private var mnemonicSize = MnemonicSize.MNEMONIC12
    private val snapHelper = SnapToBlock(PAGE_SIZE)
    private var currentPage = MutableLiveData(0)
    private val znsName by lazy {
        intent.getStringExtra(EXTRA_CREATE_ZNS_NAME)
    }
    private val adapter = MnemonicEditTextAdapter(
        this,
        mnemonicSize,
        { isValid ->
            btnConfirm.isEnabled = isValid
        },
        { position ->
            val size = when(mnemonicSize) {
                MnemonicSize.MNEMONIC12 -> 12
                MnemonicSize.MNEMONIC24 -> 24
            }
            if(position+1<size) {
                val viewHolder = recyclerMnemonic.findViewHolderForAdapterPosition(position+1)
                            as MnemonicEditTextAdapter.ViewHolder
                viewHolder.editTextView.requestFocus()
            }
        }
    )

    companion object {
        private const val EXTRA_CREATE_ZNS_NAME = "EXTRA_CREATE_ZNS_NAME"

        fun newIntent(
            context: Context,
            znsName: String
        ): Intent {
            val intent = Intent(
                context,
                ImportWalletActivity::class.java
            )
            intent.putExtra(EXTRA_CREATE_ZNS_NAME, znsName)
            return intent
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_wallet)

        setupComponents()
        setUpListeners()
    }

    private fun setupComponents() {
        constraintLayout = findViewById(R.id.main)
        textviewNavTitle = findViewById(R.id.textview_navtitle)
        textviewNavSubtitle = findViewById(R.id.textview_navsubtitle)
        textviewTitle = findViewById(R.id.textview_title)
        textviewDesc = findViewById(R.id.textview_desc)
        cardviewInfo = findViewById(R.id.cardview_info)
        relativeLayoutSize = findViewById(R.id.relativelayout_size)
        spinnerMnemonicSize = findViewById(R.id.spinner_mnemonic_size)
        cardViewLeft = findViewById(R.id.cardview_left)
        cardViewRight = findViewById(R.id.cardview_right)
        textViewCurrentPosition = findViewById(R.id.textview_current_position)
        recyclerMnemonic = findViewById(R.id.recycler_mnemonic)
        ivBack = findViewById(R.id.ivBack)
        btnConfirm = findViewById(R.id.btnConfirm)

        val znsNameComplete = "$znsName.zelf"
        textviewNavTitle.text = znsNameComplete
        textviewNavSubtitle.text = getString(R.string.activity_import_wallet_subheader)

        constraintLayout.getViewTreeObserver().addOnGlobalLayoutListener {
            val r = Rect(0, 0, 0, 0)
            constraintLayout.getWindowVisibleDisplayFrame(r)
            val screenHeight = constraintLayout.rootView.height

            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // keyboard is opened
                if (!isKeyboardShowing) {
                    isKeyboardShowing = true;
                    onKeyboardVisibilityChanged(true);
                }
            } else {
                // keyboard is closed
                if (isKeyboardShowing) {
                    isKeyboardShowing = false;
                    onKeyboardVisibilityChanged(false);
                }
            }
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.activity_1224_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinnerMnemonicSize.adapter = adapter
        }

        recyclerMnemonic.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerMnemonic.adapter = adapter
        recyclerMnemonic.setHasFixedSize(true)
        snapHelper.attachToRecyclerView(recyclerMnemonic)
    }

    private fun setUpListeners() {
        ivBack.setOnClickListener { _: View? -> finish() }

        spinnerMnemonicSize.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        mnemonicSize = MnemonicSize.MNEMONIC12
                    }
                    1 -> {
                        mnemonicSize = MnemonicSize.MNEMONIC24
                    }
                }
                adapter.setMnemonicSize(mnemonicSize)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }

        snapHelper.setSnapBlockCallback(
            object : SnapToBlock.SnapBlockCallback {
                override fun onBlockSnap(snapPosition: Int) {
                }

                override fun onBlockSnapped(snapPosition: Int) {
                    currentPage.value = snapPosition
                }
            }
        )

        cardViewLeft.setOnClickListener {
            currentPage.value = if((currentPage.value ?: 0) > 0) currentPage.value?.minus(PAGE_SIZE) else 0
            val pos = currentPage.value ?: 0
            recyclerMnemonic.smoothScrollToPosition(pos)
        }

        cardViewRight.setOnClickListener {
            val size = when(mnemonicSize) {
                MnemonicSize.MNEMONIC12 -> 12
                MnemonicSize.MNEMONIC24 -> 24
            }
            currentPage.value = if((currentPage.value ?: 0) < size-PAGE_SIZE) currentPage.value?.plus(PAGE_SIZE) else size-PAGE_SIZE
            val pos = currentPage.value ?: 0
            recyclerMnemonic.smoothScrollToPosition(pos)
        }

        btnConfirm.setOnClickListener {
            val mnemonicWords = adapter.words.joinToString(" ").trim()
            startActivity(PasswordActivity.newIntent(this, znsName ?: "", mnemonicWords))
        }

        currentPage.observe(this) {
            textViewCurrentPosition.text = "${it+1} - ${it+PAGE_SIZE}"
        }
    }

    private fun onKeyboardVisibilityChanged(keyboardVisible: Boolean) {
        val visibility = if (keyboardVisible) View.GONE else View.VISIBLE
        textviewTitle.visibility = visibility
        textviewDesc.visibility = visibility
        cardviewInfo.visibility = visibility
        relativeLayoutSize.visibility = visibility
        btnConfirm.visibility = visibility
    }
}