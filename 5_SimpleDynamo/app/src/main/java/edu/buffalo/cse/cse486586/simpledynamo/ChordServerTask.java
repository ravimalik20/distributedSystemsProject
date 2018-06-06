package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ravi on 4/5/18.
 */

public class ChordServerTask extends AsyncTask
{
    private static final String TAG = "DEBUG_TAG";

    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};

    private ContentResolver cr ;
    private Uri mUri;

    public static ArrayList<String> ring = new ArrayList<String>();

    private void executeRequest(Socket conn) throws JSONException, IOException, NoSuchAlgorithmException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String message;

        while ((message = in.readLine()) != null) {
            if (message.isEmpty())
                continue;

            Message msgObj = Message.deserialize(message);

            if (msgObj.type == Message.INSERT) {
                final Message  m = msgObj;

                String[] kv = m.msg.split("_", -1);
                String k = kv[0];
                String v = kv[1];

                ChordNode.table.put(k, v);

                ChordNode.table_local.put(k, v);

                Log.i(TAG, "Insert. Key: " + k + " Value: " + v);
            }
            else if (msgObj.type == Message.QUERY_RESPONSE) {
                Log.i("query_response_received", msgObj.serialize());

                synchronized (ChordNode.shared_kv) {
                    String []kv = msgObj.msg.split("_", -1);

                    ChordNode.shared_kv.key = kv[0];
                    ChordNode.shared_kv.value = kv[1];

                    ChordNode.shared_kv.notifyAll();
                }
            }
            else if (msgObj.type == Message.QUERY_VAL) {
                String selection = msgObj.msg;
                String val = null;

                val = ChordNode.table_local.get(selection);

                Log.i("query_received", msgObj.serialize());

                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), msgObj.port);
                send_message(sock, new Message(msgObj.msg+"_"+val, Message.QUERY_RESPONSE, ChordNode.my_port, ChordNode.my_id));

                Log.i("query_received_sent", selection);
            }
            else if (msgObj.type == Message.GET_ALL) {
                Log.i("get_all", msgObj.serialize());

                JSONObject obj = new JSONObject(msgObj.msg);

                JSONArray keys = obj.getJSONArray("keys");
                JSONArray values = obj.getJSONArray("values");

                Iterator i = ChordNode.table_local.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry key_val = (Map.Entry) i.next();

                    keys.put(key_val.getKey().toString());
                    values.put(key_val.getValue().toString());
                }

                obj.put("keys", keys);
                obj.put("values", values);

                String payload = obj.toString();

                if (ChordNode.get_successor_port() == msgObj.port) {
                            /* Last node in the line of request. Send final message to originating node. */
                    Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), msgObj.port);
                    send_message(sock, new Message(payload, Message.GET_ALL_RESPONSE, ChordNode.my_port, ChordNode.my_id));
                }
                else {
                    Log.i("here", "here1");
                    Log.i("ports", ChordNode.my_port + " " + ChordNode.get_successor_port());

                    Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), ChordNode.get_successor_port());
                    send_message(sock, new Message(payload, Message.GET_ALL, msgObj.port, msgObj.uid));
                }
            }
            else if (msgObj.type == Message.GET_ALL_RESPONSE) {
                Log.i("get_all_response", msgObj.serialize());

                JSONObject obj = new JSONObject(msgObj.msg);

                JSONArray keys = obj.getJSONArray("keys");
                JSONArray values = obj.getJSONArray("values");

                synchronized (ChordNode.shared_map) {
                    for (int i = 0; i < keys.length(); i++) {
                        String key = (String) keys.get(i);
                        String value = (String) values.get(i);

                        ChordNode.shared_map.put(key, value);
                    }

                    ChordNode.shared_map.notify();
                }
            }
            else if (msgObj.type == Message.DELETE) {
                cr.delete(mUri, msgObj.msg, null);
            }
            else if (msgObj.type == Message.REPLICATE_INSERT) {
                Log.i("replicate_insert", msgObj.serialize());

                String []k_v = msgObj.msg.split("_", -1);

                ChordNode.table_local.put(k_v[0], k_v[1]);

                Log.i("replicate_insert", "success");

                Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), msgObj.port);
                send_message(sock, new Message(k_v[0], Message.REPLICATE_INSERT_SUCCESS, ChordNode.my_port, ChordNode.my_id));
            }
            else if (msgObj.type == Message.REPLICATE_INSERT_SUCCESS) {
                Log.i("r_ins_success", "received: " + msgObj.serialize());

                synchronized (ChordNode.writeInProgress) {
                    ChordNode.writeInProgress.notifyAll();
                }
            }
            else if (msgObj.type == Message.REPLICATE_DELETE) {
                if (ChordNode.table_local.containsKey(msgObj.msg)) {
                    ChordNode.table_local.remove(msgObj.msg);
                }
            }

            break;
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        Log.i(TAG, "server started");

        cr = (ContentResolver) objects[0];
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");

        join_ring();

        try {
            ServerSocket socket_server = getServerSocket(10000);

            while (true) {
                final Socket conn = socket_server.accept();

                try {
                    executeRequest(conn);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void join_ring()
    {
        for (int port: sibling_ports) {
            String uid_p = null;
            try {
                uid_p = genHash(Integer.toString(port/2));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            ChordServerTask.ring.add(uid_p);
            Log.i("ring", uid_p);
        }

        Collections.sort(ChordServerTask.ring);
    }

    private void send_message(Socket sock, Message msg)
    {
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

    private ServerSocket getServerSocket(int myPort) {
        try {
            ServerSocket socket = new ServerSocket(myPort);

            return socket;
        } catch (IOException e) {
            e.printStackTrace();

        }

        return null;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
