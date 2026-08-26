package com.message.sms.texting.app.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.message.sms.texting.app.R
import com.message.sms.texting.app.utils.AnalyticsManager

enum class NativeAdTemplate { SMALL, MEDIUM }

/** Fixed row height for [NativeAdTemplate.SMALL] â€” matches the message list row's own height
 * (52dp avatar + 8dp top/bottom padding) exactly, and is applied to both the skeleton and the
 * loaded ad's AndroidView, so swapping one for the other never shifts the list around it. */
private val SmallNativeAdHeight = 68.dp

/** Fixed card height for [NativeAdTemplate.MEDIUM] â€” generous enough to fit icon+headline+body,
 * the media block (even at its tallest clamp), and the button, applied to both the skeleton and
 * the loaded ad's AndroidView so swapping one for the other never shifts surrounding content. */
private val MediumNativeAdHeight = 280.dp

/** Shorter skeleton height for [NativeAdView]'s [compact] MEDIUM variant (smaller media clamp
 * below) â€” currently only ChatScreen, which sits right above the keyboard/input and shouldn't
 * eat as much vertical space as the After Call screen's full-size card. */
private val CompactMediumNativeAdHeight = 220.dp

/**
 * Reusable native ad, not placed on any screen yet. [template] picks the layout: SMALL for a
 * compact list-row style, MEDIUM for a bigger card with a media (image/video) view. Shows a
 * shimmer skeleton shaped like the template (icon/headline/body/button blocks) while the ad
 * is loading, so there's no layout jump once it arrives.
 */
@Composable
fun NativeAdView(
    adUnitId: String,
    template: NativeAdTemplate,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    // The Activity intercepts uiMode config changes (see AndroidManifest) instead of recreating,
    // so this AndroidView's background â€” resolved once from XML at inflate time â€” would otherwise
    // never refresh when the system/app theme flips. Reading isSystemInDarkTheme() here forces
    // this composable (and the AndroidView's update{} below) to rerun when it changes, so
    // bindNativeAd can re-apply the correct color each time.
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    var nativeAd by remember(adUnitId) { mutableStateOf(NativeAdCache.take(adUnitId)) }
    var hasFailed by remember(adUnitId) { mutableStateOf(false) }

    DisposableEffect(adUnitId) {
        // A cached ad (preloaded ahead of time via NativeAdCache, e.g. from Splash) is already
        // in hand â€” skip loading a fresh one.
        if (nativeAd != null) {
            return@DisposableEffect onDispose { nativeAd?.destroy() }
        }
        hasFailed = false
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                AnalyticsManager.logAdEvent("native", adUnitId, "loaded")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    hasFailed = true
                    AnalyticsManager.logAdEvent("native", adUnitId, "failed_to_load")
                }

                override fun onAdClicked() {
                    AnalyticsManager.logAdEvent("native", adUnitId, "clicked")
                }
            })
            .build()
        AnalyticsManager.logAdEvent("native", adUnitId, "request")
        adLoader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    // No fill / network error / misconfigured unit id â€” collapse rather than shimmering forever.
    if (hasFailed) return

    val ad = nativeAd
    if (ad == null) {
        when (template) {
            NativeAdTemplate.SMALL -> SmallNativeAdSkeleton(modifier)
            NativeAdTemplate.MEDIUM -> MediumNativeAdSkeleton(modifier, compact)
        }
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (template) {
                    // Fixed to match the adjacent list row's own height exactly.
                    NativeAdTemplate.SMALL -> Modifier.height(SmallNativeAdHeight)
                    // No fixed height here (unlike the skeleton) â€” native_ad_medium.xml's root is
                    // wrap_content, and at large system font sizes the headline/body/CTA text
                    // wraps to more lines than MediumNativeAdHeight has room for. Forcing that
                    // fixed height then clips the CTA button almost entirely off the bottom
                    // (reported from the After Call screen) instead of letting the card grow.
                    NativeAdTemplate.MEDIUM -> Modifier
                }
            ),
        factory = { ctx ->
            val layoutRes = when (template) {
                NativeAdTemplate.SMALL -> R.layout.native_ad_small
                NativeAdTemplate.MEDIUM -> R.layout.native_ad_medium
            }
            LayoutInflater.from(ctx).inflate(layoutRes, null) as com.google.android.gms.ads.nativead.NativeAdView
        },
        update = { adView ->
            // isDarkTheme is read above purely to make this whole update{} block rerun when the
            // theme flips (see the comment on that read) â€” re-resolving from resources here picks
            // up the current, correct color/drawable each time regardless of its actual value.
            refreshCardBackground(adView, template)
            bindNativeAd(adView, ad, compact)
        }
    )
}

