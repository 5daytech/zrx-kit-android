package com.blocksdecoded.zrxkit.relayer.model

data class FeeRecipientsResponse(
    val total: Int,
    val page: Int,
    val perPage: Int,
    val records: List<String>
)