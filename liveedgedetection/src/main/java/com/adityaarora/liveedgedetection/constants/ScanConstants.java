package com.adityaarora.liveedgedetection.constants;


import android.os.Environment;

/**
 * This class defines constants
 */

public class ScanConstants {
    public static final String SCANNED_RESULT = "scannedResult";
    public static final String IMAGES_DIR = "OMRTechno/";
    public static final String IMAGE_NAME = "image";
    public static final String MARKER_NAME = "omr_marker.jpg";
//    This one doesn't need context
    public static final String STORAGE_FOLDER=  Environment.getExternalStorageDirectory().getAbsolutePath() +"/" + IMAGES_DIR;

    public static final int HIGHER_SAMPLING_THRESHOLD = 2200;

}
