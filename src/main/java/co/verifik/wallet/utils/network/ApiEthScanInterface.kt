package co.verifik.wallet.utils.network

import co.verifik.wallet.data.remote.ApiResponse
import co.verifik.wallet.data.remote.EtherscanGasOracleResponse
import co.verifik.wallet.data.remote.EtherscanTransaction
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ApiEthScanInterface {
    @GET
    suspend fun getBalance(
        @Url url: String,
        @QueryMap params: Map<String, String>
    ): ApiResponse<String?>

    @GET
    suspend fun gasOracle(
        @Url url: String,
        @QueryMap params: Map<String, String>
    ): ApiResponse<EtherscanGasOracleResponse?>

    @GET
    suspend fun getTransactions(
        @Url url: String,
        @QueryMap params: Map<String, String>
    ): ApiResponse<List<EtherscanTransaction>?>

    @GET
    suspend fun getTransaction(
        @Url url: String,
        @QueryMap params: Map<String, String>
    ): ApiResponse<EtherscanTransaction?>
}