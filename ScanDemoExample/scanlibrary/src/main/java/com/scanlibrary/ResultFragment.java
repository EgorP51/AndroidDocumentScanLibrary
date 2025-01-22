package com.scanlibrary;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by jhansi on 29/03/15.
 */
public class ResultFragment extends Fragment {

    private View view;
    private ImageView scannedImageView;
    private Button doneButton;
    private Bitmap original;
    private Button originalButton;
    private Button MagicColorButton;
    private Button grayModeButton;
    private Button bwButton;
    private Button rotanticButton;
    private Button rotcButton;
    private Bitmap transformed;
    private Bitmap rotoriginal;
    private static ProgressDialogFragment progressDialogFragment;

    public ResultFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.result_layout, null);
        init();
        return view;
    }

    private void init() {
        Log.i("ResultFragment", "ResultFragment.java init start");
        scannedImageView = (ImageView) view.findViewById(R.id.scannedImage);
        originalButton = (Button) view.findViewById(R.id.original);
        if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_ORG_TEXT) != null){
            originalButton.setText(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_ORG_TEXT));
        }
        originalButton.setOnClickListener(new OriginalButtonClickListener());
        MagicColorButton = (Button) view.findViewById(R.id.magicColor);
        MagicColorButton.setOnClickListener(new MagicColorButtonClickListener());
        grayModeButton = (Button) view.findViewById(R.id.grayMode);
        grayModeButton.setOnClickListener(new GrayButtonClickListener());
        bwButton = (Button) view.findViewById(R.id.BWMode);
        if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_BNW_TEXT) != null){
            bwButton.setText(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_BNW_TEXT));
        }
        bwButton.setOnClickListener(new BWButtonClickListener());

        rotanticButton = (Button) view.findViewById(R.id.rotanticButton);
        rotanticButton.setOnClickListener(new ResultFragment.RotanticlockButtonClickListener());
        if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_ROTATE_LEFT_TEXT) != null){
            rotanticButton.setText(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_ROTATE_LEFT_TEXT));
        }
        rotcButton = (Button) view.findViewById(R.id.rotcButton);
        if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_ROTATE_RIGHT_TEXT) != null){
            rotcButton.setText(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_ROTATE_RIGHT_TEXT));
        }
        rotcButton.setOnClickListener(new ResultFragment.RotclockButtonClickListener());

        Bitmap bitmap = getBitmap();
        transformed = bitmap;
        rotoriginal = bitmap;
        setScannedImage(bitmap);
        doneButton = (Button) view.findViewById(R.id.doneButton);
        if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_SAVE_TEXT) != null){
            doneButton.setText(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_SAVE_TEXT));
        }
        doneButton.setOnClickListener(new DoneButtonClickListener());
    }

    private Bitmap getBitmap() {
        Uri uri = getUri();
        try {
            original = Utils.getBitmap(getActivity(), uri);
            getActivity().getContentResolver().delete(uri, null, null);
            return original;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Uri getUri() {
        Uri uri = getArguments().getParcelable(ScanConstants.SCANNED_RESULT);
        return uri;
    }

    public void setScannedImage(Bitmap scannedImage) {
        // Define the output stream for compression
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Compress the bitmap to JPEG format with 80% quality
        boolean isCompressed = scannedImage.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);

        if (isCompressed) {
            // Convert the compressed image to a byte array
            byte[] compressedImageData = outputStream.toByteArray();

            // Optionally, save the compressed image to a file (example code commented out)
            // FileOutputStream fileOutputStream = new FileOutputStream(new File(getExternalFilesDir(null), "compressed_image.jpg"));
            // fileOutputStream.write(compressedImageData);
            // fileOutputStream.close();

            // Decode the byte array back into a Bitmap
            Bitmap compressedBitmap = BitmapFactory.decodeByteArray(compressedImageData, 0, compressedImageData.length);

            // Set the compressed Bitmap to the ImageView
            scannedImageView.setImageBitmap(compressedBitmap);
        } else {
            // Handle the error case where compression failed
            Log.e("ImageCompression", "Failed to compress the image");
        }
    }


    private class DoneButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_LOADING_MESSAGE) != null){
                showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_LOADING_MESSAGE));
            }else{
                showProgressDialog(getResources().getString(R.string.loading));
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent data = new Intent();
                        Bitmap bitmap = transformed;
                        if (bitmap == null) {
                            bitmap = original;
                        }
                        Uri uri = Utils.getUri(getActivity(), bitmap);
                        data.putExtra(ScanConstants.SCANNED_RESULT, uri);
                        getActivity().setResult(Activity.RESULT_OK, data);
                        original.recycle();
                        System.gc();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dismissDialog();
                                getActivity().finish();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private class BWButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE) != null){
                showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE));
            }else{
                showProgressDialog(getResources().getString(R.string.applying_filter));
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed = ((ScanActivity) getActivity()).getBWBitmap(rotoriginal);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class MagicColorButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE) != null){
                showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE));
            }else{
                showProgressDialog(getResources().getString(R.string.applying_filter));
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed = ((ScanActivity) getActivity()).getMagicColorBitmap(rotoriginal);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class OriginalButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE) != null){
                    showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE));
                }else{
                    showProgressDialog(getResources().getString(R.string.applying_filter));
                }
                transformed = rotoriginal;
                scannedImageView.setImageBitmap(rotoriginal);
                dismissDialog();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                dismissDialog();
            }
        }
    }

    private class GrayButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE) != null){
                showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE));
            }else{
                showProgressDialog(getResources().getString(R.string.applying_filter));
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed = ((ScanActivity) getActivity()).getGrayBitmap(rotoriginal);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class RotanticlockButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE) != null){
                showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE));
            }else{
                showProgressDialog(getResources().getString(R.string.applying_filter));
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        //android.graphics.Matrix matrix = new android.graphics.Matrix();
                        // matrix.postRotate(90);

                        Bitmap imageViewBitmap=((android.graphics.drawable.BitmapDrawable)scannedImageView.getDrawable()).getBitmap();

                        android.graphics.Matrix matrix = new android.graphics.Matrix();
                        matrix.postRotate(-90);
                        rotoriginal = Bitmap.createBitmap(rotoriginal, 0, 0, rotoriginal.getWidth(), rotoriginal.getHeight(), matrix, true);
                        transformed = Bitmap.createBitmap(imageViewBitmap, 0, 0, imageViewBitmap.getWidth(), imageViewBitmap.getHeight(), matrix, true);

                        //transformed = ((ScanActivity) getActivity()).getBWBitmap(original);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }



    private class RotclockButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            if(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE) != null){
                showProgressDialog(getActivity().getIntent().getStringExtra(ScanConstants.SCAN_APPLYING_FILTER_MESSAGE));
            }else{
                showProgressDialog(getResources().getString(R.string.applying_filter));
            }
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap imageViewBitmap=((android.graphics.drawable.BitmapDrawable)scannedImageView.getDrawable()).getBitmap();

                        android.graphics.Matrix matrix = new android.graphics.Matrix();
                        matrix.postRotate(90);
                        rotoriginal = Bitmap.createBitmap(rotoriginal, 0, 0, rotoriginal.getWidth(), rotoriginal.getHeight(), matrix, true);
                        transformed = Bitmap.createBitmap(imageViewBitmap, 0, 0, imageViewBitmap.getWidth(), imageViewBitmap.getHeight(), matrix, true);

                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    protected synchronized void disableButtons() {
        doneButton.setEnabled(false);
        originalButton.setEnabled(false);
        MagicColorButton.setEnabled(false);
        grayModeButton.setEnabled(false);
        bwButton.setEnabled(false);
        rotanticButton.setEnabled(false);
        rotcButton.setEnabled(false);
    }

    protected synchronized void enableButtons() {
        doneButton.setEnabled(true);
        originalButton.setEnabled(true);
        MagicColorButton.setEnabled(true);
        grayModeButton.setEnabled(true);
        bwButton.setEnabled(true);
        rotanticButton.setEnabled(true);
        rotcButton.setEnabled(true);
    }

    protected synchronized void showProgressDialog(String message) {
        disableButtons();
        if (progressDialogFragment != null && progressDialogFragment.isVisible()) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment.dismissAllowingStateLoss();
        }
        progressDialogFragment = null;
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected synchronized void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
        enableButtons();
    }
}