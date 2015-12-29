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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoActivity extends DrawerActivity {
    private ImageView contactImage;
    private Button buttonSavePic;
    private Bitmap imageBTM;
    final String TAG = "Photo Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_photo);

        byte[] byteArray = getIntent().getByteArrayExtra("image");
        imageBTM = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        imageBTM = Bitmap.createBitmap(imageBTM, 0, 0, imageBTM.getWidth(), imageBTM.getHeight(), matrix, true);
        //imageBTM = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        defaultSave(imageBTM); // persistent storage of all the pictures receive within the current session
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
        File pictureFile = getOutputMediaFile("persistent");
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
    private  File getOutputMediaFile(String location){
        File mediaFile=null;
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());

        if ( location.compareTo("persistent")==0){
            File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                    + "/Android/data/"
                    + getApplicationContext().getPackageName()
                    + "/Files");
            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    return null;
                }
            }

            String mImageName="MI_"+ timeStamp +".jpg";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        } else if ( location.compareTo("temporary")==0 ){
            File f3=new File(getApplicationContext().getCacheDir()+"/inpaint/");
            if(!f3.exists()){
                f3.mkdirs();
            }
            //OutputStream outStream = null;
            mediaFile = new File(getApplicationContext().getCacheDir() + "/inpaint/"+timeStamp+".jpg");
        }
        return mediaFile;
    }

    public void defaultSave(Bitmap b){
        File pictureFile = getOutputMediaFile("temporary");
        Toast.makeText(getApplicationContext(), "Saved On: "+pictureFile, Toast.LENGTH_LONG).show();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            b.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }

    }
}
