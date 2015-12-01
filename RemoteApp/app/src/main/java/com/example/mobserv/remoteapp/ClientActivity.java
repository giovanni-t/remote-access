package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
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
    private Handler updateConversationHandler;
    private EditText et;
    private Thread th;
    private String myName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        Intent it = getIntent();
        text = (TextView) findViewById(R.id.idClientText);
        text.setMovementMethod(new ScrollingMovementMethod());

        et = (EditText) findViewById(R.id.idClientEditText);
        updateConversationHandler = new Handler();

        if(this.serverip.isEmpty()) {
            this.serverip = it.getStringExtra("serverip");
            et.setFocusable(false);
            myName = "unknown";
            th = new Thread(new ClientThread());
            th.start();

            // TODO avoid reconnect when activity is created again, for ex. after rotation
            // (I tried using this if statement but is not effective)
            // an idea could be keep the bg thread alive somehow, and start it only when the
            // 'connect' button in the main activity is pressed
            // TODO: rotation also erases the text in the textview, which is the 'current conversation'
            // temporary fix: forbid rotation
        }
    }

    public void onClick(View view) {
        String str = et.getText().toString();
        sendMsg(str);
    }

    /** Write the string on the socket, no matter what is the format.
     *  So the 'msg' string received need to be already in the right format
     * @param msg
     */
    public void sendMsg(String msg){
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);

            out.write(msg);
            out.flush();
            // tried to make sent text blue but is not working this way:
//            str = "<font color=blue>"+str+"</font>";
//            text.setText(text.getText().toString() + Html.fromHtml(str) + "\n");

            text.setText(text.getText().toString() + msg + "\n");
            final Layout layout = text.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(text.getLineCount() - 1)
                        - text.getScrollY() - text.getHeight();
                if(scrollDelta > 0)
                    text.scrollBy(0, scrollDelta);
            }
            et.setText(null);
            if (myName.equals("unknown"))
                myName = msg;

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

    public void onClickWrite(View view){
        String tmp = et.getText().toString();
        tmp += "/write/";
        et.setText(tmp);
        et.setSelection(et.getText().toString().length());
    }

    public void onClickRead(View view){
        String tmp = et.getText().toString();
        tmp += "/read/";
        et.setText(tmp);
        et.setSelection(et.getText().toString().length());
    }

    public void onClickExec(View view){
        String tmp = et.getText().toString();
        tmp += "/exec/";
        et.setText(tmp);
        et.setSelection(et.getText().toString().length());
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
                    String splits1[] = read.split(" ");
                    String splits2[] = splits1[1].split("/");
                    //for(String s : splits2)
                        //runOnUiThread(new makeToast("received "+s));
                    updateConversationHandler.post(new updateUIThread(read));
                    String senderName = splits1[0].substring(1,splits1[0].length()-1);
                    messageDispatch(senderName, splits2);
                } catch (IOException e) {
                    e.printStackTrace();
                    if(!socket.isClosed())
                        runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                    else
                        runOnUiThread(new makeToast(e.getMessage()));
                    finish();
                    return;
                }
            }
        }

        public void messageDispatch(String senderName, String[] args){
            boolean isBroadcast = false;

            if(! args[1].equals(myName))
                isBroadcast = true;

            if(!isBroadcast){
                switch (args[2]){
                    case "read":
                        messageIsRead(senderName, args);
                        break;
                    case "write":
                        // TODO: 11/30/15
                        break;
                    case "exec":
                        // TODO: 11/30/15
                        break;
                    default:
                        // this should never happen if the server is well behaved
                        runOnUiThread(new makeToast("Unknown message:\n"+ TextUtils.join("/", args)));
                        break;
                }
            } else {
                // isBroadcast == true
            }
        }

        public void messageIsRead(String senderName, String[] args){
            // TODO: 11/30/15
            String[] reply = null;
            switch (args[3]){
                case "gps":
                    // TODO: 12/1/15
                    // temporary fake position
                    reply[0]="12234";
                    reply[1]="56789";
                    break;
                default:
                    runOnUiThread(new makeToast("Unknown message:\n"+ TextUtils.join("/", args)));
                    break;
            }

            String msg = composeMsg(senderName, reply);
            sendMsg(msg);
        }

        public String composeMsg(String to, String[] content){
            String msg = "/"; // <-- leaving field 0 empty
            msg.concat(to);
            if(content == null)
                return msg;
            for(String arg : content){
                msg.concat("/").concat("arg");
            }
            return msg;
        }

    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            text.setText(text.getText().toString() + msg + "\n");
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

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to close connection to server?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        ClientActivity.super.onBackPressed();
                        // TODO try to close connection here
                        try {
                            th.interrupt();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                        }
                    }
                }).create().show();
    }


    /*
    // try if the two overrides wold preserve the connection, but they don't
    // actually, it would be better to close the connection to the server on exit
    // DONE: 11/30/15 - user is asked for a confirmation before leaving the activity,
    //                  which closes the connection
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putString("ipaddr", serverip);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        serverip = (String) savedInstanceState.getString("ipaddr");
        super.onRestoreInstanceState(savedInstanceState);
    }
    */
}
