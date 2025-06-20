package com.example.tomatofruitdiseasesscanner
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AppInstructions : AppCompatActivity() {
    private lateinit var instructionImage: ImageView
    private lateinit var textView1: TextView
    private lateinit var textView2: TextView
    private lateinit var textView3: TextView
    private lateinit var prevButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var backImageView: ImageView
    private val instructionImages = arrayOf(
        R.drawable.instruction1,
        R.drawable.instruction2,
        R.drawable.instruction3,
        R.drawable.instruction5,
        R.drawable.instruction4,
        R.drawable.instruction6
    )
    private val instructionTexts1 = arrayOf(
        "1. Plug first the power brick to have a power for generating light and to initialize the camera",
        "4. Press the “Start Scanning” Button to initialize the camera and Start Capturing.",
        "5. Put the Diseased Tomato inside the Capture Chamber.",
        "7. This button is used to recapture the image",
        "7.1 After image processing, this is what type of tomato fruit disease is presented",
        "10. Press this button to get back to Result Image Page"
    )
    private val instructionTexts2 = arrayOf(
        "2.  Press the power button to initialize the user interface",
        "Wait for the camera to initialize, the second page would initialize as the camera is connected.",
        "Make sure that the diseased part of tomato fruit were pointed at the camera",
        "8. The button is where you can find the description of the disease, Causes of the disease, and Recommended Solution",
        "7.2  The confidence level is indicated on how the classification is accurate",
        "10.1 This shows the Description, Causes and Recommended Solution"
    )
    private val instructionTexts3 = arrayOf(
        "3.  Press the button to turn on the lights inside the capture chamber",
        "",
        "6. Now Pressed the button “Capture Image” to capture the photo ",
        "9. The button is use to get back to the start of the interface",
        "",
        ""
    )
    private var currentImageIndex = 0
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

        setContentView(R.layout.activity_app_instructions)
        instructionImage = findViewById(R.id.Instruction_Images)
        textView1 = findViewById(R.id.Text_1)
        textView2 = findViewById(R.id.Text_2)
        textView3 = findViewById(R.id.Text_3)
        prevButton = findViewById(R.id.Prev_Button)
        nextButton = findViewById(R.id.Next_Button)
        backImageView = findViewById(R.id.Back_Button)
        updateInstruction()
        prevButton.setOnClickListener {
            if (currentImageIndex > 0) {
                currentImageIndex--
                updateInstruction()
            }
        }
        nextButton.setOnClickListener {
            if (currentImageIndex < instructionImages.size - 1) {
                currentImageIndex++
                updateInstruction()
            }
        }
        backImageView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun updateInstruction() {
        instructionImage.setImageResource(instructionImages[currentImageIndex])
        textView1.text = instructionTexts1[currentImageIndex]
        textView2.text = instructionTexts2[currentImageIndex]
        textView3.text = instructionTexts3[currentImageIndex]
        // Show/hide buttons based on position
        prevButton.visibility = if (currentImageIndex == 0) View.INVISIBLE else View.VISIBLE
        nextButton.visibility = if (currentImageIndex == instructionImages.size - 1) View.INVISIBLE else View.VISIBLE
    }

}

