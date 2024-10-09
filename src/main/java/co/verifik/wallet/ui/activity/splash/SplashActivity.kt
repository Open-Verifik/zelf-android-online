package co.verifik.wallet.ui.activity.splash

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import co.verifik.wallet.R
import com.sensecrypt.sdk.core.SenseCryptSdkException
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.preprocesswallet.MainActivity
import co.verifik.wallet.ui.activity.wallet.main.WalletActivity
import co.verifik.wallet.BuildConfig
import co.verifik.wallet.CryptUtil

class SplashActivity : AppCompatActivity() {

    private lateinit var textviewVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view
        setContentView(R.layout.activity_splash)

        textviewVersion = findViewById(R.id.tvVersion)
        textviewVersion.text = BuildConfig.VERSION_NAME

        val activity = this

        if (savedInstanceState != null) {
            return
        }

        // Record the current time
        val currentTime = System.currentTimeMillis()
        CryptUtil.init(application = application) {
            if (it.isFailure) {
                val exception = it.exceptionOrNull()
                if (exception is SenseCryptSdkException.LicenseExpired) {
                    runOnUiThread {
                        UIHelper.showInfoDialog(
                            activity,
                            R.string.license_expired,
                            R.string.license_expired_detail,
                            false,
                        ) {
                            startActivity(MainActivity.newIntent(this@SplashActivity))
                            finish()
                        }
                    }
                } else if (exception is SenseCryptSdkException.InvalidLicenseFile) {
                    runOnUiThread {
                        UIHelper.showInfoDialog(
                            activity,
                            R.string.invalid_license_file,
                            R.string.invalid_license_file_detail,
                            false,
                        ) {
                            finish()
                        }
                    }
                } else if (exception is SenseCryptSdkException.LicenseNotFound) {
                    runOnUiThread {
                        UIHelper.showInfoDialog(
                            activity,
                            R.string.license_not_found,
                            R.string.license_not_found_detail,
                            false,
                        ) {
                            finish()
                        }
                    }
                } else {
                    runOnUiThread {
                        UIHelper.showInfoDialog(
                            activity,
                            R.string.initialization_error,
                            R.string.initialization_error_detail,
                            false,
                        ) {
                            // startActivity(MainActivity.newIntent(this@SplashActivity))
                            finish()
                        }
                    }
                }
            } else {
                // SDK is initialized
                // Record the elapsed time
                val timeElapsed = System.currentTimeMillis() - currentTime

                // We don't want the splash screen to disappear too quickly
                // so we wait for at least 1 second
                if (timeElapsed < 1000) {
                    Thread.sleep(1000 - timeElapsed)
                }

                val pref = getSharedPreferences("wallet_prefs", MODE_PRIVATE)
                val withWallet = pref.getBoolean("with_wallet", false)

                if (withWallet) {
                    val intent = WalletActivity.newIntent(this@SplashActivity)
                    startActivity(intent)
                    finish()
                }
                else {
                    // Start the main activity
                    startActivity(MainActivity.newIntent(this@SplashActivity))
                    finish()
                }
            }
        }
    }
}
