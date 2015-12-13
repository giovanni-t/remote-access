package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by pacellig on 13/12/2015.
 */
public class MapActivity extends Activity implements LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener {

    Double lat, lon;
    MapFragment mapFragment;
    GoogleMap googleMap;
    String nametoshow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapfragment);

        Intent it = getIntent();
        Bundle b = it.getExtras();
        lat = b.getDouble("latitude");
        lon =b.getDouble("longitude");
        nametoshow = b.getString("nametoshow");

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        LatLng position = new LatLng(lat,lon);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)
                .zoom(17)
                .bearing(90)
                .tilt(30)
                .build();
        this.googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(nametoshow)
                        .snippet(String.valueOf(position.latitude)+","+String.valueOf(position.longitude))
        );
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(this);
    }
}
