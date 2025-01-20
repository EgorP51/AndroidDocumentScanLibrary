package com.scanlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jhansi on 05/04/15.
 */
public class Utils {
    static Date currentTime;
    private Utils() {

    }

    public static Uri getUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int quality = 100;
        final int MAX_SIZE = 2 * 1024 * 1024;

        do {
            bytes.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bytes);
            int sizeInBytes = bytes.size();

            quality -= 5;

            if (quality < 0) {
                Log.wtf("IMAGE SIZE", "Image cannot be compressed below 0% quality");
                return null;
            }

            Log.wtf("CURRENT QUALITY", "Quality: " + quality + ", Size: " + sizeInBytes + " bytes");
        } while (bytes.size() > MAX_SIZE);

        Log.wtf("PATH", "before insertImage");
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title" + " - " + (currentTime = Calendar.getInstance().getTime()), null);
        Log.wtf("PATH", path);
        return Uri.parse(path);
    }


    public static Bitmap getBitmap(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
}