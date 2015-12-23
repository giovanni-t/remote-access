package com.example.mobserv.remoteapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.mobserv.remoteapp.camera.CameraStreamer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview Camera";
    private static final int NO_MODE = 0;
    private static final int CAMERA_MODE = 1;
    private static final int LIVE_MODE = 2;
    private int mode = NO_MODE;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Context context;

    //PHOTO
    private Size mPreviewSize;
    private List<Size> mSupportedPreviewSizes;
    private Camera mCamera;
    private Semaphore semaphore;

    //LIVE
    private static final String PREF_CAMERA = "camera";
    private static final int PREF_CAMERA_INDEX_DEF = 0;
    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_SIZE = "size";
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 30;
    // preview sizes will always have at least one element, so this is safe
    private static final int PREF_PREVIEW_SIZE_INDEX_DEF = 7;

    private SharedPreferences mPrefs = null;
    private String mIpAddress = "";
    private int mCameraIndex = PREF_CAMERA_INDEX_DEF;
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private int mPort = PREF_PORT_DEF;
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private int mPrevieSizeIndex = PREF_PREVIEW_SIZE_INDEX_DEF;

    private PowerManager.WakeLock mWakeLock = null;
    private boolean mPreviewDisplayCreated = false;
    private boolean mRunning = false;
    private CameraStreamer mCameraStreamer = null;

    public CameraPreview(Context context, SurfaceView sv) {
        super(context);
        this.context = context;
        mSurfaceView = sv;
        semaphore = new Semaphore(0);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mode = NO_MODE;
    }

    public void setCamera() {
        int cameraId = 0;
        mode = CAMERA_MODE;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
            }
        }
        mCamera = Camera.open(cameraId);
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();

            // get Camera parameters
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureFormat(ImageFormat.JPEG);
            List<Size> sizes = params.getSupportedPictureSizes();
            //the camera size is set to the lowest possible size
            Size size = sizes.get(sizes.size() - 1);
            params.setPictureSize(size.width, size.height);
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
            }
            List<String> flashModes = params.getSupportedFlashModes();
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)){
                params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            }
            mCamera.setParameters(params);
        }
    }

    public void openSurface(){
        surfaceCreated(mHolder);
    }

    public void liveSetId(){
        mode = LIVE_MODE;

        //addPreferencesFromResource(R.xml.preferences);
        new LoadPreferencesTask().execute();

        mIpAddress = tryGetIpV4Address();
        boolean isAvailable = false;
        while(!isAvailable) {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(mIpAddress, mPort));
                mPort++;
            }
            catch(ConnectException ce){
                isAvailable = true;
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        updatePrefCacheAndUi();
        final PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            switch (mode){
                case CAMERA_MODE:
                    if (mCamera != null) {
                        mCamera.setPreviewDisplay(holder);
                        mCamera.startPreview();
                    }
                    break;
                case LIVE_MODE:
                    mPreviewDisplayCreated = true;
                    tryStartCameraStreamer();
                    break;
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    private void tryStartCameraStreamer()
    {
        if (mRunning && mPreviewDisplayCreated && mPrefs != null)
        {
            mCameraStreamer = new CameraStreamer(mCameraIndex, mUseFlashLight, mPort,
                    mPrevieSizeIndex, mJpegQuality, mHolder);
            mCameraStreamer.start();
        } // if
    }

    public void closeSurface(){
        surfaceDestroyed(mHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        switch (mode){
            case CAMERA_MODE:
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null); // 7/12 Giovanni: added this line. This link could be useful: https://github.com/SanaMobile/android_plugin_camera/blob/master/Camera/src/com/sana/android/plugin/camera/CameraPreview.java
                    mCamera.release();
                    mCamera = null;
                }
                break;
            case LIVE_MODE:
                mPreviewDisplayCreated = false;
                ensureCameraStreamerStopped();
                break;
        }
    }

    public void onResume(){
        switch(mode){
            case LIVE_MODE:
                mRunning = true;
                if (mPrefs != null)
                {
                    mPrefs.registerOnSharedPreferenceChangeListener(
                            mSharedPreferenceListener);
                } // if
                updatePrefCacheAndUi();
                tryStartCameraStreamer();
                mWakeLock.acquire();
                break;
        }
    }

    public void onPause(){
        switch(mode){
            case LIVE_MODE:
                mRunning = false;
                if (mPrefs != null)
                {
                    mPrefs.unregisterOnSharedPreferenceChangeListener(
                            mSharedPreferenceListener);
                } // if
                ensureCameraStreamerStopped();
                break;
        }
    }

    private void ensureCameraStreamerStopped()
    {
        if (mCameraStreamer != null)
        {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    }

    public String takePicture(){
        final String[] encodedImage = new String[1];
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Toast.makeText(context, "Focus success: " + String.valueOf(success), Toast.LENGTH_LONG).show();
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        encodedImage[0] = Base64.encodeToString(data, Base64.DEFAULT);
                        semaphore.release();
                    }
                });
            }
        });
        semaphore.acquireUninterruptibly();
        return encodedImage[0];
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(mode == CAMERA_MODE) {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                requestLayout();

                mCamera.setParameters(parameters);
                mCamera.startPreview();
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    private final int getPrefInt(final String key, final int defValue)
    {
        // We can't just call getInt because the preference activity
        // saves everything as a string.
        try
        {
            return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
        } // try
        catch (final NullPointerException e)
        {
            return defValue;
        } // catch
        catch (final NumberFormatException e)
        {
            return defValue;
        } // catch
    }

    private final void updatePrefCacheAndUi()
    {
        mCameraIndex = getPrefInt(PREF_CAMERA, PREF_CAMERA_INDEX_DEF);
        if (hasFlashLight())
        {
            if (mPrefs != null)
            {
                mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT,
                        PREF_FLASH_LIGHT_DEF);
            } // if
            else
            {
                mUseFlashLight = PREF_FLASH_LIGHT_DEF;
            } // else
        } //if
        else
        {
            mUseFlashLight = false;
        } // else

        // XXX: This validation should really be in the preferences activity.
        mPort = getPrefInt(PREF_PORT, PREF_PORT_DEF);
        // The port must be in the range [1024 65535]
        if (mPort < 1024)
        {
            mPort = 1024;
        } // if
        else if (mPort > 65535)
        {
            mPort = 65535;
        } // else if

        mPrevieSizeIndex = getPrefInt(PREF_JPEG_SIZE, PREF_PREVIEW_SIZE_INDEX_DEF);
        mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);
        // The JPEG quality must be in the range [0 100]
        if (mJpegQuality < 0)
        {
            mJpegQuality = 0;
        } // if
        else if (mJpegQuality > 100)
        {
            mJpegQuality = 100;
        } // else if
        Log.i("Camera Preview","http://" + mIpAddress + ":" + mPort + "/");
    }

    private boolean hasFlashLight()
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private static String tryGetIpV4Address()
    {
        try
        {
            final Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr =
                        intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements())
                {
                    final  InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        final String addr = inetAddress.getHostAddress().toUpperCase();
                        try {
                            if (Inet4Address.getByName(addr) != null)
                            {
                                return addr;
                            }
                        } catch (UnknownHostException ex) {
                            Log.i("TAG", String.valueOf(ex.getStackTrace()));
                        }
                    } // if
                } // while
            } // for
        } // try
        catch (final Exception e)
        {
            Log.i("TAG", String.valueOf(e.getStackTrace()));
            // Ignore
        } // catch
        return null;
    }

    public void releaseMWakeLock(){if(mRunning == true) mWakeLock.release();}
    public String getIpServer(){ return mIpAddress; }
    public int getPortServer(){ return mPort; }
    private final class LoadPreferencesTask extends AsyncTask<Void, Void, SharedPreferences>
    {
        private LoadPreferencesTask()
        {
            super();
        } // constructor()

        @Override
        protected SharedPreferences doInBackground(final Void... noParams)
        {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Override
        protected void onPostExecute(final SharedPreferences prefs)
        {
            mPrefs = prefs;
            prefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
            updatePrefCacheAndUi();
            tryStartCameraStreamer();
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceListener =
            new SharedPreferences.OnSharedPreferenceChangeListener()
            {
                @Override
                public void onSharedPreferenceChanged(final SharedPreferences prefs,
                                                      final String key)
                {
                    updatePrefCacheAndUi();
                } // onSharedPreferenceChanged(SharedPreferences, String)

            }; // mSharedPreferencesListener

}
