package co.verifik.wallet.utils

import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

class QRUtil {

    companion object {

        fun createQRCodeFromBytes(byteArray: ByteArray, width: Int, height: Int): Bitmap {
            val qrCodeWriter = QRCodeWriter()

            // Convert raw byte array to ISO-8859-1 encoded string
            val iso88591String = String(byteArray, Charsets.ISO_8859_1)

            // Optional: Set encoding hints (set character set to ISO-8859-1)
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "ISO-8859-1"
            )

            // Generate QR code as BitMatrix
            val bitMatrix: BitMatrix = qrCodeWriter.encode(iso88591String, BarcodeFormat.QR_CODE, width, height, hints)

            // Convert BitMatrix to a Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }

            return bitmap
        }
    }
}