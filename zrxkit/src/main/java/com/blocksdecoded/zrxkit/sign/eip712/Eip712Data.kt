package com.blocksdecoded.zrxkit.sign.eip712

import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

internal class Eip712Data {
    class Entry @JsonCreator
    constructor(
        @param:JsonProperty(value = "name") val name: String,
        @param:JsonProperty(value = "type") val type: String
    )

    class EIP712Domain @JsonCreator
    constructor(
        @param:JsonProperty(value = "name") val name: String,
        @param:JsonProperty(value = "version") val version: String,
        @param:JsonProperty(value = "chainId") val chainId: Long,
        @param:JsonProperty(value = "verifyingContract") val verifyingContract: String
    )

    class EIP712Message @JsonCreator
    constructor(
        @param:JsonProperty(value = "types") val types: HashMap<String, List<Entry>>,
        @param:JsonProperty(value = "primaryType") val primaryType: String,
        @param:JsonProperty(value = "message") val message: Any,
        @param:JsonProperty(value = "domain") val domain: EIP712Domain
    ) {

        override fun toString(): String {
            return ("EIP712Message{"
                    + "primaryType='" + this.primaryType + '\''.toString()
                    + ", message='" + this.message + '\''.toString()
                    + '}'.toString())
        }
    }
}