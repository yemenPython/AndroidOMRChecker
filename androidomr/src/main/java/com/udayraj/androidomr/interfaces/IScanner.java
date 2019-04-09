package com.udayraj.androidomr.interfaces;

import android.graphics.Bitmap;

import com.udayraj.androidomr.enums.ScanHint;

/**
 * Interface between activity and surface view
 */

public interface IScanner {
    void displayHint(ScanHint scanHint);
    void onPictureClicked(Bitmap bitmap);
}
