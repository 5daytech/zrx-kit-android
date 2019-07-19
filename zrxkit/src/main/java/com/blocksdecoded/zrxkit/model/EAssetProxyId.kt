package com.blocksdecoded.zrxkit.model

enum class EAssetProxyId(
    val id: String
) {
    ERC20("0xf47261b0"),
    ERC721("0x02571792"),
    MultiAsset("0x94cfcdd7"),
    ERC1155("0xa7cb5fb7");

    fun encode(asset: String): String = when(this) {
        ERC20 -> "${id}000000000000000000000000" + asset.replace("0x", "").toLowerCase()
        ERC721 -> TODO("ERC721 tokens are not supported yet.")
        MultiAsset -> TODO("MultiAsset tokens are not supported yet.")
        ERC1155 -> TODO("ERC1155 tokens are not supported yet.")
    }

    fun decode(asset: String): String = when(this) {
        ERC20 -> "0x" + asset.replace("${id}000000000000000000000000", "").toLowerCase()
        ERC721 -> TODO("ERC721 tokens are not supported yet.")
        MultiAsset -> TODO("MultiAsset tokens are not supported yet.")
        ERC1155 -> TODO("ERC1155 tokens are not supported yet.")
    }
}