package co.verifik.wallet.data.local

import co.verifik.wallet.data.db.QrEntity

data class SendCryptoInfo(
    val qrEntityId: Int,
    val sendToAddress: String,
    val network: String,
    val amount: String
)
