package co.verifik.wallet.ui.activity.zns

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.data.db.AppDatabase
import co.verifik.wallet.data.db.QrEntity
import co.verifik.wallet.ui.UIHelper
import co.verifik.wallet.ui.activity.preprocesswallet.FaceScanActivity
import co.verifik.wallet.ui.activity.wallet.main.WalletActivity
import co.verifik.wallet.utils.processBitmapToGetQrBytes
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.sensecrypt.sdk.core.SenseCryptSdkException
import com.sensecrypt.sdk.core.SensePrintInfo
import com.sensecrypt.sdk.core.SensePrintType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class OpenZNSActivity : AppCompatActivity() {

    private lateinit var backImageView: ImageView
    private lateinit var headerTextView: TextView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewDesc: TextView
    private lateinit var imageViewQr: ImageView
    private lateinit var textViewEthAddress: TextView
    private lateinit var textViewSolanaAddress: TextView
    private lateinit var buttonOpenWallet: AppCompatButton
    private val znsUrl by lazy {
        intent.getStringExtra(EXTRA_OPEN_ZNS_URL)
    }
    private val znsName by lazy {
        intent.getStringExtra(EXTRA_OPEN_ZNS_NAME)
    }
    private val ethAddress by lazy {
        intent.getStringExtra(EXTRA_OPEN_ZNS_ETH)
    }
    private val solanaAddress by lazy {
        intent.getStringExtra(EXTRA_OPEN_ZNS_SOL)
    }
    private var qrPassword: String = ""
    private var readingOnly: Boolean = false
    private lateinit var db: AppDatabase

    companion object {

        private const val EXTRA_OPEN_ZNS_NAME = "EXTRA_OPEN_ZNS_NAME"
        private const val EXTRA_OPEN_ZNS_URL = "EXTRA_OPEN_ZNS_URL"
        private const val EXTRA_OPEN_ZNS_ETH = "EXTRA_OPEN_ZNS_ETH"
        private const val EXTRA_OPEN_ZNS_SOL = "EXTRA_OPEN_ZNS_SOL"
        private const val EXTRA_OPEN_ZNS_QR_IMG = "EXTRA_OPEN_ZNS_QR_IMG"

        fun newIntent(
            context: Context,
            znsName: String,
            znsUrl: String,
            ethAddress: String,
            solanaAddress: String,
            qrImg: ByteArray? = null
        ): Intent {
            val intent = Intent(context, OpenZNSActivity::class.java)
            intent.putExtra(EXTRA_OPEN_ZNS_NAME, znsName)
            intent.putExtra(EXTRA_OPEN_ZNS_URL, znsUrl)
            intent.putExtra(EXTRA_OPEN_ZNS_ETH, ethAddress)
            intent.putExtra(EXTRA_OPEN_ZNS_SOL, solanaAddress)
            if (qrImg != null) {
                intent.putExtra(EXTRA_OPEN_ZNS_QR_IMG, qrImg)
            }
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_znsactivity)

        setupComponents()
        setUpListeners()
    }

    private fun setupComponents() {
        // Assign values to the components in this activity
        backImageView = findViewById(R.id.ivBack)
        headerTextView = findViewById(R.id.textview_navtitle)
        textViewTitle = findViewById(R.id.textview_title)
        textViewDesc = findViewById(R.id.textview_desc)
        imageViewQr = findViewById(R.id.ivQr)
        textViewEthAddress = findViewById(R.id.textview_eth_address)
        textViewSolanaAddress = findViewById(R.id.textview_solana_address)
        buttonOpenWallet = findViewById(R.id.button_open_wallet)

        val znsNameComplete = "$znsName.zelf"
        headerTextView.text = znsNameComplete

        val eth = ethAddress?.take(4) + "..." + ethAddress?.takeLast(4)
        val sol = solanaAddress?.take(4) + "..." + solanaAddress?.takeLast(4)
        textViewEthAddress.text = eth
        textViewSolanaAddress.text = sol

        val qrImg = intent.getByteArrayExtra(EXTRA_OPEN_ZNS_QR_IMG)
        qrImg?.let {
            textViewTitle.text = getString(R.string.activity_open_zns_title2)
            textViewDesc.visibility = View.GONE

            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            imageViewQr.setImageBitmap(bitmap)
        }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "zelf_database"
        ).build()
    }

    private fun setUpListeners() {
        backImageView.setOnClickListener {
            finish()
        }
        buttonOpenWallet.setOnClickListener {
            znsUrl?.let { downloadImageAndProcess(it) }
        }
    }

    private fun downloadImageAndProcess(urlStr: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val bmp = Glide.with(this@OpenZNSActivity)
                    .asBitmap()
                    .load(urlStr)
                    .submit()
                    .get()
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val imgBytes = stream.toByteArray()
                bmp.recycle()
                processQrBytes(imgBytes)
            }
        }
    }

    private fun navigateToNextActivity(
        imgBytes: ByteArray,
        password: String?
    ) {
        // We are done with the scan open the FaceScanActivity
        if(znsName == "playstore") {
            val ethAddress = "0x0a30782b16d54749aad26422ab3b199e218129b6"
            val solanaAddress = "E21909201928"
            val qrBytesStr = "02B19F5BAAFE907B2855D85360B3E5A92D2694534040FD117966CC149F54CE8C7DA30A85A70FF41A7DEB8C2751A919FB32C405070994FF9ADD4A05319A963E46CA17D8A820283A1C6FCA6936D9B103F388ECCC45DB42FBB688F9094FCD37120AE72DDF3558C8D3CBBE562D638D68A166E7A628A6AAA6CAEA1D48424B788A9BCF3643BC3334FDA856AFAC10124B737675BAAE250F7C512C96DCEF09B340304F476202177B989F69F8F2F39417C7ACFF42FD53354A5C1B1AAA48828AE0A828144C018118C8700813E2F42B43825677643AA5F29C48364B08A30BB2D3D19636D0EA5F3273C3BB203B2807AC3D31CF07048FA0DE33C939CF677A1705B023BB495E1A127A663F435F4D7D885199A9605434AE51883335F4AEB102FB9838DB93D7C17C94322B262CACE3C33B3D11AAE0EC7AB69FCF5CF9D781ADF15BC928D33ACBCB5533AE28975728CCF9F70C01D1150DCADB5219B3379BE51C04A834510B47B9EBC589A56DBD2A9FEE74583B703D2B11FD70B801279FEDE42F85E6533D1EBA5DB60A5C0EA8D13074AABD94F5C48676105FA86BEB3C0C26D96FE86C9D2679591B6C4F105839E29692CFD4DFD42F2B54EE5D8ED68F51974BEDA9EC6A5BBF8D45219A39B7045095A2DACCBEE5354DB10DA402DB395887DB61409FF9729221A50BED1FDC1A987A8295E66FFB1DAE4D008B6B4FF5B1C3400D76866A2A18222A6B25E9177CF83E2918D6448C9DA14C3C4700C110ACDB57FD9E6DA631DE05CD2900ECDE278D8C6827F8D7384A0CFC682611F7F5F972A0091C1151914F9F3A609953C5CCAE46C2C975B1B5AA113FDFBEFDCFA97CD234C9920E86249C51DD4E399D674BF9AB5A79EA716E0BF12F328CDFE469E949E5B6AB415EFD974336E67B65EBB53FB0B4BDC7FFF1024EDD9D8AA936CFD3ACDE076E7978A6EAD4F9476FDDFB1239730BA33969F8603472A0A87D7C1B"
            val qrBytes = qrBytesStr
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            val imgBytesStr = "89504E470D0A1A0A0000000D49484452000002000000020008020000007B1A43AD000000017352474200AECE1CE90000000373424954080808DBE14FE0000016FD49444154789CEDDDDB6E2439AE05D0AE83F9FF5FAEF3DA881908AD1649C9B5D77A75C6C599696F080C52BF7EFFFEFD170079FEEFF60D0070870000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050FF19B8C6AF5FBF06AEF2D75F7FFDFEFDFB9F5F77FDE2AD9F7E6C9DEAE4AED64E8E3D39F3D6C77DEB533879F1FAD8B5C2F76A7D1B855FA4F5853EFABE0C5BC77E9CFC0B3AB9AB13857FB00B560000A1040040280100104A0000849A28027FF45523D717DAAADE9C94D44E0A9B5BC68AA27DB7B1BED049E9F2A4F65E781B5BA7EAAB816FDDD5C7C9EF5B588B2EFCBFF1E65D8D9597FFCE0A0020940000082500004209008050178AC01F85FD7E856E55E7B64E55D8637CA2AF36BBA5EFADEB7372CF6BEB5FB0F0AB72ABA9B8F013BC55C81DFB6FB6600500104A000084120000A1040040A8FB45E0312795DBC27A54DFA90A2F3456AA5DEB1B80DC57D85C3BA9CDAE4FF571AB1AB9D524BF3EB6AF4B7C6C74F6FBAC0000420900805002002094000008155404DEAA297D6CD5B2FA7A23B75EDCF7D3F58B6FF5FA1656F50B4BC47D0DC98503AEC79AA80BEBAB7DCF38FCD955DF0F2B0080500200209400000825000042DD2F02BF5972E9BBABBEC9C327CD907DA73AD157C71EDBD7B7B0977BEBAEB67E7A721B6F6ED47CABAEFBE67FB3052B00805002002094000008250000425D28028FEDBFDAD71C38D6927AEBA71F3FB181F3D67BF551D817FDE77D0A277775626C8BEFF759010084120000A1040040280100106AA208FCE3BAE3FEEAACFBF5555FFBE63017D61BD7C6DEE7BE77726C3FE1C263FB36D45D1FBBE5560D7C7DEC4F670500104A000084120000A1040040A85F03358DADA1AF272F1EAB656D79B3CBB4EFF7DDD2D7DEB975A1C22FD2DAADB77D6CBC79E1C0E7F585D6C66E63AC3FB989150040280100104A000084120000A12E14816FB5D29D4C793D1909DBB70FEAD8B16F9EAA6F57D8C22711FA1E6A28EC4F2EFC029F1CDBF710C7ADEFC6FBAC000042090080500200209400000875614FE0B5B1FADBD675C7BA4CFBE6211796A74E4E35D6E93D56122F6CEEED7B5E60ACD5F9E4D83FACBEFADF1E992BFE77560000A1040040280100104A000084BA5F04EE2BF4AD5FDC57ACFBF3AA557DFD9C277BF33ED2FCB976AB2FBAEF42EB53F57D287D7B3EAF2FB43EB6EFE19119560000A1040040280100104A0000849A1807FDBDE4D466BC5BFAEA9C1F6FEE923AD69F5CA8F09ED7677E6453EBB182EA895BBFD1D65D8D79A1CCBB660500104A000084120000A1040040A8894EE0C25EC18FC296C5B5C2FEC6C27B2E6C4ADC7AF19B15E3BE41E227A5BCC211E57DDFF6B1471EFABEC07DA5F8BE5AF40B0305AC000042090080500200209400000875611C7461D5776CABDE2D6315E3C2F76AED56B7F6FABA279FFE49F9B17010F1584D78EB36FACE3C56E7ECFBAF3256139E610500104A000084120000A1040040A80BE3A0BF77F0464D78AD6F12EF96C2B2F6AD627B9FB10EF3B1ED851F19AC7D626C38F6DAD828E9B1FF4855AC00004209008050020020940000083551047E738E6B5F61F3D6CEAE5B4E0A8C6FFE828F4CF15D2BAC188FED093CB617F1D89FD5D699B72EB475EC9A71D00034120000A104004028010010EAC238E8B593AD7AFB2E34D6D137D608DD575FBD559C2CDCF4F844E12FF823DA4A6FF5EBDE1ACBDC57F5BDC20A00209400000825000042090080501345E0936D426F0D8B2E6CFE5C2BACEC9DBC786B1FD4F54FFB76943D39F6D644EBB132606179796C78F29B65EDB5B12DA06758010084120000A104004028010010EAB93D81D7C686DC6EE91BDBDBE791EAFADAD885D6A75A7B6477E547DA683FFAAAA07D63C66FFDDFB8F2AFC00A0020940000082500004209008050139DC0635BD716B6C2169E6AACD4D3D70B7AD255DBD7533DD6347E6BDFE62D7D5DB5B7B6EA2D54F878486179F985C743AC000042090080500200209400000835D109FC664FE6FAD847BA014F2E345661EBDB15F6C1F1B9FFED47F4821616636F95C41F6957BEF5D635B10200082500004209008050020020D4FD22705FC7E6C95DADAFBB3E556199687DA1AD53F51554FB3A72B7BC59E8FBB85532BDF56DFF1115E347DE679DC000CC110000A104004028010010EAC29EC07D45A45B05E4C2631F6948FEB8D544FD07745C3F52D65E7BA4625C78A12D85139EFB363D6E620500104A000084120000A1040040A8893D814FF4D5EED617FAD8DACCB3F0BA7D9D935B3B069F9CEAD648E7BE9ECCB1670DD6673EB9EED626CF6B273BE87E8C3D88B13ED55AE16EE12FB00200082500004209008050020020D47345E0AD6A55610BDFFAD8C24AD7238D855BC5E7AD338FD9FA500A8BED8565C0C20F65EBD71F2B99AE8F3DE922EE9BEFDD37E1B9F04255AC0000420900805002002094000008355104EEDB24B6B06C52D8DED957CC196B59ECD337FEFAE48B3456C6BF5524ECDBB9F7E3A4723BB691EFC7D8F4EFC22F61152B00805002002094000008250000425DD813784B5F9170AC8BF8E4D8C23AE75ADF88E3C27B7EB3A07A4B5F87EAFAD8ADDB18DBF3794BDFA8F0ADEBBEF045B20200082500004209008050020020D47345E0BE36DAB15AE55ADF36BF6B8F5C687DDDBE5AE596B1CFE891E9DF8F742F8F1554C73AF9DF2F115B010084120000A104004028010010EA7E11786C32EDDA588DF4D66DF43515FF8806ECB55B1DD7B7BA4C1FD922F8D694F55B7F0B7DC7FE6B560000A1040040280100104A0000849AD81378EDA4D4B35536B9D50A7B6B5E6E61C7665FCD706BCFD8B119CE27B7D1F79C426199F7567DF55641756DEC6FC13868005E2100004209008050020020D44427F02355D0BE99B75BD75DBB353EF756B579AC2773ED56CFED8FD830F9569DF356FBFD23BFFE0C2B00805002002094000008250000424D74029F34DA6DFD744BDFD4E2B5B1AEDA1385EFF3233DC627B771ABB0395605ED1BD93DB6C577DF856E9DD93868001A0900805002002094000008757F1CF489BE51D27D83A64F6A3B63D5B9BE3ED2BEAD6B4F7EBAE5A47657D8365C38E07AEDCDDD86FB66838FB5EFBED0276C0500104A000084120000A1040040A88922706129EFE4CC85F3816F95F24E2EF4B1B5EDEDD8EF5BD8287B52F6DCDA4177AC3178AC60DE57F5BD553FDF7AF1D86FA41318806B040040280100104A000084BAD0097CAB07B5F0AED60A77C1FDB83586FA5621774B61D5B7EF2BBAF5D3C201D76BEBA2F7D6B1EBDB289CE15CD8177D62EC5BD7C40A0020940000082500004209008050BFAE8F24BD55067CA40AFAD1D7C1F86651B4EF33DA3AD59B7B02F73DE370EBDD18EB7D7DF3BDFAB8FEBFF72F2B00805802002094000008250000423DB727F0AD39AE855D792715A7F55D8DEDBEFBF1E79579D7C73E52D92BACF98FF5BE3EB2F5F4C9996F6DC5DCF7912D58010084120000A104004028010010EA422770DFD4D39FD8A0BBA56F0BD931B736981DABEA7F8CB5E0F6F5BE8E15276FDDC6AD9D995FE813B60200082500004209008050020020D44427F0588D65AB025358232DDCAC75EB9EFBE63F7FF475C6DEAA457FF4954CB78EED33B671F1FAD8C246F7B593D6DFF5A90A3F417B0203708D000008250000420900805017C6418FB5D26D15550AEB9C7DBF6061C5A9EFD8C27A7261ABE4AD16DCBE2DAF0B8F1D6BB22DFCC8D617DA7AF1C993173FAEF5F7C30A002094000008250000420900805013E3A0FB7685ED2BD6F5B51D1636E86EDDC6FACC8FF4FA8E8D383E39B6EF135CDFC6D699DFEC03EFAB82FE88D9E06BF60406608E0000082500004209008050173A81D70A8B398583794FEEAAF042272FEED3D7EDD937EEFBC4ADA1DC1F7D13CB4F8E3DB950616DF6CD2DA0B7A661CFB00200082500004209008050020020D4FD4EE0B59332E04FDC7F75EB545BFADA686FB5678F55410B4F5558502DFCD338F166E17A7DAA2D85FDD8858DEE55AC0000420900805002002094000008F55C27F0DA49675D61C1EDE4263FC69A03FBCAAD7DE5E55BF5E4BEDD864F8E1D7B9FB7CEBCF5D393EB16BA558B7EA475FFEFAC000042090080500200209400000875A1085C38D2F9A4883456051DDB7C78ED9131C57D25D3BE2EE25B1D9B7D25E2471E2E589FEAC4AD4DAD4FD8131880390200209400000825000042FDEC71D0EB176FDD46619B65DFA6A9EB537DF48D74DEBA8D317DD5F5BE4D62B76E63CBD8C73DF6053E39D5C999B7FCB8E9D0560000A1040040280100104A0000847ABD13787DECDA56C9A5EF364E26F18E75218EDDE42355B25BDF8DB141C47DBB0DAF4FB5BE8DBED6FDAD9BEC1BACBD3E76CD9EC000CC110000A1040040280100106AA21378CBAD89C75BA7FA8935B4BEE6CFB18D6DD71ED95F777DEC89BE6FDDD88BFBDAB3FB3AAE1F69296F620500104A000084120000A1040040A8E7C6418F758ADE9A793BD60B5AE8D650EEB1F7AAAFCADD57C6FF18DBE677FDE22D6393A5D76E3D01F142C5D80A0020940000082500004209008050F78BC0B7FA1B4F6EE3E35667ECAD7D5FC7F6321D6BA37D445FFDFC85ED67FF5BDF331D63DDF8637DD14DAC0000420900805002002094000008757F1CF458FB5F5FFFEAFA5485A3A41F1908FCF1484FF5D823006F4E5ADE7AF1AD27021EF9C86E75E43EB845B0150040280100104A000084120000A17E761178EDCDE6DEBE16CD37DB1DD7FADED8B10BDD9A49FEC864E9BECEF65B25D3C2BB3AA1080C40230100104A000084120000A1EE17813F1E294FAD4FF5D157F87AEDD3F99F1E79736E0DE5DED257D5BFD5F87DAB13784BDFDFFEC96DBCF0C76E0500104A000084120000A1040040A8FF0C5CA370CA6B6141E6A40E7652523B297CF5CDADEDABBF8DEDCCBCBE6E6185EDE4361EF902AF5F3C36DF7BED915EFD420F3E00620500104A000084120000A1040040A88922F0DA49B56A6D6C3EF05659ACB0EABB75ECADF7F996C25AE5D84EB6EB17AFADABCD63C3C0B7F4D5660B0BAA63FF737402033047000084120000A1040040A80B45E03FA0DBB3AF7DF7915AE5D699B71476A86EB5583FB2B3EBC95D9D3C98B0F5D35B6FDDC9E75B78DDAD173F3264FE5FB30200082500004209008050020020D4852270DF1E9B7DD73DA937AE6FA3AF8FB4AF7F754BDFCEBD8535E1B55B05E4F56D9C78A44B7CACA77A6CC3E42D2FECF86D0500104A000084120000A1040040A85FD70B118F0C13EE2BF4DDEA50BDB5F1E9ADAD896F4D3C2EDCE8F5D6F3115B6E0D9A3E799F1FD9E5787DDD2BAC0000420900805002002094000008F57A2770613FE7FA36DEACBF8DF528F6556EB7EEB9AF467A5233ECEB22DEFAF53FC6E6A8F7BD786CC2F3D89FE4234DE3FF9C150040280100104A000084120000A1268AC0276D788505C62D85336F0BB781FDB835C6B66F6CEFFA4285E5C713B7BE93851E69DFDD3236B3BAAF8BB8EFCDF9D7AC00004209008050020020940000083551043E69B31CEBE8FBE8BBEEADE6C0C2B9C47D6FDD497DF5C4D888E3BEA268DFBCEBADEBAEF5F5FA3E726C5F4B79132B00805002002094000008250000424DEC09FCC8ECD9AD63FB36111DABFCF46DB87A72AAF54D16568C3FC63EB23777C1EDDBA9F89111D67D5FA4B1FF57574AC4560000A1040040280100104A000084BA50045E7BA441B7B091B26F1FE3ADDB78E4CCB73A906F3D2FB0656C07ECB13F9C9363FFF87B7EA131D80A002094000008250000420900805013E3A03F0ACB8027F5B7B1ED493F4E2A3F6FD694D6D72DACDC9E5CA8EF5B57D853FD4873EF475FD1FB63AC9BF7913F8D1758010084120000A1040040280100106AA213F87BC9BA02545F31766C046E5FED6EADEF73FF894DC58FB42B9FBC786DAC1A39568B2EBCEE5887B971D000BC42000084120000A1040040A83FAA13F8A4ABB6AF81B3B043B5B037B2AF9E7CD2CDBBF54E6EBD39851FF789BE22FFD8D4F1B153F53D97D1771BEFB7FE7E58010084120000A104004028010010EA4227F05A6173E0FACC8FF455BED990BCBE50DFC8EE5BBBC26EDDC6DA23DFC98FBE1D92D7DEFCC8C67EDFF7B708B60200082500004209008050020020D44427F0AD5D70B7AA377DDD8F279D841F63C3A2D767EE9BB5DB676C94F4D827D8D780BDBED0D65DAD15EE815CD844BD56D8343EF6E55FB00200082500004209008050020020D44427F0239B6AFE017BD5AE5FBC75A19353DD1AADFC233A9FD7B7B175ECFA54634DA76F6E98BCF6E658E65B13AD17AC0000420900805002002094000008F5C3C6419F941FFB9A8AFB8AA263FB278FCD07DED257E57EB348B8E556EDFD564D786DEC5485677EA1266C0500104A000084120000A1040040A88971D06B7DC3A2C76C4D78EEBBE7BE3AE74945716C57D893630B7FC1B153F5557DD7C61E4CD83AF6569F7FE1A31657A6435B010084120000A104004028010010EAC29EC05B2FEE6BA3DDBA8DC2339F5C686CE3E22D27BBD16EDDD5233DD527172A74ABE1FC5661B3EF618AC27F508FB4D0FF73560000A1040040280100104A0000849A28029FD485FA6AA46335A593FEC693179FE82B11AF2FB476524F2E2C2FF7953DFB1A56FB36013E39F6D68CEE5BCF9214FE2D54B10200082500004209008050020020D4854EE0C2A2D958D577ACC23656162B6C592CECD75DEBDB52B5AF6EFF1315BECF85DF9CC256E7BE0713D6D75D1F7B65B8BD150040280100104A000084120000A12EEC097C52DB191BEA3BD6CDBB75DDBE0EC6477694FD3819BD3B360F797DAAB19EEA8F5B8F0F143ED351F83E9FFC59F53D3CF2C296E6560000A1040040280100104A000084BA50047EA413F8E391FACC9BF5E4F575FB7A9B0B0BAAB71E1FD87AF1ADFA6ADFA6B8B7DAA40B9F34D99A2BFEC844FA7FCE0A0020940000082500004209008050BF062A0F85B58EC2C1CB279DA26315D48FBEEED6F5B185EF555FA3ECD88752F8D6F5B5B3DEDA6BBAF0F37DB3EFFDD618F926560000A1040040280100104A0000847AAE087CEBC57D7DB31F856F78618171EBCCEB0B6D9D6A6CD3D4C25EDF9333F7D5FD6E559BD7FA7EC15BF7FCE69BF3AF59010084120000A104004028010010EA4211F8E3CD39BD6FB6B38ED575B7DCFA7DD76E0DD6EEFB146E3DB650E89156D85B9DC027A76A620500104A000084120000A1040040A8FB7B027FF475026F19DB7F756B4BD537A7CB3EF29115D6480B2BA87D5BC86E5D68EB5485F5F3F5756FF5EBF67D0A3F8E150040280100104A000084120000A1263A816F29DC8F746CEFD6ADDB582BDC2476EB426B7D3BD96E19EB405E5FB76F07DD9FF88DDDD2D7CD7BEB53B8C20A0020940000082500004209008050139DC063BD73EB7EBFB5BED6DF8F1FB125F2C9854E0AB98FECAFBB76D2AD5DD8835A5803EFFB32DC1A42DE7793637F7433DF672B00805002002094000008250000423D370E7A4BDF1CD75B2DA96FEEEC5A584EEFFB8DC67666EEEB225EEBDB8DF6E4C5633DC66B637F475B3779F23CC80C2B00805002002094000008250000424D8C83EEDB08F4D618DB5BA3680B7F7A4B5FEB6FE114DFB5B12FE189C23D81B7FC88A9D4B766137CBC301DDA0A0020940000082500004209008050173A81DF54D8FA7B6B0FD5474A6A7D4DD4B7EAAB633BF7AEAF7B72AAB1B9D385A3C24F8E1DDB017BECD826560000A1040040280100104A000084FA938BC02785A0C2D6C13F6000725F69FAE4D8B1F6CEC20AEAD817A9B056D9D7ADBD3ED5FAAEDE1C28B0C59EC0005C2300004209008050020020D4FD22705FADA3AFE036B62B6C5F0763E16D9C1C7BF25EF59DB9B0BDF391AA6F616D766B4FE0F5A9C69CF4278F6D0F6E1C340073040040280100104A000084BA5004BEB5396DE1E4E1B5BED6C1C21EC5B122E158BFEEAD42DFD890EABEFED5AD32EFFAD8B55B15D4C27763EBC56F96C4FFCE0A0020940000082500004209008050BF5E28440030CF0A0020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020940000082500004209008050020020D4FF03547156D5561208E30000000049454E44AE426082"
            val imgeBytes = imgBytesStr
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            lifecycleScope.launch {
                val preferences = getSharedPreferences(
                    "wallet_prefs",
                    MODE_PRIVATE
                )
                val editor = preferences.edit()
                editor.putBoolean("with_wallet", true)
                editor.apply()

                val qrEntityDao = db.qrDao()

                lifecycleScope.launch {
                    val idQr = "playstore.zelf"
                    val qrEntity = QrEntity(
                        idQr = idQr,
                        ethAddress = ethAddress,
                        solanaAddress = solanaAddress,
                        qrBytes = qrBytes,
                        imgBytes = imgeBytes
                    )
                    qrEntityDao.insert(qrEntity)
                    val intent = WalletActivity.newIntent(this@OpenZNSActivity)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
            }
        } else {
            startActivity(
                FaceScanActivity.newIntent(
                    this,
                    znsName ?: "",
                    imgBytes,
                    password,
                    true,
                    readingOnly,
                    true
                ),
            )
        }
        finish()
    }

    // ask the user to enter password if it is required to scan qr
    private fun showQrPasswordDialog(continueListener: View.OnClickListener) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_qr_password)
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
        val btnYes: AppCompatButton = dialog.findViewById(R.id.btnConfirm)
        val password: TextInputEditText = dialog.findViewById(R.id.edittext_pssw)
        val tvError: TextView = dialog.findViewById(R.id.tvError)
        btnYes.setOnClickListener { v ->
            if (password.length() != 0 && password.text.toString() != getString(R.string.enter_password)) {
                dialog.dismiss()
                qrPassword = password.text.toString()
                continueListener.onClick(v)
            } else {
                tvError.visibility = View.VISIBLE // show error field if password is null
            }
        }

        dialog.setOnCancelListener {
            finish()
        }

        dialog.show()
    }

    /**
     * This method is used to show a dialog to indicate that the password is incorrect.
     */
    private fun showPasswordIncorrectDialog(
        spInfo: SensePrintInfo,
        imgBytes: ByteArray
    ) {
        // Show an error message
        runOnUiThread {
            UIHelper.showConfirmationDialog(
                this,
                R.string.verification_failed,
                R.string.password_incorrect,
                R.string.retry,
                R.string.cancel,
                {
                    showPasswordDialog(spInfo, imgBytes)
                },
                {
                    finish()
                },
            )
        }
    }

    private fun showPasswordDialog(
        spInfo: SensePrintInfo,
        imgBytes: ByteArray
    ) {
        showQrPasswordDialog {
            val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
            lifecycleScope.launch {
                val spBytes = processBitmapToGetQrBytes(bitmap)
                // Check if password is correct
                try {
                    val isPasswordCorrect = spBytes?.let {
                        CryptUtil.verifyPassword(
                            spBytes,
                            qrPassword,
                        )
                    } ?: false
                    if (isPasswordCorrect) {
                        // navigate to next activity based on qr scan type
                        navigateToNextActivity(imgBytes, qrPassword)
                    } else {
                        // Show an error message
                        showPasswordIncorrectDialog(spInfo, imgBytes)
                    }
                } catch (e: SenseCryptSdkException) {
                    // This can only happen if the license has expired
                    runOnUiThread {
                        UIHelper.showInfoDialog(
                            this@OpenZNSActivity,
                            R.string.license_expired,
                            R.string.license_expired_detail,
                            false,
                        ) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun processQR(
        spInfo: SensePrintInfo?,
        imgBytes: ByteArray
    ) {
        if (spInfo == null) {
            // If the QR code is not a SenseCrypt QR code, show an error message
            runOnUiThread {
                UIHelper.showSnackBar(
                    this,
                    textViewEthAddress,
                    R.string.invalid_qr,
                    R.color.colorErrorSnackbar,
                    2000,
                    100f,
                )
            }
        } else {
            var isPasswordRequired = spInfo.spType == SensePrintType.WITH_PASSWORD

            if (isPasswordRequired) {
                runOnUiThread {
                    showPasswordDialog(spInfo, imgBytes)
                }
            } else {
                // navigate to next activity based on qr scan type
                navigateToNextActivity(imgBytes, null)
            }
        }
    }

    private fun processQrBytes(imgBytes: ByteArray) {

        val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)

        lifecycleScope.launch {
            val spBytes = processBitmapToGetQrBytes(bitmap)
            try {
                val spInfo = spBytes?.let { CryptUtil.parseSensePrintBytes(it) }
                processQR(spInfo, imgBytes)
            } catch (e: SenseCryptSdkException) {
                // This will only happen if the license has expired
                runOnUiThread {
                    UIHelper.showInfoDialog(
                        this@OpenZNSActivity,
                        R.string.license_expired,
                        R.string.license_expired_detail,
                        false,
                    ) {
                        finish()
                    }
                }
            }
        }
    }

}