package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * OnPTestClickListener demonstrates how to access a ContentProvider. First, please read 
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start. Please note that our use of a ContentProvider is a bit different from the
 * standard way of using it as described in the PA2 spec. The bottom line is that our
 * ContentProvider does not have full support for SQL. It is just a key-value table, like a hash
 * table. It just needs to be able to insert (key, value) pairs, store them, and return them when
 * queried.
 * 
 * A ContentProvider has a unique URI that other apps use to access it. ContentResolver is
 * the class to use when accessing a ContentProvider.
 * 
 * @author stevko
 *
 */
public class OnSendClickListener implements OnClickListener {

    private static final String TAG = OnSendClickListener.class.getName();

    private final EditText mTextView;
    private final ContentResolver mContentResolver;

    private String message;
    private int my_port;

    public OnSendClickListener(EditText _tv, ContentResolver _cr) {
        mTextView = _tv;
        mContentResolver = _cr;
    }

    @Override
    public void onClick(View v) {
        message = mTextView.getText().toString();

        mTextView.setText("");

        Log.i(TAG, "Send request initiated.");
        Log.i(TAG, "Message: " + message);

        new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
    }


}
