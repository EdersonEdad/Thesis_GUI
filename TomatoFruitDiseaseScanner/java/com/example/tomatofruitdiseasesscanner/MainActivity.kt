package com.example.tomatofruitdiseasesscanner
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var startScanningButton: Button
    private lateinit var deviceInstructionButton: Button
    private lateinit var loadingBar: ProgressBar
    private lateinit var loadingText: TextView
    private val loadingDotsHandler = Handler(Looper.getMainLooper())
    private var dotCount = 0
    private val maxDots = 3

    private val imageResources = arrayOf(
        R.drawable.kamatis1,
        R.drawable.kamatis2,
        R.drawable.kamatis3,
        R.drawable.kamatis4,
        R.drawable.kamatis5
    )

    private var currentIndex = 0
    private val imageSwitchDelay: Long = 3000
    private val handler = Handler(Looper.getMainLooper())

    private val connectionTimeoutHandler = Handler(Looper.getMainLooper())
    private val connectionTimeout: Long = 15000 // 15 seconds

    private val imageSwitcher = object : Runnable {
        override fun run() {
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 500
                fillAfter = true
            }

            val fadeIn = AlphaAnimation(0f, 1f).apply {
                duration = 1000
                fillAfter = true
            }

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    currentIndex = (currentIndex + 1) % imageResources.size
                    imageView.setImageResource(imageResources[currentIndex])
                    imageView.startAnimation(fadeIn)
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            imageView.startAnimation(fadeOut)
            handler.postDelayed(this, imageSwitchDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        startScanningButton = findViewById(R.id.Start_Scanning)
        deviceInstructionButton = findViewById(R.id.Instruction)
        imageView = findViewById(R.id.kamatisbg)
        loadingBar = findViewById(R.id.loading_bar)
        loadingText = findViewById(R.id.loading_text)
        imageView.setImageResource(imageResources[currentIndex])
        handler.postDelayed(imageSwitcher, imageSwitchDelay)

        startScanningButton.setOnClickListener {
            showLoadingScreen()
            waitForConnectionAndStart()
        }

        deviceInstructionButton.setOnClickListener {
            val instructionGui = Intent(this, AppInstructions::class.java)
            startActivity(instructionGui)
            finish()
        }
    }

    private fun showLoadingScreen() {
        startScanningButton.visibility = View.GONE
        deviceInstructionButton.visibility = View.GONE
        loadingBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        startLoadingDots()
    }

    private fun waitForConnectionAndStart() {
        val checkHandler = Handler(Looper.getMainLooper())

        val checkRunnable = object : Runnable {
            override fun run() {
                if (isConnectedToEsp32()) {
                    stopLoadingDots()
                    connectionTimeoutHandler.removeCallbacksAndMessages(null)
                    val intent = Intent(this@MainActivity, CaptureImages::class.java)
                    intent.putExtra("START_STREAMING", true)
                    startActivity(intent)
                    finish()
                } else {
                    checkHandler.postDelayed(this, 2000)
                }
            }
        }

        checkHandler.post(checkRunnable)

        connectionTimeoutHandler.postDelayed({
            checkHandler.removeCallbacksAndMessages(null)
            stopLoadingDots()
            showConnectionFailed()
        }, connectionTimeout)
    }

    private fun isConnectedToEsp32(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return true
        }
        return false
    }
    private fun startLoadingDots() {
        loadingDotsHandler.post(object : Runnable {
            override fun run() {
                val dots = ".".repeat(dotCount % (maxDots + 1))
                loadingText.text = "Connecting$dots"
                dotCount++
                loadingDotsHandler.postDelayed(this, 500)
            }
        })
    }
    private fun stopLoadingDots() {
        loadingDotsHandler.removeCallbacksAndMessages(null)
    }
    private fun showConnectionFailed() {
        stopLoadingDots()
        loadingText.text = "Failed to Connect"
        loadingText.setTextColor(getColor(android.R.color.holo_red_light))
        Handler(Looper.getMainLooper()).postDelayed({
            loadingText.text = ""
            loadingText.setTextColor(getColor(android.R.color.white))
            startScanningButton.visibility = View.VISIBLE
            deviceInstructionButton.visibility = View.VISIBLE
            loadingBar.visibility = View.GONE
        }, 2000)
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(imageSwitcher)
    }
}
