package co.jp.kpbr.gomiman.data.network

/**
 * Configuration and endpoints for Google Cloud Run backend services.
 */
object CloudRunConfig {
    /**
     * Firebase Hosting gateway URL (proxies to Cloud Run with App Check enforcement).
     * Direct Cloud Run (private): https://gomiman-backend-272780938054.asia-northeast1.run.app
     */
    const val DEFAULT_BASE_URL = "https://gomiman-af0f8.web.app"

    /**
     * Header key for Firebase App Check verification expected by Cloud Run App Check middleware / IAM.
     */
    const val APP_CHECK_HEADER = "X-Firebase-AppCheck"

    // Diagnostic & verification endpoints
    const val PATH_HEALTH = "/healthz"
    const val PATH_PING = "/api/v1/ping"
    const val PATH_TEST = "/api/v1/test"

    // Synchronization endpoints
    const val PATH_SYNC_BASE_INFO = "/user/sync-user/base-info"
    const val PATH_SYNC_PUSH_SETTING = "/user/sync-user/push-setting"
    const val PATH_SYNC_GARBAGE_SETTING = "/user/sync-user/garbage-setting"
    // Note: User feedback is submitted directly to Firestore 'feedback' collection via FirestoreSyncRepository
    const val PATH_FEEDBACK_SUBMIT = "/feedback/submit"
}
