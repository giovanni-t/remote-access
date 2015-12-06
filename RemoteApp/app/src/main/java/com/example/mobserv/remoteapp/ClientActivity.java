package com.example.mobserv.remoteapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
 * Created by pacel_000 on 22/10/2015.
 */
public class ClientActivity extends Activity {

    private Socket socket = null;
    private PrintWriter out;
    private static final int serverport = 45678;
    private String serverip = "";
    private TextView text;
    private Handler updateConversationHandler;
    private EditText et;
    private Thread th;
    private String myName;
    private SurfaceView mSurfaceView;
    private ImageView contactImage;
    CameraPreview preview;
    GPSTracker gpsTracker;
    private Boolean nameTaken = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        Intent it = getIntent();
        text = (TextView) findViewById(R.id.idClientText);
        text.setMovementMethod(new ScrollingMovementMethod());

        et = (EditText) findViewById(R.id.idClientEditText);
        updateConversationHandler = new Handler();

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        contactImage = (ImageView) findViewById(R.id.photo);
        preview = new CameraPreview(this, (SurfaceView) findViewById(R.id.surfaceView));
        preview.setKeepScreenOn(true);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mSurfaceView.setX(metrics.widthPixels+1);
        if(this.serverip.isEmpty()) {
            this.serverip = it.getStringExtra("serverip");
            et.setFocusable(false);
            myName = null;
            th = new Thread(new ClientThread());
            th.start();
            createNameDialog(false);
            // TODO avoid reconnect when activity is created again, for ex. after rotation
            // (I tried using this if statement but is not effective)
            // an idea could be keep the bg thread alive somehow, and start it only when the
            // 'connect' button in the main activity is pressed
            // TODO: rotation also erases the text in the textview, which is the 'current conversation'
            // temporary fix: forbid rotation
        }

