package com.tdbe.urlchatcast

import android.content.Context

object Prefs {
    private const val FILE = "urlchatcast"

    fun getWatchedPackages(ctx: Context): Set<String> =
        ctx.getSharedPreferences(FILE, 0).getStringSet("packages", emptySet())!!
    fun setWatchedPackages(ctx: Context, pkgs: Set<String>) =
        ctx.getSharedPreferences(FILE, 0).edit().putStringSet("packages", pkgs).apply()

    fun getPattern(ctx: Context): String =
        ctx.getSharedPreferences(FILE, 0).getString("pattern", "open https:")!!
    fun setPattern(ctx: Context, p: String) =
        ctx.getSharedPreferences(FILE, 0).edit().putString("pattern", p).apply()

    fun autoOpen(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, 0).getBoolean("auto_open", false)
    fun setAutoOpen(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(FILE, 0).edit().putBoolean("auto_open", v).apply()

    fun autoOpenUseApp(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, 0).getBoolean("auto_open_app", false)
    fun setAutoOpenUseApp(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(FILE, 0).edit().putBoolean("auto_open_app", v).apply()
}