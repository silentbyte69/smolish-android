package wtf.cuteslavicboy.smolishapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageButton
import kotlin.math.abs

class ReloadButtonController(
    private val activity: Activity,
    private val button: ImageButton,
) {

    fun setup() {
        setupDrag()
        restorePosition()
    }

    fun restorePosition() {
        button.post { applyPosition() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startLeft = 0
        var startTop = 0
        var moved = false

        button.setOnTouchListener listener@{ v, event ->
            val container = v.parent as? FrameLayout ?: return@listener false
            val lp = v.layoutParams as? FrameLayout.LayoutParams ?: return@listener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startLeft = v.left
                    startTop = v.top
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) moved = true
                    if (moved) {
                        lp.gravity = Gravity.TOP or Gravity.START
                        lp.leftMargin = (startLeft + dx).toInt().coerceIn(0, maxOf(0, container.width - v.width))
                        lp.topMargin = (startTop + dy).toInt().coerceIn(0, maxOf(0, container.height - v.height))
                        v.layoutParams = lp
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        prefs().edit()
                            .putInt(KEY_RELOAD_X, lp.leftMargin)
                            .putInt(KEY_RELOAD_Y, lp.topMargin)
                            .apply()
                    } else {
                        v.performClick()
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun applyPosition() {
        val prefs = prefs()
        if (!prefs.contains(KEY_RELOAD_X)) return
        val container = button.parent as? FrameLayout ?: return
        val lp = button.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.gravity = Gravity.TOP or Gravity.START
        lp.leftMargin = prefs.getInt(KEY_RELOAD_X, 0).coerceIn(0, maxOf(0, container.width - button.width))
        lp.topMargin = prefs.getInt(KEY_RELOAD_Y, 0).coerceIn(0, maxOf(0, container.height - button.height))
        button.layoutParams = lp
    }

    private fun prefs() = activity.getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_UI = "ui"
        private const val KEY_RELOAD_X = "reload_x"
        private const val KEY_RELOAD_Y = "reload_y"
    }
}