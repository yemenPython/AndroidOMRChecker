package com.udayraj.androidomr.constants;


import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mohammedalaa.seekbar.RangeSeekBarView;
import com.udayraj.androidomr.R;
import com.udayraj.androidomr.activity.ScanActivity;

import org.opencv.core.Mat;

/**
 * This class defines constants
 */

public class SC {
    private static final String TAG = SC.class.getSimpleName();

    public static final String SCANNED_RESULT = "scannedResult";
    public static final String IMAGES_DIR = "OMRTechno/";
    public static final String IMAGE_NAME = "image.jpg";
    public static final String MARKER_NAME = "omr_marker.jpg";
    //    This one doesn't need context
    public static final String STORAGE_FOLDER=  Environment.getExternalStorageDirectory().getAbsolutePath() +"/" + IMAGES_DIR;
    //    this needs string
    public static String APPDATA_FOLDER;

    public static final int uniform_width_hd = (int) (1000 / 1.5);
    public static final int marker_scale_fac = 38;
    public static final int uniform_height_hd = (int)(1231 / 1.5);
    public static final double thresholdVar = 0.3;

    public static int AUTOCAP_TIMER = 3; // will be multiplied by 1000
    public static int CANNY_THRESHOLD_L = 55;
    public static int CANNY_THRESHOLD_U = 185;
    public static int KSIZE_BLUR = 3;
    public static int KSIZE_CLOSE = 10;
    public static int TRUNC_THRESH = 220;
    public static int ZERO_THRESH = 155;
    public static int GAMMA_HIGH = 125 ; // will be divided by 100

    //TODO: put these into interface
    public static boolean CLAHE_ON = true;
    public static boolean GAMMA_ON = true;
    public static boolean ERODE_ON = true;
    public static Mat markerToMatch;
    public static Mat markerEroded;

    private final RangeSeekBarView s_AUTOCAP_TIMER;
    private final RangeSeekBarView s_CANNY_THRESHOLD_L;
    private final RangeSeekBarView s_CANNY_THRESHOLD_U;
    private final RangeSeekBarView s_KSIZE_BLUR;
    private final RangeSeekBarView s_KSIZE_CLOSE;
    private final RangeSeekBarView s_TRUNC_THRESH;
    private final RangeSeekBarView s_ZERO_THRESH;
    private final RangeSeekBarView s_GAMMA_HIGH;

    public SC(ScanActivity s) {
        s_AUTOCAP_TIMER = s.findViewById(R.id.autocap_timer);
        s_CANNY_THRESHOLD_L = s.findViewById(R.id.canny_l);
        s_CANNY_THRESHOLD_U = s.findViewById(R.id.canny_u);
        s_KSIZE_BLUR = s.findViewById(R.id.ksize_blur);
        s_KSIZE_CLOSE = s.findViewById(R.id.ksize_morph);
        s_TRUNC_THRESH = s.findViewById(R.id.trunc_thresh);
        s_ZERO_THRESH = s.findViewById(R.id.zero_thresh);
        s_GAMMA_HIGH = s.findViewById(R.id.gamma);
    }
    public void updateConfig(){
        AUTOCAP_TIMER = s_AUTOCAP_TIMER.getValue();
        CANNY_THRESHOLD_L = s_CANNY_THRESHOLD_L.getValue();
        CANNY_THRESHOLD_U = s_CANNY_THRESHOLD_U.getValue();
        KSIZE_BLUR = s_KSIZE_BLUR.getValue();
        KSIZE_CLOSE = s_KSIZE_CLOSE.getValue();
        TRUNC_THRESH = s_TRUNC_THRESH.getValue();
        ZERO_THRESH = s_ZERO_THRESH.getValue();
        GAMMA_HIGH = s_GAMMA_HIGH.getValue();
    }
}
