package com.udayraj.androidomr.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.hardware.Camera;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.udayraj.androidomr.constants.SC;
import com.udayraj.androidomr.enums.ScanHint;
import com.udayraj.androidomr.interfaces.IScanner;


public class ScanCameraViewListener implements CvCameraViewListener2 {
    private static final String TAG = ScanCameraViewListener.class.getSimpleName();

    public boolean isAutoCaptureScheduled = false;
    public Map<Integer, Boolean> buttonChecked = new HashMap<>();

//    SurfaceView mSurfaceView;
    private final ScanCanvasView scanCanvasView;
//    private int vWidth = 0;
//    private int vHeight = 0;
//
//    private final Context context;
//    private Camera camera;
//
    private final IScanner iScanner;
    private CountDownTimer autoCaptureTimer;
    private int secondsLeft;
    private Camera.Size previewSize;
    private boolean isCapturing = false;


    private Mat inMat;
    private Mat outMat;
    private Mat markerToMatch;
    private Scalar CONTOUR_COLOR;


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

    public void onCameraViewStarted(int width, int height) {
        outMat = new Mat(height, width, CvType.CV_8UC4);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        outMat.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // internally it is a mat-
        outMat = inputFrame.rgba();
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(outMat, contours, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Log.e(TAG, "Contours count: " + contours.size());
        Imgproc.drawContours(outMat, contours, -1, CONTOUR_COLOR);

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
}
