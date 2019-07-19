package com.blocksdecoded.zrxkit.relayer.remote

import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.relayer.model.*
import com.google.gson.GsonBuilder
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class RelayerApiClient(relayerConfig: RelayerConfig) {
    private val client: RelayerNetworkClient

    init {
        val logger = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logger)

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(relayerConfig.baseUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        client = retrofit.create(RelayerNetworkClient::class.java)
    }

    private val prefix = "${relayerConfig.suffix}${relayerConfig.version}"

    fun feeRecipients(networkId: Int = 42): Flowable<List<String>> =
        client.getFeeRecipients("$prefix/fee_recipients", networkId)
            .map { it.records }.toFlowable()

    fun getOrderbook(base: String, quote: String, networkId: Int = 42): Flowable<OrderBookResponse> =
        client.getOrderBook(
            "$prefix/orderbook",
            base,
            quote,
            networkId
        ).toFlowable()

    fun getAssets(limit: Int = 100, networkId: Int = 42): Flowable<List<AssetPair>> =
        client.getAssetPairs(
            "$prefix/asset_pairs",
            limit,
            networkId
        ).toFlowable().map { it.records }

    fun postOrder(order: SignedOrder, networkId: Int = 42): Flowable<Unit> = client.postOrder(
        "$prefix/order",
        order,
        networkId
    ).toFlowable()

    private interface RelayerNetworkClient {
        @GET
        fun getOrderBook(
            @Url url: String,
            @Query("baseAssetData") baseAsset: String,
            @Query("quoteAssetData") quoteAsset: String,
            @Query("networkId") int: Int
        ): Single<OrderBookResponse>

        @GET
        fun getOrder(@Url url: String, @Query("networkId") int: Int = 42): Single<OrderRecord>

        @GET
        fun getFeeRecipients(
                @Url url: String,
                @Query("networkId") int: Int
        ): Single<FeeRecipientsResponse>

        @POST
        fun postOrder(
            @Url url: String,
            @Body order: SignedOrder,
            @Query("networkId") int: Int
        ): Completable

        @GET
        fun getAssetPairs(
            @Url url: String,
            @Query("perPage") perPage: Int,
            @Query("networkId") int: Int
        ): Single<AssetPairsResponse>
    }
}