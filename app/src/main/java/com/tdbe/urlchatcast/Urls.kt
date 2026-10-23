package com.tdbe.urlchatcast

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object Urls {
    private val TOKEN = Regex("""^\s*(\S+)""")

    /** Find `pattern` (literal, case-insensitive), return the URL token after it. */
    fun extract(text: String, pattern: String): String? {
        val i = text.indexOf(pattern, ignoreCase = true)
        if (i < 0) return null
        val colonSlashSlashIdx = text.indexOf("://", ignoreCase = true)
        if (colonSlashSlashIdx < 0) return null

        val kotlinSucksReversedText = text.reversed()
        // drop function in Kotlin removes characters from the beginning of a string up to the specified index
        val kotlinSucksSubstring = kotlinSucksReversedText.drop(kotlinSucksReversedText.indexOf("//:"))
        // ok now we have "my command httpx://" but in reverse
        val kotlinSucksList = kotlinSucksSubstring.split(" ")
        // to get "httpx://"
        val urlPrefix = kotlinSucksList.first().reversed()

        val url = text.drop(text.indexOf(urlPrefix))
        return url
    }

    /** The default browser, or null when none is set (resolver would ask every time). */
    fun browserPackage(pm: PackageManager): String? {
        val pkg = pm.resolveActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName ?: return null
        // "android" means the system ResolverActivity, i.e. no default chosen yet.
        return if (pkg == "android") null else pkg
    }

    /** Non-browser app that handles this URL, or null if only the browser does. */
    fun appPackageFor(pm: PackageManager, url: String, browser: String?): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .firstOrNull { it != browser }
    }
}