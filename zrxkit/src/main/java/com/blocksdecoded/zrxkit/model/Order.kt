package com.blocksdecoded.zrxkit.model

data class Order(
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
    override val takerFee: String,
    override val salt: String
): IOrder