package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

   private Socket socket;
   private static final int serverport = 45678;
   private String serverip;
   private TextView text;
   Handler updateConversationHandler;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        Intent it = getIntent();
        this.serverip = it.getStringExtra("serverip");

        text = (TextView) findViewById(R.id.idClientText);

        updateConversationHandler = new Handler();
        new Thread(new ClientThread()).start();
   }

   public void onClick(View view) {
      try {
         EditText et = (EditText) findViewById(R.id.idClientEditText);
         String str = et.getText().toString();
         PrintWriter out = new PrintWriter(new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream())),
                 true);
         out.println(str);
          // TODO find a way to get rid of the \n (print w/o ln doesnt work
          // TODO reset the edittext hint
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   class ClientThread implements Runnable {
      BufferedReader inputStream;

      @Override
      public void run() {

         try {
             InetAddress serverAddr = InetAddress.getByName(serverip);
             socket = new Socket(serverAddr, serverport);
//             this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

         } catch (Exception e){
             e.printStackTrace();
             Toast.makeText(getApplicationContext(), "ERROR :(\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
             finish();
             return;
         }

         /*while (!Thread.currentThread().isInterrupted()) {
            try {
               String read = inputStream.readLine();
               updateConversationHandler.post(new updateUIThread(read));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "ERROR :(\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
         }*/
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
}
