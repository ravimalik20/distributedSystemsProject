package edu.buffalo.cse.cse486586.groupmessenger2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ravi on 3/4/18.
 */

public class Message
{
    public static final int MULTICAST = 1;
    public static final int ORDER_FINAL = 2;

    public int id, id_parent;
    public String message;
    public int type;
    public int sender_port;

    public Message(String msg, int t, int port, int i, int i_p)
    {
        id = i;
        id_parent = i_p;
        message = msg;
        type = t;
        sender_port = port;
    }

    public static Message deserialize(String json)
    {
        try {
            JSONObject obj = new JSONObject(json);

            int i = obj.getInt("id");
            int i_p = obj.getInt("id_parent");
            String msg = obj.getString("message");
            int t = obj.getInt("type");
            int port = obj.getInt("sender_port");

            return new Message(msg, t, port, i, i_p);
        }
        catch (JSONException e) {
            return null;
        }
    }

    public String serialize()
    {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("id_parent", id_parent);
            obj.put("message", message);
            obj.put("type", type);
            obj.put("sender_port", sender_port);

            return obj.toString();
        }
        catch (JSONException e) {
            return null;
        }
    }
}

