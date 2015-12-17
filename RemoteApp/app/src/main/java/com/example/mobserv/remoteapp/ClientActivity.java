package com.example.mobserv.remoteapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.example.mobserv.remoteapp.fragments.OneFragment;
import com.example.mobserv.remoteapp.fragments.PagerAdapter;
import com.example.mobserv.remoteapp.fragments.ThreeFragment;
import com.example.mobserv.remoteapp.fragments.TwoFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pacel_000 on 22/10/2015.
 */
public class ClientActivity extends AppCompatActivity implements TaskFragment.TaskCallbacks{
    /*private SmartFragmentStatePagerAdapter adapterViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        ViewPager vpPager = (ViewPager) findViewById(R.id.vpPager);
        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);
        vpPager.setOffscreenPageLimit(3);
    }

    // Extend from SmartFragmentStatePagerAdapter now instead for more dynamic ViewPager items
    public static class MyPagerAdapter extends SmartFragmentStatePagerAdapter {
        private static int NUM_ITEMS = 3;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return TwoFragment.newInstance(0, "Page # 1");
                case 1: // Fragment # 0 - This will show FirstFragment different title
                    return TwoFragment.newInstance(1, "Page # 2");
                case 2: // Fragment # 1 - This will show SecondFragment
                    return TwoFragment.newInstance(2, "Page # 3");
                default:
                    return null;
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }
    }*/
    private List<String> clientsList;
    private static final String TAG = TaskFragment.class.getSimpleName();
    private static final boolean DEBUG = true; // Set this to false to disable logs .

    private static final int serverport = 45678;
    private static final String CLIENTS_LIST = "clientsList";

    private static final int HOME_POSITION = 1;
    private static final int LIVE_POSITION = 2;
    private static final int PHOTO_POSITION = 3;

    private String serverip = "another dummy IP";

    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private TaskFragment mTaskFragment;

    private OneFragment oneFragment;
    private TwoFragment twoFragment;
    private ThreeFragment threeFragment;

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private PagerAdapter adapter;

