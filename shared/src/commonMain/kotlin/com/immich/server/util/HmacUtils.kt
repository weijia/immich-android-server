package com.immich.server.util

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    /**
     * Compute HMAC-SHA256 and return result as hex string.
     */
    fun hmacSha256Hex(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val result = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return result.toHexString()
    }
    
    /**
     * Compute HMAC-SHA256 and return result as Base64 string.
     */
    fun hmacSha256Base64(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val result = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getEncoder().encodeToString(result)
    }
    
    /**
     * Compute SHA256 hash and return result as hex string.
     */
    fun sha256Hex(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val result = digest.digest(data.toByteArray(Charsets.UTF_8))
        return result.toHexString()
    }
}

/**
 * Extension to convert ByteArray to hex string.
 */
fun ByteArray.toHexString(): String {
    return this.joinToString("") { byte -> "%02x".format(byte) }
}