package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
   }

   public void onClickServer (View view ){
      startActivity(new Intent("com.example.mobserv.remoteapp.ServerActivity")); // TODO manifest
   }

   public void onClickClient (View view ){
      startActivity(new Intent("com.example.mobserv.remoteapp.ClientActivity")); // TODO manifest
   }
}
