package com.example.mobserv.remoteapp.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.mobserv.remoteapp.R;

/**
 * Created by alessioalberti on 15/12/15.
 */
public class ThreeFragment extends Fragment{
    private ImageView contactImage;
    private Bitmap bitmap;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_three, container, false);
        contactImage = (ImageView) view.findViewById(R.id.photo);
        contactImage.setImageBitmap(bitmap);
        return view;
    }
    public void setBitmap(Bitmap bitmap){this.bitmap = bitmap;}
}