        gpsTracker = new GPSTracker(this, getParent());
    }

    public void onClick(View view) {
        String str = et.getText().toString();
        sendMsg(str);
        if (myName == null){
            myName = str;
            Log.d("debug", "My name as client: " + myName);
        }
        et.setText(null);
    }

    /** Write the string on the socket, no matter what is the format.
     *  So the 'msg' string received need to be already in the right format
     * @param msg the message to send
     */
    public void sendMsg(String msg){
        out.write(msg);
        out.flush();
        updateConversationHandler.post(new updateUIThread(msg));
    }

    public void onClickEnterText(View view){
        String tmp = et.getText().toString();
        tmp += "/" + ((Button)view).getText().toString();
        et.setText(tmp);
        et.setSelection(et.getText().toString().length());
    }

    class ClientThread implements Runnable {
        BufferedReader inputStream;

        @Override
        public void run() {
            runOnUiThread(new makeToast("Connecting to " + serverip + ":" + serverport + "..."));
            try {
                InetAddress serverAddr = InetAddress.getByName(serverip);
                socket = new Socket(serverAddr, serverport);
                this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                        true);

             // success =)
                runOnUiThread(new makeToast("Connected to " + serverAddr + " " + serverport));
                runOnUiThread(new Runnable() {@Override public void run() {et.setFocusableInTouchMode(true);}});
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                finish();
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = inputStream.readLine();
                    updateConversationHandler.post(new updateUIThread(read));

                    boolean isOK = checkReceivedMessageFormat(read);
                    if(!isOK){
                        runOnUiThread(new makeToast(read));
                    } else {
                        String senderName = read.substring(1, read.indexOf(">"));
                        String[] args = read.substring(read.indexOf(">")+2, read.length()).split("/");
                        messageDispatch(senderName, args);
                        //runOnUiThread(new makeToast(senderName));
                        //runOnUiThread(new makeToast(TextUtils.join("/", args)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if(!socket.isClosed())
                        runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                    else
                        runOnUiThread(new makeToast(e.getMessage()));
                    finish();
                    return;
                }
            }
        }

        /** Check if received message should be dispatched or not
         * (if it is a protocol-like message or human-like message)
         * @param msg the message to be checked
         * @return true if it is protocol-like, false otherwise
         */
        private boolean checkReceivedMessageFormat(String msg) {
            try {
                String splits1[] = msg.split(" ");
                if(splits1[1] == null){
                    Log.d("ReceivedMessageFormat", "second part is null :: " + msg);
                    return false;
                }
                if(!splits1[0].matches("^<.*>$")) {
                    Log.d("ReceivedMessageFormat", "format of first part does not match :: " + msg);
                    return false;
                }
                if(!splits1[1].matches("[^/]*/[^/]+/[^/]+.*")) {
                    Log.d("ReceivedMessageFormat", "format of second part does not match :: " + msg);
                    return false;
                }
            } catch (NullPointerException | IndexOutOfBoundsException | PatternSyntaxException e){
                //TODO: it enters here when image is being transferring, although the image is retrieved in messageIsWrite
                Log.d("ReceivedMessageFormat", "Exception: " + e.getClass().getName() + " " + e.getMessage());
                Log.d("ReceivedMessageFormat", "Exception :: " + msg);
                return false;
            }
            return true;
        }

        public void messageDispatch(String senderName, String[] args){
            boolean isBroadcast = false;

            if(!args[1].equals(myName))
                isBroadcast = true;

            switch (args[2]){
                case "read":
                    messageIsRead(senderName, args);
                    break;
                case "write":
                    messageIsWrite(senderName, args);
                    break;
                case "exec":
                    messageIsExec(senderName, args);
                    break;
                default:
                    // this should never happen if the server is well behaved
                    runOnUiThread(new makeToast("Unknown message:\n" + TextUtils.join("/", args)));
                    break;
            }

        }
        public void messageIsWrite(String senderName, String[] args){
            LinkedList<String> reply = new LinkedList<>();
            switch (args[3]){
                case "photo":
                    //TODO: show the received photo
                    String encodedImage;
                    StringBuilder total = new StringBuilder();
                    String line;
                    try {
                        while ((line = inputStream.readLine()) != null) {
                            if (line.length() >= 5){
                                if(line.substring(line.length() - 5,line.length()).compareTo("_end_") == 0) {
                                    //total.append(total.substring(0,total.length()-6));
                                    break;
                                }
                            }
                            total.append(line+"\n");
                        }
                        encodedImage = total.toString();
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        decodedByte = Bitmap.createBitmap(decodedByte, 0, 0, decodedByte.getWidth(), decodedByte.getHeight(), matrix, true);
                        updateConversationHandler.post(new updateUIImage(decodedByte));

                        /*File file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                        file.createNewFile();
                        try {
                            OutputStream fOut = new FileOutputStream(file);

                            decodedByte.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                            fOut.flush();
                            fOut.close(); // do not forget to close the stream

                            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        */
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        public void messageIsExec(String senderName, String[] args){

        }
        public void messageIsRead(String senderName, String[] args){
            LinkedList<String> reply = new LinkedList<>();
            String data = null;
            switch (args[3]){
                case "gps":
                    if (gpsTracker.getIsGPSTrackingEnabled()) {
                        reply.add("OK");
                        reply.add(String.valueOf(gpsTracker.longitude));
                        reply.add(String.valueOf(gpsTracker.latitude));
                        reply.add(String.valueOf(gpsTracker.altitude));
                    } else {
                        Log.d("GPS ERROR", "GPS is not enabled");
                        runOnUiThread(new makeToast("GPS ERROR: GPS is not enabled"));
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
                    if (!nameTaken){
                        updateConversationHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                createNameDialog(true);
                            }
                        }); // TODO remove
                    }
                    break;
                case "Welcome!":
                    nameTaken = true;
                    break;
                default:
                   runOnUiThread(new makeToast("Unknown message:\n" + TextUtils.join("/", args)));
                break;
            }
            if(reply.size() != 0) {
                String msg = composeMsg(senderName, reply);
                sendMsg(msg);
                if(data != null){
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
        public String composeMsg(String to, LinkedList<String> content){
            String msg = "/"; // <-- leaving field 0 empty
            // Log.d("composeMsg", "to: "+ to+" Content: "+content.toString());
            msg += to;
            if(content == null)
                return msg;
            for(String arg : content){
                msg += "/" + arg;
            }
            return msg;
        }

    }

    class updateUIThread implements Runnable {
        private String msg;
        public updateUIThread(String str) { this.msg = str; }
        @Override
        public void run() {
            text.setText(text.getText().toString() + msg + "\n");
            // code below just makes the text scroll on update/receive of messages
            final Layout layout = text.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(text.getLineCount() - 1)
                        - text.getScrollY() - text.getHeight();
                if(scrollDelta > 0)
                    text.scrollBy(0, scrollDelta);
            }
        }
    }

    class updateUIImage implements Runnable {
        private Bitmap bitmap;
        public updateUIImage(Bitmap bitmap) {this.bitmap = bitmap; }
        @Override
        public void run() {
            contactImage.setImageBitmap(bitmap);
        }
    }

    class makeToast implements Runnable{
        private String msg;
        public makeToast(String msg){ this.msg = msg; }
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to close connection to server?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        ClientActivity.super.onBackPressed();
                        // close connection here
                        try {
                            th.interrupt();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(new makeToast("ERROR:\n" + e.getMessage()));
                        }
                    }
                }).create().show();
    }

    class updateUIClientsList implements Runnable{
        Integer numOfClients;
        List<String> clientsList;
        public updateUIClientsList(Integer numOfClients, List<String> clientsList) {
            this.clientsList = clientsList;
            this.numOfClients = numOfClients;
        }

        @Override
        public void run() {
            ViewGroup linearLayout = (ViewGroup) findViewById(R.id.clientsLinearLayout);
            linearLayout.removeAllViews();
            for (String clientName : clientsList){
                if ( !clientName.equalsIgnoreCase(myName) ) {
                    Button bt = new Button(getApplicationContext());
                    bt.setText(clientName);
                    bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClickEnterText(v);
                        }
                    });
                    bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linearLayout.addView(bt);
                }
            }
        }
    }

    public void createNameDialog(Boolean alreadyTaken) {

        final EditText name = new EditText(this);
        name.setHint("Name...");

        if ( !alreadyTaken){
            new AlertDialog.Builder(this)
                    .setTitle("Please choose a username")
                    .setView(name)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            sendMsg(name.getText().toString());
                        }
                    }).create().show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Please choose another usernname")
                    .setMessage("The name you chose had already been picked")
                    .setView(name)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            sendMsg(name.getText().toString());
                        }
                    }).create().show();
        }
    }

}
