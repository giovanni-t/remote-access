package com.example.mobserv.remoteapp;

import android.content.Intent;

/**
 * Model class for the messages
 */
public class ChatMessage {
    private long id;
    private boolean isMe;
    private boolean isFromServer;
    private String message;
    private long senderId = -1;
    private String dateTime;
    private String senderName = "";
    private boolean clickable;
    private Intent intent;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setIsMe(boolean isMe) {
        this.isMe = isMe;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getDate() {
        return dateTime;
    }

    public void setDate(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
        setIsFromServer(senderName.equals(MyConstants.USERNAME_SERVER));
    }


    public boolean isFromServer() {
        return isFromServer;
    }

    public void setIsFromServer(boolean isFromServer) {
        this.isFromServer = isFromServer;
    }

}
