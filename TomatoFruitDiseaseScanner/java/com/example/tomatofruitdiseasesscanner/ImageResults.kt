package com.example.tomatofruitdiseasesscanner
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
class ImageResults : AppCompatActivity() {
    private val TAG = "ImageResults"
    private lateinit var file: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        try {
            setContentView(R.layout.activity_image_results)
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating layout: ${e.message}", e)
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val imageView = findViewById<ImageView>(R.id.Result_Image)
        val resultTextView = findViewById<TextView>(R.id.Resulted_Text)
        val confidenceTextView = findViewById<TextView>(R.id.confidence_result)
        val recaptureButton = findViewById<Button>(R.id.Image_Recapture)
        val recommendationButton = findViewById<Button>(R.id.View_Recommendation)
        val imagePath = intent.getStringExtra("imageFilePath")
        val predictedClass = intent.getStringExtra("predictedClass")
        val confidence = intent.getFloatExtra("confidence", 0f)
        if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Invalid or missing image path: $imagePath")
        }
        resultTextView.text = if (predictedClass != null) {
            "Image Results: $predictedClass"
        } else {
            "Image Results: Not available"
        }
        confidenceTextView.text = if (predictedClass != null) {
            "%.2f%%".format(confidence * 100)
        } else {
            "Confidence: Not available"
        }
        // Show and enable recommendation button only for disease results
        if (predictedClass in listOf("Sun Scald", "Anthracnose", "End Rot", "Tomato Cracks")) {
            recommendationButton.visibility = View.VISIBLE
            recommendationButton.isEnabled = true
            recommendationButton.setOnClickListener {
                try {
                    val targetClass = when (predictedClass) {
                        "Sun Scald" -> SunscaldDisease::class.java
                        "Anthracnose" -> AnthracnoseDisease::class.java
                        "End Rot" -> EndRot::class.java
                        "Tomato Cracks" -> FruitCracks::class.java
                        else -> null
                    }
                    if (targetClass != null && !imagePath.isNullOrEmpty()) {
                        val intent = Intent(this, targetClass).apply {
                            putExtra("imageFilePath", imagePath)
                            putExtra("predictedClass", predictedClass)
                            putExtra("confidence", confidence)
                        }
                        startActivity(intent)
                        Log.i(TAG, "Navigated to $predictedClass with imagePath: $imagePath")
                        // Do not call finish() to keep ImageResults active
                    } else {
                        Log.w(
                            TAG,
                            "Invalid navigation: targetClass=$targetClass, imagePath=$imagePath"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to recommendation: ${e.message}", e)
                }
            }
        } else {
            recommendationButton.visibility = View.GONE
            Log.d(TAG, "Recommendation button hidden for predictedClass: $predictedClass")
        }
        recaptureButton.setOnClickListener {
            try {
                if (!imagePath.isNullOrEmpty()) {
                    deleteImageFromGallery(imagePath)
                }
                val intent = Intent(this, CaptureImages::class.java).apply {
                    putExtra("START_STREAMING", true)
                }
                startActivity(intent)
                finish()
                Log.i(TAG, "Navigated to CaptureImages for recapture")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting CaptureImages: ${e.message}", e)
            }
        }
        val backImageView = findViewById<ImageView>(R.id.Back_Button)
        backImageView.setOnClickListener {
            try {
                if (!imagePath.isNullOrEmpty()) {
                    deleteImageFromGallery(imagePath)
                }
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                Log.i(TAG, "Navigated to MainActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MainActivity: ${e.message}", e)
            }
        }
    }
    private fun deleteImageFromGallery(imagePath: String): Boolean {
        val imageFile = File(imagePath)
        return if (imageFile.exists()) {
            val deleted = imageFile.delete()
            if (deleted) {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(imageFile)
                sendBroadcast(mediaScanIntent)
            }
            deleted
        } else {
            false
        }
    }

}
