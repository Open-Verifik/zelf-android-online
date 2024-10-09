package co.verifik.wallet.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.Objects

class KeyValueStore {
    private var sharedPreferences: SharedPreferences
    private val keyGalleryBucketId = "KEY_GALLERY_BUCKET_ID"

    init {
        sharedPreferences = getEncryptedSharedPreferences()
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = getMasterKey()
        return EncryptedSharedPreferences.create(
            Objects.requireNonNull<Context>(context),
            "co.verifik.zelf.KeyValueStore",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getMasterKey(): MasterKey {
        val spec =
            KeyGenParameterSpec.Builder(
                "_androidx_security_master_key_",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256)
                .build()
        return MasterKey.Builder(context!!).setKeyGenParameterSpec(spec).build()
    }

    companion object {
        private var instance: KeyValueStore? = null
        private var context: Context? = null

        fun getInstance(context: Context): KeyValueStore {
            Companion.context = context
            if (instance != null) {
                return instance!!
            }
            instance = KeyValueStore()
            return instance!!
        }
    }

    /**
     * Gets the gallery bucket id
     *
     * @return the gallery bucket id
     */
    fun getGalleryBucketId(): String? {
        return sharedPreferences!!.getString(keyGalleryBucketId, null)
    }

    /**
     * Sets the gallery bucket id
     *
     * @param bucketId the gallery bucket id
     */
    fun setGalleryBucketId(bucketId: String?) {
        val editor = sharedPreferences!!.edit()
        editor.putString(keyGalleryBucketId, bucketId)
        editor.apply()
        editor.commit()
    }
}
