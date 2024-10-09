package co.verifik.wallet.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Utility class for image operations
 */
class ImageUtil {
    companion object {
        /**
         * Convert a ByteBuffer to a ByteArray
         *
         * @return The byte array
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind() // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data) // Copy the buffer into a byte array
            return data.clone() // Return the byte array
        }

        /**
         * Get a square center crop bitmap from an ImageProxy
         * @param image The ImageProxy
         * @param isFrontCamera Whether the camera is facing front (will mirror the image)
         *
         * @return The center crop bitmap
         */
        fun getCenterCropBitmap(
            image: ImageProxy,
            isFrontCamera: Boolean,
        ): Bitmap {
            val imageBitmap = imageProxyToBitmap(image, isFrontCamera)
            val width = imageBitmap.width
            val height = imageBitmap.height
            val size = Math.min(width, height)
            val x = (width - size) / 2
            val y = ((height - size) / 3)
            return Bitmap.createBitmap(imageBitmap, x, y, size, size)
        }

        /**
         * Convert an ImageProxy to a Bitmap
         *
         * @param image The ImageProxy
         * @param isFrontCamera Whether the camera is facing front (will mirror the image)
         *
         * @return The bitmap
         */
        private fun imageProxyToBitmap(
            image: ImageProxy,
            isFrontCamera: Boolean,
        ): Bitmap {
            val height = image.height
            val width = image.width

            // Get the camera rotation
            val rotation = image.imageInfo.rotationDegrees

            // Get the image buffer
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()

            // Create a bitmap
            val bitmap = createBitmapFromRGBA(data, width, height)

            // Rotate the bitmap depending on the camera rotation and whether the camera is facing back
            return rotateBitmap(bitmap, rotation.toFloat(), isFrontCamera)
        }

        /**
         * Convert a bitmap to a byte array
         *
         * @param bitmap The bitmap
         * @return The byte array
         */
        @Synchronized
        fun bitMap2ByteArray(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            return stream.toByteArray()
        }

        /**
         * Create a bitmap from an RGBA byte array
         *
         * @param bytes The byte array
         * @param width The width of the image
         * @param height The height of the image
         */
        private fun createBitmapFromRGBA(
            bytes: ByteArray,
            width: Int,
            height: Int,
        ): Bitmap {
            // Create a ByteBuffer from the byte array
            val buffer = ByteBuffer.wrap(bytes)

            // Create a Bitmap using BitmapFactory
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }

        /**
         * Convert a byte array to a bitmap
         *
         * @param byteArray The byte array
         * @return The bitmap
         */
        @Synchronized
        fun byteArray2Bitmap(byteArray: ByteArray): Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        /**
         * Rotate a bitmap
         *
         * @param bitmap The bitmap
         * @param degrees The degrees to rotate the bitmap
         * @param isFrontCamera Whether the camera is facing front (this will mirror the image)
         *
         * @return The rotated bitmap
         */
        private fun rotateBitmap(
            bitmap: Bitmap,
            degrees: Float,
            isFrontCamera: Boolean,
        ): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)

            // Since we are showing a mirrored preview, we need to flip the image vertically
            if (isFrontCamera) matrix.preScale(1f, -1f)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}
