package com.example.mykidsdrawingapp

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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.mykidsdrawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var drawingView:DrawingView?=null
    private var mImageCurrentPaint:ImageButton?=null
    private var customDialog:Dialog?=null
    val openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground:ImageView = binding.ivBackground
                imageBackground.setImageURI(result.data?.data)
            }
        }

    val requestPermission:ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.forEach{
                val permissionName=it.key
                val isGranted =it.value
                if (isGranted){
                    Toast.makeText(this,"permission granted you can now read the storage",Toast.LENGTH_LONG).show()
                    val pickIntent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }else{
                    if (permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"you just denied the permission",Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        drawingView=binding.drawingView
        binding.drawingView?.setBrushSize(20.toFloat())
        mImageCurrentPaint=binding.llPaintColors[1] as ImageButton
        mImageCurrentPaint!!.setImageResource(R.drawable.pallete_pressed)

        binding.ibBrush.setOnClickListener { showBrushSizeChooser() }

        binding.ibGallery.setOnClickListener { requestStoragePermission() }

        binding.ibUndo.setOnClickListener { binding.drawingView.onUndoClicked() }

        binding.ibSave.setOnClickListener {
            showProgressDialog()
            if (isReadStorageAllowed()){
                lifecycleScope.launch {
                    val flDrawingView:FrameLayout=findViewById(R.id.fl_layout)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }

    private fun showBrushSizeChooser(){
        val brushdialog=Dialog(this)
        brushdialog.setContentView(R.layout.dialog_brush_size)
        brushdialog.setTitle("set brush size:")
        val smallBTN=brushdialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBTN.setOnClickListener {
            binding.drawingView?.setBrushSize(10.toFloat())
            brushdialog.dismiss()
        }
        val mediumBTN=brushdialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBTN.setOnClickListener {
            binding.drawingView?.setBrushSize(20.toFloat())
            brushdialog.dismiss()
        }
        val largeBTN=brushdialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBTN.setOnClickListener {
            binding.drawingView?.setBrushSize(30.toFloat())
            brushdialog.dismiss()
        }
            brushdialog.show()
    }

     fun paintClicked(view:View){
        if (view!=mImageCurrentPaint){
            val imageButton=view as ImageButton
            val colorTag=imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallete_pressed))
            mImageCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallete_normal))
            mImageCurrentPaint=view
        }
    }

    private fun showRationaleDialog(title:String,message:String){
        val builder:AlertDialog.Builder=AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){dialog,_ ->dialog.dismiss()}
        builder.create().show()
    }

    private fun isReadStorageAllowed():Boolean{
        var result=ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("kids drawing app","app needs your permission to access storage")
        }else{
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun getBitmapFromView(view: View):Bitmap{
        val returnbitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas= Canvas(returnbitmap)
        val bgBackground=view.background
        if (bgBackground != null){
            bgBackground.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnbitmap
    }

    private suspend fun saveBitmapFile(mBitmap:Bitmap?):String{
        var result=""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes=ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f=File(externalCacheDir?.absoluteFile.toString() +
                    File.separator +"kidsDrawingApp_" + System.currentTimeMillis()/1000 + ".png")
                    val fo=FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result=f.absolutePath

                    runOnUiThread {
                        cancelDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"file was saved on: $result",Toast.LENGTH_SHORT).show()
                            shareFile(result)
                        }else{
                            Toast.makeText(this@MainActivity,"something went wrong",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e:Exception){
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customDialog= Dialog(this)
        customDialog?.setContentView(R.layout.dialog_custom_progress)
        customDialog?.show()
    }

    private fun cancelDialog(){
        if (customDialog!=null){
            customDialog?.dismiss()
            customDialog=null
        }
    }

    private fun shareFile(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri ->
            val shareIntent=Intent()
            shareIntent.action=Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type="image/png"
            startActivity(Intent.createChooser(shareIntent,"share"))
        }
    }




}