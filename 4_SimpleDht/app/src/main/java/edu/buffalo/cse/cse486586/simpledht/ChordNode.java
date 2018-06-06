package edu.buffalo.cse.cse486586.simpledht;

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

/**
 * Created by ravi on 4/5/18.
 */

public class ChordNode
{
    private static final String TAG = "DEBUG_TAG";

    public static boolean setup_complete = false;

    public static int predecessor_port = 0, successor_port = 0, my_port = 0;
    public static String predecessor_id = null, successor_id = null, my_id = null;

    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};

    public static int leader_port = 11108;

    public static HashMap<String, Integer> nodeIDs;

    private Context context;

    /* Shared variable between Chord ring and content provider. */
    public static HashMap<String, String> table = new HashMap<String, String>();

    public static SharedKeyValue shared_kv;

    public static HashMap<String, String> table_local = new HashMap<String, String>();

    public static HashMap<String, String> shared_map = new HashMap<String, String>();
    public static int ring_size = 0;

    public ChordNode(Context ctx) {
        try {
            context = ctx;

            my_port = SimpleDhtActivity.myPort;
            my_id = genHash(SimpleDhtActivity.uid);

            prepareNodeIDs();

            shared_kv = new SharedKeyValue();

            //computeRingLocation();

            //Log.i(TAG, "pred: " + predecessor_id + " my_id: "+ my_id +" succ:" + successor_id);

            new ChordServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ctx.getContentResolver());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static boolean keyOnLocal(String key)
    {
        if (predecessor_id == null)
            return true;

        return key.compareTo(predecessor_id) > 0 && key.compareTo(my_id) < 0;
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
        int index = ChordServerTask.ring.indexOf(ChordNode.my_id);

        int n = ChordServerTask.ring.size();
        String sid = ChordServerTask.ring.get(0);

        for (int i=n-1 ; i >= 0 ; i--) {
            if (ChordNode.my_id.equals(ChordServerTask.ring.get(i))) {
                break;
            }
            sid = ChordServerTask.ring.get(i);
        }

        return nodeIDs.get(sid) * 2;
    }

    private void computeRingLocation()
    {
        ArrayList<String> ring = new ArrayList<String>();

        for (int port: sibling_ports) {
            try {
                String node_id = genHash(Integer.toString((port/2)));

                ring.add(node_id);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(ring);
        int i = ring.indexOf(my_id);

        if (i == 0) {
            predecessor_id = ring.get(4);
            successor_id = ring.get(i+1);
        }
        else if (i == 4) {
            predecessor_id = ring.get(i-1);
            successor_id = ring.get(0);
        }
        else {
            predecessor_id = ring.get(i-1);
            successor_id = ring.get(i+1);
        }

        predecessor_port = nodeIDs.get(predecessor_id);
        successor_port = nodeIDs.get(successor_id);
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

    private String genHash(String input) throws NoSuchAlgorithmException {
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

    private void test()
    {
        Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

        for (int port: sibling_ports) {
            if (my_port == port) {
                ContentValues cv = new ContentValues();
                cv.put("key", "ronaldo");
                cv.put("value", "ronaldo");

                context.getContentResolver().insert(mUri, cv);
            }
        }
    }
}
