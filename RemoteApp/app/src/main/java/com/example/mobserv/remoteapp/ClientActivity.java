package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by pacel_000 on 22/10/2015.
 */
public class ClientActivity extends Activity {

    private Socket socket = null;
    private static final int serverport = 45678;
    private String serverip = "";
    private TextView text;
    Handler updateConversationHandler;
    EditText et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        Intent it = getIntent();
        text = (TextView) findViewById(R.id.idClientText);
        et = (EditText) findViewById(R.id.idClientEditText);
        updateConversationHandler = new Handler();

        if(this.serverip.isEmpty()) {
            this.serverip = it.getStringExtra("serverip");
            et.setFocusable(false);
            new Thread(new ClientThread()).start();
            // TODO avoid reconnect when activity is created again, for ex. after rotation
            // (I tried using this if statement but is not effective)
            // an idea could be keep the bg thread alive somehow, and start it only when the
            // 'connect' button in the main activity is pressed
            // TODO: rotation also erases the text in the textview, which is the 'current conversation'
        }
    }

    public void onClick(View view) {
        try {
            String str = et.getText().toString();
            PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())),
            true);

            out.write(str);
            out.flush();
            // tried to make sent text blue but is not working this way:
//            str = "<font color=blue>"+str+"</font>";
//            text.setText(text.getText().toString() + Html.fromHtml(str) + "\n");

            text.setText(text.getText().toString() + str + "\n");
            et.setText(null);

        } catch (UnknownHostException e) {
            e.printStackTrace();
            runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
        }
    }


    class ClientThread implements Runnable {
        BufferedReader inputStream;

        @Override
        public void run() {
            runOnUiThread(new makeToast("Connecting to " + serverip + ":" + serverport + "..."));
            try {
                InetAddress serverAddr = InetAddress.getByName(serverip);
                socket = new Socket(serverAddr, serverport);
                this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

             // success =)
                runOnUiThread(new makeToast("Connected to " + serverAddr + " " + serverport));
                runOnUiThread(new Runnable() {@Override public void run() {et.setFocusableInTouchMode(true);}});
            } catch (UnknownHostException e) {
                e.printStackTrace();
                runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                finish();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                finish();
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = inputStream.readLine();
                    updateConversationHandler.post(new updateUIThread(read));
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                    finish();
                    return;
                }
            }
        }
    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            text.setText(text.getText().toString()+ msg + "\n");
        }
    }

    class makeToast implements Runnable{
        private String msg;

        public makeToast(String msg){ this.msg = msg; }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
