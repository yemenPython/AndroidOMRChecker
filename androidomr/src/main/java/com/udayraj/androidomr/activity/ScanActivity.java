package com.udayraj.androidomr.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.shapes.PathShape;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.udayraj.androidomr.R;
import com.udayraj.androidomr.constants.SC;
import com.udayraj.androidomr.enums.ScanHint;
import com.udayraj.androidomr.interfaces.IScanner;
import com.udayraj.androidomr.util.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import com.udayraj.androidomr.util.ImageDetectionProperties;
import com.udayraj.androidomr.util.Utils;
import org.opencv.android.CameraBridgeViewBase;
import android.view.WindowManager;
import android.view.SurfaceView;

import com.udayraj.androidomr.view.Quadrilateral;
import com.udayraj.androidomr.view.ScanCanvasView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;

/**
 * This class initiates camera and detects edges on live view
 */
public class ScanActivity extends AppCompatActivity implements IScanner, CameraBridgeViewBase.CvCameraViewListener2 {
    // required for Imread etc: https://stackoverflow.com/questions/35090838/no-implementation-found-for-long-org-opencv-core-mat-n-mat-error-using-opencv
    private static final String mOpenCvLibrary = "opencv_java3";
    static {
        System.loadLibrary(mOpenCvLibrary);
    }
    private static final String TAG = ScanActivity.class.getSimpleName();

