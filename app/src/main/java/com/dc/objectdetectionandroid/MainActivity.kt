package com.dc.objectdetectionandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dc.objectdetectionandroid.mlkit.ObjectDetectMlKitActivity
import com.dc.objectdetectionandroid.tflite.ObjectDetectTFLiteActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startActivity(Intent(this, ObjectDetectTFLiteActivity::class.java))
    }
}