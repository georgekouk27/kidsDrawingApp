package gr.georkouk.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null

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
    }

}