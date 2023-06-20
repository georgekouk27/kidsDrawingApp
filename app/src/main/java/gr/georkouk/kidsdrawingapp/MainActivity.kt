package gr.georkouk.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
                if(result.resultCode == RESULT_OK && result.data != null){
                    val ivBackground: ImageView = findViewById(R.id.ivBackground)
                    ivBackground.setImageURI(result.data?.data)
                }
        }

    private val requestPersmission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions -> permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()

                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialize()
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)

        val btSmall: ImageButton = brushDialog.findViewById(R.id.ibSmallBrush)
        btSmall.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val btMedium: ImageButton = brushDialog.findViewById(R.id.ibMediumBrush)
        btMedium.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val btLarge: ImageButton = brushDialog.findViewById(R.id.ibLargeBrush)
        btLarge.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    private fun paintClicked(view: View){
        if(view == imageButtonCurrentPaint){
            return
        }

        val imageButton: ImageButton = view as ImageButton
        val colorTag = imageButton.tag.toString()

        drawingView?.setColor(colorTag)

        imageButton.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        imageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_normal)
        )

        imageButtonCurrentPaint = view
    }

    private fun initialize(){
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(20.toFloat())

        val llPaintColors: LinearLayout = findViewById(R.id.llPaintColors)

        imageButtonCurrentPaint = llPaintColors[1] as ImageButton
        imageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        for (i in 0..8){
            val button: ImageButton = llPaintColors[i] as ImageButton
            button.setOnClickListener { view -> paintClicked(view) }
        }

        val ibBrush: ImageButton = findViewById(R.id.ibBrush)
        ibBrush.setOnClickListener { showBrushSizeChooserDialog() }

        val ibGallery: ImageButton = findViewById(R.id.ibGallery)
        ibGallery.setOnClickListener { requestStoragePermission() }

        val ibUndo: ImageButton = findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener { drawingView?.onClickUndo() }

        val ibSave: ImageButton = findViewById(R.id.ibSave)
        ibSave.setOnClickListener {
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.flDrawingViewContainer)

                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }

    private fun showRationalDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setNegativeButton("Cancel"){ dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationalDialog("Kids Drawing App", "App needs to access your external storage")
        }
        else{
            requestPersmission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun getBitmapFromView(view: View): Bitmap{
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background

        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return bitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap?): String{
        var result = ""

        withContext(Dispatchers.IO){
            if(bitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val file = File(
                        externalCacheDir?.absoluteFile.toString() +
                        File.separator + "kidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(file)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()

                        if(result.isNotEmpty()){
                            Toast.makeText(
                                this@MainActivity,
                                "File saved at $result",
                                Toast.LENGTH_LONG
                            ).show()

                            shareImage(result)
                        }
                        else{
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.setCancelable(false)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(path: String){
        MediaScannerConnection.scanFile(this, arrayOf(path), null){
            path, uri ->

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"

            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}