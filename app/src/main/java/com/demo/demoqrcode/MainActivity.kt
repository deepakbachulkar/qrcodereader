package com.demo.demoqrcode

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeReader
import java.io.FileNotFoundException


class MainActivity : AppCompatActivity() {
    var btnGallery: Button? = null
    var btnScan: Button? = null
    var imgScan: ImageView? = null
    var messageText: TextView? = null
    var messageFormat:TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGallery = findViewById(R.id.gallery)

        imgScan = findViewById(R.id.scanImage)

        btnGallery?.setOnClickListener {
            openGallery()
//            galleryLauncher.launch("image/*")
        }

    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val galleryUri = it
        try{
            imgScan?.setImageURI(galleryUri)
            try {
                galleryUri?.let { uri->
                    val inputStream = contentResolver.openInputStream(uri)
                    var bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap == null) {
                        Log.e("TAG", "uri is not a bitmap,$uri")
//                        return
                    }
                    val width = bitmap!!.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    bitmap.recycle()
                    bitmap = null
                    val source = RGBLuminanceSource(width, height, pixels)
                    val bBitmap = BinaryBitmap(HybridBinarizer(source))
                    val reader = MultiFormatReader()
                    try {
                        val result = reader.decode(bBitmap)
                        Toast.makeText(
                            this,
                            "The content of the QR image is: " + result.getText(),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("TAG", "decode exception", e)
                    }
                }

            } catch (e: FileNotFoundException) {
                Log.e("TAG", "can not open file" + galleryUri.toString(), e)
            }
        }catch(e:Exception){
            e.printStackTrace()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.contents == null) {
                Toast.makeText(baseContext, "Cancelled", Toast.LENGTH_SHORT).show()
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message
                messageText!!.text = intentResult.contents
                messageFormat!!.text = intentResult.formatName
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val openGalleryRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                it.data?.data?.let { uri -> decodeQRCode(uri) }
            }
        }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        openGalleryRequest.launch(Intent.createChooser(intent, "Scan Gallery"))
    }

    private fun decodeQRCode(imageUri: Uri) {
        try {
            kotlin.runCatching {
                imgScan?.setImageURI(imageUri)
            }
            val inputStream = this.contentResolver?.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = QRCodeReader()
            val result = reader.decode(binaryBitmap)

            // The QR code content is in result.text
            val qrCodeContent = result.text
            Log.d("Data", "Text -------------> $qrCodeContent")
            openLink(qrCodeContent)
        } catch (e: Exception) {
            // Handle exceptions (e.g., QR code not found)
            e.printStackTrace()
        }
    }

    private fun proceedQrData(qrCodeContent:String){
        Toast.makeText(this, qrCodeContent, Toast.LENGTH_LONG).show()
    }

    private fun openLink(data: String) {
        startActivityIfExists(Intent.ACTION_VIEW, data)
    }

    private fun startActivityIfExists(action: String, uri: String) {
        val intent = Intent(action, Uri.parse(uri))
        startActivityIfExists (intent)
    }

    private fun startActivityIfExists(intent: Intent) {
        intent.apply {
            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity (packageManager) != null) {
            startActivity(intent)
        }
    }

}