package com.example.mobserv.remoteapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by giovanni on 27/12/15.
 */
public class ChatFragment extends Fragment{
    private static final String TAG = ChatFragment.class.getSimpleName();

    private List<ChatMessage> chat;
    private HashMap<String, Long> clients;
    private long idCounter = 1; // SERVER_SENDER_ID + 1

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
        clients = new HashMap<>();
    }

    public List<ChatMessage> getChat() {
        return chat;
    }

    public void setChat(List<ChatMessage> chat) {
        this.chat = chat;
    }

    public List<String> getClients() {
        return new ArrayList<String>(clients.keySet());
    }

    public void setClients(List<String> clientsList) {
        for(String cl : clientsList){
            if(!clients.containsKey(cl)){
                addClient(cl);
            }
        }
        Iterator<HashMap.Entry<String,Long>> iter = clients.entrySet().iterator();
        while (iter.hasNext()) {
            HashMap.Entry<String,Long> entry = iter.next();
            if(!clientsList.contains(entry.getKey())){
                iter.remove();
            }
        }
        Log.d(TAG, "clients and id are "+clients.toString());
    }

    public void addClient(String name){
        clients.put(name, idCounter);
        idCounter++;
    }

    public long getSenderId(String sender){
        if(sender.equals(MyConstants.USERNAME_SERVER))
            return MyConstants.SERVER_SENDER_ID;
        if(sender.charAt(0) == '<' && sender.charAt(sender.length()-1) == '>')
            sender = sender.substring(1, sender.length()-1);
        if(clients.containsKey(sender))
            return clients.get(sender);
        else{
            Log.d(TAG, "Cannot find sender id for sender \""+sender+"\"");
            return -1;
        }
    }
}
