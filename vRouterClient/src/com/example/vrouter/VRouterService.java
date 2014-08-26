package com.example.vrouter;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import com.example.vrouter.util.HelpTools;
import com.example.vrouter.util.HttpQuery;
import com.example.vrouter.util.IPPacket;
import com.example.vrouter.util.ThreadRead;

import android.os.AsyncTask;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class VRouterService extends VpnService implements Handler.Callback, Runnable {
    private static final String TAG = "VRouterService";

    private String mServerAddress = "127.0.0.1";//Local loopback server, for testing purpose only; emulating a VPN server
    private int mServerPort = 9040;
    private PendingIntent mConfigureIntent;
    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;

//    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    private DatagramChannel mTunnel = null;
    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);// I handle callback by my own handleMessage()
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }
        // Start a new session by creating a new thread.
        mThread = new Thread(this, "vRouterThread");// I myself is a runnable with a run()
        mThread.start();
        
        //httpQuery hQ = new httpQuery();
		//hQ.execute("John");
		
        //  START_STICKY is used for services that are explicitly started and stopped as needed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");

            // Set up server
            InetSocketAddress server = new InetSocketAddress(
                    mServerAddress, mServerPort);
            mHandler.sendEmptyMessage(R.string.connecting);
      
            runServer(server);
            
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
            try {
                mInterface.close();
            } catch (Exception e2) {
                // ignore
            }
            mHandler.sendEmptyMessage(R.string.disconnected);

        } finally {
           
        }
    }

    private boolean runServer(InetSocketAddress server) throws Exception {
        boolean connected = false;
        
//        android.os.Debug.waitForDebugger();
        
            // Create a DatagramChannel as the VPN tunnel.
        	mTunnel = DatagramChannel.open();
        	DatagramSocket dataSocket = mTunnel.socket();
        	if (dataSocket == null) {
        		Log.d(TAG, "ERROR! Datagram Socket is null!");
        	}
        	
            // Protect the tunnel before connecting to avoid loopback.
            if (!protect(dataSocket)) {
                throw new IllegalStateException("Cannot protect the local tunnel");
            }

            // Connect to the server.
            mTunnel.connect(server);

            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            mTunnel.configureBlocking(false);

            // Authenticate and configure the virtual network interface.
            handshake();

            // Now we are connected. Set the flag and show the message.
            connected = true;
            mHandler.sendEmptyMessage(R.string.connected);
            
            new Thread ()
            {
            	public void run ()
            	{
//            		DatagramChannel tunnel = mTunnel;

              	  	// Allocate the buffer for a single packet.
                    ByteBuffer packet = ByteBuffer.allocate(1024);
		            // Packets to be sent are queued in this input stream.
		            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
		            // Read the outgoing packet from the input stream.
		            int length;
		            
		            try
		            {
		            	HttpQuery hQ = new HttpQuery();
		        		hQ.execute("12345678901234567890");
		            	
		            	while (true)
		            	{
				            while ((length = in.read(packet.array())) > 0) {
				                // Write the outgoing packet to the tunnel.
				                packet.limit(length);
				                
				                Log.d(TAG, "Outgoing packet written to the tunnel");
				                //debugPacket(packet);
			                    
				                /*
				                1. Parse the TCP/UDP header
				                2. Create an own socket with the same src/dest port/ip
				                3. Use protect() on this socket so it is not routed over tun0
				                4. Send the packet body (excluding the header)
				                5. Obtain the response
				                6. Add the TCP header to the response and forward it
				                */
				                IPPacket pkt = new IPPacket();
				                ByteBuffer duplicateIPHeader = ByteBuffer.allocate(2048);
			                	HelpTools.debugPacket(packet, pkt, duplicateIPHeader);
			                	Log.d(TAG, "Pkt info: "+pkt.toString());
			                	
			                	if (pkt.pktType == 6) { //TCP
			                		Log.d(TAG, "got TCP packet!!!");
			                		Socket socket = SocketChannel.open().socket();
			                		if ((null != socket) && (null != this)) {
			                			protect(socket);
			                		}
			                		Log.d(TAG, "start connection.");
				                	socket.connect(new InetSocketAddress(pkt.dstIP, pkt.dstPort), 1000);
				                
			                		Log.d(TAG, "send out TCP pkt size "  + packet.remaining());
				                	DataOutputStream outToServer = new
				                		DataOutputStream(socket.getOutputStream());
				                
				                	byte[] pktBuf = new byte[packet.remaining()];
			                		Arrays.fill(pktBuf, (byte)0);
			                		packet.get(pktBuf, 0, packet.remaining());
				                	outToServer.write(pktBuf);
				                	
				                	BufferedReader inFromServer = new
				                			BufferedReader(new InputStreamReader(socket.getInputStream()));
				                	String sentenceFromServer = inFromServer.readLine();
				                	Log.d(TAG, "From Server: " + sentenceFromServer);

			                	} else if (pkt.pktType == 17) { //UDP
			                		DatagramSocket socket = DatagramChannel.open().socket();
			                		if ((null != socket) && (null != this)) {
			                			protect(socket);
			                		}
			                		
			                		//Must bind this socket to a port and our local IP;
			                		//Otherwise a random unknown port will be assigned
			                		try {
			                			socket.bind(new 
			                					InetSocketAddress(InetAddress.getByName("192.168.43.129"), 33337));
			                					//InetSocketAddress(InetAddress.getByName("192.168.2.10"), 33337));
			                		} catch (IllegalArgumentException ex) {
			                			Log.d(TAG, "bind exception. ", ex);
			                		} catch (SocketException ex) {
			                			Log.d(TAG, "bind exception. ", ex);
			                		}
			                		
			                		/*try {
			                			socket.setSoTimeout(1000);
			                			socket.connect(new InetSocketAddress(pkt.dstIP, pkt.dstPort));
			                		} catch (SocketException se) {
			                			se.printStackTrace();
			                		}*/
			                		
			                		//ByteBuffer pktBuf = packet.wrap(packet.array(), 20, pkt.pktLen-20);
			                		byte[] pktBuf = new byte[500];
			                		Arrays.fill(pktBuf, (byte)0);
			                		//Log.d(TAG, "prepare to read "+(pkt.pktLen-28) + ";" + packet.remaining());
			                		packet.get(pktBuf, 0, packet.remaining());
			                		DatagramPacket outPacket = new DatagramPacket(
			                				pktBuf,
			                				pkt.pktLen-28, 
			                				InetAddress.getByName(pkt.dstIP), pkt.dstPort);
			                		//Log.d(TAG, "TX bytes: " + bytesToHex(pktBuf));
			                		socket.send(outPacket);
			                		Log.d(TAG, "sent UDP pkt size " + outPacket.getLength());

			                		
			                		
			                		if (socket.isBound()) {
			                			Log.d(TAG, "UDP socket bound to " 
			                				+ socket.getLocalAddress().toString()+":"
			                				+ socket.getLocalPort());
			                		}
			                		Arrays.fill(pktBuf, (byte)0);
			                		DatagramPacket receivePacket =
			                				new DatagramPacket(pktBuf, pktBuf.length);
			                		try {
			                			socket.receive(receivePacket);
			                		} catch (IOException ioe) {
			                			ioe.printStackTrace();
			                			Log.d(TAG, "receive socket exception. ", ioe);
			                		}
			                		
			                		//Write IP packet to local VPN source socket
			                		//Note: cannot use UDP socket to send; need to use TUN
			                		//packet.clear();
			                		//packet.put(receivePacket.getData(), 0, receivePacket.getLength());
			                		// reverse the src and dst of this packet and return it back to 
			                		// VPN source address
			                		HelpTools.reversePacketAddr(duplicateIPHeader, pkt, receivePacket);
			                        
					                duplicateIPHeader.limit(receivePacket.getLength() + 28);
					                Log.d(TAG, "print pkt to local tunnel total size: "+ duplicateIPHeader.limit());
//					                printIPPacket(duplicateIPHeader);
			                		//tunnel.write(duplicateIPHeader);//send to VPN server
					                
					                
					                //write back to local VPN tunnel and to requester
					                Log.d(TAG, "Written to local requester");
					                duplicateIPHeader.rewind();
					                FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
						            out.write(duplicateIPHeader.array(), 0, receivePacket.getLength() + 28);
					                
						            out.close();
			                	} else {
			                		Log.d(TAG, "Wrong packet type " + pkt.pktType);
			                	}
			                	
				                packet.clear();
				
				            }
		            	}
		            }
		            catch (IOException e)
		            {
		            	e.printStackTrace();
		            }
		            
            	}
            }.start();
            
            ThreadRead threadRead = new ThreadRead(mTunnel, mInterface);
            threadRead.run();
            
        return connected;
    }

    private void handshake() throws Exception {
       
    	if (mInterface == null)
    	{
	        Builder builder = new Builder();
	        
	        builder.setMtu(1400);
	        builder.addAddress("10.0.0.2",24);
	        //builder.addAddress("192.168.2.9",32);
	        builder.addRoute("0.0.0.0",0);
	        //builder.addDnsServer("8.8.8.8");
	       // builder.addSearchDomain("wisc.edu");
	        
	        // Close the old interface since the parameters have been changed.
	        try {
	            mInterface.close();
	        } catch (Exception e) {
	            // ignore
	        }
	        
	
	        // Create a new interface using the builder and save the parameters.
	        mInterface = builder.setSession(mServerAddress)
	                .setConfigureIntent(mConfigureIntent)
	                .establish();
    	}
    }
    
}
