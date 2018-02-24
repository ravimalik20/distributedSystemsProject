package edu.buffalo.cse.cse486586.groupmessenger1;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by ravi on 2/20/18.
 */

public class ClientTask extends AsyncTask<String, Integer, Void> {
    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};
    private int my_port = 0;
    private String message;

    private static final String TAG = ClientTask.class.getName();

    @Override
    protected Void doInBackground(String...messages) {
        my_port = GroupMessengerActivity.port_num;
        message = messages[0];

        Log.i("final_port", ""+my_port);

        sendMessages(message);

        return null;
    }

    private void sendMessages(String message) {
        for (int port : sibling_ports) {
            try {
                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);

                send_message(sock, message);
                Log.i(TAG, "Message sent final: " + message + ", " + port);
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Could not connect to server socket at: " + port);
            }
        }
    }

    private void send_message(Socket sock, String message) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(sock.getOutputStream());
            out.println(message);
            out.flush();
        } catch (IOException e) {
            //e.printStackTrace();
            Log.e(TAG, "Could not send message.");
        }

    }
}
