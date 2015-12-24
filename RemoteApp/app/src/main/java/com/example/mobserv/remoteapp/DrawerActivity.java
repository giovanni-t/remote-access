package com.example.mobserv.remoteapp;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Created by giovanni on 24/12/15.
 */
public class DrawerActivity extends AppCompatActivity {

    protected DrawerLayout fullLayout;
    protected FrameLayout frameLayout;

    @Override
    public void setContentView(int layoutResID) {
        fullLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_drawer, null);
        frameLayout = (FrameLayout) fullLayout.findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, frameLayout, true);
        super.setContentView(fullLayout);

        // Set up the toolbar and the side menu toggle
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
}
