package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ravi on 4/5/18.
 */

public class ChordNode
{
    private static final String TAG = "DEBUG_TAG";

    public static int predecessor_port = 0, successor_port = 0, my_port = 0;
    public static String predecessor_id = null, successor_id = null, my_id = null;

    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};

    public static HashMap<String, Integer> nodeIDs;

    private Context context;

    /* Shared variable between Chord ring and content provider. */
    public static HashMap<String, String> table = new HashMap<String, String>();
    public static SharedKeyValue shared_kv;
    public static SharedKeyValue writeInProgress = new SharedKeyValue();

    public static ConcurrentHashMap<String, String> table_local = new ConcurrentHashMap<String, String>();

    public static HashMap<String, String> shared_map = new HashMap<String, String>();
    public static int ring_size = 0;

    public ChordNode(Context ctx) {
        try {
            context = ctx;

            my_port = SimpleDynamoActivity.myPort;
            my_id = genHash(SimpleDynamoActivity.uid);

            Log.i("self_info", my_id + "_" + my_port);

            prepareNodeIDs();

            shared_kv = new SharedKeyValue();
            writeInProgress.key = "writeInProgress";
            writeInProgress.value = "false";

            new ChordServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ctx.getContentResolver());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static int key_owner_port(String key)
    {
        for (String node_id: ChordServerTask.ring) {
            if (node_id.compareTo(key) > 0)
                return nodeIDs.get(node_id)*2;
        }

        if (ChordServerTask.ring.size() == 0)
            return ChordNode.my_port;
        else
            return nodeIDs.get(ChordServerTask.ring.get(0)) * 2;
    }

    public static int get_successor_port()
    {
        return get_successor_port_id(ChordNode.my_id);
    }

    public static int get_successor_port_id(String id)
    {
        String oid = null;
        try {
            oid = genHash(Integer.toString(key_owner_port(id)/2));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String sid = null;

        int i = ChordServerTask.ring.indexOf(oid);

        if (i == ChordServerTask.ring.size()-1)
            sid = ChordServerTask.ring.get(0);
        else
            sid = ChordServerTask.ring.get(i+1);

        return nodeIDs.get(sid) * 2;
    }

    private void prepareNodeIDs()
    {
        nodeIDs = new HashMap<String, Integer>();

        for (int port : sibling_ports ) {
            try {
                int uid = port / 2;
                String id = genHash(Integer.toString(uid));

                nodeIDs.put(id, uid);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public static String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
