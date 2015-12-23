package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickServer (View view ){
        startActivity(new Intent("com.example.mobserv.remoteapp.ServerActivity"));
    }

    public void onClickClient (View view ){
        Intent it = new Intent("com.example.mobserv.remoteapp.ClientActivity");
        EditText etIpaddr = (EditText) findViewById(R.id.idIpAddrEditText);
        String address = etIpaddr.getText().toString();
        it.putExtra("serverip", address);
        startActivity(it);
    }

    public void onClickChatUI (View view ){
        startActivity(new Intent(MainActivity.this, ChatActivity.class));
    }
}
