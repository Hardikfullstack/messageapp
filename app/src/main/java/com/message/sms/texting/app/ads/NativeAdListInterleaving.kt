package com.message.sms.texting.app.ads

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

object ListAdCache {
    private const val MAX_ENTRIES = 30

    private val ads = object : LinkedHashMap<String, NativeAd>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NativeAd>): Boolean {
            if (size <= MAX_ENTRIES) return false
            eldest.value.destroy()
            return true
        }
    }
    private val loadingKeys = mutableSetOf<String>()

    fun get(key: String): NativeAd? = ads[key]

    fun put(key: String, ad: NativeAd) {
        ads[key] = ad
    }

    /** Loads [adUnitId] ahead of time into slot [key], before that slot's own NativeAdView ever
     * composes -- used to look ahead a few rows past what's currently visible while scrolling a
     * list, so a slot seen for the very first time doesn't always start its load from scratch
     * right when it scrolls into view. A no-op if [key] is already cached or already loading. */
    fun preload(context: Context, adUnitId: String, key: String) {
        if (ads.containsKey(key) || key in loadingKeys) return
        loadingKeys += key
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                loadingKeys -= key
                put(key, ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingKeys -= key
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }
}

/**
 * Inserts a `null` "ad marker" right after every 3rd real item â€” used to interleave native ads
 * into plain (non-paginated) lists: Scheduled Messages, Starred, Blocked. Render `null` rows as
 * a [NativeAdView] and non-null rows as the normal item.
 */
fun <T> List<T>.interleaveAdEvery3(adEnabled: Boolean): List<T?> {
    if (!adEnabled || isEmpty()) return this
    return buildList {
        this@interleaveAdEvery3.forEachIndexed { index, item ->
            add(item)
            if ((index + 1) % 3 == 0) add(null)
        }
    }
}

/**
 * Same "ad after every 3rd real item" rule as [interleaveAdEvery3], but for Paging's
 * `LazyPagingItems` (Archived screen) where items are addressed by index rather than a
 * materialized list. Returns a row list where `null` = ad marker and `Int` = real item index.
 */
fun buildPagingAdRows(itemCount: Int, adEnabled: Boolean): List<Int?> {
    return buildList {
        for (i in 0 until itemCount) {
            add(i)
            if (adEnabled && (i + 1) % 3 == 0) add(null)
        }
    }
}
