package com.blocksdecoded.zrxkit.model

interface IOrder {
    val exchangeAddress: String
    val makerAssetData: String
    val takerAssetData: String
    val makerAssetAmount: String
    val takerAssetAmount: String
    val makerAddress: String
    val takerAddress: String
    val expirationTimeSeconds: String
    val senderAddress: String
    val feeRecipientAddress: String
    val makerFee: String
    val takerFee: String
    val salt: String
}