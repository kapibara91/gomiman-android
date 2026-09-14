package co.jp.kpbr.gomiman.data.network

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that automatically attaches the Firebase App Check token
 * to every outgoing request destined for Cloud Run services.
 *
 * If the Cloud Run server responds with HTTP 401 or 403, it performs a one-time
 * automatic retry with a forced token refresh.
 */
class AppCheckInterceptor : Interceptor {

    companion object {
        private const val TAG = "AppCheckInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        try {
            val token = runBlocking {
                AppCheckManager.getAppCheckToken(forceRefresh = false)
            }

            if (!token.isNullOrBlank()) {
                requestBuilder.header(CloudRunConfig.APP_CHECK_HEADER, token)
                Log.d(TAG, "Attached App Check token to request: ${originalRequest.url}")
            } else {
                Log.w(TAG, "No App Check token available for request: ${originalRequest.url}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining App Check token for: ${originalRequest.url}", e)
        }

        val response = chain.proceed(requestBuilder.build())

        // If Cloud Run rejects with 401/403, retry once with a freshly minted token
        if (response.code == 401 || response.code == 403) {
            Log.w(TAG, "Cloud Run returned ${response.code}. Attempting token force-refresh retry...")
            try {
                val freshToken = runBlocking {
                    AppCheckManager.getAppCheckToken(forceRefresh = true)
                }
                if (!freshToken.isNullOrBlank()) {
                    response.close()
                    val retryRequest = originalRequest.newBuilder()
                        .header(CloudRunConfig.APP_CHECK_HEADER, freshToken)
                        .build()
                    return chain.proceed(retryRequest)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh retry failed", e)
            }
        }

        return response
    }
}