/** Re-resolves and re-applies the card background color/drawable from resources â€” needed because
 * the Activity doesn't recreate on theme changes (see [NativeAdView]'s isDarkTheme read), so the
 * XML-resolved background from inflate time would otherwise never refresh. */
private fun refreshCardBackground(adView: com.google.android.gms.ads.nativead.NativeAdView, template: NativeAdTemplate) {
    when (template) {
        NativeAdTemplate.SMALL -> {
            adView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(adView.context, R.color.native_ad_card_bg))
        }
        NativeAdTemplate.MEDIUM -> {
            adView.background = androidx.core.content.ContextCompat.getDrawable(adView.context, R.drawable.native_ad_medium_card_bg)
        }
    }
}

@Composable
private fun SmallNativeAdSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SmallNativeAdHeight)
            .background(colorResource(R.color.native_ad_card_bg))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .adShimmerEffect()
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(15.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(20.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(18.dp))
                .adShimmerEffect()
        )
    }
}

@Composable
private fun MediumNativeAdSkeleton(modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) CompactMediumNativeAdHeight else MediumNativeAdHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.native_ad_card_bg))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row {
            Box(modifier = Modifier.size(52.dp).adShimmerEffect())
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.width(20.dp).height(9.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).adShimmerEffect())
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(if (compact) 80.dp else 130.dp).clip(RoundedCornerShape(12.dp)).adShimmerEffect())
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(18.dp)).adShimmerEffect())
    }
}

private fun bindNativeAd(adView: com.google.android.gms.ads.nativead.NativeAdView, nativeAd: NativeAd, compact: Boolean = false) {
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val mediaView = adView.findViewById<MediaView?>(R.id.ad_media)

    headlineView.text = nativeAd.headline
    adView.headlineView = headlineView

    if (nativeAd.body == null) {
        bodyView.visibility = View.INVISIBLE
    } else {
        bodyView.text = nativeAd.body
        bodyView.visibility = View.VISIBLE
    }
    adView.bodyView = bodyView

    val icon = nativeAd.icon
    if (icon == null) {
        iconView.visibility = View.GONE
    } else {
        iconView.setImageDrawable(icon.drawable)
        iconView.visibility = View.VISIBLE
    }
    adView.iconView = iconView

    if (nativeAd.callToAction == null) {
        ctaView.visibility = View.INVISIBLE
    } else {
        ctaView.text = nativeAd.callToAction
        ctaView.visibility = View.VISIBLE
    }
    adView.callToActionView = ctaView

    if (mediaView != null) {
        adView.mediaView = mediaView
        val mediaContent = nativeAd.mediaContent
        mediaView.mediaContent = mediaContent

        // MediaView letterboxes its content to fit a fixed height, leaving most of the box
        // empty if the asset's own aspect ratio is very different â€” size the box to the ad's
        // actual aspect ratio instead so the image/video fills it edge to edge.
        val aspectRatio = mediaContent?.aspectRatio
        if (aspectRatio != null && aspectRatio > 0f) {
            mediaView.post {
                val width = mediaView.width
                if (width > 0) {
                    // Clamp to a landscape-ish band so a very tall (portrait) or very wide
                    // creative can't blow the card out of proportion â€” stays close to the
                    // 130dp default either way.
                    val idealHeight = width / aspectRatio
                    val clampedHeight = if (compact) {
                        idealHeight.coerceIn(width * 0.18f, width * 0.28f)
                    } else {
                        idealHeight.coerceIn(width * 0.28f, width * 0.42f)
                    }
                    val params = mediaView.layoutParams
                    params.height = clampedHeight.toInt()
                    mediaView.layoutParams = params
                }
            }
        }
    }

    adView.setNativeAd(nativeAd)
}
