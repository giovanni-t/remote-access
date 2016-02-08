package com.example.mobserv.remoteapp;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            textView.setText("Nothing to show");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickDeleteTotalLog(View view){
        getApplicationContext().deleteFile(MyConstants.TOTAL_LOG_FILENAME);
        onBackPressed();
    }

    public void onClickExport(View view){
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        String path = file.getAbsolutePath();
        path+="/";
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String filename = "DroneControllerLog_"+timeStamp;
        // copy all the content of the log file
        FileInputStream fin = null;
        try {
            fin = getApplicationContext().openFileInput(MyConstants.TOTAL_LOG_FILENAME);
            int c;
            String temp="";
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            //string temp contains all the data of the file.
            fin.close();
            filename+=".txt";
            path+=filename;
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fop = new FileOutputStream(path);
            fop.write(temp.getBytes());
            fop.flush();
            fop.close();
            Toast.makeText(getApplicationContext(), "Exported to : "+path, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Nothing to export yet", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
