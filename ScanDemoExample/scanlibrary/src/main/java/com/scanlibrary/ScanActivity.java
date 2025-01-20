package com.scanlibrary;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;  // Import Log class

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhansi on 28/03/15.
 */
public class ScanActivity extends Activity implements IScanner, ComponentCallbacks2 {

    private static final String TAG = "ScanActivity"; // Tag for logging
    String[] permissions = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity created");

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
        Log.d(TAG, "onCreate: Permissions set, checking permissions");
        checkPermissions();
    }

    private void checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking permissions");
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            int result = ContextCompat.checkSelfPermission(this, p);
            Log.d(TAG, "checkPermissions: Checking permission " + p + " - result: " + result);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            Log.d(TAG, "checkPermissions: Requesting permissions: " + listPermissionsNeeded);
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return;
        }
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Permission request code: " + requestCode);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permissions granted");
                init();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permissions denied");
            }
        }
    }

    private void init() {
        Log.d(TAG, "init: Initializing ScanActivity");
        PickImageFragment fragment = new PickImageFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ScanConstants.OPEN_INTENT_PREFERENCE, getPreferenceContent());
        bundle.putInt("quality", getIntent().getIntExtra("quality", 1));
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        Log.d(TAG, "init: Fragment added");
        fragmentTransaction.commit();
    }

    protected int getPreferenceContent() {
        int preference = getIntent().getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
        Log.d(TAG, "getPreferenceContent: Preference content: " + preference);
        return preference;
    }

    @Override
    public void onBitmapSelect(Uri uri) {
        Log.d(TAG, "onBitmapSelect: Bitmap selected: " + uri);
        ScanFragment fragment = new ScanFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SELECTED_BITMAP, uri);
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ScanFragment.class.toString());
        Log.d(TAG, "onBitmapSelect: Fragment added for bitmap select");
        fragmentTransaction.commit();
    }

    @Override
    public void onScanFinish(Uri uri) {
        Log.d(TAG, "onScanFinish: Scan finished with result: " + uri);
        ResultFragment fragment = new ResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SCANNED_RESULT, uri);
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ResultFragment.class.toString());
        Log.d(TAG, "onScanFinish: Fragment added for scan result");
        fragmentTransaction.commit();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "onTrimMemory: Level: " + level);
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                Log.d(TAG, "onTrimMemory: UI hidden");
                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                Log.d(TAG, "onTrimMemory: Running low on memory");
                break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                Log.d(TAG, "onTrimMemory: Memory cleanup required");
                break;
            default:
                Log.d(TAG, "onTrimMemory: Unknown memory level");
                break;
        }
    }

    public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);
    public native Bitmap getGrayBitmap(Bitmap bitmap);
    public native Bitmap getMagicColorBitmap(Bitmap bitmap);
    public native Bitmap getBWBitmap(Bitmap bitmap);
    public native float[] getPoints(Bitmap bitmap);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("Scanner");
        Log.d(TAG, "Native libraries loaded");
    }
}
