package com.example.sketch

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var drawingView: DrawingView
    private lateinit var brushButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var colorPickerButton: ImageButton
    private lateinit var galleryButton: ImageButton
    private lateinit var saveButton: ImageButton

    private lateinit var requestPermission: ActivityResultLauncher<Array<String>>
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            findViewById<ImageView>(R.id.image_view).setImageURI(result.data?.data)
        }

    // Define color buttons
    private lateinit var orangeButton: ImageButton
    private lateinit var redButton: ImageButton
    private lateinit var greenButton: ImageButton
    private lateinit var blueButton: ImageButton
    private lateinit var purpleButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawingView = findViewById(R.id.drawing_view)
        brushButton = findViewById(R.id.brush_button)
        undoButton = findViewById(R.id.undo_button)
        colorPickerButton = findViewById(R.id.color_picker_button)
        galleryButton = findViewById(R.id.gallery_button)
        saveButton = findViewById(R.id.save_button)

        // Initialize color buttons
        orangeButton = findViewById(R.id.orange_button)
        redButton = findViewById(R.id.red_button)
        greenButton = findViewById(R.id.green_button)
        blueButton = findViewById(R.id.blue_button)
        purpleButton = findViewById(R.id.purple_button)

        drawingView.changeBrushSize(10.3f)
        brushButton.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        // Set click listeners for color buttons
        orangeButton.setOnClickListener(this)
        redButton.setOnClickListener(this)
        greenButton.setOnClickListener(this)
        blueButton.setOnClickListener(this)
        purpleButton.setOnClickListener(this)

        undoButton.setOnClickListener(this)
        colorPickerButton.setOnClickListener(this)
        galleryButton.setOnClickListener(this)
        saveButton.setOnClickListener(this)

        requestPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {

                    val permissionName = it.key
                    val isGranted = it.value

                    if (isGranted && permissionName == Manifest.permission.READ_MEDIA_IMAGES) {
                        Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()

                        val pickIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    } else if (!isGranted && permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        CoroutineScope(IO).launch {
                            saveImage(getBitmapFromView(findViewById(R.id.constraint_layout1)))
                        }
                    } else {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun showBrushSizeChooserDialog() {

        val brushDialog = Dialog(this@MainActivity)
        brushDialog.setContentView(R.layout.dialog_brush)

        val seekBar = brushDialog.findViewById<SeekBar>(R.id.seek_bar)
        val showProgress = brushDialog.findViewById<TextView>(R.id.text_view_Progress)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                showProgress.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                drawingView.changeBrushSize(seekBar!!.progress.toFloat())
            }
        })

        brushDialog.show()
    }

    override fun onClick(view: View?) {

        when (view?.id) {
            R.id.orange_button -> drawingView.setColor("#FF9800")
            R.id.red_button -> drawingView.setColor("#E53935")
            R.id.green_button -> drawingView.setColor("#4CAF50")
            R.id.blue_button -> drawingView.setColor("#5472D3")
            R.id.purple_button -> drawingView.setColor("#9B59D0")

            R.id.undo_button -> drawingView.undo()

            R.id.color_picker_button -> {
                showColorPickerDialog()
            }

            R.id.gallery_button -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestStoragePermission()
                } else {
                    // get the image
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
            }

            R.id.save_button -> {

                val image = findViewById<ConstraintLayout>(R.id.constraint_layout1)
                val bitmap = getBitmapFromView(image)

                lifecycleScope.launch(IO) {
                    saveImage(bitmap)
                }

            }
        }
    }

    private fun showColorPickerDialog() {

        val colorPickerDialog = AmbilWarnaDialog(
            this@MainActivity,
            Color.BLACK,
            object : AmbilWarnaDialog.OnAmbilWarnaListener {

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                    // Do nothing
                }

                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {

                    drawingView.setColor(color)
                }
            })
        colorPickerDialog.show()
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        ) {
            showRationaleDialog()
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showRationaleDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission needed")
        builder.setMessage("Storage permission is needed to access the internal storage")
        builder.setPositiveButton("Yes") { dialog, _ ->
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap {

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private suspend fun saveImage(bitmap: Bitmap) {

        withContext(IO) {

            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString()
            val myDir = File(root, "saved_images")
            if (!myDir.exists()) myDir.mkdirs()

            val n = Random.nextInt(10000)
            val outputFile = File(myDir, "Images-$n.jpg")

            try {
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                withContext(Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Saved: ${outputFile.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}









