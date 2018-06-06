package edu.buffalo.cse.cse486586.simpledynamo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ravi on 4/5/18.
 */

public class Message
{
    public static final int JOIN = 1;
    public static final int LOC_UPDATE = 2;
    public static final int QUERY = 3;
    public static final int QUERY_RESPONSE = 4;
    public static final int SET_SUCC = 5;
    public static final int SET_PRED_SUCC = 6;
    public static final int UPDATE_RING = 7;
    public static final int INSERT = 8;
    public static final int QUERY_VAL = 9;
    public static final int GET_ALL = 10;
    public static final int GET_ALL_RESPONSE = 11;
    public static final int DELETE = 12;
    public static final int REPLICATE_INSERT = 13;
    public static final int REPLICATE_DELETE = 14;
    public static final int REPLICATE_INSERT_SUCCESS = 15;

    public String msg;
    public int type;
    public int port;
    public String uid;

    public Message (String m, int t, int p, String u)
    {
        msg = m;
        type = t;
        port = p;
        uid = u;
    }

    public static Message deserialize(String json)
    {
        try {
            JSONObject obj = new JSONObject(json);

            String m = obj.getString("message");
            int t = obj.getInt("type");
            int p = obj.getInt("port");
            String u = obj.getString("uid");

            return new Message(m, t, p, u);
        }
        catch (JSONException e) {
            return null;
        }
    }

    public String serialize()
    {
        try {
            JSONObject obj = new JSONObject();
            obj.put("message", msg);
            obj.put("type", type);
            obj.put("port", port);
            obj.put("uid", uid);

            return obj.toString();
        }
        catch (JSONException e) {
            return null;
        }
    }
}
