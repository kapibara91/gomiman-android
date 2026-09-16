package co.jp.kpbr.gomiman.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import co.jp.kpbr.gomiman.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object AdConstants {
    // Official Google AdMob Test Banner Unit ID
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    // Tab 1 (Timeline / カレンダー) Bottom Banner ID
    const val PROD_TIMELINE_BOTTOM_BANNER_ID = "ca-app-pub-6247787890080027/4304120209"

    // Tab 2 (GarbageList / ゴミの日) Bottom Banner ID
    const val PROD_COLLECTION_BOTTOM_BANNER_ID = "ca-app-pub-6247787890080027/3858168159"

    fun getTimelineBannerUnitId(): String {
        return if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_TIMELINE_BOTTOM_BANNER_ID
    }

    fun getCollectionBannerUnitId(): String {
        return if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_COLLECTION_BOTTOM_BANNER_ID
    }
}

/**
 * Reusable Banner Ad Composable for Google Mobile Ads (AdMob).
 * Loads test banner in debug build and production unit ID in release build.
 */
@Composable
fun BannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adView = remember(adUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("BannerAdView", "Ad loaded successfully: $adUnitId")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w("BannerAdView", "Ad failed to load: ${loadAdError.message} (code=${loadAdError.code}) for unit $adUnitId")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.wrapContentSize(),
            factory = { adView }
        )
    }
}
