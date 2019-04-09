package com.udayraj.androidomr.activity;

import android.Manifest;
import android.app.Activity;
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
import com.udayraj.androidomr.util.ScanUtils;
import com.udayraj.androidomr.view.PolygonPoints;
import com.udayraj.androidomr.view.PolygonView;
import com.udayraj.androidomr.view.ProgressDialogFragment;
import com.udayraj.androidomr.view.Quadrilateral;
import com.udayraj.androidomr.view.ScanSurfacePreview;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static android.view.View.GONE;

/**
 * This class initiates camera and detects edges on live view
 */
public class ScanActivity extends AppCompatActivity implements IScanner, View.OnClickListener {
    private static final String TAG = ScanActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_TOKEN= 101;

    private  Map<Integer, PointF>  scannedPoints;

    private ViewGroup containerScan;
    private FrameLayout cameraPreviewLayout;
    private FrameLayout effectsPreviewLayout;
    private ScanSurfacePreview mImageSurfaceView;
    
    private static final String mOpenCvLibrary = "opencv_java3";
    private static ProgressDialogFragment progressDialogFragment;
    private TextView captureHintText;
    private TextView timeElapsedText;
    private LinearLayout captureHintLayout;

    public final static Stack<PolygonPoints> allDraggedPointsStack = new Stack<>();
    private ImageView cropImageView;
    private View cropAcceptBtn;
    private View cropRejectBtn;
    private Bitmap copyBitmap;
    private FrameLayout cropLayout;
    private Resources res;
    private  int PERMISSION_ALL = 1;
    private  String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        init();
    }

    private void init() {
//        THIS IS CALLED EVERYTIME ACTIVITY GETS FOCUSED/RESUMED
        res = getResources();
        // outermost view = containerScan
        containerScan = findViewById(R.id.container_scan);
        cameraPreviewLayout = findViewById(R.id.camera_preview);

        cropImageView = findViewById(R.id.crop_image_view);
        captureHintLayout = findViewById(R.id.capture_hint_layout);
        timeElapsedText = findViewById(R.id.time_elapsed_text);
        captureHintText = findViewById(R.id.capture_hint_text);

        // Contains the accept/reject buttons -
        cropLayout = findViewById(R.id.crop_layout);
        cropAcceptBtn = findViewById(R.id.crop_accept_btn);
        cropRejectBtn = findViewById(R.id.crop_reject_btn);
        cropAcceptBtn.setOnClickListener(this);
        cropRejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumePreview();
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
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){ mImageSurfaceView.buttonChecked.put(key ,isChecked);}
            });
        }

        SC.APPDATA_FOLDER = ScanActivity.this.getExternalFilesDir(null).getAbsolutePath()+"/" + SC.IMAGES_DIR;

        getPermissions();
    }
    public boolean hasAllPermissions() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void getPermissions() {
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
                        mImageSurfaceView = new ScanSurfacePreview(ScanActivity.this, ScanActivity.this);
                        cameraPreviewLayout.addView(mImageSurfaceView);
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
            case NO_MESSAGE:
                captureHintLayout.setVisibility(GONE);
                break;
            default:
                break;
        }
    }

//    called from ScanSurfacePreview.pictureCallback
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
                Quadrilateral quad = ScanUtils.findPage(originalMat);
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
                        points = ScanUtils.getPolygonDefaultPoints(width, height);
                    }

                } else {
                    points = ScanUtils.getPolygonDefaultPoints(width, height);
                }

                scannedPoints = PolygonView.getOrderedPoints(points);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    TransitionManager.beginDelayedTransition(containerScan);

                cropImageView.setImageBitmap(copyBitmap);
                cropImageView.setScaleType(ImageView.ScaleType.FIT_XY);
                cropLayout.setVisibility(View.VISIBLE);

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
        String path = SC.STORAGE_FOLDER;
        Toast.makeText(this, "Saving image: " + path+SC.IMAGE_NAME, Toast.LENGTH_SHORT).show();
        boolean success = FileUtils.saveBitmap(croppedBitmap, path, SC.IMAGE_NAME);
        setResult(Activity.RESULT_OK, new Intent().putExtra(SC.SCANNED_RESULT, path));
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
