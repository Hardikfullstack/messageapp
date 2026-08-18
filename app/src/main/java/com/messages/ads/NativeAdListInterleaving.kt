package com.messages.ads

/**
 * Inserts a `null` "ad marker" right after every 3rd real item — used to interleave native ads
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
