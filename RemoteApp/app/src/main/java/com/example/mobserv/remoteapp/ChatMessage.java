package com.example.mobserv.remoteapp;

import android.content.Intent;

/**
 * Created by giovanni on 26/12/15.
 */
public class ChatMessage {
    private long id;
    private boolean isMe;
    private String message;
    private long senderId;
    private String dateTime;
    private String senderName;
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
    }



    /* Parcelable implementation ****

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ChatMessage createFromParcel(Parcel in) { return new ChatMessage(in); }
        public ChatMessage[] newArray(int size) { return new ChatMessage[size]; }
    };

    public ChatMessage(Parcel in){
        in.read
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    */
}
