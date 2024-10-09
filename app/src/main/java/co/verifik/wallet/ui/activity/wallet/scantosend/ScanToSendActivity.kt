package co.verifik.wallet.ui.activity.wallet.scantosend

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import co.verifik.wallet.R
import co.verifik.wallet.CryptUtil
import co.verifik.wallet.ui.UIHelper
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class ScanToSendActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var viewFinder: PreviewView
    private lateinit var closeImageView: ImageView
    private val TAG = "CameraXBasic"
    private var imageCapture: ImageCapture? = null

    companion object {
        fun newIntent(
            context: Context
        ): Intent {
            val intent = Intent(context, ScanToSendActivity::class.java)
            return intent
        }
    }

    private class QrAnalyzer: ImageAnalysis.Analyzer {

        var currentTimestamp: Long = 0

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            currentTimestamp = System.currentTimeMillis()
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                // ...
                scanBarcodes(imageProxy, image)
            }
        }

        private fun scanBarcodes(imageProxy: ImageProxy, image: InputImage) {
            // [START set_detector_options]
            val options = BarcodeScannerOptions.Builder()
//                .enableAllPotentialBarcodes()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC
                )
                .build()
            // [END set_detector_options]

            // [START get_detector]
            val scanner = BarcodeScanning.getClient(options)
            // Or, to specify the formats to recognize:
            // val scanner = BarcodeScanning.getClient(options)
            // [END get_detector]

            // [START run_detector]
            val result = scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Task completed successfully
                    // [START_EXCLUDE]
                    // [START get_barcodes]
                    for (barcode in barcodes) {
                        val bounds = barcode.boundingBox
                        val corners = barcode.cornerPoints

                        val rawValue = barcode.rawValue

                        val valueType = barcode.valueType
                        // See API reference for complete list of supported types
                        when (valueType) {
                            Barcode.TYPE_WIFI -> {
                                val ssid = barcode.wifi!!.ssid
                                val password = barcode.wifi!!.password
                                val type = barcode.wifi!!.encryptionType
                            }
                            Barcode.TYPE_URL -> {
                                val title = barcode.url!!.title
                                val url = barcode.url!!.url
                            }
                        }
                    }
                    // [END get_barcodes]
                    // [END_EXCLUDE]
                    imageProxy.close()
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    // ...
                    imageProxy.close()
                }
            // [END run_detector]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_to_send)

        setComponents()
        setListeners()
    }

    private fun setComponents() {
        viewFinder = findViewById(R.id.viewFinder)
        closeImageView = findViewById(R.id.imageview_back)

        UIHelper.requestCameraPermission(this, viewFinder)
    }

    private fun setListeners() {
        closeImageView.setOnClickListener {
            finish()
        }
    }

    private fun startCamera() {
        if (UIHelper.isCameraPermissionGranted(this)) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener(
                Runnable {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                },
                ContextCompat.getMainExecutor(this),
            )
        }

    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val executor = ContextCompat.getMainExecutor(this)
        val imageAnalysis =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(
                        executor,
                        QrAnalyzer()
                    )
                }

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    override fun onResume() {
        super.onResume()
        CryptUtil.initSdkIfNeeded(this) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if (UIHelper.isCameraPermissionGranted(this)) {
            cameraProviderFuture.get().unbindAll()
        }
    }
}