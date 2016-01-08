package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class GalleryImageActivity extends AppCompatActivity {

    Bitmap b = null;
    ImageView imageView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_image);
        imageView = (ImageView) findViewById(R.id.imageToBig);
        Intent intent = getIntent();
        b = intent.getParcelableExtra("BitmapImage");
        imageView.setImageBitmap(b);
    }
}
