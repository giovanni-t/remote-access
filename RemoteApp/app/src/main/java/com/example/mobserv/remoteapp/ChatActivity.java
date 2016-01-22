package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Client Activity with new GUI
 */
public class ChatActivity extends DrawerActivity implements TaskFragment.TaskCallbacks {

    // Constants
    private static final String TITLE = "Remote Access"; // Main toolbar title!
    private static final String TAG = ChatActivity.class.getSimpleName();
    private static final int SEND_GPS = 111;

    // UI elements
    private EditText messageET;
    private ListView messagesContainer;
    private Button sendBtn;
    private ChatAdapter mChatAdapter;
    private ChatFragment chatFragment;
    private ViewGroup suggestionsLL;
    private int suggestionsState;

    // Connection parameters
    private String serverip = "another dummy IP"; // Retrieved by the intent
    private boolean connected = false;

    // Clients
    private boolean nameTaken;
    private String myName = "";

    // Connected fragment
    private TaskFragment mTaskFragment;

    // Camera preview -- To be moved in other activity(?)

    private SurfaceView mSurfaceView;
    private ImageView contactImage;
    private CameraPreview preview;


    /* Streaming */
    private boolean isStreaming = false;
    private static ArrayList<String> IpList;

    /* Subscription */
    private List<Subscriber> subscribers;
    final Handler singleTimer = new Handler();
    private List<TimerTask> subscribersTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setTitle(TITLE);
        this.serverip = getIntent().getStringExtra(MyConstants.TAG_SERVERIP);

        initControls();
        initChatFragment();
        //loadDummyHistory();
        initCamera();
        initTaskFragment();

