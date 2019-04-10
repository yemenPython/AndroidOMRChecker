package com.udayraj.androidomr.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory
        ;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.udayraj.androidomr.R;
import com.udayraj.androidomr.constants.SC;
import com.udayraj.androidomr.enums.ScanHint;
import com.udayraj.androidomr.interfaces.IScanner;
import com.udayraj.androidomr.util.FileUtils;
import com.udayraj.androidomr.util.ImageDetectionProperties;
import com.udayraj.androidomr.util.Utils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * This class previews the live images from the camera
 */

public class ScanSurfacePreview extends FrameLayout implements SurfaceHolder.Callback {
    private static final String TAG = ScanSurfacePreview.class.getSimpleName();
    SurfaceView mSurfaceView;
    private final ScanCanvasView scanCanvasView;
    private int vWidth = 0;
    private int vHeight = 0;

    private final Context context;
    private Camera camera;

    private final IScanner iScanner;
    private CountDownTimer autoCaptureTimer;
    private int secondsLeft;
    public boolean isAutoCaptureScheduled;
    private Camera.Size previewSize;
    private boolean isCapturing = false;
    public Map<Integer, Boolean> buttonChecked = new HashMap<>();
    private Mat markerToMatch;

    public ScanSurfacePreview(Context context, IScanner iScanner) {
        super(context);
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        this.context = context;

        Log.d("custom"+TAG,"STORAGE_FOLDER: "+ SC.STORAGE_FOLDER);
        Log.d("custom"+TAG,"APPDATA_FOLDER: "+ SC.APPDATA_FOLDER);
        Log.d("custom"+TAG,"Marker PATH: "+ SC.STORAGE_FOLDER+ SC.MARKER_NAME);

        markerToMatch = getOrMakeMarker();

        this.scanCanvasView = new ScanCanvasView(context);
        addView(scanCanvasView);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(this);
        this.iScanner = iScanner;
    }

