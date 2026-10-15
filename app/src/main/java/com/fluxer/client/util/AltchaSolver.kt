package com.fluxer.client.util

import android.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Native ALTCHA v2 proof-of-work solver (PBKDF2/SHA-*).
 *
 * Fluxer returns an `altcha_challenge` object in `CAPTCHA_REQUIRED` / `INVALID_CAPTCHA`
 * errors. The password for each attempt is `nonce || uint32_be(counter)`; the first counter
 * whose derived key starts with `keyPrefix` is the solution. No WebView needed.
 */
object AltchaSolver {

    /** Returns the value for the `X-Captcha-Token` header, or null if unsolvable / timed out. */
    suspend fun solveToken(challenge: JsonObject, timeoutMs: Long = 90_000L): String? {
        val parameters = challenge["parameters"]?.jsonObject ?: return null
        if (challenge["signature"] == null) return null

        val algorithm = parameters["algorithm"]?.jsonPrimitive?.content ?: return null
        val hmacName = when (algorithm) {
            "PBKDF2/SHA-256" -> "HmacSHA256"
            "PBKDF2/SHA-384" -> "HmacSHA384"
            "PBKDF2/SHA-512" -> "HmacSHA512"
            else -> return null
        }
        val nonce = parameters["nonce"]?.jsonPrimitive?.content?.hexToBytes() ?: return null
        val salt = parameters["salt"]?.jsonPrimitive?.content?.hexToBytes() ?: return null
        val cost = parameters["cost"]?.jsonPrimitive?.int?.takeIf { it > 0 } ?: return null
        val keyLength = parameters["keyLength"]?.jsonPrimitive?.int ?: 32
        val keyPrefix = parameters["keyPrefix"]?.jsonPrimitive?.content?.lowercase()
            ?.takeIf { it.isNotEmpty() && it.length <= keyLength * 2 } ?: return null

        val workers = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)
        val started = System.currentTimeMillis()

        val found = withTimeoutOrNull(timeoutMs) {
            coroutineScope {
                val result = CompletableDeferred<Pair<Long, String>>()
                repeat(workers) { index ->
                    launch(Dispatchers.Default) {
                        val mac = Mac.getInstance(hmacName)
                        val password = ByteArray(nonce.size + 4).also { nonce.copyInto(it) }
                        var counter = index.toLong()
                        while (isActive && !result.isCompleted && counter <= MAX_COUNTER) {
                            password[nonce.size] = (counter shr 24).toByte()
                            password[nonce.size + 1] = (counter shr 16).toByte()
                            password[nonce.size + 2] = (counter shr 8).toByte()
                            password[nonce.size + 3] = counter.toByte()
                            val key = pbkdf2(mac, hmacName, password, salt, cost, keyLength)
                            val hex = key.toHex()
                            if (hex.startsWith(keyPrefix)) {
                                result.complete(counter to hex)
                                break
                            }
                            counter += workers
                        }
                    }
                }
                result.await().also { coroutineContext.cancelChildren() }
            }
        } ?: return null

        val token = buildJsonObject {
            put("challenge", challenge)
            put("solution", buildJsonObject {
                put("counter", JsonPrimitive(found.first))
                put("derivedKey", JsonPrimitive(found.second))
                put("time", JsonPrimitive(System.currentTimeMillis() - started))
            })
        }
        return Base64.encodeToString(token.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private const val MAX_COUNTER = 0xFFFFFFFFL

    /** PBKDF2 with a raw byte password (the JCA PBKDF2 factories only accept char[]). */
    private fun pbkdf2(
        mac: Mac,
        hmacName: String,
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int
    ): ByteArray {
        mac.init(SecretKeySpec(password, hmacName))
        val out = ByteArray(keyLength)
        val blockLen = mac.macLength
        var offset = 0
        var block = 1
        while (offset < keyLength) {
            var u = mac.doFinal(salt + byteArrayOf(
                (block shr 24).toByte(), (block shr 16).toByte(), (block shr 8).toByte(), block.toByte()
            ))
            val t = u.copyOf()
            for (i in 1 until iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val n = minOf(blockLen, keyLength - offset)
            t.copyInto(out, offset, 0, n)
            offset += n
            block++
        }
        return out
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(HEX[(b.toInt() shr 4) and 0xF]).append(HEX[b.toInt() and 0xF])
        }
        return sb.toString()
    }

    private const val HEX = "0123456789abcdef"

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
