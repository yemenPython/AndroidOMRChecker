package com.udayraj.androidomr.constants;


import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mohammedalaa.seekbar.RangeSeekBarView;
import com.udayraj.androidomr.R;
import com.udayraj.androidomr.activity.ScanActivity;

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

    public static int AUTOCAP_TIMER = 3; // will be multiplied by 1000
    public static int CANNY_THRESHOLD_L = 50;
    public static int CANNY_THRESHOLD_U = 130;
    public static int KSIZE_BLUR = 3;
    public static int KSIZE_CLOSE = 10;
    public static int TRUNC_THRESH = 220;
    public static int GAMMA_HIGH = 125 ; // will be divided by 100

    public static boolean CLAHE_ON = true;
    public static boolean GAMMA_ON = true;

    private RangeSeekBarView s_AUTOCAP_TIMER;
    private RangeSeekBarView s_CANNY_THRESHOLD_L;
    private RangeSeekBarView s_CANNY_THRESHOLD_U;
    private RangeSeekBarView s_KSIZE_BLUR;
    private RangeSeekBarView s_KSIZE_CLOSE;
    private RangeSeekBarView s_TRUNC_THRESH;
    private RangeSeekBarView s_GAMMA_HIGH;

    public SC(ScanActivity s) {

        s_AUTOCAP_TIMER = s.findViewById(R.id.autocap_timer);
        s_AUTOCAP_TIMER.setMinValue(2); s_AUTOCAP_TIMER.setMaxValue(8);

        s_CANNY_THRESHOLD_L = s.findViewById(R.id.canny_l);
        s_CANNY_THRESHOLD_L.setMinValue(10); s_CANNY_THRESHOLD_L.setMaxValue(100);

        s_CANNY_THRESHOLD_U = s.findViewById(R.id.canny_u);
        s_CANNY_THRESHOLD_U.setMinValue(100); s_CANNY_THRESHOLD_U.setMaxValue(240);

        s_KSIZE_BLUR = s.findViewById(R.id.ksize_blur);
        s_KSIZE_BLUR.setMinValue(0); s_KSIZE_BLUR.setMaxValue(10);

        s_KSIZE_CLOSE = s.findViewById(R.id.ksize_morph);
        s_KSIZE_CLOSE.setMinValue(1); s_KSIZE_CLOSE.setMaxValue(15);

        s_TRUNC_THRESH = s.findViewById(R.id.trunc_thresh);
        s_TRUNC_THRESH.setMinValue(50); s_TRUNC_THRESH.setMaxValue(250);

        s_GAMMA_HIGH = s.findViewById(R.id.gamma);
        s_GAMMA_HIGH.setMinValue(50); s_GAMMA_HIGH.setMaxValue(400);

    }
    public void updateConfig(){
        AUTOCAP_TIMER = s_AUTOCAP_TIMER.getValue();
        CANNY_THRESHOLD_L = s_CANNY_THRESHOLD_L.getValue();
        CANNY_THRESHOLD_U = s_CANNY_THRESHOLD_U.getValue();
        KSIZE_BLUR = s_KSIZE_BLUR.getValue();
        KSIZE_CLOSE = s_KSIZE_CLOSE.getValue();
        TRUNC_THRESH = s_TRUNC_THRESH.getValue();
        GAMMA_HIGH = s_GAMMA_HIGH.getValue();
    }
}
