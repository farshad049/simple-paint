package com.example.mykidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context:Context,attrs:AttributeSet):View(context,attrs) {
    private var mDrawpath:CustomPath?=null
    private var mCanvasBitmap:Bitmap?=null
    private var mDrawpaint:Paint?=null
    private var mCanvasPaint:Paint?=null
    private var mBrushsize:Float=0.toFloat()
    private var color=Color.BLACK
    private var canvas:Canvas?=null
    private val mPath=ArrayList<CustomPath>()
    private val mUndoPath=ArrayList<CustomPath>()

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        mDrawpaint= Paint()
        mDrawpath=CustomPath(color,mBrushsize)
        mDrawpaint!!.color=color
        mDrawpaint!!.style=Paint.Style.STROKE
        mDrawpaint!!.strokeJoin=Paint.Join.ROUND
        mDrawpaint!!.strokeCap=Paint.Cap.ROUND
        mCanvasPaint= Paint(Paint.DITHER_FLAG)

    }

    override fun onSizeChanged(w: Int, h: Int, wprev: Int, hprev: Int) {
        super.onSizeChanged(w, h, wprev, hprev)
         mCanvasBitmap= Bitmap.createBitmap(w ,h ,Bitmap.Config.ARGB_8888)
        canvas= Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!,0f,0f,mCanvasPaint)
        for (path in mPath){
            mDrawpaint!!.strokeWidth=path.BrushThikness
            mDrawpaint!!.color=path.color
            canvas.drawPath(path,mDrawpaint!!)
        }
        if (!mDrawpath!!.isEmpty){
            mDrawpaint!!.strokeWidth=mDrawpath!!.BrushThikness
            mDrawpaint!!.color=mDrawpath!!.color
            canvas.drawPath(mDrawpath!!,mDrawpaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX=event?.x
        val touchY=event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawpath!!.color=color
                mDrawpath!!.BrushThikness=mBrushsize
                mDrawpath!!.reset()
                mDrawpath!!.moveTo(touchX,touchY)
            }
            MotionEvent.ACTION_MOVE->{
                mDrawpath!!.lineTo(touchX,touchY)
            }
            MotionEvent.ACTION_UP ->{
                mPath.add(mDrawpath!!)
                mDrawpath=CustomPath(color,mBrushsize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setBrushSize(newsize:Float){
        mBrushsize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newsize,resources.displayMetrics)
        mDrawpaint!!.strokeWidth=mBrushsize
    }

    fun setColor(newColor:String){
        color=Color.parseColor(newColor)
        mDrawpaint!!.color=color
    }

    fun onUndoClicked(){
        if (mPath.size > 0){
            mUndoPath.add(mPath.removeAt(mPath.size - 1))
        }
        invalidate()
    }

    internal inner class CustomPath(var color:Int ,var BrushThikness:Float):Path()
}