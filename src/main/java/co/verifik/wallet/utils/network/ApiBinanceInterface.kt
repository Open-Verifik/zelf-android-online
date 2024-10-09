package co.verifik.wallet.utils.network

import co.verifik.wallet.data.remote.ApiResponse
import co.verifik.wallet.data.remote.EtherscanGasOracleResponse
import co.verifik.wallet.data.remote.BinancePriceInfo
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ApiBinanceInterface {
    @GET
    suspend fun getTickerPrice(
        @Url url: String,
        @Query("symbol") symbol: String
    ): BinancePriceInfo?
}