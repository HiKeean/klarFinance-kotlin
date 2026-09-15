package com.klarfinance.app.domain.model

/** Wraps a value returned by a repository that supports offline fallback (profile, loan
 * history, Transjakarta tickets) - [isFromCache] is true when the network was unreachable and
 * this is the last-known value from Room instead, so the UI can show an "offline" indicator
 * rather than silently presenting possibly-stale financial data as current. */
data class Cached<T>(val value: T, val isFromCache: Boolean)
