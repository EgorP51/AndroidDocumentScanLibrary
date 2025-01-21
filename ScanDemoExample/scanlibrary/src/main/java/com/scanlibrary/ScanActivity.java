package com.scanlibrary;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

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
            // Получаем изображение из Uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

            // Сжимаем изображение до минимального качества
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos); // 10 - это качество, можно уменьшить для меньшего размера
            byte[] imageBytes = baos.toByteArray();

            // Преобразуем сжатые байты обратно в Bitmap
            Bitmap compressedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            // Передаем сжатое изображение в ScanFragment
            ScanFragment fragment = new ScanFragment();
            Bundle bundle = new Bundle();
            Uri compressedUri = saveCompressedBitmap(compressedBitmap); // Метод для сохранения
            bundle.putParcelable(ScanConstants.SELECTED_BITMAP, compressedUri);
            fragment.setArguments(bundle);
            android.app.FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.content, fragment);
            fragmentTransaction.addToBackStack(ScanFragment.class.toString());
            fragmentTransaction.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для сохранения сжатого Bitmap и получения Uri
    private Uri saveCompressedBitmap(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "compressed_image.jpg"); // Сохранение во временное хранилище
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, fos); // Сжимаем до 10%
            fos.flush();
            fos.close();
            return Uri.fromFile(file); // Возвращаем Uri
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    @Override
    public void onTrimMemory(int level) {
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                // Освобождаем любые объекты UI, которые занимают память
                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                // Освобождаем ненужную память
                break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                // Освобождаем как можно больше памяти
                break;
            default:
                // Освобождаем любые некритичные структуры данных
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
    }
}
