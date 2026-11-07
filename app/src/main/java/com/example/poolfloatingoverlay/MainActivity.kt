package com.example.poolfloatingoverlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestOverlay = 701
    private val requestMediaProjection = 702
    private val poolPackageName = "com.miniclip.eightballpool"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 36, 36, 36)
            setBackgroundColor(Color.rgb(20, 25, 35))
        }

        val title = TextView(this).apply {
            text = "Real-Time Auto Aim Tool"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val info = TextView(this).apply {
            text = "ប្រព័ន្ធចាប់យករូបភាពអេក្រង់ និងគណនាបន្ទាត់ Auto Aim ស្វ័យប្រវត្តិ 100%"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 24)
        }

        val startButton = Button(this).apply {
            text = "១. បើក Floating Auto Aim"
            setBackgroundColor(Color.rgb(35, 135, 75))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    requestScreenCapturePermission()
                } else {
                    val uri = Uri.parse("package:$packageName")
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri),
                        requestOverlay
                    )
                }
            }
        }

        val launchGameButton = Button(this).apply {
            text = "២. ចូលលេង 8 Ball Pool"
            setBackgroundColor(Color.rgb(25, 110, 190))
            setTextColor(Color.WHITE)
            setOnClickListener {
                launch8BallPool()
            }
        }

        val stopButton = Button(this).apply {
            text = "បិទប្រព័ន្ធทั้งหมด"
            setBackgroundColor(Color.rgb(180, 50, 50))
            setTextColor(Color.WHITE)
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "បានបិទប្រព័ន្ធ", Toast.LENGTH_SHORT).show()
            }
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 12, 0, 12)
        }

        root.addView(title)
        root.addView(info)
        root.addView(startButton, lp)
        root.addView(launchGameButton, lp)
        root.addView(stopButton, lp)

        setContentView(root)
    }

    private fun requestScreenCapturePermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), requestMediaProjection)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestMediaProjection && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, OverlayService::class.java).apply {
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA_INTENT", data)
            }
            startService(intent)
            Toast.makeText(this, "Floating Auto Aim បានដំណើរការ!", Toast.LENGTH_SHORT).show()
            finish()
        } else if (requestCode == requestOverlay && Settings.canDrawOverlays(this)) {
            requestScreenCapturePermission()
        }
    }

    private fun launch8BallPool() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(poolPackageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$poolPackageName")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$poolPackageName")))
            }
        }
    }
}