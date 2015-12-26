package com.example.mobserv.remoteapp;

import android.os.Bundle;

public class ChatActivity extends DrawerActivity  {

    private static final String title = "Remote Access"; // Change this string to change the title!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setTitle(title);

    }
}
