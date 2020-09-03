package com.dc.objectdetectionandroid.mlkit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dc.objectdetectionandroid.databinding.ActivityObjectDetectMlKitBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.io.IOException

class ObjectDetectMlKitActivity : AppCompatActivity() {
    private val binding: ActivityObjectDetectMlKitBinding by lazy {
        ActivityObjectDetectMlKitBinding.inflate(layoutInflater)
    }
    private lateinit var objectDetector: ObjectDetector
    private lateinit var customObjectDetectorOptions: CustomObjectDetectorOptions
    private lateinit var localModel: LocalModel
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        localModel =
            LocalModel.Builder()
                .setAssetFilePath("mlkit/mobilenet_v1_1.0_224_quant.tflite")
                .build()

        customObjectDetectorOptions =
            CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.8f)
                .setMaxPerObjectLabelCount(3)
                .build()

        objectDetector =
            ObjectDetection.getClient(customObjectDetectorOptions)

        onClickListener()
    }

    private fun onClickListener() {
        binding.choose.setOnClickListener {
            openGallery()
        }

        binding.detect.setOnClickListener {
            analyzeObjects()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun analyzeObjects() {
        binding.textView.text = ""

        try {
            val image = InputImage.fromFilePath(applicationContext, imageUri!!)
            objectDetector.process(image).addOnFailureListener {
                Log.d("objectTest", it.message.toString())
            }.addOnSuccessListener { results ->
                setRect(results)
                for (detectedObject in results) {
                    Log.d("objectTest", detectedObject.toString())
                    val boundingBox = detectedObject.boundingBox
                    val trackingId = detectedObject.trackingId
                    for (label in detectedObject.labels) {
                        val text = label.text
                        val confidence = label.confidence

                        val result = "$text  $confidence \n\n"

                        binding.textView.text = binding.textView.text.toString() + result

                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun setRect(results: MutableList<DetectedObject>) {
        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        for (detectedObject in results) {
            val boundingBox = detectedObject.boundingBox
            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            canvas.drawRect(boundingBox, paint)
            binding.imageView.setImageBitmap(mutableBitmap)
        }

    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select an image"), 121)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 121) {
            imageUri = data?.data
            binding.imageView.setImageURI(imageUri)
        }
    }
}