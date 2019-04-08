package com.adityaarora.liveedgedetection.activity;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adityaarora.liveedgedetection.R;
import com.adityaarora.liveedgedetection.constants.ScanConstants;
import com.adityaarora.liveedgedetection.enums.ScanHint;
import com.adityaarora.liveedgedetection.interfaces.IScanner;
import com.adityaarora.liveedgedetection.util.FileUtils;
import com.adityaarora.liveedgedetection.util.ScanUtils;
import com.adityaarora.liveedgedetection.view.PolygonPoints;
import com.adityaarora.liveedgedetection.view.PolygonView;
import com.adityaarora.liveedgedetection.view.ProgressDialogFragment;
import com.adityaarora.liveedgedetection.view.Quadrilateral;
import com.adityaarora.liveedgedetection.view.ScanSurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static android.view.View.GONE;

/**
 * This class initiates camera and detects edges on live view
 */
public class ScanActivity extends AppCompatActivity implements IScanner, View.OnClickListener {
    private static final String TAG = ScanActivity.class.getSimpleName();

    // this int is like a CSRF token
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 101;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 101;

    private boolean isCameraPermissionNotGranted;
    private boolean isStoragePermissionNotGranted;
    private  Map<Integer, PointF>  scannedPoints;

    private ViewGroup containerScan;
    private FrameLayout cameraPreviewLayout;
    private ScanSurfaceView mImageSurfaceView;
    
    private static final String mOpenCvLibrary = "opencv_java3";
    private static ProgressDialogFragment progressDialogFragment;
    private TextView captureHintText;
    private TextView timeElapsedView;
    private LinearLayout captureHintLayout;

