package com.example.mobserv.remoteapp.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobserv.remoteapp.CameraPreview;
import com.example.mobserv.remoteapp.ClientActivity;
import com.example.mobserv.remoteapp.R;

import java.util.ArrayList;
import java.util.List;

public class OneFragment extends Fragment{
    private TextView text;
    private EditText et;
    private ClientActivity act;

    private SurfaceView mSurfaceView;
    private CameraPreview preview;

    private ViewGroup clientsLinearLayout;

    private static final String TEXT_SCROLL_X = "tScrollX";
    private static final String TEXT_SCROLL_Y = "tScrollY";
    private boolean nameTaken;
/*
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }
*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_one, container, false);
        if(savedInstanceState == null) {
            act = (ClientActivity)getActivity();
            text = (TextView) view.findViewById(R.id.idClientText);
            text.setMovementMethod(new ScrollingMovementMethod());
            et = (EditText) view.findViewById(R.id.idClientEditText);
            mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
            clientsLinearLayout = (LinearLayout) view.findViewById(R.id.clientsLinearLayout);

            preview = new CameraPreview(getActivity(), mSurfaceView);
            preview.setKeepScreenOn(true);
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mSurfaceView.setX(metrics.widthPixels + 1);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState == null) { // IF first launch of the activity
            et.setFocusable(false);
        }
        act.startConnection();
    }

    public void saveInstance(Bundle outState) {
        outState.putBoolean("nameTaken", nameTaken);
        outState.putInt(TEXT_SCROLL_X, text.getScrollX());
        outState.putInt(TEXT_SCROLL_Y, text.getScrollY());
        if(act.getClientsList()!= null) {
            outState.putStringArrayList("clientsList", new ArrayList<String>(act.getClientsList()));
        }
    }

    public void scrollText(Bundle savedInstanceState) {
        final int x = savedInstanceState.getInt(TEXT_SCROLL_X);
        final int y = savedInstanceState.getInt(TEXT_SCROLL_Y);
        text.post(new Runnable() {
            @Override
            public void run() {
                text.scrollTo(x, y);
            }
        });
        nameTaken = savedInstanceState.getBoolean("nameTaken");
        if (!nameTaken)
            act.runOnUiThread(new createNameDialog(false));
    }

    public void updateUIClientsListButtons(int numOfClients, List<String> clients) {
        act.runOnUiThread(new updateUIClientsList(numOfClients, clients));
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
            clientsLinearLayout.removeAllViews();
            for (String clientName : clientsList){
                // let's keep also own name so we can send msgs to ourselves for debugging purposes
                //if ( !clientName.equalsIgnoreCase(myName) ) {
                Button bt = new Button(getContext());
                bt.setText(clientName);
                bt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClickEnterText(v);
                    }
                });
                bt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                clientsLinearLayout.addView(bt);
                //}
            }
            act.setClientsList(clientsList);
        }
    }
    public void createNameDialog(Boolean taken){
        act.runOnUiThread(new createNameDialog(taken));
    }

    class createNameDialog implements Runnable {
        Boolean alreadyTaken;
        ClientActivity activity;
        public createNameDialog(Boolean alreadyTaken) {
            this.alreadyTaken = alreadyTaken;
            this.activity = act;
        }

        @Override
        public void run() {
            final EditText name = new EditText(activity);
            name.setHint("Name...");
            if (!alreadyTaken) {
                new AlertDialog.Builder(activity)
                        .setTitle("Please choose a username")
                        .setView(name)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                act.getmTaskFragment().sendMsg(name.getText().toString());
                            }
                        }).create().show();
            } else {
                new AlertDialog.Builder(activity)
                        .setTitle("Please choose another username")
                        .setMessage("The name you chose had already been picked")
                        .setView(name)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                act.getmTaskFragment().sendMsg(name.getText().toString());
                            }
                        }).create().show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i("DEBUG", "view destroyed");
    }

    /**
     * Takes the name of the button and concatenates it to
     * the current composing message
     * @param view (the button)
     */
    public void onClickEnterText(View view) {
        String tmp = et.getText().toString();
        tmp += "/" + ((Button) view).getText().toString();
        et.setText(tmp);
        et.setSelection(et.getText().toString().length());
    }

    /*************************
     **** Getter & Setter ****
     *************************/
    public EditText getEt(){return et;}
    public TextView gettext(){return text;}
    public CameraPreview getPreview(){return preview;}
    public void setNameTaken(boolean nameTaken) { this.nameTaken = nameTaken; }
}