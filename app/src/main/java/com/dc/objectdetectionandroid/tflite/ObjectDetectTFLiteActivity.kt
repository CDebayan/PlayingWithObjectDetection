package com.dc.objectdetectionandroid.tflite

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.dc.objectdetectionandroid.databinding.ActivityObjectDetectTFLiteBinding
import com.dc.objectdetectionandroid.tflite.classifier.Classifier
import com.dc.objectdetectionandroid.tflite.classifier.TFLiteObjectDetectionAPIModel
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class ObjectDetectTFLiteActivity : AppCompatActivity() {
    private var resizedBitmap: Bitmap? = null
    private var detector: Classifier? = null

    private val binding: ActivityObjectDetectTFLiteBinding by lazy {
        ActivityObjectDetectTFLiteBinding.inflate(layoutInflater)
    }
    private var imageUri: Uri? = null

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val TF_OD_API_INPUT_SIZE = 300
    private val TF_OD_API_IS_QUANTIZED = true
    private var TF_OD_API_MODEL_FILE = "tf/ssd_mobilenet.tflite"
    private var TF_OD_API_LABELS_FILE = "file:///android_asset/tf/labelmap.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //initializeInterpreter(this)
        onClickListener()


        if (intent.getStringExtra("type") == "tflite"){
             TF_OD_API_MODEL_FILE = "tf/detect.tflite"
             TF_OD_API_LABELS_FILE = "file:///android_asset/tf/labelmap.txt"
        }else if (intent.getStringExtra("type") == "yolo"){
             TF_OD_API_MODEL_FILE = "other/yolov2_tiny.tflite"
             TF_OD_API_LABELS_FILE = "file:///android_asset/other/yolov2_tiny.txt"
        }else if (intent.getStringExtra("type") == "ssd"){
             TF_OD_API_MODEL_FILE = "other/ssd_mobilenet.tflite"
             TF_OD_API_LABELS_FILE = "file:///android_asset/other/ssd_mobilenet.txt"
        }

        detector = TFLiteObjectDetectionAPIModel.create(
            assets,
            TF_OD_API_MODEL_FILE,
            TF_OD_API_LABELS_FILE,
            TF_OD_API_INPUT_SIZE,
            TF_OD_API_IS_QUANTIZED
        )


    }


    @SuppressLint("SetTextI18n")
    private fun onClickListener() {
        binding.choose.setOnClickListener {
            openGallery()
        }

        binding.detect.setOnClickListener {
            binding.textView.text = ""
            val recognitionList = detector?.recognizeImage(resizedBitmap)
            recognitionList?.let {
                setRect(it)
                for (recognition in it) {
                    val title = recognition.title
                    val confidence = recognition.confidence

                    val result = "$title  $confidence \n\n"

                    binding.textView.text = binding.textView.text.toString() + result
                }
            }

        }
    }

    private fun setRect(results: MutableList<Classifier.Recognition>) {
        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        for (detectedObject in results) {
            val boundingBox = detectedObject.location
            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            canvas.drawRect(boundingBox, paint)
            binding.imageView.setImageBitmap(mutableBitmap)
        }

    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
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
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            resizedBitmap = getResizedBitmap(bitmap,300)
            binding.imageView.setImageBitmap(resizedBitmap)
        }
    }
}