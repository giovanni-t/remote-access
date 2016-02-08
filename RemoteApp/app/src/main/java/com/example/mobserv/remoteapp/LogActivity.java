package com.example.mobserv.remoteapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LogActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        textView = (TextView)findViewById(R.id.logTextId);

        try {
            FileInputStream fin = getApplicationContext().openFileInput(MyConstants.LOG_FILENAME);
            int c;
            String temp="";
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            //string temp contains all the data of the file.
            textView.setText(temp);
            fin.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            textView.setText("Nothing to show");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
