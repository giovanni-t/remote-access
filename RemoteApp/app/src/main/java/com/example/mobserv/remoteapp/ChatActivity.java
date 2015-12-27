package com.example.mobserv.remoteapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends DrawerActivity  {

    // Constants
    private static final String TITLE = "Remote Access"; // Main toolbar title!
    private static final String TAG = ChatActivity.class.getSimpleName();

    // UI elements
    private EditText messageET;
    private ListView messagesContainer;
    private Button sendBtn;
    private ChatAdapter mChatAdapter;
    private ChatFragment chatFragment;

    // Connection parameters
    private String serverip = "another dummy IP"; // Retrieved by the intent
    private boolean connected = false;

    // Clients
    private boolean nameTaken;
    private List<String> clientsList;

    // Connected fragment
    private TaskFragment mTaskFragment;

    // Camera preview -- To be moved in other activity
    /*
    private SurfaceView mSurfaceView;
    private ImageView contactImage;
    private CameraPreview preview;
    */

    /* Streaming
    private boolean isStreaming = false;
    private ArrayList<String> IpList; */

    /* Subscription
    private List<Subscriber> subscribers;
    final Handler singleTimer = new Handler();
    private List<TimerTask> subscribersTimer; */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setTitle(TITLE);

        // DUMMY CONNECTION
        connected = true;

        initControls();
        initChatFragment();
        //loadDummyHistory();
        this.serverip = getIntent().getStringExtra(MyConstants.TAG_SERVERIP);
        //initTaskFragment();

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

    private void initControls() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
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
                chatMessage.setId(122);//dummy
                chatMessage.setMessage(messageText);
                chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                chatMessage.setIsMe(true);
                messageET.setText("");

                //mTaskFragment.sendMsg(messageText);
                displayMessage(chatMessage);
            }
        });

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



    /* TODO: use onPause to save data and onResume to reload */
}
