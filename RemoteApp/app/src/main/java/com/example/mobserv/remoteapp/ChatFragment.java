package com.example.mobserv.remoteapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by giovanni on 27/12/15.
 */
public class ChatFragment extends Fragment{

    private List<ChatMessage> chat;

    /**
     * This method will only be called once since the
     * Fragment is retained.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    public ChatFragment() {
        chat = new ArrayList<>();
    }

    public List<ChatMessage> getChat() {
        return chat;
    }

    public void setChat(List<ChatMessage> chat) {
        this.chat = chat;
    }
}
