package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GalleryImageActivity extends AppCompatActivity {

    Bitmap b = null;
    ImageView imageView = null;
    final String TAG = "Gallery activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_image);
        imageView = (ImageView) findViewById(R.id.imageToBig);
        Intent intent = getIntent();
        b = intent.getParcelableExtra("BitmapImage");
        imageView.setImageBitmap(b);
    }

    public void onClickSaveOnSDGallery(View view){
        saveonSD(b);
        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_LONG).show();
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
}
