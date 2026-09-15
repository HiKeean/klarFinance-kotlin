package com.klarfinance.app.core.network

import com.klarfinance.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic HTTP client mirroring the Angular apps' `ApiService` (`get`/`post`/`put`/`delete`
 * against a path, response auto-decoded into [ApiResponse]) - callers don't declare a
 * per-endpoint Retrofit interface anymore, they just call this directly. HMAC signing
 * (X-Timestamp/X-Signature/X-Client-Type) happens transparently via [HmacInterceptor] on
 * the shared [OkHttpClient] - nothing here has to know about it.
 *
 * `path` is relative to [BuildConfig.BASE_URL] and must include the `api/v1/...` prefix
 * (e.g. "api/v1/auth/request-otp") - the backend's per-controller-annotation prefix isn't
 * derivable client-side, so it has to be spelled out at the call site, same as before.
 */
@Singleton
class ApiService @Inject constructor(
    val okHttpClient: OkHttpClient,
    val json: Json,
) {
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend inline fun <reified T> get(path: String, params: Map<String, String> = emptyMap()): ApiResponse<T> =
        run(buildRequest("GET", path, params, null))

    suspend inline fun <reified T> post(path: String, params: Map<String, String> = emptyMap()): ApiResponse<T> =
        run(buildRequest("POST", path, params, null))

    suspend inline fun <reified T, reified B> post(
        path: String,
        body: B,
        params: Map<String, String> = emptyMap(),
    ): ApiResponse<T> = run(buildRequest("POST", path, params, json.encodeToString(body).toRequestBody(jsonMediaType)))

    suspend inline fun <reified T> put(path: String, params: Map<String, String> = emptyMap()): ApiResponse<T> =
        run(buildRequest("PUT", path, params, null))

    suspend inline fun <reified T, reified B> put(
        path: String,
        body: B,
        params: Map<String, String> = emptyMap(),
    ): ApiResponse<T> = run(buildRequest("PUT", path, params, json.encodeToString(body).toRequestBody(jsonMediaType)))

    suspend inline fun <reified T, reified B> patch(
        path: String,
        body: B,
        params: Map<String, String> = emptyMap(),
    ): ApiResponse<T> = run(buildRequest("PATCH", path, params, json.encodeToString(body).toRequestBody(jsonMediaType)))

    suspend inline fun <reified T> delete(path: String, params: Map<String, String> = emptyMap()): ApiResponse<T> =
        run(buildRequest("DELETE", path, params, null))

    /** Multipart (file upload) request - see [HmacInterceptor] for why this skips signing. */
    suspend inline fun <reified T> postMultipart(path: String, body: MultipartBody): ApiResponse<T> =
        run(buildRequest("POST", path, emptyMap(), body))

    fun buildRequest(method: String, path: String, params: Map<String, String>, body: RequestBody?): Request {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val cleanPath = path.trimStart('/')
        val urlBuilder = "$base/$cleanPath".toHttpUrl().newBuilder()
        params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val requestBuilder = Request.Builder().url(urlBuilder.build())
        when (method) {
            "GET" -> requestBuilder.get()
            "DELETE" -> if (body != null) requestBuilder.delete(body) else requestBuilder.delete()
            "POST" -> requestBuilder.post(body ?: EMPTY_BODY)
            "PUT" -> requestBuilder.put(body ?: EMPTY_BODY)
            "PATCH" -> requestBuilder.patch(body ?: EMPTY_BODY)
        }
        return requestBuilder.build()
    }

    suspend inline fun <reified T> run(request: Request): ApiResponse<T> {
        val (raw, isSuccessful) = withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    response.body?.string().orEmpty() to response.isSuccessful
                }
            } catch (e: IOException) {
                throw NetworkUnavailableException("Unable to reach KlarFinance. Check your connection and try again.", e)
            }
        }

        val decoded = runCatching { json.decodeFromString<ApiResponse<T>>(raw) }.getOrNull()
        if (!isSuccessful) {
            throw IllegalStateException(decoded?.message ?: "Something went wrong")
        }
        return decoded ?: throw IllegalStateException("Unexpected response from server")
    }

    companion object {
        val EMPTY_BODY: RequestBody = "".toRequestBody(null)
    }
}

/** Thrown instead of a generic exception specifically when the request never reached the
 * server (device offline, DNS failure, timeout, etc.) - repositories that support an offline
 * cache fallback (see [com.klarfinance.app.domain.model.Cached]) catch this type specifically,
 * as opposed to a real server-side rejection (4xx/5xx), which should still surface as an error. */
class NetworkUnavailableException(message: String, cause: Throwable? = null) : IOException(message, cause)
