package com.scanlibrary;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhansi on 28/03/15.
 */
public class ScanActivity extends Activity implements IScanner, ComponentCallbacks2 {

    private static final int MAX_WIDTH = 1920; // Maximum width in pixels
    private static final int MAX_HEIGHT = 1080; // Maximum height in pixels
    private static final int MAX_FILE_SIZE_KB = 1024; // Maximum file size in KB

    String[] permissions = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33) {
            permissions[0] = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissions[0] = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        permissions[1] = Manifest.permission.CAMERA;

        setContentView(R.layout.scan_layout);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        checkPermissions();
    }

    private void checkPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            int result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 100);
            return;
        }
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            }
        }
    }

    private void init() {
        PickImageFragment fragment = new PickImageFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ScanConstants.OPEN_INTENT_PREFERENCE, getPreferenceContent());
        bundle.putInt("quality", getIntent().getIntExtra("quality", 1));
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.commit();
    }

    protected int getPreferenceContent() {
        return getIntent().getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
    }

    @Override
    public void onBitmapSelect(Uri uri) {
        try {
            Bitmap originalBitmap = getBitmapFromUri(this, uri);

            // Check and resize image if necessary
            Bitmap resizedBitmap = resizeBitmapIfNeeded(originalBitmap, MAX_WIDTH, MAX_HEIGHT);

            // Compress image to ensure it's under the file size limit
            File compressedFile = compressBitmapToFile(resizedBitmap, MAX_FILE_SIZE_KB);

            Uri compressedUri = Uri.fromFile(compressedFile);

            ScanFragment fragment = new ScanFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(ScanConstants.SELECTED_BITMAP, compressedUri);
            fragment.setArguments(bundle);

            android.app.FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.content, fragment);
            fragmentTransaction.addToBackStack(ScanFragment.class.toString());
            fragmentTransaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onScanFinish(Uri uri) {
        ResultFragment fragment = new ResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SCANNED_RESULT, uri);
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ResultFragment.class.toString());
        fragmentTransaction.commit();
    }

    /**
     * Resize the bitmap if its dimensions exceed the maximum allowed size.
     */
    private Bitmap resizeBitmapIfNeeded(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > maxWidth || height > maxHeight) {
            float aspectRatio = (float) width / height;
            if (width > height) {
                width = maxWidth;
                height = (int) (maxWidth / aspectRatio);
            } else {
                height = maxHeight;
                width = (int) (maxHeight * aspectRatio);
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        return bitmap;
    }

    /**
     * Compress the bitmap to a file with a maximum size limit.
     */
    private File compressBitmapToFile(Bitmap bitmap, int maxFileSizeKB) throws IOException {
        File tempFile = File.createTempFile("compressed_", ".jpg", getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);

        int quality = 100; // Start with maximum quality
        do {
            fos.flush();
            fos.close();

            fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);

            quality -= 5; // Reduce quality by 5% each iteration
        } while (tempFile.length() > maxFileSizeKB * 1024 && quality > 10); // Stop if quality is too low

        fos.close();
        return tempFile;
    }

    /**
     * Get a bitmap from the given URI.
     */
    private Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        return android.provider.MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    @Override
    public void onTrimMemory(int level) {
        // Handle memory trim events here
    }

    public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);

    public native Bitmap getGrayBitmap(Bitmap bitmap);

    public native Bitmap getMagicColorBitmap(Bitmap bitmap);

    public native Bitmap getBWBitmap(Bitmap bitmap);

    public native float[] getPoints(Bitmap bitmap);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("Scanner");
    }
}
