package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
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
 * Created by giovanni on 13/12/15.
 */

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 */
public class TaskFragment extends Fragment {

    private static final String TAG = TaskFragment.class.getSimpleName();
    private static final boolean DEBUG = true; // Set this to false to disable logs.
    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface TaskCallbacks {
        //void onConnected();
        //void onProgressUpdate(int percent);
        //void onCancelled();
        //void onTextReceived();
        void onShowToast(String str);
    }

    private TaskCallbacks mCallbacks;
    private ClientThread mTask;
    private Thread th;
    private String myName;
    private String serverip = "dummy IP";
    private static final int serverport = 45678;

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (TaskCallbacks) activity;
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Get the ip of server from the intent
        Bundle bd = getArguments();
        serverip = bd.getString("serverip");

        // Create and execute the background task.
        mTask = new ClientThread();
        th = new Thread(mTask);
        th.start();
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void onShowToast(String str){
        Log.d("taskfragment", "onShowToast -- "+str);
    }

    private class ClientThread implements Runnable {

        BufferedReader inputStream;

        @Override
        public void run() {
            mCallbacks.onShowToast("Connecting to " + serverip + ":" + serverport + "...");
            /*try {
                InetAddress serverAddr = InetAddress.getByName(serverip);
                socket = new Socket(serverAddr, serverport);
                this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                        true);

                // success =)
                mCallbacks.onShowToast("Connected to " + serverAddr + " " + serverport);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        et.setFocusableInTouchMode(true);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                finish();
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = inputStream.readLine();
                    if (read == null || socket.isClosed()) {
                        mCallbacks.onShowToast("Connection closed by server");
                        break;
                    }
                    updateConversationHandler.post(new updateUIThread(read));

                    boolean isOK = checkReceivedMessageFormat(read);
                    if (!isOK) {
                        mCallbacks.onShowToast(read);
                    } else {
                        String senderName = read.substring(1, read.indexOf(">"));
                        String[] args = read.substring(read.indexOf(">") + 2, read.length()).split("/");
                        messageDispatch(senderName, args);
                        //mCallbacks.onShowToast(senderName);
                        //runOnUiThread(new makeToast(TextUtils.join("/", args)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (!socket.isClosed())
                        runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                    else
                        runOnUiThread(new makeToast(e.getMessage()));
                    finish();
                    return;
                }
            }

            finish();
            */
        }

        /**
         * Check if received message should be dispatched or not
         * (if it is a protocol-like message or human-like message)
         *
         * @param msg the message to be checked
         * @return true if it is protocol-like, false otherwise
         */
        private boolean checkReceivedMessageFormat(String msg) {
            try {
                String splits1[] = msg.split(" ");
                if (splits1[1] == null) {
                    Log.d("ReceivedMessageFormat", "second part is null :: " + msg);
                    return false;
                }
                if (!splits1[0].matches("^<.*>$")) {
                    Log.d("ReceivedMessageFormat", "format of first part does not match :: " + msg);
                    return false;
                }
                if (!splits1[1].matches("[^/]*/[^/]*/[^/]+.*")) {
                    Log.d("ReceivedMessageFormat", "format of second part does not match :: " + msg);
                    return false;
                }
            } catch (NullPointerException | IndexOutOfBoundsException | PatternSyntaxException e) {
                //------> it enters here when image is being transferring, although the image is retrieved in messageIsWrite
                // TODO: should be fixed now, but double check to be sure
                Log.d("ReceivedMessageFormat", "Exception: " + e.getClass().getName() + " " + e.getMessage());
                Log.d("ReceivedMessageFormat", "Exception :: " + msg);
                return false;
            }
            return true;
        }

