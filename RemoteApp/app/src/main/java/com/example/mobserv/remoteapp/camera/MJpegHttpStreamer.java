/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mobserv.remoteapp.camera;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* package */ public final class MJpegHttpStreamer
{
    private static final String TAG = MJpegHttpStreamer.class.getSimpleName();

    private static final String BOUNDARY = "--gc0p4Jq0M2Yt08jU534c0p--";
    private static final String BOUNDARY_LINES = "\r\n" + BOUNDARY + "\r\n";

    private static final String HTTP_HEADER =
        "HTTP/1.0 200 OK\r\n"
        + "Server: Peepers\r\n"
        + "Connection: close\r\n"
        + "Max-Age: 0\r\n"
        + "Expires: 0\r\n"
        + "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, "
            + "post-check=0, max-age=0\r\n"
        + "Pragma: no-cache\r\n"
        + "Access-Control-Allow-Origin:*\r\n"
        + "Content-Type: multipart/x-mixed-replace; "
            + "boundary=" + BOUNDARY + "\r\n"
        + BOUNDARY_LINES;

    private final int mPort;

    private boolean mNewJpeg = false;
    private boolean mStreamingBufferA = true;
    private final byte[] mBufferA;
    private final byte[] mBufferB;
    private int mLengthA = Integer.MIN_VALUE;
    private int mLengthB = Integer.MIN_VALUE;
    private long mTimestampA = Long.MIN_VALUE;
    private long mTimestampB = Long.MIN_VALUE;
    private final Object mBufferLock = new Object();

    private Thread mWorker = null;
    private volatile boolean mRunning = false;

    /* package */ MJpegHttpStreamer(final int port, final int bufferSize)
    {
        super();
        mPort = port;
        mBufferA = new byte[bufferSize];
        mBufferB = new byte[bufferSize];
    } // constructor(int, int)

    /* package */ void start()
    {
        if (mRunning)
        {
            throw new IllegalStateException("MJpegHttpStreamer is already running");
        } // if

        mRunning = true;
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
        mWorker = new Thread(new Runnable()
        {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                Socket clientSocket = null;
                try {
                    serverSocket = new ServerSocket(mPort);
                    serverSocket.setSoTimeout(2000 /* milliseconds */);
                    System.out.println("Waiting for clients to connect...");
                    while (true) {
                        try {
                            clientSocket = serverSocket.accept();
                            clientProcessingPool.submit(new ClientTask(clientSocket));
                        } // try
                        catch (final SocketTimeoutException e) {
                            if (!mRunning) {
                                return;
                            } // if
                        } // catch
                    }
                } catch (IOException e) {
                    System.err.println("Unable to process client request");
                    e.printStackTrace();
                } finally {
                    if (serverSocket != null) {
                        try {
                            serverSocket.close();
                            Log.d(TAG, "serverSocked is closed");
                        } // try
                        catch (final IOException closingServerSocket) {
                            System.err.println(closingServerSocket);
                        } // catch
                    } // if
                } // run()
            }
        });
        mWorker.start();
    } // start()

    /* package */ void stop()
    {
        if (!mRunning)
        {
            throw new IllegalStateException("MJpegHttpStreamer is already stopped");
        } // if

        mRunning = false;
        mWorker.interrupt();
    } // stop()

    /* package */ void streamJpeg(final byte[] jpeg, final int length, final long timestamp)
    {
        synchronized (mBufferLock)
        {
            final byte[] buffer;
            if (mStreamingBufferA)
            {
                buffer = mBufferB;
                mLengthB = length;
                mTimestampB = timestamp;
            } // if
            else
            {
                buffer = mBufferA;
                mLengthA = length;
                mTimestampA = timestamp;
            } // else
            System.arraycopy(jpeg, 0 /* srcPos */, buffer, 0 /* dstPos */, length);
            mNewJpeg = true;
            mBufferLock.notify();
        } // synchronized
    } // streamJpeg(byte[], int, long)

/*
    public void startServer() throws IOException{
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                Socket clientSocket = null;
                try {
                    serverSocket = new ServerSocket(mPort);
                    //serverSocket.setSoTimeout(1000);
                    System.out.println("Waiting for clients to connect...");
                    do
                    {
                        try
                        {
                            clientSocket = serverSocket.accept();
                            clientProcessingPool.submit(new ClientTask(clientSocket));
                        } // try
                        catch (final SocketTimeoutException e)
                        {
                            if (!mRunning)
                            {
                                return;
                            } // if
                        } // catch
                    } while (clientSocket == null);
                } catch (IOException e) {
                    System.err.println("Unable to process client request");
                    e.printStackTrace();
                } finally {
                    /*
                    if (serverSocket != null)
                    {
                        try
                        {
                            serverSocket.close();
                        } // try
                        catch (final IOException closingServerSocket)
                        {
                            System.err.println(closingServerSocket);
                        } // catch
                    } // if
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }*/

    private class ClientTask implements Runnable {
        private final Socket socket;

        private ClientTask(Socket clientSocket) {
            socket = clientSocket;
        }

        @Override
        public void run() {
            DataOutputStream stream = null;
            try {
                stream = new DataOutputStream(socket.getOutputStream());
                stream.writeBytes(HTTP_HEADER);
                stream.flush();

                while (mRunning) {
                    final byte[] buffer;
                    final int length;
                    final long timestamp;

                    synchronized (mBufferLock) {
                        while (!mNewJpeg) {
                            try {
                                mBufferLock.wait();
                            } // try
                            catch (final InterruptedException stopMayHaveBeenCalled) {
                                // stop() may have been called
                                return;
                            } // catch
                        } // while

                        mStreamingBufferA = !mStreamingBufferA;

                        if (mStreamingBufferA) {
                            buffer = mBufferA;
                            length = mLengthA;
                            timestamp = mTimestampA;
                        } // if
                        else {
                            buffer = mBufferB;
                            length = mLengthB;
                            timestamp = mTimestampB;
                        } // else

                        mNewJpeg = false;
                    } // synchronized

                    stream.writeBytes(
                            "Content-type: image/jpeg\r\n"
                                    + "Content-Length: " + length + "\r\n"
                                    + "X-Timestamp:" + timestamp + "\r\n"
                                    + "\r\n"
                    );
                    stream.write(buffer, 0 /* offset */, length);
                    stream.writeBytes(BOUNDARY_LINES);
                    stream.flush();
                } // while
            } // try
            catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        Log.d("MjpegStreaming", "Streaming closed");
                        stream.close();
                    } // try
                    catch (final IOException closingStream) {
                        System.err.println(closingStream);
                    } // catch
                } //
                if (socket != null) {
                    try {
                        Log.d("MjpegStreaming", "Socket closed");
                        socket.close();
                    } // try
                    catch (final IOException closingSocket) {
                        System.err.println(closingSocket);
                    } // catch
                } // socket

            }
        }
    }
}