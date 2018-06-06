package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

import java.lang.reflect.Array;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static edu.buffalo.cse.cse486586.simpledynamo.ChordNode.genHash;

/**
 * Created by ravi on 5/7/18.
 */

public class DynamoRing
{
    private static final int[] sibling_ports = {11108, 11112, 11116, 11120, 11124};

    private HashMap<String, Integer> nodeIDPorts = new HashMap<String, Integer>();

    private ArrayList<String> ring_uid = new ArrayList<String>();
    private ArrayList<Integer> ring_ports = new ArrayList<Integer>();

    public DynamoRing()
    {
        prepareNodeIDs();

        for (int port : sibling_ports) {
            String uid_p = null;
            try {
                uid_p = genHash(Integer.toString(port / 2));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            ring_uid.add(uid_p);
        }

        Collections.sort(ring_uid);

        for (String uid : ring_uid) {
            ring_ports.add(nodeIDPorts.get(uid));
        }
    }

    public String succ_uid(String id)
    {
        int index = ring_uid.indexOf(id);

        if (index == -1) {
            return null;
        }
        else if (index == ring_uid.size()-1) {
            return ring_uid.get(0);
        }
        else {
            return ring_uid.get(index+1);
        }
    }

    public int succ_port(int port)
    {
        int index = ring_ports.indexOf(port);

        if (index == -1) {
            return -1;
        }
        else if (index == ring_ports.size()-1) {
            return ring_ports.get(0);
        }
        else {
            return ring_ports.get(index+1);
        }
    }

    private void prepareNodeIDs()
    {
        for (int port : sibling_ports ) {
            try {
                int uid = port / 2;
                String id = genHash(Integer.toString(uid));

                nodeIDPorts.put(id, port);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }
}
