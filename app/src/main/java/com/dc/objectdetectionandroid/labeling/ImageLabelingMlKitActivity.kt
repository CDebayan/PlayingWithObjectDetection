package com.dc.objectdetectionandroid.labeling

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.dc.objectdetectionandroid.databinding.ActivityImageLabelingMlKitBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class ImageLabelingMlKitActivity : AppCompatActivity() {
    private val binding : ActivityImageLabelingMlKitBinding by lazy {
        ActivityImageLabelingMlKitBinding.inflate(layoutInflater)
    }
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        onClickListener()
    }

    private fun onClickListener() {
        binding.choose.setOnClickListener {
            openGallery()
        }

        binding.detect.setOnClickListener {
            //analyzeObjects()

            custom()
        }
    }

    fun custom(){
        val localModel =
            LocalModel.Builder()
                .setAssetFilePath("mlkit/object_detection_mobile.tflite")
                .build()

        val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localModel)
                .setConfidenceThreshold(0.5f)
                .setMaxResultCount(5)
                .build()

        val imageLabeler =
            ImageLabeling.getClient(customImageLabelerOptions)
        val image = InputImage.fromFilePath(this, imageUri!!)
        imageLabeler.process(image)
            .addOnSuccessListener { labels ->
                for (label in labels) {
                    val text = label.text
                    val confidence = label.confidence
                    val index = label.index

                    val result = "$text  $confidence \n\n"

                    binding.textView.text = binding.textView.text.toString() + result
                }
            }.addOnFailureListener {
                print(it.message)
            }

    }

    @SuppressLint("SetTextI18n")
    private fun analyzeObjects() {
        binding.textView.text = ""
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, imageUri!!)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            labeler.process(image)
                .addOnSuccessListener { labels ->

                    for (label in labels) {
                        val text = label.text
                        val confidence = label.confidence
                        val index = label.index

                        val result = "$text  $confidence \n\n"

                        binding.textView.text = binding.textView.text.toString() + result
                    }
                }
        } catch (e: IOException) {
            e.printStackTrace()
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