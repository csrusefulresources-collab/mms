package com.shaheen.appcloner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class MainActivity : Activity() {

    private data class InstalledApp(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )

    private val blue = Color.rgb(49, 95, 232)
    private val ink = Color.rgb(22, 32, 51)
    private val muted = Color.rgb(101, 112, 133)
    private val backgroundColor = Color.rgb(244, 246, 251)
    private val prefs by lazy { getSharedPreferences("app_cloner_data", MODE_PRIVATE) }

    private lateinit var listContainer: LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var statusText: TextView
    private lateinit var savedTabButton: Button
    private lateinit var allAppsTabButton: Button
    private var showingSaved = true
    private var allApps: List<InstalledApp> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 24, 40)
        window.navigationBarColor = Color.rgb(16, 24, 40)
        window.decorView.systemUiVisibility = 0
        buildScreen()
        loadInstalledApps()
        renderApps()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun button(
        label: String,
        description: String = label,
        primary: Boolean = false,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(if (primary) Color.WHITE else ink)
            background = rounded(if (primary) blue else Color.rgb(232, 237, 247), 12)
            minHeight = dp(48)
            minimumHeight = dp(48)
            contentDescription = description
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { onClick() }
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(16, 24, 40), 22)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        header.addView(text("App Cloner", 25f, Color.WHITE, true))
        header.addView(text("Manage your app shortcuts in one place", 14f, Color.rgb(210, 219, 235)))
        val explanation = text(
            "Note: entries are shortcuts to installed apps. They are not independent copies and do not create separate app data.",
            12f, Color.rgb(225, 231, 242)
        )
        explanation.setPadding(0, dp(10), 0, 0)
        header.addView(explanation)
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val searchLabel = text("Search installed apps", 14f, ink, true)
        searchLabel.setPadding(dp(2), dp(14), 0, dp(5))
        root.addView(searchLabel)

        searchBox = EditText(this).apply {
            hint = "Type an app name"
            textSize = 16f
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(muted)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(Color.WHITE, 14)
            contentDescription = "Search installed apps"
            minHeight = dp(52)
        }
        root.addView(searchBox, LinearLayout.LayoutParams(-1, dp(52)))

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(4))
        }
        savedTabButton = button("My Clones", "Show saved app shortcuts", showingSaved) {
            if (!showingSaved) {
                showingSaved = true
                renderApps()
            }
        }
        allAppsTabButton = button("All Apps", "Show all launchable installed apps", !showingSaved) {
            if (showingSaved) {
                showingSaved = false
                renderApps()
            }
        }
        tabs.addView(
            savedTabButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) }
        )
        tabs.addView(
            allAppsTabButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) }
        )
        root.addView(tabs)

        statusText = text("", 13f, muted)
        statusText.setPadding(dp(2), dp(5), dp(2), dp(8))
        statusText.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        root.addView(statusText)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            contentDescription = "Applications list"
        }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = text(
            "For true separate app copies, Android must provide a supported work profile or virtualization environment.",
            11f, muted
        )
        footer.setPadding(dp(2), dp(8), dp(2), dp(4))
        root.addView(footer)

        setContentView(root)
        searchBox.addTextChangedListener(SimpleTextWatcher { renderApps() })
    }

    private fun loadInstalledApps() {
        val found = mutableListOf<InstalledApp>()
        val pm = packageManager
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (info: ApplicationInfo in packages) {
                if (info.packageName == packageName) continue
                val launchIntent = pm.getLaunchIntentForPackage(info.packageName) ?: continue
                try {
                    found.add(
                        InstalledApp(
                            pm.getApplicationLabel(info).toString(),
                            info.packageName,
                            pm.getApplicationIcon(info)
                        )
                    )
                } catch (_: Exception) {
                    // Ignore apps whose label or icon cannot be read.
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not read installed apps: ${e.message}", Toast.LENGTH_LONG).show()
        }
        allApps = found.sortedBy { it.label.lowercase() }
    }

    private fun savedPackages(): MutableSet<String> {
        return prefs.getStringSet("saved_packages", emptySet())?.toMutableSet()
            ?: mutableSetOf()
    }

    private fun renderApps() {
        if (!::listContainer.isInitialized) return
        listContainer.removeAllViews()

        val saved = savedPackages()
        val query = if (::searchBox.isInitialized) searchBox.text.toString().trim() else ""
        val source = if (showingSaved) {
            allApps.filter { saved.contains(it.packageName) }
        } else {
            allApps
        }
        val visible = source.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }

        statusText.text = if (showingSaved) {
            "${visible.size} saved shortcut(s)"
        } else {
            "${visible.size} installed app(s)"
        }
        updateTabStyles()

        if (visible.isEmpty()) {
            val empty = text(
                if (showingSaved) "No shortcuts yet. Open All Apps and add an app."
                else "No matching launchable apps were found.",
                15f, muted
            )
            empty.gravity = Gravity.CENTER
            empty.setPadding(dp(20), dp(35), dp(20), dp(35))
            listContainer.addView(empty, LinearLayout.LayoutParams(-1, -2))
            return
        }

        visible.forEach { app ->
            listContainer.addView(createAppCard(app, saved.contains(app.packageName)))
        }
    }

    private fun updateTabStyles() {
        if (!::savedTabButton.isInitialized || !::allAppsTabButton.isInitialized) return
        savedTabButton.background = rounded(
            if (showingSaved) blue else Color.rgb(232, 237, 247), 12
        )
        savedTabButton.setTextColor(if (showingSaved) Color.WHITE else ink)
        allAppsTabButton.background = rounded(
            if (!showingSaved) blue else Color.rgb(232, 237, 247), 12
        )
        allAppsTabButton.setTextColor(if (!showingSaved) Color.WHITE else ink)
    }

    private fun createAppCard(app: InstalledApp, isSaved: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 18)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = dp(1).toFloat()
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(this).apply {
            setImageDrawable(app.icon)
            contentDescription = "${app.label} app icon"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        top.addView(icon, LinearLayout.LayoutParams(dp(52), dp(52)))

        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(4), 0)
        }
        names.addView(text(app.label, 16f, ink, true))
        val packageLabel = text(app.packageName, 11f, muted)
        packageLabel.maxLines = 2
        names.addView(packageLabel)
        top.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(top)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        actions.addView(
            button("Open", "Open ${app.label}") { launchInstalledApp(app.packageName) },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(4) }
        )
        val savedButtonLabel = if (isSaved) "Remove" else "Add"
        actions.addView(
            button(
                savedButtonLabel,
                if (isSaved) "Remove ${app.label} from My Clones" else "Add ${app.label} to My Clones",
                !isSaved
            ) { toggleSaved(app.packageName) },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )
        actions.addView(
            button("Info", "Open ${app.label} app information") {
                openAppInfo(app.packageName)
            },
            LinearLayout.LayoutParams(0, dp(46), 0.75f).apply { marginStart = dp(4) }
        )
        card.addView(actions)

        val store = button("Open Play Store page", "Open Play Store page for ${app.label}") {
            openStorePage(app.packageName)
        }
        val storeParams = LinearLayout.LayoutParams(-1, dp(44))
        storeParams.topMargin = dp(5)
        card.addView(store, storeParams)

        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
        card.layoutParams = params
        card.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        return card
    }

    private fun toggleSaved(pkg: String) {
        val saved = savedPackages()
        if (saved.contains(pkg)) {
            saved.remove(pkg)
            Toast.makeText(this, "Removed from My Clones", Toast.LENGTH_SHORT).show()
        } else {
            saved.add(pkg)
            Toast.makeText(this, "Added to My Clones", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putStringSet("saved_packages", saved).apply()
        renderApps()
    }

    private fun launchInstalledApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(this, "This app cannot be opened", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "App is not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(pkg: String) {
        try {
            startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:$pkg"))
            )
        } catch (_: Exception) {
            Toast.makeText(this, "App information is not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStorePage(pkg: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (_: Exception) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, "Play Store page could not be opened", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private class SimpleTextWatcher(
    private val changed: () -> Unit
) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        changed()
    }
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
