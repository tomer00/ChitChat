package com.tomer.chitchat.crypto

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.spec.IvParameterSpec

class CipherUtils {

    companion object {
        val G = BigInteger("5")
        val P = BigInteger("FFFFFFFFFFFFFFFFADF85458A2BB4A9AAFDC5620273D3CF1D8B9C583CDB1152D31A7004EEDB121B47A41D0D62DCC4024FFFFFFFFFFFFFFFF", 16)
        val ivSpecProvider = IvParameterSpec("9mf8f4&haj(#964@".toByteArray(StandardCharsets.UTF_8))

        fun generateRandom() = BigInteger(2048, SecureRandom())
    }
}