    public final static Stack<PolygonPoints> allDraggedPointsStack = new Stack<>();
    private ImageView cropImageView;
    private View cropAcceptBtn;
    private View cropRejectBtn;
    private Bitmap copyBitmap;
    private FrameLayout cropLayout;
    private Resources res;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    private void init() {
        res = getResources();
        // outermost view = containerScan
        containerScan = findViewById(R.id.container_scan);
        cameraPreviewLayout = findViewById(R.id.camera_preview);
        captureHintLayout = findViewById(R.id.capture_hint_layout);
        timeElapsedView = findViewById(R.id.time_elapsed_text);
        captureHintText = findViewById(R.id.capture_hint_text);
        cropImageView = findViewById(R.id.crop_image_view);
        cropAcceptBtn = findViewById(R.id.crop_accept_btn);
        cropRejectBtn = findViewById(R.id.crop_reject_btn);
        cropLayout = findViewById(R.id.crop_layout);
        ScanConstants.APPDATA_FOLDER = ScanActivity.this.getExternalFilesDir(null).getAbsolutePath()+"/" + ScanConstants.IMAGES_DIR;

        cropAcceptBtn.setOnClickListener(this);
        cropRejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumePreview();
            }
        });
        checkStoragePermissions();
        checkCameraPermissions();
    }

    private void checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            isCameraPermissionNotGranted = true;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Log.d("custom"+TAG,"Enable camera permission from settings (App info)");

                Toast.makeText(this, "Enable camera permission from settings (App info)", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            if (!isCameraPermissionNotGranted) {
                mImageSurfaceView = new ScanSurfaceView(ScanActivity.this, this);
                cameraPreviewLayout.addView(mImageSurfaceView);
            } else {
                isCameraPermissionNotGranted = false;
            }
        }
    }

    private void checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            isStoragePermissionNotGranted = true;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d("custom"+TAG,"Enable storage permission from settings (App info)");
                Toast.makeText(this, "Enable storage permission from settings (App info)", Toast.LENGTH_SHORT).show();
            } else {
                // Show the accept popup
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE);
            }
        } else {
            if (!isStoragePermissionNotGranted) {
                // create intermediate directories
                FileUtils.checkMakeDirs(ScanConstants.APPDATA_FOLDER);
                FileUtils.checkMakeDirs(ScanConstants.STORAGE_FOLDER);
            } else {
                isStoragePermissionNotGranted = false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                onRequestCamera(grantResults);
                break;
            default:
                break;
        }
    }

    private void onRequestCamera(int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageSurfaceView = new ScanSurfaceView(ScanActivity.this, ScanActivity.this);
                            cameraPreviewLayout.addView(mImageSurfaceView);
                        }
                    });
                }
            }, 500);

        } else {
            Toast.makeText(this, getString(R.string.camera_activity_permission_denied_toast), Toast.LENGTH_SHORT).show();
            this.finish();
        }
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
            case NO_MESSAGE:
                captureHintLayout.setVisibility(GONE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onPictureClicked(final Bitmap bitmap) {
        try {
            copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            int height = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();
            int width = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWidth();

            copyBitmap = ScanUtils.resizeToScreenContentSize(copyBitmap, width, height);
            Mat originalMat = new Mat(copyBitmap.getHeight(), copyBitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(copyBitmap, originalMat);
            ArrayList<PointF> points;
            try {
                Quadrilateral quad = ScanUtils.detectLargestQuadrilateral(originalMat);
                if (null != quad) {
                    double resultArea = Math.abs(Imgproc.contourArea(quad.contour));
                    double previewArea = originalMat.rows() * originalMat.cols();
                    if (resultArea > previewArea * 0.08) {
                        points = new ArrayList<>();
                        points.add(new PointF((float) quad.points[0].x, (float) quad.points[0].y));
                        points.add(new PointF((float) quad.points[1].x, (float) quad.points[1].y));
                        points.add(new PointF((float) quad.points[3].x, (float) quad.points[3].y));
                        points.add(new PointF((float) quad.points[2].x, (float) quad.points[2].y));
                    } else {
                        points = ScanUtils.getPolygonDefaultPoints(copyBitmap);
                    }

                } else {
                    points = ScanUtils.getPolygonDefaultPoints(copyBitmap);
                }

                scannedPoints = PolygonView.getOrderedPoints(points);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    TransitionManager.beginDelayedTransition(containerScan);

//TODO                call template matching here-
//TODO                draw marker bounds on copyBitmap here-


                // Contains the accept/reject buttons -
                cropLayout.setVisibility(View.VISIBLE);
                cropImageView.setImageBitmap(copyBitmap);
                cropImageView.setScaleType(ImageView.ScaleType.FIT_XY);

                startAcceptCountDown();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
//
//    private synchronized void showProgressDialog(String message) {
//        if (progressDialogFragment != null && progressDialogFragment.isVisible()) {
//            // Before creating another loading dialog, close all opened loading dialogs (if any)
//            progressDialogFragment.dismissAllowingStateLoss();
//        }
//        progressDialogFragment = null;
//        progressDialogFragment = new ProgressDialogFragment(message);
//        FragmentManager fm = getFragmentManager();
//        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
//    }
//
//    private synchronized void dismissDialog() {
//        progressDialogFragment.dismissAllowingStateLoss();
//    }

    static {
        System.loadLibrary(mOpenCvLibrary);
    }

    public void startAcceptCountDown(){
        new CountDownTimer(ScanConstants.ACCEPT_TIMER, 1000) {
            public void onTick(long millisUntilFinished) {
                timeElapsedView.setText( res.getString(R.string.timer_text,millisUntilFinished / 1000));
            }

            public void onFinish() {
                timeElapsedView.setText( res.getString(R.string.timer_text,0));
                cropAcceptBtn.performClick();
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
//        called on clicking accept button (because of : cropAcceptBtn.setOnClickListener(this);)
        Map<Integer, PointF> points = scannedPoints;//polygonView.getPoints();
        Log.d("custom"+TAG, "onClick called.");
        Bitmap croppedBitmap;

        if (ScanUtils.isScanPointsValid(points)) {
            Point point1 = new Point(points.get(0).x, points.get(0).y);
            Point point2 = new Point(points.get(1).x, points.get(1).y);
            Point point3 = new Point(points.get(2).x, points.get(2).y);
            Point point4 = new Point(points.get(3).x, points.get(3).y);
            croppedBitmap = ScanUtils.enhanceReceipt(copyBitmap, point1, point2, point3, point4);
        } else {
            croppedBitmap = copyBitmap;
        }
        String path = ScanConstants.APPDATA_FOLDER;
        boolean success = FileUtils.saveImg(croppedBitmap, path, ScanConstants.IMAGE_NAME);
        setResult(Activity.RESULT_OK, new Intent().putExtra(ScanConstants.SCANNED_RESULT, path));
        Log.d("custom"+TAG, "Resuming.");

        resumePreview();
        //bitmap.recycle();
    }

    public void resumePreview(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            TransitionManager.beginDelayedTransition(containerScan);
        cropLayout.setVisibility(View.GONE);
        mImageSurfaceView.setPreviewCallback();
    }
    public void exitApp(){
        System.gc();
        finish();
    }
}
