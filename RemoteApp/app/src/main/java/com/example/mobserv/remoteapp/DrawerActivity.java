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

    protected RelativeLayout fullLayout;
    protected FrameLayout frameLayout;

    @Override
    public void setContentView(int layoutResID) {
        fullLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_drawer, null);
        frameLayout = (FrameLayout) fullLayout.findViewById(R.id.drawer_frame);
        getLayoutInflater().inflate(layoutResID, frameLayout, true);
        super.setContentView(fullLayout);

        //My drawer content..
        // Set up the toolbar and the side menu toggle
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.navigation_drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,myToolbar,R.string.app_name,R.string.app_name);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
    }
}
