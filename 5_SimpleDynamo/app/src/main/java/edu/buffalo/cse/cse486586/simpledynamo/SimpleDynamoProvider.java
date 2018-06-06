package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class SimpleDynamoProvider extends ContentProvider {

	private static final String TAG = "DEBUG_TAG";

	public static DynamoRing dring = new DynamoRing();

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub

		try {

			synchronized (ChordNode.writeInProgress) {
				while (ChordNode.writeInProgress.value == "true") {
					ChordNode.writeInProgress.wait();
				}
			}

			String key = genHash(selection);

			int op = ChordNode.key_owner_port(key);
			if (op != ChordNode.my_port) {
				// Send query
				Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), op);
				send_message(sock, new Message(selection, Message.DELETE, ChordNode.my_port, ChordNode.my_id));

				return 0;
			}

			if (selection.equals("*")) {
				for (String node: ChordServerTask.ring) {
					int port = ChordNode.nodeIDs.get(node) * 2;

					Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
					send_message(sock, new Message("@", Message.DELETE, ChordNode.my_port, ChordNode.my_id));
				}

			}
			else if (selection.equals("@")) {
				Iterator i = ChordNode.table_local.entrySet().iterator();

				while (i.hasNext()) {
					Map.Entry key_val = (Map.Entry) i.next();
					String k = key_val.getKey().toString();

					getContext().deleteFile(k);
				}

				ChordNode.table_local.clear();

				return 0;
			}

			if (ChordNode.table.containsKey(selection))
				ChordNode.table.remove(selection);

			if (ChordNode.table_local.containsKey(selection))
				ChordNode.table_local.remove(selection);

			replicate_delete(selection);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, final ContentValues values) {
		// TODO Auto-generated method stub


		synchronized (ChordNode.writeInProgress) {
			try {

				while (ChordNode.writeInProgress.value == "true")
					ChordNode.writeInProgress.wait();

				ChordNode.writeInProgress.value = "true";

				String key = (String) values.get("key");
				String value = (String) values.get("value");

				String key_prev = key;
				key = genHash(key);

				ChordNode.table.put(key_prev, value);

				int op = ChordNode.key_owner_port(key);
				if (op != ChordNode.my_port) {
					// Send insert query to owner and initiate replicate
					Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), op);
					send_message(sock, new Message(key_prev + "_" + value, Message.INSERT, ChordNode.my_port, ChordNode.my_id));
				} else {
					ChordNode.table_local.put(key_prev, value);

					Log.i(TAG, "Insert. Key: " + key_prev + " Value: " + value);
				}

				replicate_insert(key_prev, value);

				for (int count=0 ; count < 2 ; count++)
					ChordNode.writeInProgress.wait();

				ChordNode.writeInProgress.value = "false";

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return uri;
	}

	private void replicate_insert(String key, String value)
	{
		try {
			int op = ChordNode.key_owner_port(genHash(key));

			/*Log.i("replicating_key", key);
			Log.i("replicating_key", ""+op);*/

			for (int count = 0 ; count < 2 ; count++) {
				int port_succ = dring.succ_port(op);

				//Log.i("replicating_key", ""+port_succ);

				Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port_succ);
				send_message(sock, new Message(key + "_" + value, Message.REPLICATE_INSERT, ChordNode.my_port, ChordNode.my_id));

				op = port_succ;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private void replicate_delete(String key)
	{
		try {
			int op = ChordNode.key_owner_port(genHash(key));

			/*Log.i("replicating_key", key);
			Log.i("replicating_key", ""+op);*/

			for (int count = 0 ; count < 2 ; count++) {
				int port_succ = dring.succ_port(op);

				//Log.i("replicating_key", ""+port_succ);

				Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port_succ);
				send_message(sock, new Message(key, Message.REPLICATE_DELETE, ChordNode.my_port, ChordNode.my_id));

				op = port_succ;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, final String selection, String[] selectionArgs,
						String sortOrder) {
		// TODO Auto-generated method stub

		synchronized (ChordNode.writeInProgress) {
			while (ChordNode.writeInProgress.value == "true") {
				try {
					ChordNode.writeInProgress.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		String key = null;
		try {
			key = genHash(selection);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}


		if (selection.equals("*")) {
			ChordNode.ring_size = ChordServerTask.ring.size() - 1;

			synchronized (ChordNode.shared_map) {
				ChordNode.shared_map.clear();

				Iterator i = ChordNode.table_local.entrySet().iterator();

				while (i.hasNext()) {
					Map.Entry key_val = (Map.Entry) i.next();

					ChordNode.shared_map.put(key_val.getKey().toString(), key_val.getValue().toString());
				}
			}

			boolean waitingForReponse = false;

			if (ChordServerTask.ring.size() > 1) {
				waitingForReponse = true;

				int port = ChordNode.get_successor_port();

				JSONObject obj = new JSONObject();

				JSONArray keys = new JSONArray();
				JSONArray values = new JSONArray();

				try {
					obj.put("keys", keys);
					obj.put("values", values);
				} catch (JSONException e) {
					e.printStackTrace();
				}

				String payload = obj.toString();
				Socket sock = null;

				try {
					obj.put("keys", keys);
					obj.put("values", values);

					sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);

					send_message(sock, new Message(payload, Message.GET_ALL, ChordNode.my_port, ChordNode.my_id));

					//Log.i("get_all_sent", "sent");
				} catch (IOException e) {
					e.printStackTrace();
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
			}

			if (waitingForReponse) {
				synchronized (ChordNode.shared_map) {
					try {
						ChordNode.shared_map.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			String[] header = new String[] { "key", "value" };

			MatrixCursor cursor = new MatrixCursor(header);

			Iterator i = ChordNode.shared_map.entrySet().iterator();

			while (i.hasNext()) {
				Map.Entry key_val = (Map.Entry) i.next();

				cursor.addRow(new Object[] { key_val.getKey(), key_val.getValue() });
			}

			return cursor;
		}
		else if (selection.equals("@")) {
			String[] header = new String[] { "key", "value" };
			MatrixCursor cursor = new MatrixCursor(header);

			Iterator i = ChordNode.table_local.entrySet().iterator();

			while (i.hasNext()) {
				Map.Entry key_val = (Map.Entry) i.next();

				cursor.addRow(new Object[] { key_val.getKey(), key_val.getValue() });
			}

			return cursor;
		}

		Boolean waitingForResponse = false;

		int op = ChordNode.key_owner_port(key);

		Log.i("query", key+ "_" + selection + "_" + op + "_" + ChordNode.my_port);

		if (op != ChordNode.my_port) {
            /* Send request */
			Socket sock = null;
			try {
				sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), op);
			} catch (IOException e) {
				e.printStackTrace();
			}
			send_message(sock, new Message(selection, Message.QUERY_VAL, ChordNode.my_port, ChordNode.my_id));

			Log.i("query_sent", selection);

			waitingForResponse = true;
			synchronized (ChordNode.shared_kv) {
				try {
					ChordNode.shared_kv.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			if (waitingForResponse)
				while (ChordNode.shared_kv.key.compareTo(selection) != 0)
					ChordNode.shared_kv.wait();

            /* Prepare cursor */
			String[] header = new String[] { "key", "value" };
			MatrixCursor cursor = new MatrixCursor(header);

			if (waitingForResponse)
				cursor.addRow(new Object[] { selection, ChordNode.shared_kv.value });
			else {
                /* Read value corresponding to key */
				String val = ChordNode.table_local.get(selection);

				cursor.addRow(new Object[] { selection, val });
			}

			return cursor;
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
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
