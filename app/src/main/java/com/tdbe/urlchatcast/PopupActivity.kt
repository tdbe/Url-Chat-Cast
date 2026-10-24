package com.tdbe.urlchatcast

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class PopupActivity : Activity() {

    companion object {
        // Focus highlight look — tweak freely.
        private const val HIGHLIGHT_INSET_DP = 2f     // gap between button bounds and the purple rect
        private const val HIGHLIGHT_RADIUS_DP = 4f    // corner radius of the purple rect
        private const val HIGHLIGHT_TEXT_PADDING_DP = 16f  // text left/right padding when highlighted
        private const val HIGHLIGHT_COLOR = 0xFF6200EE.toInt()
        private const val HIGHLIGHT_TEXT = 0xFFFFFFFF.toInt()

        private const val EXTRA_URL = "url"
        private const val EXTRA_BROWSER = "browser"
        private const val EXTRA_APP = "app"

        fun intent(ctx: Context, url: String, browser: String?, app: String?): Intent =
            Intent(ctx, PopupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_BROWSER, browser)
                putExtra(EXTRA_APP, app)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val browser = intent.getStringExtra(EXTRA_BROWSER)
        val app = intent.getStringExtra(EXTRA_APP)

        val pad = (12 * resources.displayMetrics.density).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad * 2, pad * 2, pad * 2, pad * 2)
        }
        col.addView(TextView(this).apply {
            text = "URL received"
            textSize = 18f
            setPadding(0, 0, 0, pad)
        })
        col.addView(TextView(this).apply {
            text = url
            textSize = 14f
            setPadding(0, 0, 0, pad * 2)
        })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun addStyled(b: Button, r: LinearLayout, p: Boolean) {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            if (p == true && r.childCount > 0) lp.marginStart = b.paint.textSize.toInt() // 1em gap
            b.focusHighlight()
            r.addView(b, lp)
        }

        if (app != null) {   // hidden when no dedicated app handles this URL
            addStyled(Button(this).apply {
                text = "Open in\ndefault browser"
                setOnClickListener { openAndFinish(url, browser) }
                requestFocus()   // D-pad friendly on TV
            }, row, true)
            addStyled(Button(this).apply {
                text = "Open in\ndefault app"
                setOnClickListener { openAndFinish(url, app) }
            }, row, true)
        } else {
            addStyled(Button(this).apply {
                text = "Open in\ndefault browser"
                setOnClickListener { openAndFinish(url, browser) }
                requestFocus()   // D-pad friendly on TV
            }, row, false)
            /*
            addSpaced(Button(this).apply {
                text = "Cancel"
                setOnClickListener { finish() }
            }, row, true)*/
        }
        col.addView(row)
        /*
        if(app != null){
            val extraRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            addSpaced(Button(this).apply {
                text = "Cancel"
                setOnClickListener { finish() }
            }, extraRow, true)
            col.addView(extraRow)
        }*/

        setContentView(col)
    }

    // DeviceDefault's gray-on-gray focus highlight is nearly invisible; use a solid purple fill instead.
    private fun Button.focusHighlight() {
        val normalBg = background
        val normalText = textColors
        val normalPadding = intArrayOf(paddingLeft, paddingTop, paddingRight, paddingBottom)
        fun apply(has: Boolean) {
            if (has) {
                val d = resources.displayMetrics.density
                background = InsetDrawable(
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = HIGHLIGHT_RADIUS_DP * d
                        setColor(HIGHLIGHT_COLOR)
                    },
                    (HIGHLIGHT_INSET_DP * d).toInt())
                setTextColor(HIGHLIGHT_TEXT)
                // Swapping the background drops the nine-patch's built-in content padding,
                // so give the text its own breathing room while highlighted.
                val hpad = (HIGHLIGHT_TEXT_PADDING_DP * d).toInt()
                setPadding(hpad, normalPadding[1], hpad, normalPadding[3])
            } else {
                background = normalBg
                setTextColor(normalText)
                setPadding(normalPadding[0], normalPadding[1], normalPadding[2], normalPadding[3])
            }
        }
        setOnFocusChangeListener { _, has -> apply(has) }
        // The default button gets focused during construction, before this listener exists;
        // re-sync after layout so the initial focus is visible too.
        post { apply(isFocused) }
    }

    private fun openAndFinish(url: String, pkg: String?) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) setPackage(pkg)
        }
        // No default browser set: show the "Open with" dialog like a normal link tap.
        try { startActivity(if (pkg == null) Intent.createChooser(i, null) else i) }
        catch (_: Exception) { }
        finish()
    }
}