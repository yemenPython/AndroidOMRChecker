package com.udayraj.androidomr.interfaces;

import com.udayraj.androidomr.constants.ScanHint;

/**
 * Interface between activity and surface view
 */

public interface IScanner {
    void displayHint(ScanHint scanHint);
    // void onPictureClicked(Bitmap bitmap);
}
