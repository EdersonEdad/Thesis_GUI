package com.example.tomatofruitdiseasesscanner
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.example.tomatofruitdiseasesscanner.ml.TomatoFruit
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
@Suppress("DEPRECATION")
class CaptureImages : AppCompatActivity() {
    private lateinit var file: File
    private lateinit var appDir: File
    private var picturesDir: File? = null
    private lateinit var filename: String
    private lateinit var folderName: String
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    @SuppressLint("MissingInflatedId", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_images)
        // Immersive mode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        // Request storage permission
        requestStoragePermission()
        webView = findViewById(R.id.Live_Preview)
        val imageCapture: Button = findViewById(R.id.Image_Capture)
        val backButton: ImageView = findViewById(R.id.Back_Button)
        val invalidText: TextView = findViewById(R.id.Invalid_Image)
        if (intent.getBooleanExtra("START_STREAMING", false)) {
            startImageStream()
        }
        imageCapture.setOnClickListener {
            handler.postDelayed({ captureImageFromPiStream() }, 2000)
        }
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
        }
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun startImageStream() {
        webView.webViewClient = WebViewClient()

        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.loadUrl("http://192.168.1.100:8889/cam")
        webView.scaleY = 1.7f
    }
    private fun captureImageFromPiStream() {
        val imageUrl = "http://192.168.1.100:8881/snapshot.jpg"
        val maxRetries = 3
        val retryDelayMillis = 500L
        Thread {
            var attempt = 0
            var bitmap: Bitmap? = null
            while (attempt < maxRetries && bitmap == null) {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.requestMethod = "GET"
                    connection.doInput = true
                    connection.useCaches = false
                    connection.connect()
                    if (connection.responseCode == HttpURLConnection.HTTP_OK &&
                        connection.contentType?.contains("image") == true
                    ) {
                        val inputStream = BufferedInputStream(connection.inputStream)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null) {
                            break
                        }
                    }
                    attempt++
                    Thread.sleep(retryDelayMillis)
                } catch (e: Exception) {
                    attempt++
                    Thread.sleep(retryDelayMillis)
                }
            }
            if (bitmap == null) {
                return@Thread
            }
            runOnUiThread {
                saveImageToGallery(bitmap)
                processImage(bitmap)
            }
        }.start()
    }
    private fun saveImageToGallery(bitmap: Bitmap) {
        folderName = "TomatoScanner"
        filename = "Tomato_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }.jpg"
        picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        appDir = File(picturesDir, folderName)
        if (!appDir.exists()) appDir.mkdirs()
        file = File(appDir, filename)
        try {
            val outputStream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun processImage(bitmap: Bitmap) {
        val resizedBitmap = bitmap.scale(224, 224)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
        try {
            val model = TomatoFruit.newInstance(this)
            val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            inputFeature.loadBuffer(byteBuffer)
            val outputs = model.process(inputFeature)
            val result = outputs.outputFeature0AsTensorBuffer.floatArray
            model.close()
            val predictedIndex = result.indices.maxByOrNull { result[it] } ?: -1
            val confidence = result.getOrNull(predictedIndex) ?: 0f
            val classNames = arrayOf("Healthy", "Sun Scald", "End Rot", "Anthracnose", "Tomato Cracks", "Invalid")
            val predictedClass = classNames.getOrNull(predictedIndex) ?: "Unknown"
            if (predictedClass == "Invalid") {
                val invalidText: TextView = findViewById(R.id.Invalid_Image)
                invalidText.text = "Invalid Image"
                invalidText.visibility = View.VISIBLE
                deleteImage(file.absolutePath)
                handler.postDelayed({
                    invalidText.visibility = View.GONE
                }, 2500)
                return
            }
            val intent = Intent(this, ImageResults::class.java).apply {
                putExtra("imageFilePath", file.absolutePath)
                putExtra("predictedClass", predictedClass)
                putExtra("confidence", confidence)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputImageWidth = 224
        val inputImageHeight = 224
        val inputChannels = 3
        val resizedBitmap = bitmap.scale(inputImageWidth, inputImageHeight)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * inputChannels)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputImageWidth * inputImageHeight)
        resizedBitmap.getPixels(pixels, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageHeight)
        var pixelIndex = 0
        for (i in 0 until inputImageHeight) {
            for (j in 0 until inputImageWidth) {
                val pixelValue = pixels[pixelIndex++]
                val r = ((pixelValue shr 16) and 0xFF) / 127.5f - 1
                val g = ((pixelValue shr 8) and 0xFF) / 127.5f - 1
                val b = (pixelValue and 0xFF) / 127.5f - 1
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        return byteBuffer
    }
    private fun deleteImage(imagePath: String) {
        try {
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                val deleted = imageFile.delete()
            }
        } catch (e: Exception) {

        }
    }
    private fun showToast(message: String) {
        handler.post {

        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopImageStream()
    }
    private fun stopImageStream() {
        webView.loadUrl("about:blank")
    }
}
