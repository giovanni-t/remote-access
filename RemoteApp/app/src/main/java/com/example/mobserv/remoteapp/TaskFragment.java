package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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
        void onConnected();
        void onFragmentCancel();
        void onTextReceived(String str);
        void onShowToast(String str);
        void onChooseName(Boolean taken);
        void onImageReceived(Bitmap decodedByte);
        void onClientListReceived(int numOfClients, List<String> clients);
        void onWelcome();
        String onImageRequested();
    }

    private TaskCallbacks mCallbacks;
    private ClientThread mTask;
    private Thread th;
    private String myName;
    private String serverip = "dummy IP";
    private static final int serverport = 45678;
    private Socket socket;
    private PrintWriter out;
    private Boolean nameTaken = false;
    private GPSTracker gpsTracker;
    private Activity attachedActivity;



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
        attachedActivity = activity;
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

        // Instantiate needed tools
        gpsTracker = new GPSTracker(getContext(), attachedActivity);
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

    private class ClientThread implements Runnable {
        BufferedReader inputStream;

        @Override
        public void run() {
            mCallbacks.onShowToast("Connecting to " + serverip + ":" + serverport + "...");
            try {
                InetAddress serverAddr = InetAddress.getByName(serverip);
                socket = new Socket(serverAddr, serverport);
                this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                        true);

                // success =)
                mCallbacks.onConnected();

            } catch (IOException e) {
                e.printStackTrace();
                mCallbacks.onShowToast("ERROR:\n" + e.getMessage());
                mCallbacks.onFragmentCancel();
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = inputStream.readLine();
                    if (read == null || socket.isClosed()) {
                        mCallbacks.onShowToast("Connection closed by server");
                        break;
                    }
                    mCallbacks.onTextReceived(read);

                    boolean isOK = checkReceivedMessageFormat(read);
                    if (!isOK) {
                        mCallbacks.onShowToast(read);
                    } else {
                        String senderName = read.substring(1, read.indexOf(">"));
                        String[] args = read.substring(read.indexOf(">") + 2, read.length()).split("/");
                        messageDispatch(senderName, args);
                        //mCallbacks.onShowToast(senderName);
                        //mCallbacks.onShowToast(TextUtils.join("/", args));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (!socket.isClosed())
                        mCallbacks.onShowToast("ERROR:\n" + e.getMessage());
                    else
                        mCallbacks.onShowToast(e.getMessage());
                    mCallbacks.onFragmentCancel();
                    return;
                }
            }

            mCallbacks.onFragmentCancel();
            return;
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
                // should be fixed now
                Log.d("ReceivedMessageFormat", "Exception: " + e.getClass().getName() + " " + e.getMessage());
                Log.d("ReceivedMessageFormat", "Exception :: " + msg);
                return false;
            }
            return true;
        }


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
                    // ok msg
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
                    // show the received photo
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
                        mCallbacks.onImageReceived(decodedByte);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "gps":
                    try {
                        Double lon = Double.parseDouble(args[4]);
                        Double lat = Double.parseDouble(args[5]);
                        Double alt = Double.parseDouble(args[6]);
                        // TODO put in a onGPS
                        onGPSReceived(lat, lon, senderName);
                        mCallbacks.onShowToast("Received GPS position from " + senderName + ":\n" + TextUtils.join("/", Arrays.asList(args).subList(4, 7)));

                    } catch (ArrayIndexOutOfBoundsException e){
                        Log.d("msgIsWrite", "bad format in msg write gps: "+ TextUtils.join("/", args));
                    }
                    break;
                default:
                    mCallbacks.onShowToast("Unknown WRITE message:\n" + TextUtils.join("/", args));

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
                    mCallbacks.onClientListReceived(numOfClients, clients);
                    break;
                case "photo":
                    String encodedImage = mCallbacks.onImageRequested();
                    reply.add("write");
                    reply.add("photo");
                    data = encodedImage;
                    break;
                case "nametaken":
                    if (!nameTaken) {
                        mCallbacks.onChooseName(true);
                    }
                    break;
                case "Welcome!":
                    myName = args[1];
                    Log.d("debug", "My name as client: " + myName);
                    nameTaken = true;
                    mCallbacks.onWelcome();
                    break;
                case "Hello":
                    mCallbacks.onChooseName(false);
                    break;
                default:
                    mCallbacks.onShowToast("Unknown READ message:\n" + TextUtils.join("/", args));
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
        mCallbacks.onTextReceived(msg); // would be better to have a "onTextSent" to have a different handling
    }

    public void closeSocket(){
        try {
            socket.close();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
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

    public void onGPSReceived(Double lat, Double lon, String sname){
        Intent it = new Intent("com.example.mobserv.remoteapp.MapActivity");
        it.putExtra("latitude", lat);
        it.putExtra("longitude", lon);
        it.putExtra("nametoshow", sname);
        startActivity(it);
    }
}