    private Mat getOrMakeMarker() {
        File mFile = new File (SC.STORAGE_FOLDER, SC.MARKER_NAME);
        if(! mFile.exists()){
            Bitmap bm = BitmapFactory.decodeResource( getResources(), R.drawable.default_omr_marker);
            boolean success = FileUtils.saveBitmap(bm, SC.STORAGE_FOLDER, SC.MARKER_NAME);
            if(success) {
                Log.d("custom" + TAG, "Marker copied successfully to storage folder.");
                Toast.makeText(context, "Marker copied successfully to: " + SC.STORAGE_FOLDER, Toast.LENGTH_SHORT).show();
            }
            else
                Log.d("custom"+TAG,"Error copying Marker to storage folder.");
        }
        else{
            Log.d("custom"+TAG,"Marker found in storage folder");
        }

        Mat marker = Utils.resize_util(Imgcodecs.imread(mFile.getAbsolutePath(),Imgcodecs.IMREAD_GRAYSCALE), (int) SC.uniform_width_hd/SC.marker_scale_fac);
        return marker;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            requestLayout();
            openCamera();
            this.camera.setPreviewDisplay(holder);
            setPreviewCallback();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void clearAndInvalidateCanvas() {
        scanCanvasView.clear();
        invalidateCanvas();
    }

    public void invalidateCanvas() {
        scanCanvasView.invalidate();
    }

    private void openCamera() {
        if (camera == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int defaultCameraId = 0;
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    defaultCameraId = i;
                }
            }
            camera = Camera.open(defaultCameraId);
            Camera.Parameters cameraParams = camera.getParameters();

            List<String> flashModes = cameraParams.getSupportedFlashModes();
            if (null != flashModes && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                cameraParams.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            }

            camera.setParameters(cameraParams);
        }
    }

    //    SurfaceView
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (vWidth == vHeight) {
            return;
        }
        if (previewSize == null)
            previewSize = Utils.getOptimalPreviewSize(camera, vWidth, vHeight);

        Camera.Parameters parameters = camera.getParameters();
        camera.setDisplayOrientation(Utils.configureCameraAngle((Activity) context));
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        if (parameters.getSupportedFocusModes() != null
                && parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (parameters.getSupportedFocusModes() != null
                && parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        Camera.Size size = Utils.determinePictureSize(camera, parameters.getPreviewSize());
        parameters.setPictureSize(size.width, size.height);
        parameters.setPictureFormat(ImageFormat.JPEG);

        camera.setParameters(parameters);
        requestLayout();
        setPreviewCallback();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        stopPreviewAndFreeCamera();
        if (camera != null) {
            // Call stopPreview() to stop updating the preview surface.
            camera.stopPreview();
            camera.setPreviewCallback(null);
            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            camera.release();
            camera = null;
        }
    }

    public void setPreviewCallback() {
        this.camera.startPreview();
        this.camera.setPreviewCallback(previewCallback);
    }


    public Mat byteArrayToMat(byte[] data, Camera.Size pictureSize){
        Mat yuv = new Mat(new Size(pictureSize.width, pictureSize.height * 1.5), CV_8UC1);
        yuv.put(0, 0, data);
        Mat mat = new Mat(new Size(pictureSize.width, pictureSize.height), CvType.CV_8UC4);
        Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2BGR_NV21, 4);
        yuv.release();
        return mat;
    }

    //    This is the "LIVE" function, the HEART of this app.
    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//                        data contains the image as byte[] in YUV color scheme
            if (null != camera) {
                // CALLBACK FOR HANDLING IMAGES
                Camera.Size pictureSize = camera.getParameters().getPreviewSize();
//                    Log.d("custom"+TAG, "onPreviewFrame - received image " + pictureSize.width + "x" + pictureSize.height);

                Mat mat = byteArrayToMat(data, pictureSize);
                Mat processedMat = Utils.preProcessMat(mat);
                mat.release();

                if (!getBool(buttonChecked, R.id.xray_btn)) {
                    scanCanvasView.unsetCameraBitmap();
//                    scanCanvasView.unsetHoverBitmap();
                    try {
                        Quadrilateral largestQuad = Utils.findPage(processedMat);
                        if (null != largestQuad) {
                            Size originalPreviewSize = processedMat.size();
                            int originalPreviewArea = processedMat.rows() * processedMat.cols();
                            guidedDrawRect(processedMat, largestQuad.contour, largestQuad.points, originalPreviewSize, originalPreviewArea);

//                            Imgproc.rectangle(processedMat,  new Point(50,50), new Point(75,75), new Scalar(5, 5, 5), 4);
//                            Bitmap cameraBitmap = Utils.matToBitmapRotate(processedMat);
//                            scanCanvasView.setCameraBitmap(cameraBitmap);
                        } else {
                            showFindingReceiptHint();
                        }
                        // set to render frame again
//                        clearAndInvalidateCanvas();
                        processedMat.release();
                    } catch (Exception e) {
                        Log.d(TAG, "Uh oh.. Camera error?");
                        showFindingReceiptHint();
                    }
                }
                else{
                    iScanner.displayHint(ScanHint.NO_MESSAGE);

//                    TODO : Can apply more live filters here:
                    if(getBool(buttonChecked, R.id.thresh_btn))
//                        Utils.thresh(processedMat);
                        Utils.normalize(processedMat);
                    if(getBool(buttonChecked, R.id.canny_btn))
                        Utils.canny(processedMat);
                    if(getBool(buttonChecked, R.id.morph_btn))
                        Utils.morph(processedMat);
                    if(getBool(buttonChecked, R.id.contour_btn))
                        Utils.drawContours(processedMat);
                    // TODO : templateMatching output here?!

                    // rotate the bitmap for portrait
                    Bitmap cameraBitmap = Utils.matToBitmapRotate(processedMat);
                    scanCanvasView.setCameraBitmap(cameraBitmap);
                    // set to render frame again
                    clearAndInvalidateCanvas();
                    processedMat.release();
                }
            }
        }
    };
    private Boolean getBool(Map<Integer,Boolean> map, Integer k){
        return (map.containsKey(k)) ? map.get(k) : false;
    }
    private void guidedDrawRect(Mat processedMat, MatOfPoint2f approx, Point[] points, Size stdSize, int previewArea) {
        Path path = new Path();
        // ATTENTION: axes are swapped
        float previewWidth = (float) stdSize.height;
        float previewHeight = (float) stdSize.width;

        //Points are drawn in anticlockwise direction
        path.moveTo(previewWidth - (float) points[0].y, (float) points[0].x);
        path.lineTo(previewWidth - (float) points[1].y, (float) points[1].x);
        path.lineTo(previewWidth - (float) points[2].y, (float) points[2].x);
        path.lineTo(previewWidth - (float) points[3].y, (float) points[3].x);
        path.close();

        double area = Math.abs(Imgproc.contourArea(approx));

        PathShape newBox = new PathShape(path, previewWidth, previewHeight);
        Paint paint = new Paint();
        Paint border = new Paint();

        //Height calculated on Y axis
        double resultHeight = Math.max(points[1].x - points[0].x, points[2].x - points[3].x);
        //Width calculated on X axis
        double resultWidth = Math.max(points[3].y - points[0].y, points[2].y - points[1].y);

        ImageDetectionProperties imgDetectionPropsObj
                = new ImageDetectionProperties(previewWidth, previewHeight, resultWidth, resultHeight,
                previewArea, area, points[0], points[1], points[2], points[3]);

        final ScanHint scanHint;

        if (imgDetectionPropsObj.isDetectedAreaBeyondLimits()) {
            scanHint = ScanHint.FIND_RECT;
            cancelAutoCapture();
        }
        else if (imgDetectionPropsObj.isDetectedAreaBelowLimits()) {
            cancelAutoCapture();
            if (imgDetectionPropsObj.isEdgeTouching()) {
                scanHint = ScanHint.MOVE_AWAY;
            } else {
                scanHint = ScanHint.MOVE_CLOSER;
            }
        }
        else if (imgDetectionPropsObj.isDetectedHeightAboveLimit()
                || imgDetectionPropsObj.isDetectedWidthAboveLimit()
                || imgDetectionPropsObj.isDetectedAreaAboveLimit()
                || imgDetectionPropsObj.isEdgeTouching()) {
            cancelAutoCapture();
            scanHint = ScanHint.MOVE_AWAY;
        }
        else {
            if (imgDetectionPropsObj.isAngleNotCorrect(approx)) {
                cancelAutoCapture();
                scanHint = ScanHint.ADJUST_ANGLE;
            }
            else {
                if(!Utils.checkForMarkers(processedMat, points, markerToMatch)) {
                    cancelAutoCapture();
                    scanHint = ScanHint.FIND_MARKERS;
                }
                else{
                    // markers found too
                    scanHint = ScanHint.CAPTURING_IMAGE;
                    clearAndInvalidateCanvas();

                    if (!isAutoCaptureScheduled) {
                        scheduleAutoCapture(scanHint);
                    }
                }
            }
        }
//        Log.i(TAG," Area: " + area +
//                " Preview Area: "+ previewArea +
//                " ROI Area: " + Math.round(100 *  area / previewArea)+"%" +
//                " Label: " + scanHint.toString());

        border.setStrokeWidth(7);
        iScanner.displayHint(scanHint);
        setPaintAndBorder(scanHint, paint, border);
        scanCanvasView.clear();
        scanCanvasView.addShape(newBox, paint, border);
        invalidateCanvas();
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

                camera.takePicture(mShutterCallBack, null, pictureCallback);
                camera.setPreviewCallback(null);
//                iScanner.displayHint(ScanHint.NO_MESSAGE);
//                clearAndInvalidateCanvas();
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

    private void showFindingReceiptHint() {
        iScanner.displayHint(ScanHint.FIND_RECT);
        clearAndInvalidateCanvas();
    }

    private void setPaintAndBorder(ScanHint scanHint, Paint paint, Paint border) {
        int paintColor = 0;
        int borderColor = 0;

        switch (scanHint) {
            case MOVE_CLOSER:
            case MOVE_AWAY:
            case ADJUST_ANGLE:
                paintColor = Color.argb(30, 255, 38, 0);
                borderColor = Color.rgb(255, 38, 0);
                break;
            case FIND_RECT:
                paintColor = Color.argb(0, 0, 0, 0);
                borderColor = Color.argb(0, 0, 0, 0);
                break;
            case CAPTURING_IMAGE:
                paintColor = Color.argb(30, 38, 216, 76);
                borderColor = Color.rgb(38, 216, 76);
                break;
        }

        paint.setColor(paintColor);
        border.setColor(borderColor);
    }


//    Things done 'during' capture
    private final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.stopPreview();
            iScanner.displayHint(ScanHint.NO_MESSAGE);
            clearAndInvalidateCanvas();

            Bitmap bitmap = Utils.decodeBitmapFromByteArray(data,SC.HIGHER_SAMPLING_THRESHOLD, SC.HIGHER_SAMPLING_THRESHOLD);

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
//            --> Picture Taken step.. onClick be called next
            Log.d("customCheck","pictureCallbackWhen");
            iScanner.onPictureClicked(rotated);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    isCapturing = false;
                }
            }, 3000);

        }
    };

    private final Camera.ShutterCallback mShutterCallBack = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            if (context != null) {
                AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (null != mAudioManager)
                    mAudioManager.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
            }
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        vWidth = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        vHeight = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(vWidth, vHeight);
        previewSize = Utils.getOptimalPreviewSize(camera, vWidth, vHeight);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 0) {

            int width = r - l;
            int height = b - t;

            int previewWidth = width;
            int previewHeight = height;

            if (previewSize != null) {
                previewWidth = previewSize.width;
                previewHeight = previewSize.height;

                int displayOrientation = Utils.configureCameraAngle((Activity) context);
                if (displayOrientation == 90 || displayOrientation == 270) {
                    previewWidth = previewSize.height;
                    previewHeight = previewSize.width;
                }

                Log.d(TAG, "previewWidth:" + previewWidth + " previewHeight:" + previewHeight);
            }

            int nW;
            int nH;
            int top;
            int left;

            float scale = 1.0f;

            // Center the child SurfaceView within the parent.
            if (width * previewHeight < height * previewWidth) {
                Log.d(TAG, "center horizontally");
                int scaledChildWidth = (int) ((previewWidth * height / previewHeight) * scale);
                nW = (width + scaledChildWidth) / 2;
                nH = (int) (height * scale);
                top = 0;
                left = (width - scaledChildWidth) / 2;
            } else {
                Log.d(TAG, "center vertically");
                int scaledChildHeight = (int) ((previewHeight * width / previewWidth) * scale);
                nW = (int) (width * scale);
                nH = (height + scaledChildHeight) / 2;
                top = (height - scaledChildHeight) / 2;
                left = 0;
            }
            mSurfaceView.layout(left, top, nW, nH);
            scanCanvasView.layout(left, top, nW, nH);

            Log.d("layout", "left:" + left);
            Log.d("layout", "top:" + top);
            Log.d("layout", "right:" + nW);
            Log.d("layout", "bottom:" + nH);
        }
    }
}
