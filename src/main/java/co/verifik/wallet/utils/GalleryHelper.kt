package co.verifik.wallet.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import co.verifik.wallet.R
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat

class GalleryHelper {
    companion object {

        @Throws(IOException::class)
        fun saveImageToGallery(
            context: Context,
            bitmap: Bitmap,
        ): Uri? {
            val imageOutStream: OutputStream?
            val external = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val file = File(Environment.DIRECTORY_PICTURES, context.getString(R.string.app_name))
            val fileName: String =
                SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(System.currentTimeMillis()) + ".jpg"
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, fileName)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                file.toString(),
            )
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.MediaColumns.WIDTH, bitmap.width)
            values.put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
            val uri =
                context.contentResolver.insert(
                    external,
                    values,
                )
            imageOutStream = context.contentResolver.openOutputStream(uri!!)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageOutStream!!)
            imageOutStream.close()
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            val id = ContentUris.parseId(uri)

            // If we don't have the bucket id, record the bucket id
            if (KeyValueStore.getInstance(context).getGalleryBucketId() == null) {
                val projection =
                    arrayOf(
                        MediaStore.Images.ImageColumns.BUCKET_ID,
                    )
                val cursor =
                    context.contentResolver
                        .query(
                            external,
                            projection,
                            MediaStore.Images.Media._ID + "=?",
                            arrayOf("" + id),
                            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC",
                        )
                // Put it in the image view
                if (cursor!!.moveToFirst()) {
                    val bucketId = cursor.getString(0)
                    KeyValueStore.getInstance(context).setGalleryBucketId(bucketId)
                }
            }

            // Wait until MINI_KIND thumbnail is generated.
            val miniThumb =
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null,
                )
            return uri
        }
    }
}
