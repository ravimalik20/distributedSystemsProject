package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by ravi on 4/5/18.
 */

public class ChordClientTask extends AsyncTask {
    private static int leader_port = 11108;

    @Override
    protected Object doInBackground(Object[] objects) {
        Log.i("log", "client started");

        return null;
    }
}
