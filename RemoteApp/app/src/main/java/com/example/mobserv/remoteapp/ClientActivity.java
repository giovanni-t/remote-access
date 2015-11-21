package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

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

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_client);
      Intent it = getIntent();
      this.serverip = it.getStringExtra("serverip");

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
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   class ClientThread implements Runnable {

      @Override
      public void run() {

         try {
            InetAddress serverAddr = InetAddress.getByName(serverip);

            socket = new Socket(serverAddr, serverport);

         } catch (UnknownHostException e1) {
            e1.printStackTrace();
         } catch (IOException e1) {
            e1.printStackTrace();
         }

      }

   }

}
