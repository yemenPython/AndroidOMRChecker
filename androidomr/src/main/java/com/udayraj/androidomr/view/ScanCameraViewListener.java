package com.udayraj.androidomr.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.udayraj.androidomr.R;
import com.udayraj.androidomr.constants.SC;
import com.udayraj.androidomr.enums.ScanHint;
import com.udayraj.androidomr.interfaces.IScanner;
import com.udayraj.androidomr.util.Utils;


public class ScanCameraViewListener implements CvCameraViewListener2 {
    private static final String TAG = ScanCameraViewListener.class.getSimpleName();

    public boolean isAutoCaptureScheduled = false;
    public Map<Integer, Boolean> buttonChecked = new HashMap<>();

//    SurfaceView mSurfaceView;
    private final ScanCanvasView scanCanvasView;
    private final IScanner iScanner;
    private CountDownTimer autoCaptureTimer;
    private int secondsLeft;
    private Camera.Size previewSize;
    private boolean isCapturing = false;


    private Mat inMat;
    private Mat outMat;
    private Mat markerToMatch;
    private Scalar CONTOUR_COLOR = new Scalar(155, 155, 155);


    public ScanCameraViewListener(ScanCanvasView scanCanvasView, IScanner iScanner) {

        Log.d("custom" + TAG, "STORAGE_FOLDER: " + SC.STORAGE_FOLDER);
        Log.d("custom" + TAG, "APPDATA_FOLDER: " + SC.APPDATA_FOLDER);
        Log.d("custom" + TAG, "Marker PATH: " + SC.STORAGE_FOLDER + SC.MARKER_NAME);
//        markerToMatch = getOrMakeMarker();
        this.scanCanvasView = scanCanvasView;
//        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
//        surfaceHolder.addCallback(this);
        this.iScanner = iScanner;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // internally it is a mat-
        outMat = inputFrame.rgba();
        Utils.logShape("outMat",outMat);
        Mat processedMat = Utils.preProcessMat(outMat);
        Utils.logShape("processedMat",processedMat);
//            iScanner.displayHint(ScanHint.NO_MESSAGE);

//                    TODO : Can apply more live filters here:
        if (!getBool(buttonChecked, R.id.xray_btn)) {
            if (getBool(buttonChecked, R.id.thresh_btn))
                Utils.thresh(processedMat);
            if (getBool(buttonChecked, R.id.canny_btn))
                Utils.canny(processedMat);
            if (getBool(buttonChecked, R.id.morph_btn))
                Utils.morph(processedMat);
            if (getBool(buttonChecked, R.id.contour_btn))
                Utils.drawContours(processedMat);
        }
//            scanCanvasView.unsetCameraBitmap();
//            scanCanvasView.unsetHoverBitmap();
//            try {
//                Quadrilateral largestQuad = Utils.findPage(processedMat);
//                if (null != largestQuad) {
//                    Size originalPreviewSize = processedMat.size();
//                    int originalPreviewArea = processedMat.rows() * processedMat.cols();
//                    double contourArea = Math.abs(Imgproc.contourArea(largestQuad.contour));
////                    guidedDrawRect(processedMat, largestQuad.points, contourArea, originalPreviewSize, originalPreviewArea);
//                } else {
//                    showFindingReceiptHint();
//                }
//                // set to render frame again
////                        clearAndInvalidateCanvas();
////                processedMat.release();
//            } catch (Exception e) {
//                Log.d(TAG, "Uh oh.. Camera error?");
//                showFindingReceiptHint();
//            }
//        }
//        else{
//            iScanner.displayHint(ScanHint.NO_MESSAGE);
//
////                    TODO : Can apply more live filters here:
//            if(getBool(buttonChecked, R.id.thresh_btn))
////                        Utils.thresh(processedMat);
//                Utils.normalize(processedMat);
//            if(getBool(buttonChecked, R.id.canny_btn))
//                Utils.canny(processedMat);
//            if(getBool(buttonChecked, R.id.morph_btn))
//                Utils.morph(processedMat);
//            if(getBool(buttonChecked, R.id.contour_btn))
//                Utils.drawContours(processedMat);
//            // TODO : templateMatching output here?!
//
//            // rotate the bitmap for portrait
//            Bitmap cameraBitmap = Utils.matToBitmapRotate(processedMat);
//            scanCanvasView.setCameraBitmap(cameraBitmap);
//            // set to render frame again
//            clearAndInvalidateCanvas();
////            processedMat.release();
//    }
        outMat = Utils.resize_util(processedMat, outMat.cols(), outMat.rows());
        return outMat;
    }


    private void scheduleAutoCapture(final ScanHint scanHint) {
        isAutoCaptureScheduled = true;
        secondsLeft = 0;
        autoCaptureTimer = new CountDownTimer( SC.AUTOCAP_TIMER, 500) {
            public void onTick(long millisUntilFinished) {
                if (Math.round((float) millisUntilFinished / 1000.0f) != secondsLeft) {
                    secondsLeft = Math.round((float) millisUntilFinished / 1000.0f);
                }
                Log.v(TAG, "" + millisUntilFinished / 1000);
                if(secondsLeft == 1)
                    autoCapture(scanHint);
            }

            public void onFinish() {
                isAutoCaptureScheduled = false;
            }
        };
        autoCaptureTimer.start();
    }

    private void autoCapture(ScanHint scanHint) {
        if (isCapturing) return;
        if (ScanHint.CAPTURING_IMAGE.equals(scanHint)) {
            try {
                isCapturing = true;
                iScanner.displayHint(ScanHint.CAPTURING_IMAGE);

                // different approach - directly call the onCapture method
//                camera.takePicture(mShutterCallBack, null, pictureCallback);
//                camera.setPreviewCallback(null);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelAutoCapture() {
        if (isAutoCaptureScheduled) {
            isAutoCaptureScheduled = false;
            if (null != autoCaptureTimer) {
                autoCaptureTimer.cancel();
            }
        }
    }

    private Boolean getBool(Map<Integer,Boolean> map, Integer k){
        return (map.containsKey(k)) ? map.get(k) : false;
    }

    public void invalidateCanvas() {
        scanCanvasView.invalidate();
    }

    public void clearAndInvalidateCanvas() {
        scanCanvasView.clear();
        invalidateCanvas();
    }
    private void showFindingReceiptHint() {
        iScanner.displayHint(ScanHint.FIND_RECT);
        clearAndInvalidateCanvas();
    }
    // After invoking this the frames will start to be delivered to client via the onCameraFrame() callback.
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted ");
    }

    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped ");
    }

}
