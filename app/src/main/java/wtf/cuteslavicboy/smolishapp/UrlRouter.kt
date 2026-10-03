package wtf.cuteslavicboy.smolishapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.net.URISyntaxException

class UrlRouter(private val activity: Activity) {

    private var inAuthFlow = false

    fun resetAuthFlow() {
        inAuthFlow = false
    }

    fun route(url: Uri): Boolean {
        when (url.scheme?.lowercase()) {
            "http", "https" -> {
                val host = url.host
                when {
                    isAppHost(host) -> inAuthFlow = false
                    isGoogleHost(host) || isAuthHost(host) -> inAuthFlow = true
                    !inAuthFlow -> {
                        openExternally(Intent(Intent.ACTION_VIEW, url))
                        return true
                    }
                }
                return false
            }

            "intent" -> return openIntentUri(url)

            "mailto", "tel", "sms", "geo" -> {
                openExternally(Intent(Intent.ACTION_VIEW, url))
                return true
            }

            else -> return true
        }
    }

    private fun openIntentUri(url: Uri): Boolean {
        val intent = try {
            Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
        } catch (e: URISyntaxException) {
            return true
        }
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.component = null
        intent.selector = null
        if (openExternally(intent)) return true

        val fallback = intent.getStringExtra("browser_fallback_url")?.let { Uri.parse(it) }
        val scheme = fallback?.scheme?.lowercase()
        if (fallback != null && (scheme == "http" || scheme == "https")) return route(fallback)
        return true
    }

    fun deepLinkFrom(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val scheme = data.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (!isAppHost(data.host)) return null
        return data.toString()
    }

    private fun isAppHost(host: String?): Boolean = hostMatches(host, setOf(HOST))

    private fun isAuthHost(host: String?): Boolean = hostMatches(host, AUTH_HOSTS)

    private fun isGoogleHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return GOOGLE_HOST_REGEX.containsMatchIn(h)
    }

    private fun hostMatches(host: String?, set: Set<String>): Boolean {
        val h = host?.lowercase() ?: return false
        return set.any { h == it || h.endsWith(".$it") }
    }

    private fun openExternally(intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        private const val HOST = "smolish.com"
        private val AUTH_HOSTS = setOf("discord.com", "discordapp.com", "hcaptcha.com")
        private val GOOGLE_HOST_REGEX = Regex(
            "(^|\\.)(google\\.[a-z]{2,3}(\\.[a-z]{2})?|gstatic\\.com|googleapis\\.com|" +
                    "googleusercontent\\.com|youtube\\.com|recaptcha\\.net)$"
        )
    }
}