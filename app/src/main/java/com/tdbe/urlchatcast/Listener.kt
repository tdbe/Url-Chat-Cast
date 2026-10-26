package com.tdbe.urlchatcast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast

class Listener : NotificationListenerService() {

    companion object {
        private const val FG_ID = 1
        private const val FG_CHANNEL = "listening"

        // So PopupActivity (not a service itself) can clear notifications after opening a URL.
        @Volatile var instance: Listener? = null
    }

    private var lastKey: String? = null

    override fun onListenerConnected() {
        instance = this
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(FG_CHANNEL, "Listening", NotificationManager.IMPORTANCE_LOW)
                .apply { setSound(null, null) })
        val n = Notification.Builder(this, FG_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Watching notifications")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        // Foreground status exempts us from background-activity-start restrictions.
        try { startForeground(FG_ID, n) } catch (_: Exception) { }
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) { }
    }

    /** Dismiss active notifications from watched apps that match the trigger pattern.
     * If we don't do this you may sometimes find you're opening an older / oldest command url instead of the newest.
     * And I don't want to make this app hold a log of what links you visited.*/
    fun clearMatchedNotifications() {
        if (!Prefs.clearNotifications(this)) return
        val pattern = Prefs.getPattern(this)
        val watched = Prefs.getWatchedPackages(this)
        val active = try { activeNotifications } catch (_: Exception) { null } ?: return
        for (sbn in active) {
            if (sbn.packageName !in watched) continue
            val extras = sbn.notification.extras
            val text = listOfNotNull(
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ).joinToString("\n")
            if (Urls.extract(text, pattern) != null) cancelNotification(sbn.key)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // "Do nothing if no apps are picked": empty set matches nothing.
        if (sbn.packageName !in Prefs.getWatchedPackages(this)) return
        if (sbn.isOngoing) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        ).joinToString("\n")

        val url = Urls.extract(text, Prefs.getPattern(this)) ?: return

        val key = sbn.key + "|" + url
        if (key == lastKey) return
        lastKey = key

        Toast.makeText(this, "Url cast received: $url", Toast.LENGTH_SHORT).show()

        val pm = packageManager
        val browser = Urls.browserPackage(pm)
        val app = Urls.appPackageFor(pm, url, browser)

        clearMatchedNotifications()
        if (Prefs.autoOpen(this)) {
            val pkg = if (Prefs.autoOpenUseApp(this)) app ?: browser else browser
            openUrl(url, pkg)
        } else {
            try { startActivity(PopupActivity.intent(this, url, browser, app)) }
            catch (_: Exception) { }
        }
    }

    private fun openUrl(url: String, pkg: String?): Boolean {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) setPackage(pkg)
        }
        // No default browser set: show the "Open with" dialog like a normal link tap.
        return try {
            startActivity(if (pkg == null)
                Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else i)
            true
        } catch (_: Exception) { false }
    }
}
