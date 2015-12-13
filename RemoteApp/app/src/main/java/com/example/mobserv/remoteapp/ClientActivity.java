package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
/**
 * Created by pacel_000 on 22/10/2015.
 */
public class ClientActivity extends FragmentActivity implements TaskFragment.TaskCallbacks {

    private static final String TAG = TaskFragment.class.getSimpleName();
    private static final boolean DEBUG = true; // Set this to false to disable logs .

    private static final int serverport = 45678;
    private static final String CHAT_HISTORY = "chatHistory";

    private Socket socket = null;
    private PrintWriter out;
    private String serverip = "";
    private TextView text;
    private Handler updateConversationHandler;
    private EditText et;
    private Thread th;
    private String myName;
    private SurfaceView mSurfaceView;
    private ImageView contactImage;
    CameraPreview preview;
    GPSTracker gpsTracker;
    private Boolean nameTaken = false;


    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private TaskFragment mTaskFragment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        text = (TextView) findViewById(R.id.idClientText);
        text.setMovementMethod(new ScrollingMovementMethod());
        et = (EditText) findViewById(R.id.idClientEditText);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        contactImage = (ImageView) findViewById(R.id.photo);

        FragmentManager fm = getSupportFragmentManager();
        mTaskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        Intent it = getIntent();
        this.serverip = it.getStringExtra("serverip");

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTaskFragment == null) {
            Bundle bd = new Bundle();
            bd.putString("serverip", serverip);
            mTaskFragment = new TaskFragment();
            mTaskFragment.setArguments(bd);
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }


        if(savedInstanceState == null) { // IF first launch of the activity


            updateConversationHandler = new Handler();

            preview = new CameraPreview(this, (SurfaceView) findViewById(R.id.surfaceView));
            preview.setKeepScreenOn(true);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mSurfaceView.setX(metrics.widthPixels + 1);
            if (this.serverip.isEmpty()) {
                et.setFocusable(false);
                myName = null;
                //th = new Thread(new ClientThread());
                //th.start();

                // TODO avoid reconnect when activity is created again, for ex. after rotation
                // (I tried using this if statement but is not effective)
                // an idea could be keep the bg thread alive somehow, and start it only when the
                // 'connect' button in the main activity is pressed
                // TODO: rotation also erases the text in the textview, which is the 'current conversation'
                // temporary fix: forbid rotation
            }

            gpsTracker = new GPSTracker(this, getParent());
        }
    }

    public void onClick(View view) {
        String str = et.getText().toString();
        sendMsg(str);
        et.setText(null);
    }

    /**
     * Write the string on the socket, no matter what is the format.
     * So the 'msg' string received need to be already in the right format
     *
     * @param msg the message to send
     */
    public void sendMsg(String msg) {
        out.write(msg);
        out.flush();
        updateConversationHandler.post(new updateUIThread(msg));
    }

    public void onClickEnterText(View view) {
        String tmp = et.getText().toString();
        tmp += "/" + ((Button) view).getText().toString();
        et.setText(tmp);
        et.setSelection(et.getText().toString().length());
    }

    class updateUIThread implements Runnable {
        private String msg;
        public updateUIThread(String str) { this.msg = str; }
        @Override
        public void run() {
            text.setText(text.getText().toString() + msg + "\n");
            // code below just makes the text scroll on update/receive of messages
            final Layout layout = text.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(text.getLineCount() - 1)
                        - text.getScrollY() - text.getHeight();
                if(scrollDelta > 0)
                    text.scrollBy(0, scrollDelta);
            }
        }
    }

    class updateUIImage implements Runnable {
        private Bitmap bitmap;
        public updateUIImage(Bitmap bitmap) {this.bitmap = bitmap; }
        @Override
        public void run() {
            contactImage.setImageBitmap(bitmap);
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
                        // close connection here
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

    class updateUIClientsList implements Runnable{
        Integer numOfClients;
        List<String> clientsList;
        public updateUIClientsList(Integer numOfClients, List<String> clientsList) {
            this.clientsList = clientsList;
            this.numOfClients = numOfClients;
        }

        @Override
        public void run() {
            ViewGroup linearLayout = (ViewGroup) findViewById(R.id.clientsLinearLayout);
            linearLayout.removeAllViews();
            for (String clientName : clientsList){
                // let's keep also own name so we can send msgs to ourselves for debugging purposes
                //if ( !clientName.equalsIgnoreCase(myName) ) {
                    Button bt = new Button(getApplicationContext());
                    bt.setText(clientName);
                    bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClickEnterText(v);
                        }
                    });
                    bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linearLayout.addView(bt);
                //}
            }
        }
    }

    class createNameDialog implements Runnable {
        Boolean alreadyTaken;
        ClientActivity activity;

        public createNameDialog(Boolean alreadyTaken) {
            this.alreadyTaken = alreadyTaken;
            this.activity = ClientActivity.this;
        }

        @Override
        public void run() {
            final EditText name = new EditText(activity);
            name.setHint("Name...");
            if (!alreadyTaken) {
                new AlertDialog.Builder(activity)
                        .setTitle("Please choose a username")
                        .setView(name)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                sendMsg(name.getText().toString());
                            }
                        }).create().show();
            } else {
                new AlertDialog.Builder(activity)
                        .setTitle("Please choose another usernname")
                        .setMessage("The name you chose had already been picked")
                        .setView(name)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                sendMsg(name.getText().toString());
                            }
                        }).create().show();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(CHAT_HISTORY, text.getText());
        // updateConversationHandler
        outState.putString("myName", myName);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        text.setText(savedInstanceState.getCharSequence(CHAT_HISTORY));
        myName = savedInstanceState.getString("myName");
    }

    @Override
    public void onShowToast(String str){
        runOnUiThread(new makeToast(str));
    }

    /************************/
    /***** LOGS & STUFF *****/
    /************************/

    @Override
    protected void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

}
