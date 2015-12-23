package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoActivity extends Activity {
    private ImageView contactImage;
    private Button buttonSavePic;
    private Bitmap imageBTM;
    final String TAG = "ThreeFragment";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ///*******BEGIN DEFAULT*******/
        setContentView(R.layout.content_photo);
       // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      //  setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        byte[] byteArray = getIntent().getByteArrayExtra("image");
        imageBTM = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        imageBTM = Bitmap.createBitmap(imageBTM, 0, 0, imageBTM.getWidth(), imageBTM.getHeight(), matrix, true);
        //imageBTM = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        /*******END DEFAULT*******/
        contactImage = (ImageView) findViewById(R.id.photo);
        contactImage.setImageBitmap(imageBTM);
        buttonSavePic = (Button) findViewById(R.id.buttonSavePic);
        buttonSavePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveonSD(imageBTM);
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveonSD(Bitmap b){
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            b.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }

    }
    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
}