    private boolean isStreaming = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);

        adapter = new PagerAdapter(getSupportFragmentManager());
        oneFragment = new OneFragment();
        twoFragment = new TwoFragment();
        threeFragment = new ThreeFragment();
        adapter.addFragment(oneFragment, "Home", 0);
        adapter.addFragment(twoFragment, "Live", 0);
        adapter.addFragment(threeFragment, "Photo", 0);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(3);
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position != LIVE_POSITION-1)
                    twoFragment.stopPlayback();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    class notifyTabStripChanged implements Runnable{
        int position, notificationsCount;
        public notifyTabStripChanged(int position, int notificationsCount) {
            this.position = position;
            this.notificationsCount = notificationsCount;
        }
        @Override
        public void run() {
            LinearLayout tabHost = (LinearLayout) tabs.getChildAt(0);
            RelativeLayout tabLayout = (RelativeLayout) tabHost.getChildAt(position);
            TextView badge = (TextView) tabLayout.findViewById(R.id.badge);
            if (notificationsCount > 0) {
                badge.setText(String.valueOf(notificationsCount));
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }

    public void startConnection(){
        FragmentManager fm = getSupportFragmentManager();
        mTaskFragment = (TaskFragment) fm.findFragmentByTag("task_fragment");

        Intent it = getIntent();
        serverip = it.getStringExtra("serverip");
        if (mTaskFragment== null) {
            Bundle bd = new Bundle();
            bd.putString("serverip", serverip);
            mTaskFragment = new TaskFragment();
            mTaskFragment.setArguments(bd);
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //*if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(clientsList != null)
            outState.putStringArrayList(CLIENTS_LIST, new ArrayList<String>(clientsList));
        oneFragment.saveInstance(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        setClientsList(savedInstanceState.getStringArrayList(CLIENTS_LIST));
        List<String> cl = getClientsList();
        if (cl != null)
            oneFragment.updateUIClientsListButtons(cl.size(), cl);
        if (savedInstanceState != null) {
            oneFragment.scrollText(savedInstanceState);
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
                        // close connection here --> kill the fragment
                        onFragmentCancel();
                    }
                }).create().show();
    }

    public void onClick(View view) {
        String str = oneFragment.getEt().getText().toString();
        mTaskFragment.sendMsg(str);
        oneFragment.getEt().setText(null);
    }

    class updateUIThread implements Runnable {
        private String msg;
        public updateUIThread(String str) { this.msg = str; }
        @Override
        public void run() {
            oneFragment.gettext().setText(oneFragment.gettext().getText().toString() + msg + "\n");
            // code below just makes the text scroll on update/receive of messages
            final Layout layout = oneFragment.gettext().getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(oneFragment.gettext().getLineCount() - 1)
                        - oneFragment.gettext().getScrollY() - oneFragment.gettext().getHeight();
                if(scrollDelta > 0)
                    oneFragment.gettext().scrollBy(0, scrollDelta);
            }
        }
    }

    class updateUIImage implements Runnable {
        private Bitmap bitmap;
        public updateUIImage(Bitmap bitmap) {this.bitmap = bitmap; }
        @Override
        public void run() {
            threeFragment.getContactImage().setImageBitmap(bitmap);
            runOnUiThread(new notifyTabStripChanged(2, 1));
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

    public void setClientsList(List<String> clientsList){ this.clientsList = clientsList; }
    @Override
    public void onShowToast(String str){
        runOnUiThread(new makeToast(str));
    }

    @Override
    public void onChooseName(Boolean taken) {
        oneFragment.createNameDialog(taken);
        //runOnUiThread(new createNameDialog(taken));
    }

    @Override
    public void onConnected() {
        runOnUiThread(new makeToast("Connected to " + serverip + " " + serverport));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                oneFragment.getEt().setFocusableInTouchMode(true);
            }
        });
        oneFragment.setNameTaken(false);
    }

    @Override
    public void onFragmentCancel() {
        mTaskFragment.closeSocket();
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().remove(mTaskFragment).commit();
        finish();
    }

    @Override
    public void onTextReceived(String str) {
        runOnUiThread(new updateUIThread(str));
    }

    @Override
    public void onImageReceived(Bitmap decodedByte) {
        runOnUiThread(new updateUIImage(decodedByte));
    }

    @Override
    public String onImageRequested() {
        String result = null;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            onShowToast("No camera on this device");
        } else {
            try {
                oneFragment.getPreview().setCamera();
                oneFragment.getPreview().openSurface();
                result = oneFragment.getPreview().takePicture();
            } catch (Exception e) {
                Log.d("ERROR", "Failed to config the camera: " + e.getMessage());
            } finally {
                oneFragment.getPreview().closeSurface();
            }
        }
        return result;
    }

    @Override
    public String onLiveRequested() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            new makeToast("No camera on this device");
        } else{
            if(isStreaming) new makeToast("This device is streaming");
            isStreaming = true;
            oneFragment.getPreview().liveSetId();
            oneFragment.getPreview().openSurface();
            oneFragment.getPreview().onResume();
            return oneFragment.getPreview().getIpServer() + ":" + String.valueOf(oneFragment.getPreview().getPortServer());
        }
        return null;
    }

    @Override
    public void onClientListReceived(int numOfClients, List<String> clients) {
        setClientsList(clients);
        oneFragment.updateUIClientsListButtons(numOfClients, clientsList);
        //runOnUiThread(new OneFragment.updateUIClientsList(numOfClients, clientsList));
    }

    public void onIpListReceived(int numOfIps, List<String> ips){
        Log.d(TAG, "number of ips " + numOfIps);
        runOnUiThread(new notifyTabStripChanged(1, numOfIps));
        twoFragment.updateUIIPsListButtons(numOfIps, ips);
    }

    @Override
    public void onWelcome() {
        oneFragment.setNameTaken(true);
    }

    /**
     * Takes the name of the button and concatenates it to
     * the current composing message
     * @param view (the button)
     */
    public void onClickEnterText(View view) {
        oneFragment.onClickEnterText(view);
    }

    /************************/
    /*** GETTER & SETTER ****/
    /************************/
    public List<String> getClientsList() {
        return clientsList;
    }
    public void setmTaskFragment(TaskFragment mTaskFragment) {
        this.mTaskFragment = mTaskFragment;
    }

    public void setServerip(String serverip) {
        this.serverip = serverip;
    }

    public String getServerip() {
        return serverip;
    }

    public TaskFragment getmTaskFragment() {
        return mTaskFragment;
    }
    /***** LOGS & STUFF *****/
    /************************/

    @Override
    public void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
        if(oneFragment.getPreview() != null)
            oneFragment.getPreview().onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        if(oneFragment.getPreview() != null)
            oneFragment.getPreview().releaseMWakeLock();
        super.onPause();
        if(oneFragment.getPreview() != null)
            oneFragment.getPreview().onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        if(oneFragment.getPreview() != null)
            oneFragment.getPreview().releaseMWakeLock();
        super.onStop();
        if(oneFragment.getPreview() != null)
            oneFragment.getPreview().onPause();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        if (oneFragment.getPreview() != null)
            oneFragment.getPreview().releaseMWakeLock();
        super.onDestroy();
        if (oneFragment.getPreview() != null)
            oneFragment.getPreview().onPause();
    }

}
