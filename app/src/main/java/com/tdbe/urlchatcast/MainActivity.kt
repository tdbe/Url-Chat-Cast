package com.tdbe.urlchatcast

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView

class MainActivity : Activity() {

    private var watched: MutableSet<String> = mutableSetOf()
    private lateinit var grantButton: Button
    private lateinit var grantOverlayButton: Button
    private lateinit var picker: Button
    private lateinit var status: TextView
    private lateinit var autoOpenBox: CheckBox
    private lateinit var autoOpenAppBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        watched = Prefs.getWatchedPackages(this).toMutableSet()

        // Needed on Android 13+ so the full-screen-intent notification can be posted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad)
        }
        val em = TextView(this).paint.textSize.toInt()
        fun addSpacer() {
            col.addView(Space(this),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, em))
        }

        col.addView(ImageView(this).apply {
            setImageResource(R.drawable.tv_banner)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            maxHeight = (resources.displayMetrics.heightPixels * 0.33).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, 0, pad)
        })

        col.addView(TextView(this).apply { text = "Cast links from any device, using your own chat apps, to a Android 11+ device or TV that runs this app. It forwards the URL when it gets the command via any whitelisted app's public OS notification." })
        col.addView(TextView(this).apply { text = "Options: choice popup, auto-open, web browser vs builtin app, no-defaults OS-choice."
            textSize = 12f})
        addSpacer()

        status = TextView(this).apply { text = statusText() }
        col.addView(status)

        grantButton = Button(this).apply {
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    // fallback: adb shell settings put secure enabled_notification_listeners \
                    //   com.tdbe.urlchatcast/com.tdbe.urlchatcast.Listener
                }
            }
        }
        col.addView(grantButton)

        picker = Button(this).apply {
            setOnClickListener { showAppPicker() }
        }
        updatePickerText()
        col.addView(picker)

        grantOverlayButton = Button(this).apply {
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } catch (e: Exception) {
                    // some TVs lack this settings screen; enable under Settings > Apps > Special access
                }
            }
        }
        col.addView(grantOverlayButton)
        col.addView(TextView(this).apply {
            text = "(Using the Overlay (System Alert Window) because the only (TV-compatible) options to auto-start a foreground action, are either: this, accessibility service apps, or Leanback Launcher Recommendation Cards.)"
            textSize = 12f
        })

        addSpacer()
        col.addView(TextView(this).apply { text = "Trigger word pattern (Not case sensitive. App expects one url containing \"://\" to be found after the text you enter here, or for the url to be (partially) in this text here. Leave blank to open any received url. Anything after the url is considered part of the url.):" })
        col.addView(EditText(this).apply {
            setText(Prefs.getPattern(this@MainActivity))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    Prefs.setPattern(this@MainActivity, s?.toString()?.trim() ?: "")
                }
            })
        })
        addSpacer()

        autoOpenBox = CheckBox(this).apply {
            text = "Checked: Auto open URLs. Unchecked: a custom popup shows up with browser vs dedicated app."
            isChecked = Prefs.autoOpen(this@MainActivity)
            setOnCheckedChangeListener { _, v ->
                Prefs.setAutoOpen(this@MainActivity, v)
                if (v) {
                    autoOpenAppBox.visibility = android.view.View.VISIBLE

                } else {
                    autoOpenAppBox.visibility = android.view.View.GONE
                    autoOpenAppBox.isChecked = false
                    Prefs.setAutoOpenUseApp(this@MainActivity, false)
                }
            }
        }
        col.addView(autoOpenBox)

        autoOpenAppBox = CheckBox(this).apply {
            text = "Checked: Auto open URLs in the dedicated app if it exists, otherwise the web browser. Unchecked: use the web browser."
            isChecked = Prefs.autoOpenUseApp(this@MainActivity)
            visibility = if (Prefs.autoOpen(this@MainActivity))
                android.view.View.VISIBLE else android.view.View.GONE
            setOnCheckedChangeListener { _, v ->
                Prefs.setAutoOpenUseApp(this@MainActivity, v)
            }
        }
        col.addView(autoOpenAppBox)

        addSpacer()
        col.addView(Button(this).apply {
            text = "Simulate receiving a notification"
            setOnClickListener { simulate() }
        })
        col.addView(TextView(this).apply {
            text = "(You must check Url Chat Cast in the app dropdown for Simulate to work.)"
            textSize = 12f
        })

        val scroll = ScrollView(this).apply { addView(col) }
        val footer = TextView(this).apply {
            autoLinkMask = Linkify.WEB_URLS
            text = "https://github.com/tdbe/Url-Chat-Cast"
            gravity = Gravity.CENTER
            movementMethod = LinkMovementMethod.getInstance()
            setPadding(pad, pad / 2, pad, pad)
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(scroll, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(footer)
        })
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.split(":").any { it.startsWith("$packageName/") }
    }

    private fun statusText(): String =
        if (isNotificationListenerEnabled())
            "Url Chat Cast is armed ✅"
        else "Notification access NOT granted! Open android settings, enable Url Chat Cast."

    private fun simulate() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("simulated", "Simulated", NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        var prefix = Prefs.getPattern(this)
        if (prefix.indexOf(":") < 0 && prefix.indexOf("http") < 0)
            prefix += "https:"
        else if (prefix.indexOf(":") < 0 && prefix.indexOf("http") >= 0)
            prefix += ":"
        val text = prefix + "//www.youtube.com"
        nm.notify(System.currentTimeMillis().toInt(),
            Notification.Builder(this, "simulated")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setAutoCancel(true)
                .build())
    }

    private fun updateGrantText() {
        grantButton.text = if (isNotificationListenerEnabled())
            "Notification Access Granted" else "Try to go to Notification Access from here"
    }

    // On Android 10+ holding this permission also lets us start activities from the background.
    private fun updateOverlayText() {
        grantOverlayButton.text = if (Settings.canDrawOverlays(this))
            "Overlay permission granted" else "Try to go to Overlay Permission from here"
    }

    private fun updatePickerText() {
        picker.text = when {
            watched.isEmpty() -> "Listening apps: NONE - pick some!"
            else -> "Listening apps: ${watched.size} (tap to change)"
        }
    }

    private fun showAppPicker() {
        val pm = packageManager
        val entries = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        val labels = entries.map { it.second }.toTypedArray()
        val checked = entries.map { watched.contains(it.first) }.toBooleanArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Listen to notifications from")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val pkg = entries[which].first
                if (isChecked) watched.add(pkg) else watched.remove(pkg)
            }
            .setPositiveButton("Save") { _, _ ->
                Prefs.setWatchedPackages(this, watched)
                updatePickerText()
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }

    override fun onResume() {
        super.onResume()
        status.text = statusText()
        updateGrantText()
        updateOverlayText()
        updatePickerText()
    }
}