    private Resources res;
    private static final int MY_PERMISSIONS_REQUEST_TOKEN= 101;
    private  String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };
    private Bitmap copyBitmap;
    private ViewGroup containerScan;
    private FrameLayout acceptLayout;
    private LinearLayout captureHintLayout;
    private View cropAcceptBtn;
    private TextView captureHintText;
    private TextView timeElapsedText;
    // private ImageView cropImageView;
    // private ScanCameraBridgeView mImageSurfaceView;
    private FrameLayout cameraPreviewLayout;
    CameraBridgeViewBase mOpenCvCameraView;


    public boolean acceptLayoutShowing = false;

    private ScanCanvasView scanCanvasView;
    private CountDownTimer autoCaptureTimer;
    private int secondsLeft;
    private boolean isCapturing = false;

    private Mat outMat;
    private Mat markerToMatch;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "called onCreate");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan);

        init();
    }
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause activity.");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void init() {
//        THIS IS CALLED EVERYTIME ACTIVITY GETS FOCUSED/RESUMED
        res = getResources();
        // outermost view = containerScan
        containerScan = findViewById(R.id.container_scan);
        cameraPreviewLayout = findViewById(R.id.camera_preview);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setMaxFrameSize(600, 600);

        // custom implemented feature - https://stackoverflow.com/questions/16669779/opencv-camera-orientation-issue
        // mOpenCvCameraView.setUserRotation(90);

        captureHintLayout = findViewById(R.id.capture_hint_layout);
        timeElapsedText = findViewById(R.id.time_elapsed_text);
        captureHintText = findViewById(R.id.capture_hint_text);

        // Contains the accept/reject buttons -
        acceptLayout = findViewById(R.id.crop_layout);
        cropAcceptBtn = findViewById(R.id.crop_accept_btn);
        final Activity context = this;
        cropAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                v.setClickable(false);
                Log.d("custom"+TAG, "Image Accepted.");
                String path = SC.STORAGE_FOLDER;
                cancelAutoCapture();
                Toast.makeText(context, "Saving to: " + path+SC.IMAGE_NAME, Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean success = FileUtils.saveBitmap(copyBitmap, SC.STORAGE_FOLDER, SC.IMAGE_NAME);
                        Log.d("custom"+TAG, "Image Saved.");
                    }

                }).start();
                Log.d("custom"+TAG, "Save Thread started.");
                v.setEnabled(true);
                v.setClickable(true);
            }
        });
        
        for( int id : new int []{R.id.xray_btn,R.id.canny_btn,R.id.morph_btn,R.id.thresh_btn,R.id.contour_btn} ){
            ToggleButton button = findViewById(id);
            button.setChecked(false);
        }
        SC.APPDATA_FOLDER = ScanActivity.this.getExternalFilesDir(null).getAbsolutePath()+"/" + SC.IMAGES_DIR;

        markerToMatch = getOrMakeMarker();
        getPermissionsAndStart();
    }

    private Mat getOrMakeMarker() {
        File mFile = new File (SC.STORAGE_FOLDER, SC.MARKER_NAME);
        if(! mFile.exists()){
            Bitmap bm = BitmapFactory.decodeResource( getResources(), R.drawable.default_omr_marker);
            boolean success = FileUtils.saveBitmap(bm, SC.STORAGE_FOLDER, SC.MARKER_NAME);
            if(success) {
                Log.d("custom" + TAG, "Marker copied successfully to storage folder.");
                Toast.makeText(this, "Marker created at: " + SC.STORAGE_FOLDER, Toast.LENGTH_SHORT).show();
            }
            else
                Log.d("custom"+TAG,"Error copying Marker to storage folder.");
        }
        else{
            Log.d("custom"+TAG,"Marker found in storage folder");
            Toast.makeText(this, "Marker found", Toast.LENGTH_SHORT).show();
        }

        return Utils.resize_util(Imgcodecs.imread(mFile.getAbsolutePath(),Imgcodecs.IMREAD_GRAYSCALE), (int) SC.uniform_width_hd/SC.marker_scale_fac);
    }
    //TODO: make PermissionHandler and put this inside
    public boolean hasAllPermissions() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void getPermissionsAndStart() {
        Log.d("custom" + TAG, "Asking permissions");
        // This runs in a separate thread : Not necessarily prompts the user beforehand!
        ActivityCompat.requestPermissions(this, PERMISSIONS, MY_PERMISSIONS_REQUEST_TOKEN);
    }
    //    callback from ActivityCompat.requestPermissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        // https://stackoverflow.com/questions/34342816/android-6-0-multiple-permissions
        if (hasAllPermissions()) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            onCameraGranted();
            onStorageGranted();
        }
        else {
//            Toast.makeText(this, "This app needs both permissions in order to function", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Please manually enable the permissions from settings. Exiting App!", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    private void onCameraGranted() {
        // new Handler().postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ScanCanvasView scanCanvasView = new ScanCanvasView(ScanActivity.this);
                cameraPreviewLayout.addView(scanCanvasView);
                ScanActivity.this.scanCanvasView = scanCanvasView;
//                        mOpenCvCameraView.setCameraIndex(1); // <- Front camera
                mOpenCvCameraView.setCvCameraViewListener(ScanActivity.this);
                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            }
        });
        // }
//        TODO: Find out why this delay is there
//         }, 500);
    }
    private void onStorageGranted() {
        // create intermediate directories
        FileUtils.checkMakeDirs(SC.APPDATA_FOLDER);
        FileUtils.checkMakeDirs(SC.STORAGE_FOLDER);
    }

    //    a Runnable named CameraWorker from JavaCameraView calls deliverAndDrawFrame(..), thus the separate thread
    //  So the 'JavaCameraView' view actually instantiates JavaCameraView class!
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // internally it is a mat-
        outMat = inputFrame.rgba();
        // Utils.logShape("outMat",outMat);
        Mat processedMat = Utils.preProcessMat(outMat);
        // Core.rotate(processedMat, processedMat, Core.ROTATE_90_CLOCKWISE);
