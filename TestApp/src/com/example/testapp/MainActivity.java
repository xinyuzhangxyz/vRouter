package com.example.testapp;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {

	private Button buttonPktType,buttonPktAmount,buttonSend;
	private TextView textView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);

		buttonSend = (Button)findViewById(R.id.button1);
		buttonPktType = (Button)findViewById(R.id.button2);
		buttonPktAmount = (Button)findViewById(R.id.button3);
		textView = (TextView)findViewById(R.id.textView3);
		buttonSend.setOnClickListener(new ButtonListener());
		buttonPktType.setOnClickListener(new ButtonListener());
		buttonPktAmount.setOnClickListener(new ButtonListener());
		
	} 
	
	private class ButtonListener implements OnClickListener{
		
		public void onClick(View view)
		{
			switch(view.getId()){
			case R.id.button1:{
				textView.setText(R.string.server_status_send);
			}break; 
			case R.id.button2:{
				textView.setText(R.string.server_status_pkt_type_set);
			}break;
			case R.id.button3:{
				textView.setText(R.string.server_status_pkt_amount_set);
			}break;
			default:break;
			}		
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}
} 
