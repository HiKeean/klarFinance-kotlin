package com.klarfinance.app.core.network

import android.util.Base64
import com.klarfinance.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * Client-side counterpart of the backend's HmacSignatureFilter - signs every request
 * with X-Timestamp/X-Signature/X-Client-Type, same scheme the Angular apps use
 * (webadmin/frontend `api.service.ts`):
 *
 *   stringToSign = method + path + timestamp + rawJsonBody + apiKey
 *   X-Signature  = base64(HMAC-SHA256(secretKey, stringToSign))
 *
 * `path` must exclude the query string - it has to match request.getRequestURI()
 * on the backend, which also excludes it.
 */
class HmacInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Multipart bodies (KTP/selfie upload) are never HMAC-checked server-side -
        // HmacRequestCachingFilter skips wrapping them, so HmacSignatureFilter never
        // reaches the header check for them. Skip signing to avoid buffering raw
        // image bytes into a string for nothing.
        if (original.body?.contentType()?.type == "multipart") {
            return chain.proceed(original)
        }

        val bodyString = original.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }.orEmpty()

        val timestamp = System.currentTimeMillis().toString()
        val stringToSign = original.method + original.url.encodedPath + timestamp + bodyString + BuildConfig.API_KEY

        val signedRequest = original.newBuilder()
            .header("X-Timestamp", timestamp)
            .header("X-Signature", sign(stringToSign))
            .header("X-Client-Type", BuildConfig.CLIENT_TYPE)
            .build()

        return chain.proceed(signedRequest)
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(BuildConfig.SECRET_KEY.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
