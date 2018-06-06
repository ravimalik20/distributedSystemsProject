package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDynamoActivity extends Activity {

	public static int myPort = 0;
	public static String uid = null;

	public static String files_dir;

	private int getPortNum() {
		TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		final int myPort = Integer.parseInt(portStr) * 2;

		return myPort;
	}

	private String getUID() {
		TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

		return portStr;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		files_dir = "" + getFilesDir();

		myPort = getPortNum();
		uid = getUID();

		ChordNode node = new ChordNode(getApplicationContext());

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);

		TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_dynamo, menu);
		return true;
	}

	public void onStop()
	{
        super.onStop();
	    Log.v("Test", "onStop()");
	}

}
