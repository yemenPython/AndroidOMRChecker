package com.udayraj.androidomr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.udayraj.androidomr.activity.ScanActivity;
import com.udayraj.androidomr.constants.SC;
import com.udayraj.androidomr.util.Utils;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 101;
    private ImageView scannedImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scannedImageView = findViewById(R.id.scanned_image);
        startScan();
    }

    private void startScan() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                if(null != data && null != data.getExtras()) {
                    String filePath = data.getExtras().getString("scannedResult");
                    Bitmap baseBitmap = Utils.decodeBitmapFromFile(filePath, "image");
                    scannedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    scannedImageView.setImageBitmap(baseBitmap);
                }
            } else if(resultCode == Activity.RESULT_CANCELED) {
                finish();
            }
        }
    }
}
