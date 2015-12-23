package com.example.mobserv.remoteapp.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.mobserv.remoteapp.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alessioalberti on 15/12/15.
 */
public class ThreeFragment extends Fragment{
    private ImageView contactImage;
    private Button buttonSavePic;
    private Bitmap imageBTM;
    final String TAG = "ThreeFragment";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_three, container, false);
        contactImage = (ImageView) view.findViewById(R.id.photo);
        buttonSavePic = (Button) view.findViewById(R.id.buttonSavePic);
        buttonSavePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveonSD(imageBTM);
                Toast.makeText(getContext(), "Saved", Toast.LENGTH_LONG).show();
            }
        });
        return view;
    }

    public ImageView getContactImage() {
        return contactImage;
    }

    public void setImageBTM(Bitmap b){
        this.imageBTM = b;
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
                + getContext().getPackageName()
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
