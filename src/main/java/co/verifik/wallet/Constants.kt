package co.verifik.wallet

import co.verifik.wallet.utils.LivenessTolerance
import net.idrnd.idliveface.Tolerance

class Constants {
    companion object {
        /**
         * The URL to the API server
         */
        const val API_SERVER_URL = "your_server_endpoint"

        /**
         * The header to use for mobile authentication with the API server
         * It is either the mobile api key or a JWT token.
         */
        const val MOBILE_AUTH_HEADER = "your_api_key"

        /**
         * The probability threshold for liveness detection
         */
        const val LIVENESS_THRESHOLD = 0.5f

        /**
         * The pipeline to use for liveness checks (persephone/pegasus)
         */
        const val LIVENESS_PIPELINE = "pegasus"

        /**
         * The tolerance level for liveness checks
         */
        val LIVENESS_TOLERANCE = LivenessTolerance.SOFT


        val VERIFIER_AUTH_KEY: String? = null

        val ISSUERS_PUBLIC_KEY: String? = null

        //Etherscan constants
        const val ETHERSCAN_MAINNET_URL = "https://api.etherscan.io/api"
        const val ETHERSCAN_SEPOLIA_URL = "https://api-sepolia.etherscan.io/api"

        //Binance constants
        const val BINANCE_TICKER_PRICE_URL = "https://api.binance.com/api/v3/ticker/price"
    }
}
