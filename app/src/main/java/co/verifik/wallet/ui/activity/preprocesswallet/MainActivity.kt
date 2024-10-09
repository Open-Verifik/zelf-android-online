package co.verifik.wallet.ui.activity.preprocesswallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.ui.activity.openwallet.ReaderWalletActivity
import co.verifik.wallet.adapters.OnboardingPagerAdapter
import co.verifik.wallet.ui.activity.zns.CreateZNSActivity
import co.verifik.wallet.ui.activity.zns.OpenZNSActivity
import co.verifik.wallet.ui.fragment.onboarding.OnboardingFragment1
import co.verifik.wallet.ui.fragment.onboarding.OnboardingFragment2
import co.verifik.wallet.ui.fragment.onboarding.OnboardingFragment3
import co.verifik.wallet.utils.SliderTransformer
import co.verifik.wallet.utils.hideKeyboard
import com.github.ybq.android.spinkit.SpinKitView
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {
    private lateinit var relativeLayoutParent: RelativeLayout
    private lateinit var linearLayoutScrollview: LinearLayout
    private lateinit var linearLayoutFragmentContainer: LinearLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var imgPrev: ImageView
    private lateinit var imgNext: ImageView
    private lateinit var viewDot1: View
    private lateinit var viewDot2: View
    private lateinit var viewDot3: View
    private lateinit var editTextZNS: EditText
    private lateinit var imageViewSearch: ImageView
    private lateinit var textViewForgetName: TextView
    private lateinit var searchButton: AppCompatButton
    private lateinit var viewMask: View
    private lateinit var spinKit: SpinKitView

    companion object {
        private const val ONBOARDING_PAGES: Int = 3
        // Method to create a new Intent for MainActivity
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view
        setContentView(R.layout.activity_main)

        // Assign values to the components in this activity
        setupComponents()

        // Assign listeners to the components in this activity
        setUpListeners()
    }

    private fun setupComponents() {
        // Assign values to the components in this activity
        relativeLayoutParent = findViewById(R.id.relativelayout_parent)
        linearLayoutScrollview = findViewById(R.id.linearlayout_scrollcontent)
        linearLayoutFragmentContainer = findViewById(R.id.linearlayout_fragment_container)
        viewPager = findViewById(R.id.viewpager)
        imgPrev = findViewById(R.id.image_left)
        imgNext = findViewById(R.id.image_right)
        viewDot1 = findViewById(R.id.view_dot1)
        viewDot2 = findViewById(R.id.view_dot2)
        viewDot3 = findViewById(R.id.view_dot3)
        editTextZNS = findViewById(R.id.edittext_zns)
        imageViewSearch = findViewById(R.id.imageview_search)
        textViewForgetName = findViewById(R.id.textview_forgetname)
        searchButton = findViewById(R.id.search_button)
        viewMask = findViewById(R.id.view_mask)
        spinKit = findViewById(R.id.spin_kit)

        val adapter = OnboardingPagerAdapter(supportFragmentManager, lifecycle)
        adapter.addFragment(OnboardingFragment1())
        adapter.addFragment(OnboardingFragment2())
        adapter.addFragment(OnboardingFragment3())
        viewPager.setPageTransformer(SliderTransformer())
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.adapter = adapter
    }

    private fun setUpListeners() {

        relativeLayoutParent.setOnClickListener {
            editTextZNS.clearFocus()
            editTextZNS.hideKeyboard()
        }

        linearLayoutScrollview.setOnClickListener {
            editTextZNS.clearFocus()
            editTextZNS.hideKeyboard()
        }

        imgPrev.setOnClickListener {
            if(viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        imgNext.setOnClickListener {
            if(viewPager.currentItem < ONBOARDING_PAGES) {
                viewPager.currentItem += 1
            }
        }

        viewDot1.setOnClickListener {
            viewPager.currentItem = 0
        }

        viewDot2.setOnClickListener {
            viewPager.currentItem = 1
        }

        viewDot3.setOnClickListener {
            viewPager.currentItem = 2
        }

        viewPager.registerOnPageChangeCallback(object: OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if(state == ViewPager2.SCROLL_STATE_IDLE) {
                    when (viewPager.currentItem) {
                        ONBOARDING_PAGES - 1 -> {
                            imgPrev.alpha = 1.0f
                            imgNext.alpha = 0.5f
                            viewDot1.setBackgroundResource(R.drawable.dot_indicator)
                            viewDot2.setBackgroundResource(R.drawable.dot_indicator)
                            viewDot3.setBackgroundResource(R.drawable.dot_indicator_selected)
                        }
                        1 -> {
                            imgPrev.alpha = 1.0f
                            imgNext.alpha = 1.0f
                            viewDot1.setBackgroundResource(R.drawable.dot_indicator)
                            viewDot2.setBackgroundResource(R.drawable.dot_indicator_selected)
                            viewDot3.setBackgroundResource(R.drawable.dot_indicator)
                        }
                        0 -> {
                            imgPrev.alpha = 0.5f
                            imgNext.alpha = 1.0f
                            viewDot1.setBackgroundResource(R.drawable.dot_indicator_selected)
                            viewDot2.setBackgroundResource(R.drawable.dot_indicator)
                            viewDot3.setBackgroundResource(R.drawable.dot_indicator)
                        }
                    }
                }
            }
        })

        editTextZNS.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                linearLayoutFragmentContainer.visibility = View.GONE
            } else {
                linearLayoutFragmentContainer.visibility = View.VISIBLE
            }
        }

        editTextZNS.setOnKeyListener { view, i, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN && i == KEYCODE_ENTER) {
                editTextZNS.clearFocus()
                view?.hideKeyboard()
                searchIPFS()
                return@setOnKeyListener true
            }
            false
        }

        imageViewSearch.setOnClickListener {
            editTextZNS.clearFocus()
            editTextZNS.hideKeyboard()
            searchIPFS()
        }

        searchButton.setOnClickListener {
            searchIPFS()
        }

        textViewForgetName.setOnClickListener {
            val intent = ReaderWalletActivity.newIntent(this)
            startActivity(intent)
        }

        //TEST ONLY PURPOSE
        val mnemonic = "palace boy shock merit job off critic sudden lend interest thrive damage vessel half junk actress virtual appear empty caught kit about ring green"
    }

    private fun searchIPFS() {

        val zns = editTextZNS.text.toString().trim()

        if (zns.isEmpty()) {
            return
        }

        lifecycleScope.launch {
            searchButton.isEnabled = false
            spinKit.visibility = View.VISIBLE
            viewMask.visibility = View.VISIBLE

            val ipfsResponse = try {
                CryptUtil.findIPFS(zns)
            } catch (e: HttpException) {
                null
            }
            searchButton.isEnabled = true
            spinKit.visibility = View.GONE
            viewMask.visibility = View.GONE

            val intent = if(ipfsResponse == null) {
                CreateZNSActivity.newIntent(this@MainActivity, zns)
            } else {
                val znsUrl = ipfsResponse.url ?: ""
                val ethAddress = ipfsResponse.metadata?.keyvalues?.get("ethAddress") ?: ""
                val solanaAddress = ipfsResponse.metadata?.keyvalues?.get("solanaAddress") ?: ""
                OpenZNSActivity.newIntent(
                    this@MainActivity,
                    zns,
                    znsUrl,
                    ethAddress,
                    solanaAddress
                )
            }
            startActivity(intent)
        }
    }
}
