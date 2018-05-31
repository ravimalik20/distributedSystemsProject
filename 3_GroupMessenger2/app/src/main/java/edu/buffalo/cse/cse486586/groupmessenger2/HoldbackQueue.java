package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.PriorityQueue;

/**
 * Created by ravi on 3/4/18.
 */

public class HoldbackQueue
{
    public ArrayList<Message> queue = null;

    public HoldbackQueue ()
    {
        queue = new ArrayList<Message>();
    }

    public synchronized void add(Message msg)
    {
        for (int i=0 ; i < queue.size() ; i++) {
            if (queue.get(i).id == msg.id_parent) {
                queue.add(i+1, msg);

                return;
            }
        }

        queue.add(msg);
    }

    public Message get(int id)
    {
        Message m;

        for (int i=0 ; i < queue.size() ; i++) {
            m = queue.get(i);

            if (m.id == id) {
                return m;
            }
        }

        return null;
    }

    public String getFinalOrderJson()
    {
        JSONArray res = new JSONArray();
        JSONObject obj;

        for (Message m: queue) {
            obj = new JSONObject();

            try {
                obj.put("id", m.id);

                res.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return res.toString();
    }

    public static ArrayList<Integer> parseFinalOrder(String str)
    {
        ArrayList<Integer> order = new ArrayList<Integer>();

        try {
            JSONArray arr = new JSONArray(str);

            for (int i=0 ; i < arr.length() ; i++) {
                JSONObject obj = arr.getJSONObject(i);
                order.add(obj.getInt("id"));
            }

            return order;
        } catch (JSONException e) {
            e.printStackTrace();

            return null;
        }
    }
}
