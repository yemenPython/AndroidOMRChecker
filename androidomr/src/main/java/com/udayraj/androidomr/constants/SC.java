package com.udayraj.androidomr.constants;


import android.os.Environment;

/**
 * This class defines constants
 */

public class SC {
    public static final String SCANNED_RESULT = "scannedResult";
    public static final String IMAGES_DIR = "OMRTechno/";
    public static final String IMAGE_NAME = "image.jpg";
    public static final String MARKER_NAME = "omr_marker.jpg";
//    This one doesn't need context
    public static final String STORAGE_FOLDER=  Environment.getExternalStorageDirectory().getAbsolutePath() +"/" + IMAGES_DIR;
//    this needs string
    public static String APPDATA_FOLDER;


    public static final int uniform_width_hd = (int) (1000 / 1.5);
    public static final int uniform_height_hd = (int)(1231 / 1.5);

    public static final int ACCEPT_TIMER = 5000;
    public static final int AUTOCAP_TIMER = 5000;
    public static final int CANNY_THRESHOLD_L = 35;
    public static final int CANNY_THRESHOLD_U = 135;
    public static final int KSIZE_BLUR = 5;
    public static final int KSIZE_CLOSE = 10;

    public static final int HIGHER_SAMPLING_THRESHOLD = 200;


}