        subscribers = new LinkedList<>();
        subscribersTimer = new LinkedList<>();

    }

    private void initCamera(){
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceViewNew);
        contactImage = (ImageView) findViewById(R.id.photo);

        preview = new CameraPreview(this, (SurfaceView) findViewById(R.id.surfaceViewNew));
        preview.setKeepScreenOn(true);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mSurfaceView.setX(metrics.widthPixels + 1);
    }

    private void initControls() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        suggestionsLL = (ViewGroup) findViewById(R.id.suggestionsLinearLayout);
        suggestionsState = 0;
        suggestClean(); // clean example buttons which are loaded from the xml
        messageET = (EditText) findViewById(R.id.messageEdit);
        if(!connected){
            messageET.setFocusable(false);
        }
        sendBtn = (Button) findViewById(R.id.chatSendButton);
        sendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String messageText = messageET.getText().toString();
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(mChatAdapter.getCount() + 1);
                chatMessage.setMessage(messageText);
                chatMessage.setSenderName(myName);
                chatMessage.setIsMe(true);
                messageET.setText("");

                mTaskFragment.sendMsg(messageText);
                displayMessage(chatMessage);
            }
        });

        messageET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int slashes = 0;
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) == '/')
                        slashes++;
                    else if (s.charAt(i) == ';'){
                        slashes = 0;
                    }
                }
                if (slashes != suggestionsState) { // to avoid overhead
                    suggestionsState = slashes;
                    setSuggestions(suggestionsState);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }



    private void initChatFragment() {
        FragmentManager fm = getSupportFragmentManager();
        chatFragment = (ChatFragment) fm.findFragmentByTag(MyConstants.TAG_CHAT_FRAGMENT);
        // If the Fragment is non-null, then it is currently being retained across
        // a configuration change, but otherwise we instantiate a NEW ONE
        if(chatFragment == null) {
            chatFragment = new ChatFragment();
            fm.beginTransaction().add(chatFragment, MyConstants.TAG_CHAT_FRAGMENT).commit();
        }

        mChatAdapter = new ChatAdapter(ChatActivity.this, chatFragment.getChat());
        messagesContainer.setAdapter(mChatAdapter);
    }

    private void initTaskFragment() {
        FragmentManager fm = getSupportFragmentManager();
        mTaskFragment = (TaskFragment) fm.findFragmentByTag(MyConstants.TAG_TASK_FRAGMENT);
        // If the Fragment is non-null, then it is currently being retained across
        // a configuration change, but otherwise we instantiate a NEW ONE
        if (mTaskFragment == null) {
            Bundle bd = new Bundle();
            bd.putString(MyConstants.TAG_SERVERIP, serverip);
            mTaskFragment = new TaskFragment();
            mTaskFragment.setArguments(bd);
            fm.beginTransaction().add(mTaskFragment, MyConstants.TAG_TASK_FRAGMENT).commit();
        }
    }

    public void displayMessage(ChatMessage message) {
        mChatAdapter.add(message);
        mChatAdapter.notifyDataSetChanged();
        scrollMessageView();
    }

    public void scrollMessageView(){
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void loadDummyHistory(){
        ArrayList<ChatMessage> chatHistory = new ArrayList<>();
        ChatMessage msg = new ChatMessage();
        msg.setId(1);
        msg.setIsMe(false);
        msg.setMessage("Hi");
        msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatHistory.add(msg);
        ChatMessage msg1 = new ChatMessage();
        msg1.setId(2);
        msg1.setIsMe(false);
        msg1.setMessage("How r u doing???");
        msg1.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatHistory.add(msg1);
        // display and add
        for(int i=0; i<chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            displayMessage(message);
        }
    }

    private void suggestCommands(){
        List<String> l = new ArrayList<>();
        l.add("read");
        l.add("write");
        l.add("exec");
        setSuggestions(l);

        Button bt = new Button(getApplicationContext());
        bt.setText("send position");
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSendGPS(v);
            }
        });
        bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        suggestionsLL.addView(bt);
    }

    private void suggestActions(){
        List<String> l = new ArrayList<>();
        l.add("gps");
        l.add("photo");
        l.add("live");
        setSuggestions(l);
    }

    private void suggestSemicolon(){
        List<String> l = new ArrayList<>();
        l.add(";");
        setSuggestions(l);
    }

    private void suggestUserNames(){
        List<String> clientsList = chatFragment.getClients();
        clientsList.remove(myName);
        setSuggestions(clientsList);
    }

    private void setSuggestions(List<String> stringList){
        suggestClean();
        for (final String s : stringList) {
            Button bt = new Button(getApplicationContext());
            bt.setText(s);
            bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSuggestionClick(v);
                }
            });
            bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            suggestionsLL.addView(bt);
        }
    }

    private void setSuggestions(int state) {
        switch (state) {
            case 0:
                // suggest user names
                suggestUserNames();
                break;
            case 1:
                // suggest commands (read/write(?)/exec)
                suggestCommands();
                break;
            case 2:
                // suggest actions (gps, photo, ...)
                suggestActions();
                break;
            default: // #'/' > 2
                // suggest semicolon
                suggestSemicolon();
                break;
        }
    }

    private void onSuggestionClick(View v) {
        String buttonText = ((Button) v).getText().toString();
        String tmp = messageET.getText().toString();
        tmp += "/" + buttonText;
        if (buttonText.equals(";")){
            String dest = tmp.split("/")[1];
            tmp += "/" + dest;
        }
        messageET.setText(tmp);
        messageET.setSelection(messageET.getText().toString().length());
    }

    private void suggestClean(){
        suggestionsLL.removeAllViews();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.are_you_sure))
                .setMessage(getResources().getString(R.string.connection_will_be_close))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        ChatActivity.super.onBackPressed();
                        // close connection here --> kill the fragment
                        onTaskFragmentCancel();
                    }
                }).create().show();
    }

    /********************************
     * TaskCallbacks implementation *
     ********************************/

    @Override
    public void onConnected() {
        runOnUiThread(new MakeToast("Connected to " + serverip + " " + MyConstants.serverport));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageET.setFocusableInTouchMode(true);
            }
        });
        setNameTaken(false);
        setConnected(true);
    }

    @Override
    public void onTaskFragmentCancel() {
        mTaskFragment.closeSocket();
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().remove(mTaskFragment).commit();
        // instead of simply deleting the file, append the content to another log file 'total' for the app
        FileOutputStream fos;
        FileInputStream fis;
        String str = "";
        try {
            fos = getApplicationContext().openFileOutput(MyConstants.TOTAL_LOG_FILENAME, Context.MODE_APPEND);
            fis = getApplicationContext().openFileInput(MyConstants.LOG_FILENAME);
            int c;
            String temp="";
            while( (c = fis.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            //string temp contains all the data of the file.
            fos.write(str.getBytes());
            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("FILEOUTPUT", "File not found exception");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("FILEOUTPUT", "IO error -> "+ str );
        }
        // delete session file
        getApplicationContext().deleteFile(MyConstants.LOG_FILENAME);
        // finish
        finish(); // exit also from this activity
    }

    @Override
    public void onTextReceived(String str) {
        ChatMessage msg = new ChatMessage();
        msg.setId(mChatAdapter.getCount()+1);
        msg.setIsMe(false);
        int space = str.indexOf(" ");
        try {
            String sender = str.substring(0, space);
            msg.setSenderName(sender);
            msg.setSenderId(chatFragment.getSenderId(sender));
            msg.setMessage(str.substring(space + 1));
        } catch (IndexOutOfBoundsException e){
            Log.d(TAG, "Error in onTextReceived: trying to find sender, " +
                    "cannot split according to space ["+str+"]");
        }
        runOnUiThread(new DisplayMessage(msg));
    }

    @Override
    public void onShowToast(String str) {
        runOnUiThread(new MakeToast(str));
    }

    @Override
    public void onChooseName(Boolean taken) {
        runOnUiThread(new CreateNameDialog(taken));
    }

    @Override
    public void onImageReceived(byte[] imageByte) {
        //Convert to byte array
        Intent in1 = new Intent(this, PhotoActivity.class);
        in1.putExtra("image", imageByte);
        startActivity(in1);
    }

    @Override
    public void onClientListReceived(int numOfClients, List<String> clients) {
        chatFragment.setClients(clients);
        if(suggestionsState == 0)
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    suggestUserNames();
                }
            });
    }

    @Override
    public void onIpListReceived(int numOfIps, ArrayList<String> ips) {
        Log.d(TAG, "number of ips " + numOfIps);
        //runOnUiThread(new notifyTabStripChanged(1, numOfIps));
        IpList = ips;
    }

    @Override
    public void onWelcome(String myName) {
        setNameTaken(true);
        setMyName(myName);
    }

    @Override
    public void onExecReceived(String subscriberName, String service) {
        final Subscriber s = new Subscriber(subscriberName, service);
        subscribers.add(s);
        Timer timer = new Timer();
        subscribersTimer.add(new TimerTask() {
            @Override
            public void run() {
                singleTimer.post(new Runnable() {
                    public void run() {
                        // Toast.makeText(getBaseContext(), "try timer", Toast.LENGTH_SHORT).show();
                        LinkedList<String> reply = new LinkedList<>();
                        reply.add("write");
                        reply.add("gps");
                        reply.add(String.valueOf(mTaskFragment.gpsTracker.longitude));
                        reply.add(String.valueOf(mTaskFragment.gpsTracker.latitude));
                        reply.add(String.valueOf(mTaskFragment.gpsTracker.altitude));
                        reply.add("subscription"); // otherwise maps keep opening
                        String msg = composeMsg(s.name, reply);
                        mTaskFragment.sendMsg(msg);
                    }
                });
            }
        });
        timer.schedule(subscribersTimer.get(subscribersTimer.size() - 1), 0, 60000); //it executes this every 60000ms ( 1 minute ) TODO time should be passed
    }

    @Override
    public void onStopTimers() {
        for (TimerTask t : subscribersTimer) {
            t.cancel();
        }
    }

    public String composeMsg(String to, LinkedList<String> content) {
        String msg = "/"; // <-- leaving field 0 empty
        // Log.d("composeMsg", "to: "+ to+" Content: "+content.toString());
        msg += to;
        if (content == null)
            return msg;
        for (String arg : content) {
            msg += "/" + arg;
        }
        return msg;
    }

    private class Subscriber {
        public String name, service;

        public Subscriber(String n, String s) {
            name = n;
            service = s;
        }
    }

    @Override
    public String onLiveRequested() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            new MakeToast("No camera on this device");
        } else {
            if (isStreaming) new MakeToast("This device is streaming");
            isStreaming = true;
            preview.liveSetId();
            preview.openSurface();
            preview.onResume();
            return preview.getIpServer() + ":" + String.valueOf(preview.getPortServer());
        }
        return null;
    }

    @Override
    public String onImageRequested() {
        String result = null;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            onShowToast("No camera on this device");
        } else {
            try {
                preview.setCamera();
                preview.openSurface();
                result = preview.takePicture();
            } catch (Exception e) {
                Log.d("ERROR", "Failed to config the camera: " + e.getMessage());
            } finally {
                preview.closeSurface();
            }
        }
        return result;
    }

    @Override
    public void onGpsReceived(Double lat, Double lon, Double alt, String senderName) {
        Intent it = new Intent("com.example.mobserv.remoteapp.MapActivity");
        it.putExtra("sendOrShow", "showPosition");
        it.putExtra("latitude", lat);
        it.putExtra("longitude", lon);
        it.putExtra("nametoshow", senderName);
        startActivity(it);
    }

    /***********************
     * Runnable subclasses *
     ***********************/

    class MakeToast implements Runnable {
        private String msg;
        public MakeToast(String msg) {  this.msg = msg;  }
        @Override
        public void run() { Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show(); }
    }

    class CreateNameDialog implements Runnable {
        Boolean alreadyTaken;
        Context context = ChatActivity.this;

        public CreateNameDialog(Boolean alreadyTaken) {
            this.alreadyTaken = alreadyTaken;
        }

        @Override
        public void run() {
            final EditText name = new EditText(context);
            name.setHint("Name...");
            if (!alreadyTaken) {
                new AlertDialog.Builder(context)
                        .setTitle("Please choose a username")
                        .setView(name)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                mTaskFragment.sendMsg(name.getText().toString());
                            }
                        }).create().show();
            } else {
                new AlertDialog.Builder(context)
                        .setTitle("Please choose another username")
                        .setMessage("The name you chose had already been picked")
                        .setView(name)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                mTaskFragment.sendMsg(name.getText().toString());
                            }
                        }).create().show();
            }
        }
    }

    public class DisplayMessage implements Runnable {
        ChatMessage msg;
        public DisplayMessage(ChatMessage msg) { this.msg = msg; }
        @Override
        public void run() {displayMessage(msg);}
    }

    /*********************
     * Getters & Setters *
     *********************/

    public void setNameTaken(boolean nameTaken) {
        this.nameTaken = nameTaken;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setMyName(String myName) {
        this.myName = myName;
    }

    /**********************
     * Activity Lifecycle *
     **********************/

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(MyConstants.TAG_CONNECTED, connected);
        outState.putBoolean(MyConstants.TAG_NAMETAKEN, nameTaken);
        outState.putString(MyConstants.TAG_MYNAME, myName);
        outState.putInt(MyConstants.TAG_SUGG_STATE, suggestionsState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        connected = savedInstanceState.getBoolean(MyConstants.TAG_CONNECTED);
        messageET.setFocusableInTouchMode(connected);
        nameTaken = savedInstanceState.getBoolean(MyConstants.TAG_NAMETAKEN);
        myName = savedInstanceState.getString(MyConstants.TAG_MYNAME, "");
        suggestionsState = savedInstanceState.getInt(MyConstants.TAG_SUGG_STATE, 0);
        setSuggestions(suggestionsState);
    }

    /******
     ******/

    public static ArrayList<String> getIpList() {
        ArrayList<String> x = IpList;
        return x;
    }

    public void onClickSendGPS (View view){
        Intent i = new Intent(this, MapActivity.class);
        i.putExtra("sendOrShow","readPosition");
        i.putExtra("nametoshow", myName);
        startActivityForResult(i, SEND_GPS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEND_GPS) {
            if(resultCode == Activity.RESULT_OK){
                String latS = data.getStringExtra("latS");
                String lonS = data.getStringExtra("lonS");
                String altS = data.getStringExtra("altS");
                Toast.makeText(this, "lat: "+latS+", lon: "+lonS, Toast.LENGTH_LONG).show();
                // at this moment, edit text should be edited
                // may finish either with show or subscription, in case we want the map to be opened or not on reception
                // the format used belows implies that the user writes the name of the client o whom send the position
                // then clicks on the button 'sendGPS' and, when he cliks on the received position, edittext
                // is auto completed with the right frmat to send gps coordinates
                messageET.getText().append("/write/gps/"+lonS+"/"+latS+"/"+altS+"/show");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }


}
