package co.verifik.wallet.utils.network

import co.verifik.wallet.data.remote.ApiArrayZelfResponse
import co.verifik.wallet.data.remote.ApiSession
import co.verifik.wallet.data.remote.ApiZelfDashboardResponse
import co.verifik.wallet.data.remote.ApiZelfGasResponse
import co.verifik.wallet.data.remote.ApiZelfResponse
import co.verifik.wallet.data.remote.IPFSResponse
import co.verifik.wallet.data.remote.ZelfAuthResponse
import co.verifik.wallet.data.remote.ZelfWalletResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface ApiZelfInterface {

    @POST("api/sessions")
    suspend fun getSession(
        @Body params: Map<String, String>
    ): ApiZelfResponse<ApiSession?>

    @GET("api/ethereum/address")
    suspend fun dashboard(
        @Header("Authorization") authorization: String,
        @QueryMap params: Map<String, String>
    ): ApiZelfResponse<ApiZelfDashboardResponse?>

    @GET("api/ethereum/gas-tracker")
    suspend fun gasPrices(
        @Header("Authorization") authorization: String
    ): ApiZelfResponse<ApiZelfGasResponse?>


    @POST("api/clients/auth")
    suspend fun auth(
        @Header("x-api-key") apiKey: String,
        @Body params: Map<String, String>
    ): ZelfAuthResponse?

    @POST("api/wallets")
    suspend fun createWallet(
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): ApiZelfResponse<ZelfWalletResponse?>


    @POST("api/my-wallets/import")
    suspend fun importWallet(
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): ApiZelfResponse<ZelfWalletResponse?>

    @POST("api/my-wallets/decrypt")
    suspend fun decryptWallet(
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): ApiZelfResponse<ZelfWalletResponse?>

    @GET("api/ipfs")
    suspend fun searchIPFS(
        @Header("Authorization") authorization: String,
        @QueryMap params: Map<String, String>
    ): ApiArrayZelfResponse<IPFSResponse?>
}