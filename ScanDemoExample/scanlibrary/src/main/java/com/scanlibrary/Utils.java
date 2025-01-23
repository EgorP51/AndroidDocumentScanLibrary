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
import android.graphics.BitmapFactory;


/**
 * Created by jhansi on 05/04/15.
 */
public class Utils {
    static Date currentTime;
    private Utils() {

    }

    public static Uri getUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, bytes);
        Log.wtf("PATH", "before insertImage");
        // String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title" + " - " + (currentTime = Calendar.getInstance().getTime()), null);
        Log.wtf("PATH", path);
        return Uri.parse(path);
    }

    public static Bitmap getBitmap(Context context, Uri uri) throws IOException {
        // Получаем bitmap без сжатия
        Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

        // Изменение размера изображения
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 800, 800, true);

        // Сжимаем изображение в формат JPEG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);

        // Освобождаем оригинальный Bitmap из памяти
        originalBitmap.recycle();

        byte[] compressedData = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length);

    }
}