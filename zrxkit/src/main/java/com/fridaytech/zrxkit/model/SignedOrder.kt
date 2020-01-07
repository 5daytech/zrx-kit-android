package com.fridaytech.zrxkit.model

data class SignedOrder(
    override val chainId: Int,
    override val exchangeAddress: String,
    override val makerAssetData: String,
    override val takerAssetData: String,
    override val makerAssetAmount: String,
    override val takerAssetAmount: String,
    override val makerAddress: String,
    override val takerAddress: String,
    override val expirationTimeSeconds: String,
    override val senderAddress: String,
    override val feeRecipientAddress: String,
    override val makerFee: String,
    override val makerFeeAssetData: String,
    override val takerFee: String,
    override val takerFeeAssetData: String,
    override val salt: String,
    val signature: String
) : IOrder {

    companion object {
        fun fromOrder(order: IOrder, signature: String): SignedOrder =
            SignedOrder(
                order.chainId,
                order.exchangeAddress,
                order.makerAssetData,
                order.takerAssetData,
                order.makerAssetAmount,
                order.takerAssetAmount,
                order.makerAddress,
                order.takerAddress,
                order.expirationTimeSeconds,
                order.senderAddress,
                order.feeRecipientAddress,
                order.makerFee,
                order.makerFeeAssetData,
                order.takerFee,
                order.takerFeeAssetData,
                order.salt,
                signature
            )
    }
}