        /*
        public void messageDispatch(String senderName, String[] args) {
            boolean isBroadcast = false;

            if (!args[1].equals(myName))
                isBroadcast = true;

            switch (args[2]) {
                case "read":
                    messageIsRead(senderName, args);
                    break;
                case "write":
                    messageIsWrite(senderName, args);
                    break;
                case "exec":
                    messageIsExec(senderName, args);
                    break;
                case "OK":
                    // TODO ok msg
                    break;
                default:
                    // this should never happen if the server is well behaved
                    mCallbacks.onShowToast("Unknown message:\n" + TextUtils.join("/", args));
                    break;
            }

        }

        public void messageIsWrite(String senderName, String[] args) {
            LinkedList<String> reply = new LinkedList<>();
            switch (args[3]) {
                case "photo":
                    //TODO: show the received photo
                    String encodedImage;
                    StringBuilder total = new StringBuilder();
                    String line;
                    try {
                        while ((line = inputStream.readLine()) != null) {
                            if (line.length() >= 5) {
                                if (line.substring(line.length() - 5, line.length()).compareTo("_end_") == 0) {
                                    //total.append(total.substring(0,total.length()-6));
                                    break;
                                }
                            }
                            total.append(line + "\n");
                        }
                        encodedImage = total.toString();
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        decodedByte = Bitmap.createBitmap(decodedByte, 0, 0, decodedByte.getWidth(), decodedByte.getHeight(), matrix, true);
                        updateConversationHandler.post(new updateUIImage(decodedByte));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "gps":
                    try {
                        Double lon = Double.parseDouble(args[4]);
                        Double lat = Double.parseDouble(args[5]);
                        Double alt = Double.parseDouble(args[6]);
                        runOnUiThread(new makeToast("Received GPS position from "+senderName+":\n" + TextUtils.join("/", Arrays.asList(args).subList(4,7))));
                    } catch (ArrayIndexOutOfBoundsException e){
                        Log.d("msgIsWrite", "bad format in msg write gps: "+ TextUtils.join("/", args));
                    }
                    break;
                default:
                    runOnUiThread(new makeToast("Unknown WRITE message:\n" + TextUtils.join("/", args)));

            }
        }

        public void messageIsExec(String senderName, String[] args) {

        }



        public void messageIsRead(String senderName, String[] args) {
            LinkedList<String> reply = new LinkedList<>();
            String data = null;
            switch (args[3]) {
                case "gps":
                    if (gpsTracker.getIsGPSTrackingEnabled()) {
                        reply.add("write");
                        reply.add("gps");
                        reply.add(String.valueOf(gpsTracker.longitude));
                        reply.add(String.valueOf(gpsTracker.latitude));
                        reply.add(String.valueOf(gpsTracker.altitude));
                    } else {
                        Log.d("GPS ERROR", "GPS is not enabled");
                        mCallbacks.onShowToast("GPS ERROR: GPS is not enabled");
                    }
                    break;
                case "clientlist":
                    int numOfClients = Integer.parseInt(args[4]);
                    List<String> clients = new LinkedList<>();
                    clients.addAll(Arrays.asList(args).subList(5, args.length));
                    Log.d("msgIsRead", "Parsed list of clients: " + numOfClients + " " + clients.toString());
                    runOnUiThread(new updateUIClientsList(numOfClients, clients));
                    break;
                case "photo":
                    //PHOTO Part
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        new makeToast("No camera on this device");
                    } else {
                        try {
                            preview.setCamera();
                            preview.openSurface();

                            String encodedImage = preview.takePicture(getApplicationContext());
                            reply.add("write");
                            reply.add("photo");
                            data = encodedImage;
                        } catch (Exception e) {
                            Log.d("ERROR", "Failed to config the camera: " + e.getMessage());
                        } finally {
                            preview.closeSurface();
                        }
                    }
                    break;
                case "nametaken":
                    if (!nameTaken) {
                        updateConversationHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new createNameDialog(true));
                            }
                        });
                    }
                    break;
                case "Welcome!":
                    myName = args[1];
                    Log.d("debug", "My name as client: " + myName);
                    nameTaken = true;
                    break;
                case "Hello":
                    runOnUiThread(new createNameDialog(false));
                    break;
                default:
                    runOnUiThread(new makeToast("Unknown READ message:\n" + TextUtils.join("/", args)));
            }
            if (reply.size() != 0) {
                String msg = composeMsg(senderName, reply);
                sendMsg(msg);
                if (data != null) {
                    try {
                        Thread.sleep(500);
                        out.write(data);
                        out.flush();
                        Thread.sleep(500);
                        out.write("_end_");
                        out.flush();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("SendMsg", msg);
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
        */
    }

    /************************/
    /***** LOGS & STUFF *****/
    /************************/

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onActivityCreated(Bundle)");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        super.onStop();
    }
}
