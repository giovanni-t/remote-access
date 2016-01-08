package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

/**
 * Main activity of the app.
 * Let the user insert the IP of the running server
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickClient (View view){
        Intent it = new Intent(MainActivity.this, ChatActivity.class);
        String address = ((EditText) findViewById(R.id.idIpAddrEditText)).getText().toString();
        it.putExtra(MyConstants.TAG_SERVERIP, address);
        startActivity(it);
    }
}
