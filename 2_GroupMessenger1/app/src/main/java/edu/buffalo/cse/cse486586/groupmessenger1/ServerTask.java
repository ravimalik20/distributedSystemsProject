package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ravi on 2/20/18.
 */

class ServerTask extends AsyncTask<ContentResolver, String, Void> {

    private static final String TAG = ServerTask.class.getName();

    private ServerSocket socket_server = null;
    private int server_port = 0;

    private ContentResolver cr;

    @Override
    protected Void doInBackground(ContentResolver... resolvers) {

        cr = resolvers[0];

        server_port = 10000;
        socket_server = getServerSocket(server_port);

        while (true) {
            try {
                Log.i(TAG, "Server listening for connections.");
                Socket conn = socket_server.accept();

                Log.i(TAG, "request accepted.");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String message = new String();

                while ((message = in.readLine()) != null) {
                    if (message.isEmpty())
                        continue;

                    String[] data = {message};
                    this.publishProgress(data);

                    break;
                }
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Could not establish connection.");
                break;
            }
        }

        return null;
    }

    protected void onProgressUpdate(String...strings) {
        String message = strings[0];

        Log.e(TAG, "Message received: " + message);

        saveMessage(message);
    }

    private synchronized void saveMessage(String message) {

        int max_key = -1;
        int col_index;
        Uri mUri = null;
        Cursor resultCursor = null;

        try {
            mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            resultCursor = cr.query(mUri, null, "max", null, null);

            int keyIndex = resultCursor.getColumnIndex("key");
            int valueIndex = resultCursor.getColumnIndex("value");

            resultCursor.moveToFirst();

            String returnKey = resultCursor.getString(keyIndex);
            String returnValue = resultCursor.getString(valueIndex);

            resultCursor.close();

            max_key = Integer.parseInt(returnValue);

            Log.i(TAG, "Last sequence num:"+max_key);
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            Log.i(TAG, "First time running. No messages in DB.");
            max_key = -1;
        }

        int sequence_num = max_key + 1;

        ContentValues keyval = new ContentValues();
        keyval.put("key", Integer.toString(sequence_num));
        keyval.put("value", message);

        Uri newUri = cr.insert(mUri, keyval);

        keyval = new ContentValues();
        keyval.put("key", "max");
        keyval.put("value", Integer.toString(sequence_num));
        newUri = cr.insert(mUri, keyval);

        Log.i("Value saved", "Message :"+message+" Sequence Num: "+(sequence_num));
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private ServerSocket getServerSocket(int myPort) {
        try {
            ServerSocket socket = new ServerSocket(myPort);

            return socket;
        } catch (IOException e) {
            //e.printStackTrace();
            Log.e(TAG, "Cannot create server soket ");
        }

        return null;
    }

    private class SaveIntent extends IntentService {

        private String message;

        /**
         * Creates an IntentService.  Invoked by your subclass's constructor.
         *
         * @param name Used to name the worker thread, important only for debugging.
         */
        public SaveIntent(String name, String msg) {
            super(name);

            message = msg;
        }

        @Override
        protected void onHandleIntent(Intent intent) {

        }
    }
}


