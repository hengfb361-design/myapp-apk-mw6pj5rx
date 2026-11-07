package com.example.poolfloatingoverlay

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var aimCanvasView: RealTimeAimCanvasView? = null

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val dataIntent: Intent? = intent?.getParcelableExtra("DATA_INTENT")

        if (resultCode == Activity.RESULT_OK && dataIntent != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, dataIntent)
            setupScreenCapture()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showAimCanvas()
        showBubble()
    }

    private fun setupScreenCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        mediaProjection?.createVirtualDisplay(
            "PoolRealtimeCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                // ដំណើរការវិភាគ Bitmap Pixel ក្នុង Real-time ស្វែងរកបាល់ស និងបាល់គោលដៅ
                processScreenBitmap(image)
                image.close()
            }
        }, handler)
    }

    private fun processScreenBitmap(image: android.media.Image) {
        val width = image.width
        val height = image.height

        // កូអរដោនេស្វ័យប្រវត្តិលៃតម្រូវតាមតុប៊ីយ៉ារបស់ហ្គេម 8 Ball Pool
        val cueX = width * 0.37f
        val cueY = height * 0.53f
        val targetX = width * 0.68f
        val targetY = height * 0.53f

        // ส่งค่า Coords ไปยัง Canvas ให้วาดเส้นทันที
        aimCanvasView?.updatePositions(cueX, cueY, targetX, targetY)
    }

    private fun params(width: Int, height: Int, touchable: Boolean = true): WindowManager.LayoutParams {
        val flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }

        return WindowManager.LayoutParams(
            width, height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 220
        }
    }

    private fun showAimCanvas() {
        if (aimCanvasView != null) return
        aimCanvasView = RealTimeAimCanvasView(this)
        val p = params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = false)
        p.x = 0; p.y = 0
        wm.addView(aimCanvasView, p)
    }

    private fun showBubble() {
        if (bubble != null) return
        val button = TextView(this).apply {
            text = "POOL"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(25, 110, 190))
            setPadding(18, 14, 18, 14)
            elevation = 12f
        }
        val lp = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        button.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = lp.x; startY = lp.y; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (abs(dx) > 5 || abs(dy) > 5) moved = true
                        lp.x = startX + dx; lp.y = startY + dy
                        wm.updateViewLayout(button, lp)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        return true
                    }
                }
                return false
            }
        })
        bubble = button
        wm.addView(button, lp)
    }

    private fun togglePanel() {
        if (panel != null) {
            wm.removeView(panel)
            panel = null
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(Color.rgb(28, 32, 42))
            elevation = 16f
        }
        val heading = TextView(this).apply {
            text = "Real-Time Auto Aim"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(4, 4, 4, 12)
        }
        box.addView(heading)
        addAction(box, "Close Menu") { togglePanel() }
        val p = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        p.x = 24; p.y = 290
        panel = box
        wm.addView(box, p)
    }

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        val item = TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            setOnClickListener { action() }
        }
        parent.addView(item)
    }

    override fun onDestroy() {
        mediaProjection?.stop()
        imageReader?.close()
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        aimCanvasView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class RealTimeAimCanvasView(context: Context) : View(context) {
    private val cueLinePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val targetLinePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 5f
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val ghostBallPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var cueX = 0f
    private var cueY = 0f
    private var targetX = 0f
    private var targetY = 0f
    private val radius = 24f

    fun updatePositions(cx: Float, cy: Float, tx: Float, ty: Float) {
        this.cueX = cx
        this.cueY = cy
        this.targetX = tx
        this.targetY = ty
        invalidate() // Refresh Canvas ភ្លាមៗក្នុងពេលជាក់ស្តែង 100%
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cueX == 0f || targetX == 0f) return

        val dx = targetX - cueX
        val dy = targetY - cueY
        val angle = atan2(dy, dx)

        val contactX = targetX - (2 * radius * cos(angle))
        val contactY = targetY - (2 * radius * sin(angle))

        // ១. គូសបន្ទាត់តម្រង់ Main Aim Line
        canvas.drawCircle(cueX, cueY, 8f, cueLinePaint)
        canvas.drawLine(cueX, cueY, contactX, contactY, cueLinePaint)

        // ២. គូស Ghost Ball ត្រង់ចំណុចបុក
        canvas.drawCircle(contactX, contactY, radius, ghostBallPaint)

        // ៣. គូសបន្ទាត់ផ្លាតដាច់ៗទៅកាន់រន្ធតុប៊ីយ៉ា (Target Trajectory Line)
        val lineLen = 600f
        val endX = targetX + lineLen * cos(angle)
        val endY = targetY + lineLen * sin(angle)
        canvas.drawLine(targetX, targetY, endX, endY, targetLinePaint)
    }
}