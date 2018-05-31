package edu.buffalo.cse.cse486586.groupmessenger2;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Created by ravi on 2/20/18.
 */

class ServerTask extends AsyncTask<ContentResolver, String, Void> {

    private static final String TAG = ServerTask.class.getName();
    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};

    private Random rand;

    private ServerSocket socket_server = null;
    private int server_port = 0;

    public HoldbackQueue order_list;
    public ArrayList<Message> holdback_queue;

    private ContentResolver cr;

    @Override
    protected Void doInBackground(ContentResolver... resolvers) {

        rand = new Random();

        cr = resolvers[0];

        server_port = 10000;
        socket_server = getServerSocket(server_port);

        holdback_queue = new ArrayList<Message>();
        order_list = new HoldbackQueue();

        while (true) {
            try {
                Log.i(TAG, "Server listening for connections.");
                Socket conn = socket_server.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String message = new String();

                while ((message = in.readLine()) != null) {
                    if (message.isEmpty())
                        continue;

                    Message msgObj = Message.deserialize(message);

                    Log.i("message received", message);

                    if (msgObj.type == Message.MULTICAST) {
                        holdback_queue.add(msgObj);

                        if (GroupMessengerActivity.port_num == 11116) {
                            order_list.add(msgObj);

                            if (order_list.queue.size() >= 25) {
                                deliverFinalOrder();
                            }
                        }
                    }
                    else if (msgObj.type == Message.ORDER_FINAL) {
                        Log.i("order_received", msgObj.message);

                        ArrayList<Integer> order = HoldbackQueue.parseFinalOrder(msgObj.message);

                        saveMessages(order);
                    }



                    break;
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not establish connection.");
                break;
            }
        }


        return null;
    }

    private void deliverFinalOrder()
    {
        String order = order_list.getFinalOrderJson();
        Message msg = new Message(order, Message.ORDER_FINAL, GroupMessengerActivity.port_num, 0, 0);

        for (int port: sibling_ports) {
            try {
                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);

                PrintWriter out = new PrintWriter(sock.getOutputStream());
                out.println(msg.serialize());
                out.flush();
            }
            catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Could not connect to server socket at: " + port);
            }
        }
    }

    private void saveMessages(ArrayList<Integer> order)
    {
        for (int i=0 ; i < order.size() ; i++) {
            int id = order.get(i);

            for (int j=0 ; j < holdback_queue.size() ; j++) {
                Message m = holdback_queue.get(j);

                if (m.id == id) {
                    saveMessage(m.message, i);

                    Log.i("message_saved", m.serialize());

                    break;
                }
            }
        }
    }

    private synchronized void saveMessage(String message, int sequence_num)
    {
        Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

        ContentValues keyval = new ContentValues();
        keyval.put("key", Integer.toString(sequence_num));
        keyval.put("value", message);

        Uri newUri = cr.insert(mUri, keyval);
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
}


