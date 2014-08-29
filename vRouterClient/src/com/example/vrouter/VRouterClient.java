package com.example.vrouter;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class VRouterClient extends Activity implements View.OnClickListener {
	private static final String TAG = "VRouterService";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vrouter_client);


        findViewById(R.id.connect).setOnClickListener(this);
        //TextView debugTxt = (TextView) findViewById(R.id.textView1);
    }

    @Override
    public void onClick(View v) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
        	Log.d(TAG, "Preparing intent!");
            startActivityForResult(intent, 0);
        } else {
        	Log.d(TAG, "Intent already prepared!");
            onActivityResult(0, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Intent intent = new Intent(this, VRouterService.class);
            startService(intent);
        }
    }
    
}
