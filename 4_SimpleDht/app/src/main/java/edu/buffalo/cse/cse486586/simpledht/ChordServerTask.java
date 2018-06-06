package edu.buffalo.cse.cse486586.simpledht;

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

    private static int leader_port = sibling_ports[0];

    private ContentResolver cr ;

    public static ArrayList<String> ring = new ArrayList<String>();

    @Override
    protected Object doInBackground(Object[] objects) {
        Log.i(TAG, "server started");

        cr = (ContentResolver) objects[0];
        Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

        join_ring();

        try {
            ServerSocket socket_server = getServerSocket(10000);

            while (true) {
                Socket conn = socket_server.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String message;

                while ((message = in.readLine()) != null) {
                    if (message.isEmpty())
                        continue;

                    Message msgObj = Message.deserialize(message);

                    if (msgObj.type == Message.QUERY) {
                        String []msgArr = msgObj.msg.split("_", -1);
                        String key = msgArr[1];
                        String value = msgArr[2];
                        String operation = msgArr[0];

                        if (! ChordNode.keyOnLocal(key)) {
                            lookup(msgObj);

                            break;
                        }

                        /* Query message reached the destined node */

                        if (operation.equals("insert")) {
                            ContentValues cv = new ContentValues();
                            cv.put("key", key);
                            cv.put("value", value);

                            cr.insert(mUri, cv);
                        }
                        else if (operation.equals("query")) {
                            /* Fetch  from content provider and send message */
                            String val = new String();
                            FileInputStream inp = new FileInputStream(new File(SimpleDhtActivity.files_dir + "/" + key));
                            int c;

                            while ((c=inp.read()) != -1) {
                                val += (char)c;
                            }

                            inp.close();

                            Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), msgObj.port);

                            send_message(sock, new Message(key+"_"+val, Message.QUERY_RESPONSE, ChordNode.my_port, ChordNode.my_id));

                        }

                    }
                    else if (msgObj.type == Message.QUERY_RESPONSE) {
                        synchronized (ChordNode.shared_kv) {
                            String []kv = msgObj.msg.split("_", -1);

                            ChordNode.shared_kv.key = kv[0];
                            ChordNode.shared_kv.value = kv[1];

                            ChordNode.shared_kv.notify();
                        }
                    }
                    else if (msgObj.type == Message.JOIN) {
                        Log.i("join_request", msgObj.serialize());

                        String key = msgObj.uid;
                        ring.add(key);
                        Collections.sort(ring);

                        String ring_enc = ring_encode();

                        for (String node: ring) {
                            int port = ChordNode.nodeIDs.get(node) * 2;
                            if (port == ChordNode.my_port)
                                continue;

                            Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
                            send_message(sock, new Message(ring_enc, Message.UPDATE_RING, ChordNode.my_port, ChordNode.my_id));
                        }

                        String p_id, s_id;
                        key=ChordNode.my_id;

                        int i = ring.indexOf(key);

                        if (i == 0) {
                            p_id = ring.get(ring.size()-1);
                            s_id = ring.get(i+1);
                        }
                        else if (i == ring.size()-1) {
                            p_id = ring.get(i-1);
                            s_id = ring.get(0);
                        }
                        else {
                            p_id = ring.get(i-1);
                            s_id = ring.get(i+1);
                        }

                        int succ_port = ChordNode.nodeIDs.get(s_id) * 2;
                        int pred_port = ChordNode.nodeIDs.get(p_id) * 2;

                        ChordNode.predecessor_id = p_id;
                        ChordNode.successor_id = s_id;
                        ChordNode.predecessor_port = pred_port;
                        ChordNode.successor_port = succ_port;
                    }
                    else if (msgObj.type == Message.SET_PRED_SUCC) {
                        String []kv = msgObj.msg.split("_", -1);
                        String succ = kv[1], pred = kv[0];

                        ChordNode.successor_port = Integer.parseInt(succ);
                        ChordNode.successor_id = genHash(Integer.toString(ChordNode.successor_port / 2));

                        ChordNode.predecessor_port = Integer.parseInt(pred);
                        ChordNode.predecessor_id = genHash(Integer.toString(ChordNode.predecessor_port / 2));

                        Log.i("set_pred_succ", "pred: " + ChordNode.predecessor_id + " my_id: "+ ChordNode.my_id +" succ:" + ChordNode.successor_id);

                    }
                    else if (msgObj.type == Message.SET_SUCC) {
                        Log.i("set_succ", msgObj.serialize());

                        ChordNode.successor_port = Integer.parseInt(msgObj.msg);
                        ChordNode.successor_id = genHash(Integer.toString(ChordNode.successor_port / 2));
                    }
                    else if (msgObj.type == Message.UPDATE_RING) {
                        Log.i("update_ring", msgObj.serialize());

                        String []ring_dec = msgObj.msg.split("_", -1);

                        for (String node_id: ring_dec) {
                            ring.add(node_id);
                        }

                        Collections.sort(ring);

                        String p_id, s_id, key=ChordNode.my_id;

                        int i = ring.indexOf(key);

                        if (i == 0) {
                            p_id = ring.get(ring.size()-1);
                            s_id = ring.get(i+1);
                        }
                        else if (i == ring.size()-1) {
                            p_id = ring.get(i-1);
                            s_id = ring.get(0);
                        }
                        else {
                            p_id = ring.get(i-1);
                            s_id = ring.get(i+1);
                        }

                        int succ_port = ChordNode.nodeIDs.get(s_id) * 2;
                        int pred_port = ChordNode.nodeIDs.get(p_id) * 2;

                        ChordNode.predecessor_id = p_id;
                        ChordNode.successor_id = s_id;
                        ChordNode.predecessor_port = pred_port;
                        ChordNode.successor_port = succ_port;

                        Log.i("ring_order", "pred: " + ChordNode.predecessor_id + " my_id: "+ ChordNode.my_id +" succ:" + ChordNode.successor_id);
                    }
                    else if (msgObj.type == Message.INSERT) {
                        String []kv = msgObj.msg.split("_", -1);
                        String k = kv[0];
                        String v = kv[1];

                        ContentValues cv = new ContentValues();
                        cv.put("key", k);
                        cv.put("value", v);

                        cr.insert(mUri, cv);
                    }
                    else if (msgObj.type == Message.QUERY_VAL) {
                        String val = ChordNode.table_local.get(msgObj.msg);

                        Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), msgObj.port);
                        send_message(sock, new Message(msgObj.msg+"_"+val, Message.QUERY_RESPONSE, ChordNode.my_port, ChordNode.my_id));
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

                    break;

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String ring_encode()
    {
        String enc = new String();

        int i=0, l = ring.size();

        for (String node: ring) {
            enc += node;

            if (i < l-1)
                enc += "_";

            i++;
        }

        return enc;
    }

    private void join_ring()
    {
        if (ChordNode.my_port == ChordNode.leader_port) {
            ring.add(ChordNode.my_id);

            return;
        }


        Socket sock = null;
        try {
            sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), ChordNode.leader_port);

            send_message(sock, new Message("join", Message.JOIN, ChordNode.my_port, ChordNode.my_id));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void lookup(Message msg)
    {
        try {
            Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), ChordNode.successor_port);

            send_message(sock, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
