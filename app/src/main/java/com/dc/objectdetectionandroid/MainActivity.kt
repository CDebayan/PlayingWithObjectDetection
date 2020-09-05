package com.dc.objectdetectionandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dc.objectdetectionandroid.databinding.ActivityMainBinding
import com.dc.objectdetectionandroid.labeling.ImageLabelingMlKitActivity
import com.dc.objectdetectionandroid.mlkit.ObjectDetectMlKitActivity
import com.dc.objectdetectionandroid.tflite.ObjectDetectTFLiteActivity

class MainActivity : AppCompatActivity() {
    val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.labeling.setOnClickListener {
            startActivity(Intent(this, ImageLabelingMlKitActivity::class.java))
        }

        binding.mlKit.setOnClickListener {
            startActivity(Intent(this, ObjectDetectMlKitActivity::class.java))
        }

        binding.tfLite.setOnClickListener {
            val intent = Intent(this, ObjectDetectTFLiteActivity::class.java)
            intent.putExtra("type","tflite")
            startActivity(intent)
        }

        binding.yolo.setOnClickListener {
            val intent = Intent(this, ObjectDetectTFLiteActivity::class.java)
            intent.putExtra("type","yolo")
            startActivity(intent)
        }

        binding.ssd.setOnClickListener {
            val intent = Intent(this, ObjectDetectTFLiteActivity::class.java)
            intent.putExtra("type","ssd")
            startActivity(intent)
        }

    }
}