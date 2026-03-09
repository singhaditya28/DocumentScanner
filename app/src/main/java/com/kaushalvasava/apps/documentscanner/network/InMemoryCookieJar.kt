package com.kaushalvasava.apps.documentscanner.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Simple in-memory CookieJar that stores the HttpOnly refresh_token cookie
 * returned by the server after login and sends it on subsequent requests.
 */
class InMemoryCookieJar : CookieJar {

    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.removeAll { it.name == "refresh_token" }
        this.cookies.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies.filter { it.matches(url) }
    }

    fun getRefreshToken(): String? {
        return cookies.find { it.name == "refresh_token" }?.value
    }

    fun clear() {
        cookies.clear()
    }
}
