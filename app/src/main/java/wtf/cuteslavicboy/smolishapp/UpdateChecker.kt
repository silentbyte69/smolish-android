package wtf.cuteslavicboy.smolishapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val activity: Activity) {

    fun check() {
        val current = currentVersion() ?: return

        Thread {
            val release = try {
                fetchLatestRelease()
            } catch (e: Exception) {
                null
            } ?: return@Thread

            if (!isNewerVersion(release.tag, current)) return@Thread

            val prompted = activity.getSharedPreferences(PREFS_UPDATE, Activity.MODE_PRIVATE)
                .getString(KEY_PROMPTED_TAG, null)
            if (prompted == release.tag) return@Thread

            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                showUpdateDialog(release)
            }
        }.start()
    }

    private fun currentVersion(): String? = try {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            if (obj.optBoolean("draft") || obj.optBoolean("prerelease")) return null

            val tag = obj.optString("tag_name").trim()
            if (tag.isEmpty()) return null

            val apkUrl = obj.optJSONArray("assets")?.let { assets ->
                (0 until assets.length()).mapNotNull { i ->
                    val asset = assets.optJSONObject(i) ?: return@mapNotNull null
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) asset.optString("browser_download_url")
                        .takeIf { it.isNotEmpty() } else null
                }
            }?.firstOrNull()

            return ReleaseInfo(
                tag = tag,
                notes = obj.optString("body").trim(),
                url = apkUrl ?: obj.optString("html_url").takeIf { it.isNotEmpty() }
                    ?: RELEASES_URL,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = versionParts(latest)
        val c = versionParts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrNull(i) ?: 0
            val b = c.getOrNull(i) ?: 0
            if (a != b) return a > b
        }
        return false
    }

    private fun versionParts(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }

    private fun showUpdateDialog(release: ReleaseInfo) {
        activity.getSharedPreferences(PREFS_UPDATE, Activity.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROMPTED_TAG, release.tag)
            .apply()

        val message = buildString {
            append(activity.getString(R.string.update_available, release.tag))
            if (release.notes.isNotEmpty()) {
                append("\n\n")
                append(release.notes)
            }
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.update_title)
            .setMessage(message)
            .setPositiveButton(R.string.update_now) { _, _ ->
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.url)))
                } catch (e: Exception) {
                }
            }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }

    private data class ReleaseInfo(
        val tag: String,
        val notes: String,
        val url: String,
    )

    companion object {
        private const val PREFS_UPDATE = "update"
        private const val KEY_PROMPTED_TAG = "prompted_tag"
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/silentbyte69/smolish-android/releases/latest"
        private const val RELEASES_URL =
            "https://github.com/silentbyte69/smolish-android/releases"
    }
}