package com.home.cams

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

// ===================== EDIT THESE =====================
private const val CAM1_URL = "rtsp://admin:@192.168.1.2:554/live/ch00_0"
private const val CAM2_URL = "rtsp://admin:@192.168.1.6:554/live/ch00_0"
// Camera 02 is dual-lens. true = lenses stacked top/bottom, false = side by side
private const val LENSES_STACKED = true
// Which lens Camera 03 shows: 0 = top/left, 1 = bottom/right
private const val LENS_INDEX = 0
// ======================================================

private class Cam(val title: String, val url: String, val crop: Boolean)

class MainActivity : Activity() {

    private val cams = listOf(
        Cam("Camera 01", CAM1_URL, false),
        Cam("Camera 02", CAM2_URL, false),
        Cam("Camera 03", CAM2_URL, true)
    )

    private lateinit var libVlc: LibVLC
    private val players = ArrayList<MediaPlayer>()
    private val cells = ArrayList<FrameLayout>()
    private val rows = ArrayList<LinearLayout>()
    private lateinit var infoView: TextView
    private var fullscreen: FrameLayout? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        libVlc = LibVLC(this, arrayListOf("--rtsp-tcp", "--network-caching=300"))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        cams.forEach { cells.add(makeCell(it)) }
        infoView = TextView(this).apply {
            text = "OK: fullscreen\nBack: grid"
            setTextColor(Color.LTGRAY)
            textSize = 18f
            gravity = Gravity.CENTER
        }

        row1.addView(cells[0], weighted()); row1.addView(cells[1], weighted())
        row2.addView(cells[2], weighted()); row2.addView(infoView, weighted())
        root.addView(row1, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(row2, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        rows.add(row1); rows.add(row2)

        setContentView(root)
        cells[0].requestFocus()
    }

    private fun weighted() = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)

    private fun makeCell(cam: Cam): FrameLayout {
        val pad = dp(4)
        val cell = FrameLayout(this).apply {
            setPadding(pad, pad, pad, pad)
            clipChildren = true
            clipToPadding = true
            isFocusable = true
            isClickable = true
            setBackgroundColor(0xFF222222.toInt())
        }
        cell.setOnFocusChangeListener { v, focused ->
            v.setBackgroundColor(if (focused) 0xFF4F8CFF.toInt() else 0xFF222222.toInt())
        }
        cell.setOnClickListener {
            fullscreen = if (fullscreen == null) cell else null
            updateVisibility()
        }

        val vlcLayout = VLCVideoLayout(this)
        cell.addView(vlcLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val label = TextView(this).apply {
            text = cam.title
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(0x99000000.toInt())
        }
        cell.addView(label, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START))

        val mp = MediaPlayer(libVlc)
        // TextureView (last arg) so cropping/clipping works
        mp.attachViews(vlcLayout, null, false, true)
        mp.videoScale = if (cam.crop) MediaPlayer.ScaleType.SURFACE_FILL
                        else MediaPlayer.ScaleType.SURFACE_BEST_FIT
        players.add(mp)

        if (cam.crop) {
            // Make the video twice as big in the lens direction and shift it so only one lens shows
            cell.addOnLayoutChangeListener { v, l, t, r, b, _, _, _, _ ->
                val w = r - l - v.paddingLeft - v.paddingRight
                val h = b - t - v.paddingTop - v.paddingBottom
                if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
                val lp = vlcLayout.layoutParams as FrameLayout.LayoutParams
                val newW: Int; val newH: Int; val newL: Int; val newT: Int
                if (LENSES_STACKED) { newW = w; newH = h * 2; newL = 0; newT = -h * LENS_INDEX }
                else { newW = w * 2; newH = h; newL = -w * LENS_INDEX; newT = 0 }
                if (lp.width != newW || lp.height != newH || lp.leftMargin != newL || lp.topMargin != newT) {
                    lp.width = newW; lp.height = newH; lp.leftMargin = newL; lp.topMargin = newT
                    vlcLayout.layoutParams = lp
                }
            }
        }

        cell.layoutParams = weighted().apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
        return cell
    }

    private fun updateVisibility() {
        val fs = fullscreen
        cells.forEach { it.visibility = if (fs == null || it === fs) View.VISIBLE else View.GONE }
        infoView.visibility = if (fs == null) View.VISIBLE else View.GONE
        rows.forEach { row ->
            var any = false
            for (i in 0 until row.childCount) if (row.getChildAt(i).visibility == View.VISIBLE) any = true
            row.visibility = if (any) View.VISIBLE else View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        players.forEachIndexed { i, mp ->
            val media = Media(libVlc, Uri.parse(cams[i].url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=300")
            mp.media = media
            media.release()
            mp.play()
        }
    }

    override fun onStop() {
        super.onStop()
        players.forEach { it.stop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        players.forEach { it.detachViews(); it.release() }
        libVlc.release()
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (fullscreen != null) { fullscreen = null; updateVisibility() }
        else super.onBackPressed()
    }
}
