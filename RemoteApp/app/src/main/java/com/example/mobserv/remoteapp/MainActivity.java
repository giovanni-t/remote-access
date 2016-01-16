package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Main activity of the app.
 * Let the user insert the IP of the running server
 */
public class MainActivity extends AppCompatActivity {
    String oldAddres = "";
    TextView twIP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        twIP = (TextView) findViewById(R.id.idIpAddrEditText);
        oldAddres = PreferenceManager.getDefaultSharedPreferences(this).getString("IP_ADDRESS", "");
        if (oldAddres.compareTo("")!=0){
            twIP.setText(oldAddres);
        } else {
            twIP.setText("");
        }
        //twIP.setBackgroundColor(Color.WHITE);
    }

    /*
    public void onClickServer (View view ){
        startActivity(new Intent("com.example.mobserv.remoteapp.ServerActivity"));
    }

    public void onClickClient (View view){
        Intent it = new Intent("com.example.mobserv.remoteapp.ClientActivity");
        EditText etIpaddr = (EditText) findViewById(R.id.idIpAddrEditText);
        String address = etIpaddr.getText().toString();
        it.putExtra(MyConstants.TAG_SERVERIP, address);
        startActivity(it);
    }
    */

    public void onClickChatUI (View view){
        Intent it = new Intent(MainActivity.this, ChatActivity.class);
        String address = ((EditText) findViewById(R.id.idIpAddrEditText)).getText().toString();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("IP_ADDRESS", address).commit();
        it.putExtra(MyConstants.TAG_SERVERIP, address);
        startActivity(it);
    }
}
