package com.example.mobserv.remoteapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.mobserv.remoteapp.camera.MjpegInputStream;
import com.example.mobserv.remoteapp.camera.MjpegView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

public class LiveActivity extends AppCompatActivity {
    private final String TAG = "Live Activity";
    private MjpegView mv;
    private ViewGroup IpLinearLayout;

    private ClientActivity act;
    private List<String> IpListArray;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        IpListArray = getIntent().getStringArrayListExtra("ipList");
        mv = (MjpegView) findViewById(R.id.videoView1);
        IpLinearLayout = (LinearLayout) findViewById(R.id.IpLinearLayout);
        IpLinearLayout.removeAllViews();
        for (String ip : IpListArray){
            // let's keep also own name so we can send msgs to ourselves for debugging purposes
            //if ( !clientName.equalsIgnoreCase(myName) ) {
            Button bt = new Button(getApplicationContext());
            bt.setText(ip);
            bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickEnterText(v);
                }
            });
            bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            IpLinearLayout.addView(bt);
            //}
        }
    }
    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
        /*
        HttpResponse res = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpParams httpParams = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, 5*1000);
        if(DEBUG) Log.d(TAG, "1. Sending http request");
        try {
            res = httpclient.execute(new HttpGet(URI.create(url[0])));
            if(DEBUG) Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
            if(res.getStatusLine().getStatusCode()==401){
                //You must turn off camera User Access Control before this will work
                return null;
            }
            return new MjpegInputStream(res.getEntity().getContent());
        } catch (ClientProtocolException e) {
            if(DEBUG){
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
            }
            //Error connecting to camera
        } catch (IOException e) {
            if(DEBUG){
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException", e);
            }
            //Error connecting to camera
        }
        return null;
        */
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url[0]).openConnection();
                connection.setConnectTimeout(5 * 1000);
                connection.setReadTimeout(5 * 1000);
                Log.i(TAG, "1. Sending http request");
                connection.setRequestMethod("GET");
                connection.connect();
                Log.i(TAG, "2. Request finished, status = " + connection.getResponseCode());
                if(connection.getResponseCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(connection.getInputStream());
            } catch(SocketTimeoutException e){
                e.printStackTrace();
                Log.i(TAG, "Timeout HttpURLConnection", e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            } /*finally {
                try {
                    Log.d(TAG, "Http request is disconnecting...");
                    if(connection != null) connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace(); //If you want further info on failure...
                }
            }*/
            return null;

        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.initialize();
            //mv.surfaceCreated(null);
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
            Log.i(TAG, "Start playback");
        }
        protected void onCloseConnection(){
            mv.stopPlayback();
        }
    }
    /**
     * Takes the name of the button and concatenates it to
     * the current composing message
     * @param view (the button)
     */
    public void onClickEnterText(View view) {
        String url = "http://" + ((Button) view).getText().toString();
        stopPlayback();
        new DoRead().execute(url);
    }

    public void stopPlayback() {
        Log.i(TAG,"Stop playback");
        new DoRead().onCloseConnection();
    }
}
