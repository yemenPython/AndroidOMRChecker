package com.udayraj.androidomr.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
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
import com.udayraj.androidomr.util.Utils;
import org.opencv.android.CameraBridgeViewBase;
import android.view.WindowManager;
import android.view.SurfaceView;

import com.udayraj.androidomr.view.ScanCameraViewListener;
import com.udayraj.androidomr.view.ScanCanvasView;
import com.udayraj.androidomr.view.ScanSurfacePreview;
import android.app.Activity;
import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

/**
 * This class initiates camera and detects edges on live view
 */
public class ScanActivity extends AppCompatActivity implements IScanner {
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
    private FrameLayout cropLayout;
    private LinearLayout captureHintLayout;
    private View cropAcceptBtn;
    private View cropRejectBtn;
    private TextView captureHintText;
    private TextView timeElapsedText;
    private ImageView cropImageView;
    // private ScanSurfacePreview mImageSurfaceView;
    CameraBridgeViewBase mOpenCvCameraView;
    private FrameLayout cameraPreviewLayout;
    ScanCameraViewListener mScanCameraViewListener;

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
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_camera_preview);
        mOpenCvCameraView.setUserRotation(90);
        mOpenCvCameraView.setMaxFrameSize(600, 600);

        cropImageView = findViewById(R.id.crop_image_view);
        captureHintLayout = findViewById(R.id.capture_hint_layout);
        timeElapsedText = findViewById(R.id.time_elapsed_text);
        captureHintText = findViewById(R.id.capture_hint_text);

        // Contains the accept/reject buttons -
        cropLayout = findViewById(R.id.crop_layout);
        cropAcceptBtn = findViewById(R.id.crop_accept_btn);
        cropRejectBtn = findViewById(R.id.crop_reject_btn);
        final Activity context = this;
        cropAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("custom"+TAG, "Image Accepted.");
                String path = SC.STORAGE_FOLDER;
                Toast.makeText(context, "Saving to: " + path+SC.IMAGE_NAME, Toast.LENGTH_SHORT).show();
                boolean success = FileUtils.saveBitmap(copyBitmap, path, SC.IMAGE_NAME);
                resumePreview();
            }
        });
        cropRejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumePreview();
                mScanCameraViewListener.cancelAutoCapture();
                if(mScanCameraViewListener.isAutoCaptureScheduled)
                    Toast.makeText(context, "Capture Cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        List <Integer> toggleButtons = new ArrayList<>();
        toggleButtons.add(R.id.xray_btn);
        toggleButtons.add(R.id.canny_btn);
        toggleButtons.add(R.id.morph_btn);
        toggleButtons.add(R.id.thresh_btn);
        toggleButtons.add(R.id.contour_btn);

        for(int id : toggleButtons){
            ToggleButton button = findViewById(id);
            final int key = id;
            button.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                    mScanCameraViewListener.buttonChecked.put(key ,isChecked);
                    mScanCameraViewListener.cancelAutoCapture();
                    if(mScanCameraViewListener.isAutoCaptureScheduled)
                        Toast.makeText(context, "Capture Cancelled", Toast.LENGTH_SHORT).show();
                }
            });
        }
        SC.APPDATA_FOLDER = ScanActivity.this.getExternalFilesDir(null).getAbsolutePath()+"/" + SC.IMAGES_DIR;
        getPermissionsAndStart();
    }
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
//        TODO: Find out why this delay is there
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // mImageSurfaceView = new ScanSurfacePreview(ScanActivity.this, ScanActivity.this);
                        // cameraPreviewLayout.addView(mImageSurfaceView);
                        ScanCanvasView scanCanvasView = new ScanCanvasView(ScanActivity.this);
                        cameraPreviewLayout.addView(scanCanvasView);
                        mScanCameraViewListener = new ScanCameraViewListener(scanCanvasView,ScanActivity.this);
//                        mOpenCvCameraView.setCameraIndex(1);
                        mOpenCvCameraView.setCvCameraViewListener(mScanCameraViewListener);
                        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
                    }
                });
            }

        }, 500);
    }
    private void onStorageGranted() {
        // create intermediate directories
        FileUtils.checkMakeDirs(SC.APPDATA_FOLDER);
        FileUtils.checkMakeDirs(SC.STORAGE_FOLDER);
    }

    @Override
    public void displayHint (ScanHint scanHint) {
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

    //    This is NOT LIVE function (its in ScanSurfacePreview)
//    called after autoCapture
    @Override
    public void onPictureClicked(final Bitmap bitmap) {
        try {
            copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            int height = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();
            int width = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWidth();

            copyBitmap = Utils.resizeToScreenContentSize(copyBitmap, width, height);

            Mat originalMat = new Mat(copyBitmap.getHeight(), copyBitmap.getWidth(), CvType.CV_8UC1);
            org.opencv.android.Utils.bitmapToMat(copyBitmap, originalMat);

//            Point[] points = Utils.getPolygonDefaultPoints(width, height);
            try {
//                Quadrilateral quad = Utils.findPage(originalMat);
//                if (null != quad) {
//                    double resultArea = Math.abs(Imgproc.contourArea(quad.contour));
//                    double previewArea = originalMat.rows() * originalMat.cols();
//                    if (resultArea > previewArea * 0.08) {
//                        points = new Point[] {
//                                new Point(quad.points[0].x, quad.points[0].y),
//                                new Point(quad.points[1].x, quad.points[1].y),
//                                new Point(quad.points[3].x, quad.points[3].y),
//                                new Point(quad.points[2].x, quad.points[2].y)
//                        };
//                    }
//                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    TransitionManager.beginDelayedTransition(containerScan);

                showAcceptOverlay(copyBitmap);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    public void showAcceptOverlay(Bitmap copyBitmap){
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        cropImageView.setImageBitmap(copyBitmap);
        cropImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        cropLayout.setVisibility(View.VISIBLE);
        startAcceptCountDown();
    }

    public void startAcceptCountDown(){
        new CountDownTimer(SC.ACCEPT_TIMER, 1000) {
            public void onTick(long millisUntilFinished) {
                timeElapsedText.setText( res.getString(R.string.timer_text,millisUntilFinished / 1000));
            }

            public void onFinish() {
                timeElapsedText.setText( res.getString(R.string.timer_text,0));
                cropAcceptBtn.performClick();
            }
        }.start();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void resumePreview(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            TransitionManager.beginDelayedTransition(containerScan);
        cropLayout.setVisibility(View.GONE);
        // mImageSurfaceView.setPreviewCallback();
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
    }
    public void exitApp(){
        System.gc();
        finish();
    }
}
