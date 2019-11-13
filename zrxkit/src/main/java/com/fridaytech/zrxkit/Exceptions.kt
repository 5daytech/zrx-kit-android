package com.fridaytech.zrxkit

class InvalidSignatureException : Exception("Invalid order signature")
class UnsupportedSignatureType(type: Int) : Exception("Unsupported signature type $type")
