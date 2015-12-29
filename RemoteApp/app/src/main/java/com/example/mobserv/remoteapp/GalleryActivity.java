package com.example.mobserv.remoteapp;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class GalleryActivity extends DrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        showSavedPictures();
    }

    public void showSavedPictures(){
        String path = getApplicationContext().getCacheDir() + "/inpaint/";
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        Log.d("Files", "Size: "+ file.length);
        for (int i=0; i < file.length; i++)
        {
            Log.d("Files", "FileName:" + file[i].getName());
            Toast.makeText(getApplicationContext(), file[i].getName(), Toast.LENGTH_SHORT).show();
        }
    }
}
