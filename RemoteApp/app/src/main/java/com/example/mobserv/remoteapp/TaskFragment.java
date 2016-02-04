package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/**
 * Created by giovanni on 13/12/15.
 */

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 * It handles the connection with the server, interpret the received
 * messages and calls the corresponding callbacks.
 * The callbacks are just an interface, so they must be implemented in the
 * activity that holds this class.
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
        void onTaskFragmentCancel();
        void onTextReceived(String str);
        void onShowToast(String str);
        void onChooseName(Boolean taken);
        void onImageReceived(byte[] imageByte);
        void onClientListReceived(int numOfClients, List<String> clients);
        void onIpListReceived(int numOfIps, ArrayList<String> ips);
        void onWelcome(String myName);
        void onExecReceived(String subscriberName, String service);
        void onNetworkRequested();
        void onStopTimers();
        String onLiveRequested();
        String onImageRequested();
        void onGpsReceived(Double lat, Double lon, Double alt, String senderName);
    }

    private TaskCallbacks mCallbacks;
    private ClientThread mTask;
    private Thread th;
    private String myName;
    private String serverip = "dummy IP";
    private Socket socket;
    private PrintWriter out;
    private Boolean nameTaken = false;
    public GPSTracker gpsTracker;
    private Activity attachedActivity;
    private FileOutputStream fos, fos_total;


    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof TaskCallbacks) {
            mCallbacks = (TaskCallbacks) context;
        } else {
            Log.d(TAG, "Fatal: Given attaching context is not instanceof TaskCallbacks");
        }
        if (context instanceof Activity){
            attachedActivity = (Activity) context;
        } else {
            Log.d(TAG, "Fatal: Given attaching context is not instanceof Activity");
        }
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
        serverip = bd.getString(MyConstants.TAG_SERVERIP);

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
            mCallbacks.onShowToast("Connecting to " + serverip + ":" + MyConstants.serverport + "...");
            try {
                InetAddress serverAddr = InetAddress.getByName(serverip);
                socket = new Socket(serverAddr, MyConstants.serverport);
                this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                        true);

                // success =)
                mCallbacks.onConnected();

            } catch (IOException e) {
                e.printStackTrace();
                if (mCallbacks != null) {
                    mCallbacks.onShowToast("ERROR:\n" + e.getMessage());
                    mCallbacks.onTaskFragmentCancel();
                }
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
                        //mCallbacks.onShowToast(read);
                        Log.d(TAG, "skipping ["+read+"] - bad format");
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
                    mCallbacks.onTaskFragmentCancel();
                    return;
                }
            }

            mCallbacks.onTaskFragmentCancel();
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
                //------> it enters here when image is being transferring, although the image is retrieved in messageIsResp
                // should be fixed now
                Log.d("ReceivedMessageFormat", "Exception: " + e.getClass().getName() + " " + e.getMessage());
                //Log.d("ReceivedMessageFormat", "Exception :: " + msg);
                return false;
            }
            return true;
        }


        public void messageDispatch(String senderName, String[] args) {
            boolean isBroadcast = false;

            if (!args[1].equals(myName))
                isBroadcast = true;

            switch (args[2]) {
                case "req":
                    messageIsReq(senderName, args);
                    break;
                case "resp":
                    messageIsResp(senderName, args);
                    break;
                case "exec":
                    messageIsExec(senderName, args);
                    break;
                default:
                    // this should never happen if the server is well behaved
                    mCallbacks.onShowToast("Unknown message:\n" + TextUtils.join("/", args));
                    break;
            }

        }

        public void messageIsResp(String senderName, String[] args) {
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
                            total.append(line).append("\n");
                        }
                        encodedImage = total.toString();

                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        mCallbacks.onImageReceived(decodedString);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "gps":

                    try {
                        Double lon = Double.parseDouble(args[4]);
                        Double lat = Double.parseDouble(args[5]);
                        Double alt = Double.parseDouble(args[6]);
                        String isSub= args[7];
                        mCallbacks.onShowToast("Received GPS position from " + senderName + ":\n" + TextUtils.join("/", Arrays.asList(args).subList(4, 7)));
                        // on receiving the gps position, it must be appended to the log file
                        Object[] arr = new Double[]{lon,lat,alt};
                        writeToLog("gps", senderName, arr);
                        if ( isSub.compareTo("show")==0){
                            mCallbacks.onGpsReceived(lat, lon, alt, senderName);
                        }
                    } catch (ArrayIndexOutOfBoundsException e){
                        Log.d("msgIsResp", "bad format in msg resp gps: "+ TextUtils.join("/", args));
                    }
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
                    mCallbacks.onWelcome(myName);
                    break;
                case "clientlist":
                    int numOfClients = Integer.parseInt(args[4]);
                    List<String> clients = new LinkedList<>();
                    clients.addAll(Arrays.asList(args).subList(5, args.length));
                    Log.d("msgIsResp", "Parsed list of clients: " + numOfClients + " " + clients.toString());
                    mCallbacks.onClientListReceived(numOfClients, clients);
                    break;
                case "liveIps":
                    int numOfIPs = Integer.parseInt(args[4]);
                    ArrayList<String> ips = new ArrayList<>();
                    ips.addAll(Arrays.asList(args).subList(5, args.length));
                    Log.d("msgIsResp", "Parsed list of ips: " + numOfIPs + " " + ips.toString());
                    mCallbacks.onIpListReceived(numOfIPs, ips);
                    break;
                default:
                    mCallbacks.onShowToast("Unknown RESP message:\n" + TextUtils.join("/", args));

            }
        }

        public void messageIsExec(String senderName, String[] args) {
            LinkedList<String> reply = new LinkedList<>();
            String data = null;
            switch (args[3]) {
                case "gps":
                    //mCallbacks.onShowToast("exec gps test");
                    mCallbacks.onExecReceived(senderName,"gps"); // give the name of the client to which send periodic messages
                                                // give the name of the messages to be sent ( GPS )
                    break;
            }
        }

        public void messageIsBatch( String senderName, String[] args ){
            List<LinkedList<String>> replyList = new LinkedList<>();
            LinkedList<String> reply = null;
            String data = null;
            int n=0, index=3; // n counts the #messages, index contains the relative index of the command in the string

            for ( String s : args ){
                if ( s.compareTo(";")==0 ){
                    n++;
                }
            }

            for ( int i=0; i<=n; i++ ){
                switch (args[index]) {
                    case "gps":
                        if (gpsTracker.getIsGPSTrackingEnabled()) {
                            gpsTracker.updateGPSCoordinates(); // get the most precise recent position
                            reply = new LinkedList<>();
                            reply.add("resp");
                            reply.add("gps");
                            reply.add(String.valueOf(gpsTracker.longitude));
                            reply.add(String.valueOf(gpsTracker.latitude));
                            reply.add(String.valueOf(gpsTracker.altitude));
                            reply.add("subscription"); // ADDED FOR PERIODIC MESSAGES
                            replyList.add(reply);
                        } else {
                            Log.d("GPS ERROR", "GPS is not enabled");
                            mCallbacks.onShowToast("GPS ERROR: GPS is not enabled");
                        }
                        break;
                    case "photo":
                        String encodedImage = mCallbacks.onImageRequested();
                        reply = new LinkedList<>();
                        reply.add("resp");
                        reply.add("photo");
                        data = encodedImage;
                        replyList.add(reply);
                        break;
                    case "live":
                        String ip = mCallbacks.onLiveRequested();
                        reply = new LinkedList<>();
                        reply.add("resp");
                        reply.add("live");
                        if(ip != null) {
                            reply.add(ip);
                            replyList.add(reply);
                        }
                    case "network":
                        mCallbacks.onNetworkRequested();
                        break;
                    default:
                        mCallbacks.onShowToast("Unknown REQ-BATCH message:\n" + TextUtils.join("/", args));
                }
                index += 4;
            }

            for ( LinkedList<String> r : replyList ){
                if (r.size() != 0) {
                    String msg = composeMsg(senderName, r);
                    sendMsg(msg);
                    if (r.get(0) == "resp" && r.get(1) == "photo" && data != null) {
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
        }
        public void messageIsReq(String senderName, String[] args) {
            LinkedList<String> reply = new LinkedList<>();
            String data = null;

            Boolean isBatch = false;
            for ( String s : args ){
                if (s.compareTo(";")==0){
                    isBatch = true;
                    Log.d("ISBATCHCHECK", "true");
                    break;
                }
            }

            if ( ! isBatch ){
                switch (args[3]) {
                    case "gps":
                        if (gpsTracker.getIsGPSTrackingEnabled()) {
                            gpsTracker.updateGPSCoordinates(); // get the most precise recent position
                            reply.add("resp");
                            reply.add("gps");
                            reply.add(String.valueOf(gpsTracker.longitude));
                            reply.add(String.valueOf(gpsTracker.latitude));
                            reply.add(String.valueOf(gpsTracker.altitude));
                            reply.add("show"); // ADDED FOR PERIODIC MESSAGES
                        } else {
                            Log.d("GPS ERROR", "GPS is not enabled");
                            mCallbacks.onShowToast("GPS ERROR: GPS is not enabled");
                        }
                        break;
                    case "photo":
                        String encodedImage = mCallbacks.onImageRequested();
                        reply.add("resp");
                        reply.add("photo");
                        data = encodedImage;
                        break;
                    case "live":
                        String ip = mCallbacks.onLiveRequested();
                        reply.add("resp");
                        reply.add("live");
                        if(ip != null)
                            reply.add(ip);
                        break;
                    case "Hello":
                        mCallbacks.onChooseName(false);
                        break;
                    case "network":
                        mCallbacks.onNetworkRequested();
                        break;
                    default:
                        mCallbacks.onShowToast("Unknown REQ message:\n" + TextUtils.join("/", args));
                }
            } else {
                Log.d("DISPATCHBATCH", "dispatching");
                messageIsBatch(senderName, args);
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
        // mCallbacks.onTextReceived(msg);
    }

    public void closeSocket(){
        try {
            gpsTracker.stopUsingGPS(); // this should fix issue 6
            mCallbacks.onStopTimers();
            trimCache(getContext());
            socket.close();

        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    /* write events to the log file */
    public void writeToLog(String command, String senderName, Object[] array){
        // for now works only for gps
        // format (for gps ) is year:year month:month day:day time:hour.minute from:user command:gps lat:lat lon:lon alt:alt
        switch (command){
            case "gps":
                String str = "";
                Double lon = (Double)array[0];
                Double lat = (Double)array[1];
                Double alt = (Double)array[2];
                try {
                    fos = getContext().openFileOutput(MyConstants.LOG_FILENAME, Context.MODE_APPEND);
                    fos_total = getContext().openFileOutput(MyConstants.TOTAL_LOG_FILENAME, Context.MODE_APPEND);
                    Calendar c = Calendar.getInstance();
                    //Long tsLong = System.currentTimeMillis()/1000;
                    String ts = "year:" + String.valueOf(c.get(Calendar.YEAR)) + " month:" + String.valueOf(c.get(Calendar.MONTH)+1) +
                            " day:" + String.valueOf(c.get(Calendar.DAY_OF_MONTH))
                            + " time:" + String.valueOf(c.get(Calendar.HOUR_OF_DAY)) + "." + String.valueOf(c.get(Calendar.MINUTE));
                    str += ts + " from:" + senderName + " command:" + "gps" + " lat:" + String.valueOf(lat) + " lon:" +
                            String.valueOf(lon) + " alt:" + String.valueOf(alt) + "\n";
                    fos.write(str.getBytes());
                    fos.close();
                    fos_total.write(str.getBytes());
                    fos_total.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.d("FILEOUTPUT", "File not found exception");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("FILEOUTPUT", "IO error -> "+ str );
                }
                break;
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


}
