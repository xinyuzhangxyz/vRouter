package com.example.vrouter.util;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.AsyncTask;

/**
 * 
 * Send HTTP request to a server and get response
 * 
 *
 */

public class HttpQuery extends AsyncTask<String, Void, String> {
	String reply = null;
	@Override
	protected String doInBackground(String... strs) {
		// Sample HTTP POST request code:
		String temp1="";
		HttpClient httpclient = new DefaultHttpClient();

			HttpPost getVal = new HttpPost("http://testdbserver.appspot.com/getvalue");
		
			// ArrayList<NameValuePair> is used to send values from android app to server.
	        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	        
	        // "tag" is the name of the text form on the webserver
	        // "mytagInput" is the value that the client is submitting to the server
	        nameValuePairs.add(new BasicNameValuePair("tag", strs[0]));
	        
	      
			 try {
				 UrlEncodedFormEntity httpEntity = new UrlEncodedFormEntity(nameValuePairs);
				 getVal.setEntity(httpEntity); 
				 
				 HttpResponse response = httpclient.execute(getVal);
				 temp1 = EntityUtils.toString(response.getEntity());	
				} 
				  catch (ClientProtocolException e) {			  
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("HTTP IO Exception");
					e.printStackTrace();
				}
				 

	            // Decode the JSON array. Array is zero based so the return value is in element 2
				try {
					JSONArray jsonArray = new JSONArray(temp1);
					reply = jsonArray.getString(2);
					return reply;
				} catch (JSONException e) {
					System.out.println("Error in JSON decoding");
					e.printStackTrace();
				}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(String res) {
		//((TextView)findViewById(R.id.outVal)).setText("Temperature: "+res);
	}
}
