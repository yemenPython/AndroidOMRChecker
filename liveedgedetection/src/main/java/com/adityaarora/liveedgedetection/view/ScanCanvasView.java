package com.adityaarora.liveedgedetection.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.adityaarora.liveedgedetection.util.ScanUtils;

import java.util.ArrayList;

/**
 * Draws an array of shapes on a canvas
 */
public class ScanCanvasView extends View {

    private final ArrayList<ScanShape> shapes = new ArrayList<>();
    public Bitmap cameraBitmap;
    private boolean cameraBitmapSet=false;
    public ScanCanvasView(Context context) {
        super(context);
    }

    public ScanCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScanCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public class ScanShape {
        private final Shape mShape;
        private final Paint mPaint;
        private final Paint mBorder;

        public ScanShape(Shape shape, Paint paint) {
            mShape = shape;
            mPaint = paint;
            mBorder = null;
        }

        public ScanShape(Shape shape, Paint paint, Paint border) {
            mShape = shape;
            mPaint = paint;
            mBorder = border;
            mBorder.setStyle(Paint.Style.STROKE);
        }

        public void draw(Canvas canvas) {
            mShape.draw(canvas, mPaint);

            if (mBorder != null) {
                mShape.draw(canvas, mBorder);
            }
        }

        public Shape getShape() {
            return mShape;
        }
    }
    public void unsetCameraBitmap() {
        cameraBitmapSet = false;
    }
    public void setCameraBitmap(Bitmap bm) {
        this.cameraBitmap = bm;
        cameraBitmapSet = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw the frame.
        super.onDraw(canvas);
        if(cameraBitmapSet) {
            if(cameraBitmap.getWidth() != this.getWidth() || cameraBitmap.getHeight() != this.getHeight())
                cameraBitmap = ScanUtils.resizeToScreenContentSize(cameraBitmap, this.getWidth(),this.getHeight());
            canvas.drawBitmap(cameraBitmap, 0, 0, null);
            return;
        }

        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        for (ScanShape s : shapes) {
            s.getShape().resize(contentWidth, contentHeight);
            s.draw(canvas);
        }

    }

    public void addShape(Shape shape, Paint paint, Paint border) {
        ScanShape scanShape = new ScanShape(shape, paint, border);
        shapes.add(scanShape);
    }

//    public ScanShape addShape(Shape shape, Paint paint) {
//        ScanShape scanShape = new ScanShape(shape, paint);
//        shapes.add(scanShape);
//        return scanShape;
//    }
//
//    public void removeShape(ScanShape shape) {
//        shapes.remove(shape);
//    }
//
//    public void removeShape(int index) {
//        shapes.remove(index);
//    }

    public void clear() {
        shapes.clear();
    }
}