//        Utils.logShape("processedMat",processedMat);
        scanCanvasView.unsetCameraBitmap();
        scanCanvasView.unsetHoverBitmap();
        if (!checkBtn(R.id.xray_btn)) {
            try {
                Quadrilateral largestQuad = Utils.findPage(processedMat);
                if (null != largestQuad) {
                    Size originalPreviewSize = processedMat.size();
                    int originalPreviewArea = processedMat.rows() * processedMat.cols();
                    double contourArea = Math.abs(Imgproc.contourArea(largestQuad.contour));
                    guidedDrawRect(processedMat, largestQuad.points, contourArea, originalPreviewSize, originalPreviewArea);
                } else {
                    displayHint(ScanHint.FIND_RECT);
                }
            } catch (Exception e) {
                Log.d(TAG, "Uh oh.. Camera error?", e);
                displayHint(ScanHint.FIND_RECT);
            }
        }
        else{
            // clear the shapes
            scanCanvasView.clear();
            displayHint(ScanHint.NO_MESSAGE);
            Utils.normalize(processedMat);
//                    TODO : Can apply more live filters here:
            if(checkBtn(R.id.thresh_btn))
                Utils.thresh(processedMat);
            if(checkBtn(R.id.canny_btn))
                Utils.canny(processedMat);
            if(checkBtn(R.id.morph_btn))
                Utils.morph(processedMat);
            if(checkBtn(R.id.contour_btn))
                Utils.drawContours(processedMat);
            // TODO : templateMatching output here?!
        }
        // rotate the bitmap for portrait
        copyBitmap = Utils.matToBitmapRotate(processedMat);
        scanCanvasView.setCameraBitmap(copyBitmap);
        // set to render frame again
        invalidateCanvas();
        processedMat.release();

        return outMat;
    }

    private void guidedDrawRect(Mat processedMat, Point[] points, double contourArea, Size stdSize, int previewArea) {
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

        PathShape newBox = new PathShape(path, previewWidth, previewHeight);
        Paint paint = new Paint();
        Paint border = new Paint();

        //Height calculated on Y axis
        double resultHeight = Math.max(points[1].x - points[0].x, points[2].x - points[3].x);
        //Width calculated on X axis
        double resultWidth = Math.max(points[3].y - points[0].y, points[2].y - points[1].y);

        ImageDetectionProperties imgDetectionPropsObj
                = new ImageDetectionProperties(previewWidth, previewHeight, resultWidth, resultHeight,
                previewArea, contourArea, points[0], points[1], points[2], points[3]);

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
            if (imgDetectionPropsObj.isAngleNotCorrect(points)) {
                cancelAutoCapture();
                scanHint = ScanHint.ADJUST_ANGLE;
            }
            else {
                boolean success = true;
                try {
                    Mat warpLevel1 = Utils.four_point_transform(processedMat, points);
                    Point[] markerPts = Utils.checkForMarkers(warpLevel1, points, markerToMatch);
                    for( Point matchLoc : markerPts) {
                        //Draw rectangle on result image
                        Imgproc.rectangle(warpLevel1, matchLoc, new Point(matchLoc.x + markerToMatch.cols(), matchLoc.y + markerToMatch.rows()), new Scalar(5, 5, 5), 4);
                    }
                    Bitmap cameraBitmap = Utils.matToBitmapRotate(warpLevel1);
                    scanCanvasView.setHoverBitmap(cameraBitmap);
                }
                catch(Exception e) {
                    Log.d(TAG,"Marker prob",e);
                    success = false;
                }

                if(success){
                    // markers found too
                    scanHint = ScanHint.CAPTURING_IMAGE;
                    tryAutoCapture(scanHint);
                }
                else{
                    cancelAutoCapture();
                    scanHint = ScanHint.FIND_MARKERS;
                }
            }
        }
