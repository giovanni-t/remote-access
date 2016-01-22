package com.example.mobserv.remoteapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TotalLogActivity extends AppCompatActivity {
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_log);
        textView = (TextView)findViewById(R.id.logTotalTextId);
        try {
            FileInputStream fin = getApplicationContext().openFileInput(MyConstants.TOTAL_LOG_FILENAME);
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
            textView.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickDeleteTotalLog(View view){
        getApplicationContext().deleteFile(MyConstants.TOTAL_LOG_FILENAME);
        onBackPressed();
    }
}
