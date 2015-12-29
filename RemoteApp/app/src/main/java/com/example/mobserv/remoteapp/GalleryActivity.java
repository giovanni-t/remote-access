package com.example.mobserv.remoteapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class GalleryActivity extends DrawerActivity {
    private GridView imageGrid;
    private ArrayList<Bitmap> bitmapList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        this.imageGrid = (GridView) findViewById(R.id.gridview);
        this.bitmapList = new ArrayList<Bitmap>();

        showSavedPictures();

        this.imageGrid.setAdapter(new ImageAdapter(this, this.bitmapList));

    }

    public void showSavedPictures(){
        String path = getApplicationContext().getCacheDir() + "/inpaint/";
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();

        if ( file != null ) {
            Log.d("Files", "Size: " + file.length);
            try {
                for (int i = 0; i < file.length; i++) {
                    Log.d("Files", "FileName:" + file[i].getName());
                    Bitmap toadd = BitmapFactory.decodeFile(path + file[i].getName());
                    this.bitmapList.add(toadd);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