//        Log.i(TAG," Area: " + contourArea +
//                " Preview Area: "+ previewArea +
//                " ROI Area: " + Math.round(100 *  contourArea / previewArea)+"%" +
//                " Label: " + scanHint.toString());

        border.setStrokeWidth(7);
        displayHint(scanHint);
        setPaintAndBorder(scanHint, paint, border);
        // clear previous shapes
        scanCanvasView.clear();
        // add new shape
        scanCanvasView.addShape(newBox, paint, border);
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

    //
    // public void showAcceptOverlay(){
    //     // mOpenCvCameraView.setVisibility(SurfaceView.GONE);
    //     // cropImageView.setImageBitmap(copyBitmap);
    //     // cropImageView.setScaleType(ImageView.ScaleType.FIT_XY);
    // }

    private void autoCapture(ScanHint scanHint) {
        Log.d(TAG,"autoCapture called.");
        if (isCapturing) return;
        Log.d(TAG,"autoCapture check.");
        if (ScanHint.CAPTURING_IMAGE.equals(scanHint)) {
            try {
                Log.d(TAG,"autoCapture action.");
                isCapturing = true;
                displayHint(ScanHint.CAPTURING_IMAGE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    TransitionManager.beginDelayedTransition(containerScan);
                cropAcceptBtn.performClick();

            } catch (Exception e) {
                e.printStackTrace();
            }
            isCapturing = false;
        }
    }

    private void tryAutoCapture(final ScanHint scanHint) {
        if(!acceptLayoutShowing) {
            acceptLayoutShowing = true;
            secondsLeft = 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    acceptLayout.setVisibility(View.VISIBLE);

                    autoCaptureTimer = new CountDownTimer(SC.AUTOCAP_TIMER, 1000) {
                        public void onTick(long millisUntilFinished) {
                            secondsLeft = Math.round((float) millisUntilFinished / 1000.0f);
                            timeElapsedText.setText( res.getString(R.string.timer_text,secondsLeft));
                        }

                        public void onFinish() {
                            secondsLeft = 0;
                            acceptLayoutShowing = false;
                            timeElapsedText.setText( res.getString(R.string.timer_text,0));
                            autoCapture(scanHint);
                        }
                    };
                    autoCaptureTimer.start();
                }
            });
        }
    }
    public void cancelAutoCapture() {
        if (acceptLayoutShowing) {
            secondsLeft = 0;
            acceptLayoutShowing = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    autoCaptureTimer.cancel();
                    acceptLayout.setVisibility(View.GONE);
                }
            });
        }
    }

    private Boolean checkBtn(Integer k){
        return ((ToggleButton)findViewById(k)).isChecked();
    }

    public void invalidateCanvas() {
        // scanCanvasView.invalidate();
        scanCanvasView.postInvalidate(); // on UI thread
    }

    // After invoking this the frames will start to be delivered to client via the onCameraFrame() callback.
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted ");
    }

    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped ");
    }

    public class MyRunnable implements Runnable {
        private ScanHint scanHint;
        public MyRunnable(ScanHint scanHint) {
            this.scanHint = scanHint;
        }

        @Override
        public void run() {
            captureHintLayout.setVisibility(View.VISIBLE);
            switch (scanHint) {
                case MOVE_CLOSER:
                    captureHintText.setText(res.getString(R.string.move_closer));
                    captureHintLayout.setBackground(res.getDrawable(R.drawable.hint_red));
                    break;
                case MOVE_AWAY:
                    captureHintText.setText(res.getString(R.string.move_away));
                    captureHintLayout.setBackground(res.getDrawable(R.drawable.hint_red));
                    break;
                case ADJUST_ANGLE:
                    captureHintText.setText(res.getString(R.string.adjust_angle));
                    captureHintLayout.setBackground(res.getDrawable(R.drawable.hint_red));
                    break;
                case FIND_RECT:
                    captureHintText.setText(res.getString(R.string.finding_rect));
                    captureHintLayout.setBackground(res.getDrawable(R.drawable.hint_white));
                    break;
                case CAPTURING_IMAGE:
                    captureHintText.setText(res.getString(R.string.hold_still));
                    captureHintLayout.setBackground(res.getDrawable(R.drawable.hint_green));
                    break;
                case FIND_MARKERS:
                    captureHintText.setText(res.getString(R.string.find_marker));
                    captureHintLayout.setBackground(res.getDrawable(R.drawable.hint_white));
                    break;
                case NO_MESSAGE:
                    captureHintLayout.setVisibility(GONE);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void displayHint (ScanHint scanHint) {
        runOnUiThread(new MyRunnable(scanHint));
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void exitApp(){
        System.gc();
        finish();
    }
}
/*
*
* @Override
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
    */