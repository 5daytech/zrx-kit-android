package com.fridaytech.zrxkit.relayer.remote

import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.relayer.model.*
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
            .setLevel(HttpLoggingInterceptor.Level.BODY)

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

    fun feeRecipients(): Flowable<List<String>> =
        client.getFeeRecipients("$prefix/fee_recipients")
            .map { it.records }.toFlowable()

    fun getOrderbook(base: String, quote: String, limit: Int = 100): Flowable<OrderBookResponse> =
        client.getOrderBook(
            "$prefix/orderbook",
            base,
            quote,
            limit
        ).toFlowable()

    fun getOrders(makerAddress: String?, limit: Int = 100): Flowable<OrderBook> =
        client.getOrders(
            "$prefix/orders",
            makerAddress,
            limit
        ).toFlowable()

    fun getAssets(limit: Int = 100): Flowable<List<AssetPair>> =
        client.getAssetPairs(
            "$prefix/asset_pairs",
            limit
        ).map { it.records }.toFlowable()

    fun postOrder(order: SignedOrder): Flowable<Unit> = client.postOrder(
        "$prefix/order",
        order
    ).toFlowable()

    private interface RelayerNetworkClient {
        @GET
        fun getOrders(
            @Url url: String,
            @Query("makerAddress") makerAddress: String?,
            @Query("perPage") limit: Int?
        ): Single<OrderBook>

        @GET
        fun getOrderBook(
            @Url url: String,
            @Query("baseAssetData") baseAsset: String,
            @Query("quoteAssetData") quoteAsset: String,
            @Query("perPage") limit: Int
        ): Single<OrderBookResponse>

        @GET
        fun getFeeRecipients(
            @Url url: String
        ): Single<FeeRecipientsResponse>

        @POST
        fun postOrder(
            @Url url: String,
            @Body order: SignedOrder
        ): Completable

        @GET
        fun getAssetPairs(
            @Url url: String,
            @Query("perPage") perPage: Int
        ): Single<AssetPairsResponse>
    }
}
