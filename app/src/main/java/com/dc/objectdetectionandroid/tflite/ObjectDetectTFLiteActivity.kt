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
    private var detector: Classifier? = null
    private val isQuantized = false

    // input image dimensions for the Inception Model
    private val DIM_IMG_SIZE_X = 300
    private val DIM_IMG_SIZE_Y = 300
    private val DIM_PIXEL_SIZE = 1

    // int array to hold image data
    private val intValues: IntArray = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)


    // presets for rgb conversion
    private val IMAGE_MEAN = 127.5f
    private val IMAGE_STD = 127.5f

    // options for model interpreter
    private val tfliteOptions = Interpreter.Options()

    // tflite graph
    private var tflite: Interpreter? = null

    // holds all the possible labels for model
    private var labelList: List<String>? = null

    // holds the selected image data as bytes
    private var imgData: ByteBuffer? = null

    // holds the probabilities of each label for non-quantized graphs
    private var labelProbArray: Array<FloatArray>? = null

    // holds the probabilities of each label for quantized graphs
    private var labelProbArrayB: Array<ByteArray>? = null

    private val binding: ActivityObjectDetectTFLiteBinding by lazy {
        ActivityObjectDetectTFLiteBinding.inflate(layoutInflater)
    }
    private var imageUri: Uri? = null

    // input image dimensions for the Inception Model

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val TF_OD_API_INPUT_SIZE = 300
    private val TF_OD_API_IS_QUANTIZED = true
    private val TF_OD_API_MODEL_FILE = "tf/detect.tflite"
    private val TF_OD_API_LABELS_FILE = "file:///android_asset/tf/labelmap.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //initializeInterpreter(this)
        onClickListener()

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
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            val resizedBitmap = getResizedBitmap(bitmap,300)
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
            binding.imageView.setImageURI(imageUri)
        }
    }








    private fun analyzeObjects() {
       // val bitmap_orig = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

        val bitmap_orig = (binding.imageView.drawable as BitmapDrawable).bitmap
        // resize the bitmap to the required input size to the CNN
        val bitmap: Bitmap = getResizedBitmap(bitmap_orig, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
        // convert bitmap to byte array
        // convert bitmap to byte array
        convertBitmapToByteBuffer(bitmap)
        // pass byte data to the graph
        // pass byte data to the graph
        if (isQuantized) {
            tflite!!.run(imgData, labelProbArrayB)
        } else {
            try {
                tflite!!.run(imgData, labelProbArray)
            } catch (e: Exception) {
                print(e)
            }
        }
        // display the results
        // display the results
        printTopKLabels()
    }

    // print the top labels and respective confidences
    private fun printTopKLabels() {
        // add all results to priority queue
        for (i in labelList!!.indices) {
            print(i)
        }
    }

    // resizes bitmap to given dimensions
    fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
    }



    fun initializeInterpreter(activity: Activity) {

        //initilize graph and labels
        try {
            tflite = Interpreter(loadModelFile(activity), tfliteOptions)
            labelList = loadLabelList(activity)
            // initialize byte array. The size depends if the input data needs to be quantized or not

            // initialize byte array. The size depends if the input data needs to be quantized or not
            imgData = if (isQuantized) {
                ByteBuffer.allocateDirect(
                    DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE
                )
            } else {
                ByteBuffer.allocateDirect(
                    4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE
                )
            }

            imgData?.order(ByteOrder.nativeOrder())

            // initialize probabilities array. The datatypes that array holds depends if the input data needs to be quantized or not

            // initialize probabilities array. The datatypes that array holds depends if the input data needs to be quantized or not
            if (isQuantized) {
                labelProbArrayB = Array(1) { ByteArray(labelList!!.size) }
            } else {
                labelProbArray = Array(1) { FloatArray(labelList!!.size) }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    // loads tflite from file
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = activity.assets.openFd("tf/detect.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // loads the labels from the label txt file in assets into a string array
    @Throws(IOException::class)
    private fun loadLabelList(activity: Activity): List<String>? {
        val labelList: MutableList<String> = ArrayList()
        val reader = BufferedReader(InputStreamReader(activity.assets.open("tf/labelmap.txt")))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.let {
                labelList.add(it)
            }
        }
        reader.close()
        return labelList
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            return
        }
        imgData?.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // loop through all pixels
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                if (isQuantized) {
                    imgData?.put((value shr 16 and 0xFF).toByte())
                    imgData?.put((value shr 8 and 0xFF).toByte())
                    imgData?.put((value and 0xFF).toByte())
                } else {
                    imgData?.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData?.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData?.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
    }

}