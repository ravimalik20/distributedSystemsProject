package edu.buffalo.cse.cse486586.groupmessenger2;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

/**
 * Created by ravi on 2/20/18.
 */

public class ClientTask extends AsyncTask<String, Integer, Void> {
    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};
    private int my_port = 0;
    private String message;

    public HoldbackQueue holdback_queue;

    private Random rand;
    private Message prev_msg = null;

    private static final String TAG = ClientTask.class.getName();

    @Override
    protected Void doInBackground(String...messages) {
        rand = new Random();

        holdback_queue = GroupMessengerActivity.holdback_queue;

        my_port = GroupMessengerActivity.port_num;
        message = messages[0];

        Log.i("final_port", ""+my_port);

        sendMessages(message);

        return null;
    }

    private void sendMessages(String message) {
        int prev_id = 0;
        if (prev_msg != null)
            prev_id = prev_msg.id;

        Message msg = new Message(message, Message.MULTICAST, my_port, Math.abs(rand.nextInt()), prev_id);

        for (int port : sibling_ports) {
            try {
                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);

                send_message(sock, msg);

                Log.i(TAG, "Message sent:" + msg.serialize());
            } catch (IOException e) {
                Log.e(TAG, "Could not connect to server socket at: " + port);
            }
        }

        prev_msg = msg;
    }

    private void send_message(Socket sock, Message msg) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(sock.getOutputStream());
            out.println(msg.serialize());
            out.flush();
        } catch (IOException e) {
            //e.printStackTrace();
            Log.e(TAG, "Could not send message.");
        }

    }
}
